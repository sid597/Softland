(ns softland.runtime.line-review-proof
  "The proof of what the review of 26 November 2026 found in what the line
  answers when it does not take a word, and in what the library does with
  the answer. One command, from the repository root:

    clojure -M:runtime -m softland.runtime.line-review-proof

  On Rama's in-process cluster, with two copies of the runtime: one with the
  seam's default numbers, and one that gives a connection two watches.

  How each part is seen. What the line itself sends is read as frames, by a
  WebSocket of the JDK's that the proof opens and that keeps each frame as
  it came; the bound on the words that wait is staged on a connection that
  the proof opens with the line's own `open`. What the library does is seen
  through the library.

  State. A cluster that is thrown away at the end."
  (:require [missionary.core :as m]
            [softland.command.client :as client]
            [softland.runtime.attacks.harness :as h]
            [softland.runtime.drive :as drive]
            [softland.runtime.line :as line]
            [softland.runtime.seam :as seam]
            [softland.wire.credential :as credential]
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

(defn- refused
  "Connects, and gives the word with which the opening was refused. A
  connection that opened is closed, and gives nil."
  [url credential session]
  (let [got (client/connect {:url url :credential credential :session session})]
    (if (:could-not got)
      got
      (do (client/close! got) nil))))

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

;; --------------------------------------------------------------- the library

(defn- a-watch-that-was-not-opened
  [{:keys [small credential]}]
  (let [opened (h/line small credential nil)
        {:keys [who own-lane]} (:hello opened)
        query (fn [e] {:asks [:place e :k] :asker who :lanes [own-lane]})
        held (atom [])
        seen (atom [])
        _ (client/watch (:line opened) (query :desk-1) (fn [message] (swap! held conj message)))
        _ (client/watch (:line opened) (query :desk-2) (fn [message] (swap! held conj message)))
        both? (h/wait-for (fn [] (= 2 (count (filter :anew @held)))) 15000)
        id (client/watch (:line opened) (query :desk-3) (fn [message] (swap! seen conj message)))
        _ (h/say opened {:word :who-am-i})
        _ (Thread/sleep 300)
        holds? (contains? (:watches @(:state (:line opened))) id)]
    (check "Through the library, on a copy that gives a connection two watches: the function of a third watch is called once, with `{:ended word}` and the runtime's `:take-more`, and the library holds no watch under that number."
           (and both?
                (= [{:ended {:could-not :take-more :why :too-many-watches-on-a-connection :most 2}}] @seen)
                (not holds?))
           {:both? both? :seen @seen :holds? holds?})
    (h/close-line! opened)))

(defn- a-refused-opening
  [{:keys [url credential]}]
  (let [nobody (refused url (credential/text (UUID/randomUUID) (UUID/randomUUID) (credential/make-secret)) nil)
        no-session (refused url credential (UUID/randomUUID))]
    (check "A refused opening gives the runtime's own word, with the status of its answer: `:find-her-sign-in` for a credential that stands for nobody, and `:find-her-session` for a session that is not open under the sign-in."
           (and (= [:find-her-sign-in 401] [(:could-not nobody) (:status nobody)])
                (= [:find-her-session 401] [(:could-not no-session) (:status no-session)]))
           [nobody no-session])))

(defn- a-sign-in-that-ends
  [{:keys [url cookie me]}]
  (let [resting (drive/program url "a program that she ends while it watches" (fn [code] (drive/yes url cookie code {})))
        opened (h/line url (:credential resting) nil)
        {:keys [who own-lane]} (:hello opened)
        seen (atom [])
        _ (client/watch (:line opened) {:asks [:place :shelf-1 :k] :asker who :lanes [own-lane]}
                        (fn [message] (swap! seen conj message)))
        watching? (h/wait-for (fn [] (some :anew @seen)) 15000)
        ended (h/say me {:word :end-sign-in :sign-in (:sign-in resting)})
        bye (h/wait-for (fn [] (client/bye (:line opened))) 15000)
        _ (h/wait-for (fn [] (some :ended @seen)) 15000)
        after (client/say (:line opened) {:word :who-am-i} 3000)]
    (check "When a sign-in is ended while a connection that rests on it holds a watch, the watch's function is told, once and as the last thing, `{:ended word}` with the runtime's own word, `:find-her-sign-in`; and a word that is said on that connection after it gives the same word."
           (and watching?
                (= {:ended (:sign-in resting)} ended)
                (= :find-her-sign-in (:could-not bye))
                (= [{:ended bye}] (filterv :ended @seen))
                (= {:ended bye} (last @seen))
                (= bye after))
           {:watching? (boolean watching?) :ended ended :bye bye :seen (filterv :ended @seen) :after after})
    (h/close-line! opened)))

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
                me (h/line url (:credential program) nil)
                with {:url url :small (:url small) :the-seam (:the-seam copy) :cookie cookie
                      :credential (:credential program) :me me}]
            (a-number-in-use with)
            (over-the-connections-limit with)
            (a-watch-that-was-not-opened with)
            (a-refused-opening with)
            (a-sign-in-that-ends with)
            (h/close-line! me)
            (too-many-words-waiting with))
          (check "The proof ran to its end without a throw." true)
          (catch Throwable t
            (check "The proof ran to its end without a throw." false (str t))
            (.printStackTrace t))
          (finally
            (h/stop-copy! small)
            (h/stop-copy! copy))))))
  (finish))
