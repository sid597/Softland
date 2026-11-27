(ns softland.runtime.line-review-proof
  "The proof of what the review of 26 November 2026 found in what the line
  answers when it does not take a word. One command, from the repository
  root:

    clojure -M:runtime -m softland.runtime.line-review-proof

  On Rama's in-process cluster, with two copies of the runtime: one with the
  seam's default numbers, and one that gives a connection two watches.

  How it is seen. What the line itself sends is read as frames, by a
  WebSocket of the JDK's that the proof opens and that keeps each frame as
  it came; the bound on the words that wait is staged on a connection that
  the proof opens with the line's own `open`.

  State. A cluster that is thrown away at the end."
  (:require [missionary.core :as m]
            [softland.runtime.attacks.harness :as h]
            [softland.runtime.drive :as drive]
            [softland.runtime.line :as line]
            [softland.runtime.seam :as seam]
            [softland.wire.frame :as frame]
            [softland.wire.proof :refer [check finish]])
  (:import [java.net URI]
           [java.net.http HttpClient WebSocket WebSocket$Listener]
           [java.util UUID]
           [java.util.concurrent CompletableFuture CountDownLatch]))

;; ------------------------------------------------------------------- helpers

(defn- frames-of
  "Opens the line at an address with a WebSocket of the JDK's. Gives
  {:socket :frames}: `:frames` is an atom of each frame that came, read as
  EDN, in the order in which it came."
  [url credential]
  (let [frames (atom [])
        parts (StringBuilder.)
        listener (reify WebSocket$Listener
                   (onOpen [_ socket] (.request socket 1))
                   (onText [_ socket data last?]
                     (.append parts data)
                     (when last?
                       (swap! frames conj (second (frame/read-one (.toString parts))))
                       (.setLength parts 0))
                     (.request socket 1)
                     nil))
        socket (.join ^CompletableFuture
                (-> (HttpClient/newHttpClient)
                    (.newWebSocketBuilder)
                    (.header "Authorization" (str "Bearer " credential))
                    (.buildAsync (URI/create (str "ws" (subs url 4) "/line")) listener)))]
    {:socket socket :frames frames}))

(defn- send-frame!
  [{:keys [^WebSocket socket]} form]
  (.join ^CompletableFuture (.sendText socket (frame/write form) true)))

(defn- frame-that
  "Waits for a frame of which `held?` is true. Gives it, or nil."
  [{:keys [frames]} held?]
  (h/wait-for (fn [] (some (fn [form] (when (held? form) form)) @frames)) 15000))

(defn- under
  "The frames that came under one number."
  [{:keys [frames]} id]
  (filterv (fn [form] (= id (:id form))) @frames))

(defn- result-of
  "Says a word under a number and waits for its result. The line does one
  word at a time, so each word that was sent before it is answered by then."
  [raw id word]
  (send-frame! raw (assoc word :id id))
  (frame-that raw (fn [form] (and (= id (:id form)) (contains? form :gives)))))

(defn- watch-frame
  [id who lane e]
  {:id id :word :watch :query {:asks [:place e :k] :asker who :lanes [lane]}})

(defn- write-word
  [{:keys [who session own-lane]} e v]
  {:word :send
   :act {:version 1 :who who :session session :lane own-lane :permission [who own-lane own-lane]
         :facts [{:e e :k :k :v v}]}
   :act-id (UUID/randomUUID)})

(defn- shows?
  "Whether a frame is a change under the number that holds the value."
  [id v]
  (fn [form]
    (and (= id (:id form))
         (some (fn [[_ place-answer]] (= v (:value place-answer))) (:changed form)))))

(defn- anew?
  [id]
  (fn [form] (and (= id (:id form)) (contains? form :anew))))

;; ------------------------------------------------------------------ the line

(defn- a-number-in-use
  [{:keys [url credential]}]
  (let [raw (frames-of url credential)
        {:keys [who own-lane] :as hello} (:hello (frame-that raw :hello))
        _ (send-frame! raw (watch-frame 7 who own-lane :stool-1))
        opened (frame-that raw (anew? 7))
        _ (send-frame! raw (watch-frame 7 who own-lane :stool-2))
        _ (result-of raw 8 {:word :who-am-i})
        answered (vec (remove :anew (under raw 7)))
        wrote (result-of raw 9 (write-word hello :stool-1 :oak))
        went-on (frame-that raw (shows? 7 :oak))]
    (check "A second `:watch` under the number of a live watch gets a result under that number, which says that the number is in use, and no end: the live watch goes on and shows the next change of its place."
           (and opened
                (= [{:id 7 :gives {:could-not :read-the-word :why :number-in-use :at :id}}] answered)
                (= :accepted (get-in wrote [:gives :answer :answer]))
                went-on)
           {:opened (boolean opened) :answered answered :wrote wrote :went-on went-on})
    (.abort ^WebSocket (:socket raw))))

(defn- over-the-connections-limit
  [{:keys [small credential]}]
  (let [raw (frames-of small credential)
        {:keys [who own-lane] :as hello} (:hello (frame-that raw :hello))
        _ (send-frame! raw (watch-frame 1 who own-lane :bench-1))
        _ (send-frame! raw (watch-frame 2 who own-lane :bench-2))
        opened (and (frame-that raw (anew? 1)) (frame-that raw (anew? 2)))
        _ (send-frame! raw (watch-frame 3 who own-lane :bench-3))
        _ (result-of raw 4 {:word :who-am-i})
        answered (under raw 3)
        _ (result-of raw 5 (write-word hello :bench-1 :ash))
        _ (result-of raw 6 (write-word hello :bench-2 :elm))
        went-on [(frame-that raw (shows? 1 :ash)) (frame-that raw (shows? 2 :elm))]]
    (check "On a copy that gives a connection two watches, a third `:watch` gets `:take-more` as a result, with the limit, and no end; the two watches that the connection holds go on, and each shows the next change of its place."
           (and opened
                (= [{:id 3 :gives {:could-not :take-more :why :too-many-watches-on-a-connection :most 2}}] answered)
                (every? some? went-on))
           {:opened (boolean opened) :answered answered :went-on went-on})
    (.abort ^WebSocket (:socket raw))))

(defn- too-many-words-waiting
  "The connection is the proof's own, opened with the line's `open` for a
  visitor, so each frame and the close are seen as the line hands them to
  what carries it. The seam's `call` is replaced by one that stays until it
  is let go: the connection's one thread stays in its first word, and each
  word after it waits."
  [{:keys [the-seam]}]
  (let [sent (atom [])
        closed (promise)
        in-the-word (promise)
        let-go (CountDownLatch. 1)
        text (fn [n] (frame/write {:id n :word :who-am-i}))]
    (with-redefs [seam/call (fn [_ _ _]
                              (m/via m/blk
                                     (deliver in-the-word true)
                                     (.await let-go)
                                     {:visitor true}))]
      (let [connection (line/open the-seam (seam/visitor)
                                  {:send! (fn [frame-text] (swap! sent conj (second (frame/read-one frame-text))))
                                   :close! (fn [status reason] (deliver closed [status reason]))})
            _ (line/text! connection (text 0))
            stays? (deref in-the-word 10000 false)
            _ (dotimes [n line/max-waiting] (line/text! connection (text (inc n))))
            open-at-the-bound? (line/open? connection)
            _ (line/text! connection (text (inc line/max-waiting)))
            close (deref closed 10000 :still-open)]
        (.countDown let-go)
        (check "A connection on which as many words wait as the line holds is open; at one word more the line says `:take-more` with the limit as its last word, and closes the connection. The seam's `call` is replaced here by one that stays, so that the words wait."
               (and stays? open-at-the-bound?
                    (= {:bye {:could-not :take-more :why :too-many-waiting :most line/max-waiting}} (last @sent))
                    (= 1008 (first close))
                    (not (line/open? connection)))
               {:stays? stays? :open-at-the-bound? open-at-the-bound? :sent @sent :close close})))))

;; ---------------------------------------------------------------------- main

(defn -main
  "Runs the proof, prints the counts, and ends the process with a status."
  [& _]
  (h/with-cluster
    (fn [{:keys [cluster stand-in]}]
      (let [copy (h/start-copy! {:cluster cluster :stand-in stand-in})
            small (h/start-copy! {:cluster cluster :stand-in stand-in
                                  :numbers {:max-watches-per-connection 2}})
            url (:url copy)]
        (try
          (h/setup-first-person! copy)
          (let [cookie (:cookie (drive/sign-in url (assoc h/first-person :call-her "Sid")))
                program (drive/program url "the proof of the line's answers" (fn [code] (drive/yes url cookie code {})))
                with {:url url :small (:url small) :the-seam (:the-seam copy)
                      :credential (:credential program)}]
            (a-number-in-use with)
            (over-the-connections-limit with)
            (too-many-words-waiting with))
          (check "The proof ran to its end without a throw." true)
          (catch Throwable t
            (check "The proof ran to its end without a throw." false (str t))
            (.printStackTrace t))
          (finally
            (h/stop-copy! small)
            (h/stop-copy! copy))))))
  (finish))
