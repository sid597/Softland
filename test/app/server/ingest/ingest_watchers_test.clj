(ns app.server.ingest.ingest-watchers-test
  "Gate 14 (view-MVP WP-B2): the near-live watcher loop, JVM integration.

   Boots a real object-container test-cluster runtime, watches a temp root,
   and proves the ingest seam fires on a settled .md change WITHOUT polling as
   proof: the watcher exposes an :on-import completion callback and the test
   latches on it via a bounded LinkedBlockingQueue.poll (an event-driven
   blocking wait with a deadline, never a sleep-and-check loop -- trap 11)."
  (:require [app.server.ingest.ingest-watchers :as watchers]
            [app.server.rama.object-container :as oc]
            [app.server.rama.object-container.runtime :as ocr]
            [app.server.rama.ingest-epoch :as ingest-epoch]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [com.rpl.rama :refer [foreign-select]]
            [com.rpl.rama.path :refer [keypath MAP-VALS]])
  (:import [java.io File]
           [java.nio.file Files LinkOption]
           [java.nio.file.attribute PosixFilePermission]
           [java.util.concurrent LinkedBlockingQueue TimeUnit]))

(def ^:private latch-timeout-ms 15000)

(defn- temp-dir!
  ^File []
  (let [d (Files/createTempDirectory "ingest-watchers-test" (make-array java.nio.file.attribute.FileAttribute 0))]
    (.toFile d)))

(defn- drain!
  "Empty any events currently sitting in the queue (non-blocking; returns
   immediately when empty). Used before a write so the following bounded poll
   observes exactly that write's completion."
  [^LinkedBlockingQueue q]
  (loop [] (when (some? (.poll q)) (recur))))

(defn- await-import!
  "Bounded blocking wait for the next import-completion event. Not a poll loop:
   a single LinkedBlockingQueue.poll that blocks on the queue's condition up to
   the deadline."
  [^LinkedBlockingQueue q]
  (.poll q latch-timeout-ms TimeUnit/MILLISECONDS))

(defn- source-version-count
  [runtime source-ref]
  (count (foreign-select [(keypath (oc/source-ref-key source-ref)) MAP-VALS]
                         (:source-versions-by-ref runtime))))

(deftest default-classifier-has-no-face-edn-channel
  (let [classify (ns-resolve 'app.server.ingest.ingest-watchers 'classify)]
    (is (= :md (classify (.toPath (io/file "note.md")))))
    (is (= :jsonl (classify (.toPath (io/file "session.jsonl")))))
    (is (nil? (classify (.toPath (io/file "face.edn")))))
    (is (nil? (classify (.toPath (io/file "deps.edn")))))))

(deftest watcher-loop-md-import-and-idempotent-reimport-test
  (let [runtime (ocr/start-object-container-runtime!)
        root (temp-dir!)
        q (LinkedBlockingQueue.)
        handle (watchers/start-ingest-watchers!
                {:runtime runtime
                 :roots [(.getPath root)]
                 :debounce-ms 500
                 :on-import (fn [ev] (.put q ev))})]
    (try
      (let [md-file (io/file root "note.md")
            content "# Title\nfirst body line\nsecond body line\n"
            source-ref (.getPath md-file)]

        (testing "a settled .md write fires the existing import seam; epoch +1"
          (let [epoch-before @ingest-epoch/!ingest-epoch-atom]
            (drain! q)
            (spit md-file content)
            (let [ev (await-import! q)]
              (is (some? ev) "import completion latched (no polling-as-proof)")
              (is (= :md (:kind ev)))
              (is (= :accepted (:status ev)) "the OC decision was accepted")
              (is (= (inc epoch-before) (:epoch ev)) "epoch bumped by exactly 1")
              (is (= (inc epoch-before) @ingest-epoch/!ingest-epoch-atom))
              (is (= 1 (source-version-count runtime source-ref))
                  "exactly one source version after first import"))))

        (testing "byte-identical rewrite converges: no new source version, epoch bumps again"
          (let [epoch-before @ingest-epoch/!ingest-epoch-atom]
            (drain! q)
            (spit md-file content)
            (let [ev (await-import! q)]
              (is (some? ev) "re-import latched")
              (is (= :accepted (:status ev)) "convergent re-import replays the accepted decision")
              (is (= (inc epoch-before) (:epoch ev)) "epoch bumped again (the import ran)")
              (is (= 1 (source-version-count runtime source-ref))
                  "convergent re-import did NOT add a source version (idempotency journal)")))))
      (finally
        ((:stop! handle))
        (ocr/close-object-container-runtime! runtime)
        (io/delete-file (io/file root "note.md") true)
        (io/delete-file root true)))))

(deftest watcher-survives-import-failure-test
  (let [runtime (ocr/start-object-container-runtime!)
        root (temp-dir!)
        q (LinkedBlockingQueue.)
        handle (watchers/start-ingest-watchers!
                {:runtime runtime
                 :roots [(.getPath root)]
                 :debounce-ms 500
                 :on-import (fn [ev] (.put q ev))})]
    (try
      (testing "a file whose import throws does not kill the loop"
        (let [bad-file (io/file root "unreadable.md")]
          (drain! q)
          (spit bad-file "# Doomed\nthis slurp will fail\n")
          ;; Make the file unreadable so slurp throws inside run-import!. Perms
          ;; are NOT restored mid-test: a restore would fire a second (now
          ;; readable) import and race the good-write latch. Cleanup deletes via
          ;; the writable parent dir, which does not need file read permission.
          (try
            (Files/setPosixFilePermissions
             (.toPath bad-file)
             (java.util.HashSet. #{PosixFilePermission/OWNER_WRITE}))
            (catch Throwable _ nil))
          (let [readable? (Files/isReadable (.toPath bad-file))
                ev (await-import! q)]
            (is (some? ev) "the bad file still produced a completion event")
            (when-not readable?
              (is (= :error (:status ev))
                  "the throwing import surfaced as :error, not a crash")))))

      (testing "a subsequent good write still imports (loop survived)"
        (let [good-file (io/file root "good.md")
              epoch-before @ingest-epoch/!ingest-epoch-atom]
          (drain! q)
          (spit good-file "# Recovered\nthe loop kept running\n")
          (let [ev (await-import! q)]
            (is (some? ev) "good import latched after the failure")
            (is (= :accepted (:status ev)))
            (is (= (inc epoch-before) (:epoch ev))
                "epoch advanced by exactly 1 on the recovered import"))))
      (finally
        ((:stop! handle))
        (ocr/close-object-container-runtime! runtime)
        (io/delete-file (io/file root "unreadable.md") true)
        (io/delete-file (io/file root "good.md") true)
        (io/delete-file root true)))))
