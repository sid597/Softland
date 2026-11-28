(ns softland.server.pure-proof
  "The proof of the server's pure parts, and of what the server's source may
  hold. It needs no cluster. One command, from the repository root:

    clojure -M:server -m softland.server.pure-proof

  It checks, each by cases: a session's record
  (`softland.server.session.record`), the feed
  (`softland.server.session.lately`), a credential
  (`softland.server.session.credential`), the words and their check
  (`softland.server.words`), one frame (`softland.server.frame`), what waits
  for a slow reader (`softland.server.watching`), and the check of an ID
  token (`softland.server.google`) against tokens that a stand-in signs.

  Then it reads the source under src-server and holds what the build's page
  asks of it: one function calls the door's `send-act`, and one its
  `send-acts`; the server opens no
  table of the store; nothing requires Electric, the rig, the old server or
  Inland; and `softland.server.sign-in` is the one caller of the seam's
  `system`.

  State. It changes nothing outside its own process."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [softland.server.frame :as frame]
            [softland.server.google :as google]
            [softland.server.google-stand-in :as stand-in]
            [softland.server.proof :refer [check finish]]
            [softland.server.session.credential :as credential]
            [softland.server.session.lately :as lately]
            [softland.server.session.record :as record]
            [softland.server.watching :as watching]
            [softland.server.words :as words])
  (:import [java.util Random UUID]))

;; ---------------------------------------------------------- a session's record

(def ^:private a-hash (apply str (repeat 64 "a")))

(defn- final-row
  "Runs events over no row, in the order given. Gives the row at the end."
  [events]
  (reduce (fn [row event] (:row (record/after row event))) nil events))

(defn- record-checks []
  (let [who (UUID/randomUUID)
        session (UUID/randomUUID)
        began (record/began a-hash who session 2000 1000)
        ended (record/ended a-hash 1500)
        other (record/began a-hash :mallory (UUID/randomUUID) 9000 1200)]
    (check "A :began on no row makes a live row that holds the who, the session and the time until which it lasts."
           (= {:state :live :who who :session session :until 2000 :began 1000} (final-row [began])))
    (check "A second :began under a hash that has a row changes nothing, whoever it names: a hash is bound once."
           (= (final-row [began]) (final-row [began other])))
    (check "A second :began of another session tells nothing on the feed."
           (false? (:tell? (record/after (final-row [began]) other))))
    (check "The same :began a second time changes nothing and is told again, so a run again of the event loses no telling."
           (let [row (final-row [began])] (= {:row row :tell? true} (record/after row began))))
    (check "An :ended ends a live row and keeps who it stood for."
           (= {:state :ended :who who :session session :until 2000 :began 1000 :ended 1500} (final-row [began ended])))
    (check "An :ended on no row makes an ended row, so the hash can never begin."
           (= :ended (:state (final-row [ended began]))))
    (check "Whatever the order of a :began and an :ended of one hash, the credential ends up ended."
           (= [:ended :ended] (mapv #(:state (final-row %)) [[began ended] [ended began]])))
    (check "A thing that is none of the two events in its form is no event."
           (every? (complement record/event?)
                   [nil 7 {} {:event :began :hash a-hash} (assoc began :hash "short") (assoc began :session "s")
                    (assoc ended :event :forgotten) (assoc ended :at "now")]))
    (check "The two events in their form are events."
           (and (record/event? began) (record/event? ended)))
    (check "The standing of no row is :not-known; of a live row before its time, :live with who she is; at its time, :over; of an ended row, :ended."
           (= [{:state :not-known}
               {:state :live :who who :session session :until 2000}
               {:state :over}
               {:state :ended}]
              [(record/standing nil 1500)
               (record/standing (final-row [began]) 1999)
               (record/standing (final-row [began]) 2000)
               (record/standing (final-row [began ended]) 1700)]))
    (check "The join of two rows of one hash is the one further on, in either order of arrival."
           (let [live (final-row [began]) gone (final-row [began ended])]
             (and (= gone (record/join live gone)) (= gone (record/join gone live))
                  (= live (record/join nil live)) (= live (record/join live nil))
                  (nil? (record/join nil nil)))))))

;; ---------------------------------------------------------------------- the feed

(defn- lately-checks []
  (let [feed-of (fn [n] (reduce (fn [feed i] (lately/after feed (str "h" i) {:state :live :n i})) (lately/start) (range n)))]
    (check "A copy that has seen no value takes no entry and remembers the feed's next number."
           (= {:seen 3 :rows [] :missed? false} (lately/news nil (feed-of 3))))
    (check "A copy takes each change that it had not seen, in the order of the changes."
           (= {:seen 5 :rows [["h3" {:state :live :n 3}] ["h4" {:state :live :n 4}]] :missed? false}
              (lately/news 3 (feed-of 5))))
    (check "A copy that has seen everything takes nothing."
           (= {:seen 5 :rows [] :missed? false} (lately/news 5 (feed-of 5))))
    (check "The feed holds the last changes and no more."
           (= lately/kept (count (:changed (feed-of (* 3 lately/kept))))))
    (check "A copy that fell behind by more than the feed holds is told that it missed changes."
           (:missed? (lately/news 2 (feed-of (+ 10 lately/kept)))))
    (check "A copy that is behind by exactly what the feed holds missed nothing."
           (let [n (+ 10 lately/kept)]
             (= {:missed? false :count lately/kept}
                (let [news (lately/news 10 (feed-of n))] {:missed? (:missed? news) :count (count (:rows news))}))))
    (check "A feed whose numbers went back, as after a new launch of the module, tells a copy that it missed changes."
           (= {:seen 2 :rows [] :missed? true} (lately/news 40 (feed-of 2))))
    (check "A hash that changed twice has one entry, its latest row."
           (let [feed (-> (lately/start)
                          (lately/after "h" {:state :live})
                          (lately/after "h" {:state :ended}))]
             (= [["h" {:state :ended}]] (:rows (lately/news 0 feed)))))))

;; ------------------------------------------------------------------ a credential

(defn- credential-checks []
  (let [token (credential/make)
        hash (credential/hash-of token)]
    (check "A token that the server makes has the form of a token, and two are not the same."
           (and (credential/token? token) (not= token (credential/make))))
    (check "A token's hash is 64 hex characters, and the record takes it as a hash's text."
           (record/hash-text? hash))
    (check "A text of another form has no hash, so no row is asked for under it."
           (every? nil? (map credential/hash-of ["" "sl1_short" "nonsense" nil 7 (str token "x")])))
    (check "The short code is the first eight characters of the hash, in two fours."
           (= (credential/code-of hash) (str (subs hash 0 4) "-" (subs hash 4 8))))
    (check "A token is read from an Authorization header that says Bearer, in any case of the word."
           (= [token token nil nil nil]
              [(credential/of-header (str "Bearer " token))
               (credential/of-header (str "bearer  " token))
               (credential/of-header (str "Basic " token))
               (credential/of-header "Bearer nonsense")
               (credential/of-header nil)]))
    (check "A token is read from the server's cookie among others, and a header that holds the cookie twice gives none."
           (= [token nil nil]
              [(credential/of-cookie (str "theme=dark; softland=" token "; other=1"))
               (credential/of-cookie (str "softland=" token "; softland=" (credential/make)))
               (credential/of-cookie "theme=dark")]))))

;; ------------------------------------------------------------------- the words

(def ^:private an-act-id [:notes (UUID/randomUUID)])
(def ^:private an-act {:version 1 :lane :notes :permission (UUID/randomUUID) :facts [{:e :chair-7 :k :colour :v :green}]})
(def ^:private a-query {:asks [:place :chair-7 :colour] :lanes [:notes]})

(defn- words-checks []
  (check "Each word in its form passes the check and comes back in its one form."
         (every? :ok (map words/check
                          [{:word :who-am-i}
                           {:word :who-am-i :wait-ms 500}
                           {:word :act-ids :lane :notes}
                           {:word :act-ids :lane :notes :count 3}
                           {:word :send :act an-act}
                           {:word :send :act an-act :act-id an-act-id}
                           {:word :send-all :acts [[an-act-id an-act]]}
                           {:word :answer :act-id an-act-id :digest (apply str (repeat 64 "0"))}
                           {:word :read :query a-query}
                           {:word :watch :query a-query}
                           {:word :sign-out}])))
  (check "A part that is left out holds its default in the one form: a count of 1, a wait of 0."
         (= [1 0] [(:count (:ok (words/check {:word :act-ids :lane :notes})))
                   (:wait-ms (:ok (words/check {:word :who-am-i})))]))
  (check "A wait that is longer than the longest is cut to it."
         (= words/longest-wait-ms (:wait-ms (:ok (words/check {:word :who-am-i :wait-ms 999999999})))))
  (check "The act and the query come through the check as they arrived: the check reads nothing inside them."
         (and (identical? an-act (:act (:ok (words/check {:word :send :act an-act}))))
              (identical? a-query (:query (:ok (words/check {:word :read :query a-query}))))))
  (check "An act of a wrong form passes the check: a gate refuses it, and the log keeps it."
         (some? (:ok (words/check {:word :send :act {:version 9 :facts "none"}}))))
  (check "An act that holds :who or :session, and a query that holds :asker or :not-signed-in, are turned away at that part."
         (= [{:refuse :holds-a-part-that-the-server-writes :at :who}
             {:refuse :holds-a-part-that-the-server-writes :at :session}
             {:refuse :holds-a-part-that-the-server-writes :at :asker}
             {:refuse :holds-a-part-that-the-server-writes :at :not-signed-in}
             {:refuse :holds-a-part-that-the-server-writes :at :who}]
            [(words/check {:word :send :act (assoc an-act :who :system)})
             (words/check {:word :send :act (assoc an-act :session nil)})
             (words/check {:word :read :query (assoc a-query :asker :asha)})
             (words/check {:word :watch :query (assoc a-query :not-signed-in true)})
             (words/check {:word :send-all :acts [[an-act-id an-act] [an-act-id (assoc an-act :who :asha)]]})]))
  (check "A thing that is no word is turned away with a reason and the part."
         (= [{:refuse :not-a-word :at nil}
             {:refuse :not-a-word :at nil}
             {:refuse :word-not-known :at :word}
             {:refuse :word-not-known :at :word}
             {:refuse :part-not-known :at :extra}
             {:refuse :lane-not-named :at :lane}
             {:refuse :count-out-of-range :at :count}
             {:refuse :count-out-of-range :at :count}
             {:refuse :not-given :at :act}
             {:refuse :not-a-plain-map :at :act}
             {:refuse :act-id-has-no-form :at :act-id}
             {:refuse :act-id-not-given :at :act-id}
             {:refuse :digest-not-given :at :digest}
             {:refuse :digest-is-no-text :at :digest}
             {:refuse :wait-is-no-span :at :wait-ms}
             {:refuse :acts-not-a-series :at :acts}
             {:refuse :acts-not-a-series :at :acts}
             {:refuse :not-given :at :query}]
            [(words/check nil)
             (words/check [:send an-act])
             (words/check {})
             (words/check {:word :dance})
             (words/check {:word :who-am-i :extra 1})
             (words/check {:word :act-ids})
             (words/check {:word :act-ids :lane :notes :count 0})
             (words/check {:word :act-ids :lane :notes :count (inc words/max-act-ids)})
             (words/check {:word :send})
             (words/check {:word :send :act [an-act]})
             (words/check {:word :send :act an-act :act-id [:notes "no uuid"]})
             (words/check {:word :answer :digest "x"})
             (words/check {:word :answer :act-id an-act-id})
             (words/check {:word :answer :act-id an-act-id :digest 7})
             (words/check {:word :who-am-i :wait-ms -1})
             (words/check {:word :send-all :acts []})
             (words/check {:word :send-all :acts [an-act]})
             (words/check {:word :read})]))
  (check "Every reason that the check gave above is one of the declared reasons."
         (every? words/reasons
                 [:not-a-word :word-not-known :part-not-known :lane-not-named :count-out-of-range :not-given
                  :not-a-plain-map :act-id-has-no-form :act-id-not-given :digest-not-given :digest-is-no-text
                  :wait-is-no-span :acts-not-a-series :holds-a-part-that-the-server-writes]))
  (check "A word of the server's own is made only when it is declared, and each has a status."
         (and (= {:could-not :find-her-session :why :ended} (words/could-not :find-her-session {:why :ended}))
              (nil? (words/could-not :invent-a-word nil))
              (every? (comp int? :status) (vals words/could-nots)))))

;; ------------------------------------------------------------------- one frame

(defn- frame-checks []
  (check "A text that holds one EDN form gives the form, with a UUID and a set as what they are."
         (= [:ok {:word :send :act {:facts #{1 2} :id #uuid "36f0101c-7668-4998-8ae8-239266573e6b"}}]
            (frame/read-one "  {:word :send :act {:facts #{1 2} :id #uuid \"36f0101c-7668-4998-8ae8-239266573e6b\"}}\n")))
  (check "A text with no form, with two forms, with a form that does not end, or with a tag that EDN does not know is no frame."
         (= [[:no :empty] [:no :more-than-one-form] [:no :not-edn] [:no :not-edn] [:no :not-edn] [:no :not-edn]]
            [(frame/read-one "  ") (frame/read-one "{} {}") (frame/read-one "{:word") (frame/read-one "#=(+ 1 2)")
             (frame/read-one "#softland/evil {}") (frame/read-one nil)]))
  (check "The longest token of a text is its longest run with no blank space, comma or character at which EDN ends a number."
         (= [0 2 5 4 7]
            (mapv frame/longest-token ["" "{:a 1}" "[12345 \"ab cd\"]" "1234,5678" "#{1234567}"])))
  (check "A text with a token over the bound is no frame, and is turned away before it is read: a number of a million digits costs no more than a pass over the text."
         (let [long-number (apply str (repeat (inc frame/max-token-chars) "7"))
               began (System/nanoTime)
               got [(frame/read-one long-number)
                    (frame/read-one (str "{:word :read :query {:asks [:points-at :k " long-number "]}}"))
                    (frame/read-one (str "\"" long-number "\""))
                    (frame/read-one (str "0x" (apply str (repeat frame/max-token-chars "f"))))]
               ms (quot (- (System/nanoTime) began) 1000000)]
           (and (every? #(= [:no :a-token-too-long] %) got) (< ms 2000)))
         :too-slow-or-read)
  (check "A token at the bound is read: a string of the store's longest value text, and a number of that many digits."
         (let [digits (apply str (repeat frame/max-token-chars "7"))]
           (and (= :ok (first (frame/read-one (str "\"" digits "\""))))
                (= :ok (first (frame/read-one digits))))))
  (check "A text that nests deeper than the reader's stack is turned away and does not throw."
         (= [:no :not-edn] (frame/read-one (apply str (repeat 200000 "[")))))
  (check "What is written is one line that reads back as the same thing, also when a string holds a line end."
         (let [thing {:softland.store/depot :streaming :text "a\nb" :n 12345678901234567890N :id (UUID/randomUUID)}
               text (frame/write thing)]
           (and (not (str/includes? text "\n")) (= [:ok thing] (frame/read-one text)))))
  (check "A map whose keys share a namespace is written whole, with no prefix in front of the map."
         (str/starts-with? (frame/write {:softland.store/depot 1 :softland.store/permission 2}) "{:softland.store/")))

;; ------------------------------------------------------ what waits for a reader

(defn- random-messages
  "Gives a random run of messages of one live read over a few places: a
  whole answer first, then changes and whole answers, and sometimes an end
  as its last thing."
  [^Random random]
  (let [places [[:a :k] [:b :k] [:c :k] [:d :k]]
        place-answer (fn [] (if (zero? (.nextInt random 4))
                              {:holds :no-fact-given}
                              {:holds :value :value (.nextInt random 100) :stamp (.nextInt random 1000)}))
        whole (fn [] {:anew {:lanes [:l] :left-out []
                             :places (into {} (keep (fn [place] (let [pa (place-answer)]
                                                                  (when-not (= :no-fact-given (:holds pa)) [place pa]))))
                                           places)}})
        change (fn [] {:changed (into {} (map (fn [_] [(nth places (.nextInt random 4)) (place-answer)]))
                                      (range (inc (.nextInt random 3))))})
        body (repeatedly (.nextInt random 12) (fn [] (if (zero? (.nextInt random 6)) (whole) (change))))]
    (cond-> (into [(whole)] body)
      (zero? (.nextInt random 3)) (conj {:ended {:could-not :keep-the-read-live}}))))

(defn- watching-checks []
  (check "A whole answer anew takes the place of the answer and the changes that wait."
         (= {:anew {:n 2}} (watching/merged {:anew {:n 1} :changed {[:a :k] 1}} {:anew {:n 2}})))
  (check "Of two changes of one place the later one stays, and a change of another place is kept beside it."
         (= {:anew {:n 1} :changed {[:a :k] 2 [:b :k] 1}}
            (-> {:anew {:n 1}}
                (watching/merged {:changed {[:a :k] 1 [:b :k] 1}})
                (watching/merged {:changed {[:a :k] 2}}))))
  (check "Nothing comes after an end: what waits with an end stays as it is."
         (let [waits (watching/merged {:changed {[:a :k] 1}} {:ended :word})]
           (= waits (-> waits (watching/merged {:anew {:n 9}}) (watching/merged {:changed {[:a :k] 9}})))))
  (check "What waits is handed on in order: the whole answer, the changes, the end; and nothing for nothing."
         (= [[{:anew {:n 1}} {:changed {[:a :k] 1}} {:ended :word}] [{:ended :word}] []]
            [(watching/messages {:anew {:n 1} :changed {[:a :k] 1} :ended :word})
             (watching/messages {:changed {} :ended :word})
             (watching/messages {})]))
  (check "A change of a read of many places puts a place in, and takes a place out that says no fact."
         (= {:places {[:b :k] {:holds :value :value 2}}}
            (watching/answer-after {:places {[:a :k] {:holds :value :value 1}}}
                                   {:changed {[:a :k] {:holds :no-fact-given} [:b :k] {:holds :value :value 2}}})))
  (check "A change of a read of one place puts its place-answer under :place."
         (= {:place {:holds :value :value 2}}
            (watching/answer-after {:place {:holds :no-fact-given}} {:changed {[:a :k] {:holds :value :value 2}}})))
  (let [random (Random. 20261120)
        runs (repeatedly 500 (fn [] (random-messages random)))
        slow-ok? (fn [messages]
                   ;; A reader who takes every message, and a reader for whom
                   ;; everything after a random point waited and was merged,
                   ;; hold the same answer at the end and both see the end.
                   (let [cut (.nextInt random (inc (count messages)))
                         [head tail] (split-at cut messages)
                         merged (if (seq tail) (watching/messages (reduce watching/merged tail)) [])
                         fast (reduce watching/answer-after nil messages)
                         slow (reduce watching/answer-after nil (concat head merged))]
                     (and (= fast slow)
                          (= (boolean (some :ended messages)) (boolean (some :ended (concat head merged))))
                          (<= (count merged) 3))))]
    (check "For 500 random runs of a live read's messages, a reader for whom a part of the run waited and was merged ends with the same answer as a reader who took each message, sees the end when there is one, and is handed three things at most for what waited."
           (every? slow-ok? runs)))
  (check "The transducer that ends a flow hands on the thing that ends it, and nothing after."
         (= [1 2 :end] (into [] (watching/through #{:end}) [1 2 :end 3 4]))))

;; -------------------------------------------------------- the check of an ID token

(defn- google-checks []
  (let [google (stand-in/make)
        other (stand-in/make)
        now 1795000000000
        options {:client-id (:client-id google) :issuers #{(:issuer google)} :keys (:keys google) :now-ms now}
        good (stand-in/token google {"sub" "1082" "email" "sid@example.com"} now)
        refuse (fn [token] (:refuse (google/check token options)))
        b64 (fn [^String text] (.encodeToString (.withoutPadding (java.util.Base64/getUrlEncoder)) (.getBytes text "UTF-8")))
        [header payload signature] (str/split good #"\.")]
    (check "A token that the issuer signed, for this server, in its time, with a subject, passes, and its claims come back under the issuer's own names."
           (= ["1082" "sid@example.com"] (mapv #(get (:ok (google/check good options)) %) ["sub" "email"])))
    (check "A token that names no algorithm, or a shared secret's, is turned away before any key is looked at."
           (= [:wrong-algorithm :wrong-algorithm]
              [(refuse (str (b64 "{\"alg\":\"none\",\"kid\":\"softland-stand-in-1\"}") "." payload "."))
               (refuse (str (b64 "{\"alg\":\"HS256\",\"kid\":\"softland-stand-in-1\"}") "." payload "." signature))]))
    (check "A token whose claims were changed after it was signed is turned away."
           (= :signature-wrong
              (refuse (str header "." (b64 "{\"sub\":\"9999\",\"iss\":\"https://google-stand-in.softland.invalid\",\"aud\":\"softland-stand-in\",\"exp\":99999999999}") "." signature))))
    (check "A token that another key pair signed under the same key name is turned away."
           (= :signature-wrong (refuse (stand-in/token other {"sub" "1082"} now))))
    (check "A token whose key name the issuer does not publish is turned away."
           (= :key-not-known (:refuse (google/check good (assoc options :keys {"another" (val (first (:keys google)))})))))
    (check "A token of another issuer, for another audience, past its time, or with no subject is turned away, each with its word."
           (= [:wrong-issuer :wrong-audience :wrong-audience :expired :expired :no-subject :no-subject]
              [(refuse (stand-in/token google {"sub" "1" "iss" "https://accounts.example"} now))
               (refuse (stand-in/token google {"sub" "1" "aud" "another-client"} now))
               (refuse (stand-in/token google {"sub" "1" "aud" [(:client-id google) "another-client"]} now))
               (refuse (stand-in/token google {"sub" "1" "exp" (quot now 1000)} now))
               (refuse (stand-in/token google {"sub" "1" "exp" "tomorrow"} now))
               (refuse (stand-in/token google {} now))
               (refuse (stand-in/token google {"sub" ""} now))]))
    (check "A text that is no token is turned away and does not throw."
           (every? #(= :not-a-token %) (map refuse [nil "" "a.b" "a.b.c" "a.b.c.d" 7 (str header "." payload)])))
    (check "The name of the key that a token asks for is given only for a text of three parts whose header names RS256 and a key: a face fetches the issuer's keys for no other text."
           (= ["softland-stand-in-1" nil nil nil nil]
              [(google/key-name good)
               (google/key-name (str (b64 "{\"alg\":\"none\",\"kid\":\"softland-stand-in-1\"}") "." payload "."))
               (google/key-name (str (b64 "{\"alg\":\"RS256\"}") "." payload "." signature))
               (google/key-name "x")
               (google/key-name nil)]))
    (check "The keys of a JWKS document are read by key name, and a text that is none gives no key."
           (and (= {} (google/keys-of "not json")) (= {} (google/keys-of "{\"keys\":[{\"kty\":\"EC\",\"kid\":\"x\"}]}"))))))

;; --------------------------------------------------------- what the source holds

(defn- source-files
  "Gives each Clojure file under src-server, as [its path, its text]."
  []
  (for [^java.io.File file (file-seq (io/file "src-server"))
        :when (and (.isFile file) (re-find #"\.clj[cs]?$" (.getName file)))]
    [(.getPath file) (slurp file)]))

(defn- code-of
  "Takes the text of a Clojure file. Gives it without its docstrings and
  comments, roughly: each string and each comment is taken out, so that a
  name which a docstring only speaks of is not counted as a call."
  [text]
  (-> text
      (str/replace #"(?s)\"(?:\\.|[^\"\\])*\"" "\"\"")
      (str/replace #";[^\n]*" "")))

(defn- source-checks []
  (let [files (source-files)
        holding (fn [pattern] (sort (for [[path text] files :when (re-find pattern (code-of text))] path)))]
    (check "The source of the server is read from src-server, so this proof ran at the repository root."
           (< 5 (count files)) (count files))
    (check "Two files of the server require the store's door and its read path: the seam, and the program that opens them. No other file can call either, under any name."
           (= [["src-server/softland/server/main.clj" "src-server/softland/server/seam.clj"]
               ["src-server/softland/server/main.clj" "src-server/softland/server/seam.clj"]]
              [(holding #"softland\.store\.door(?![.\w-])") (holding #"softland\.store\.read-path(?![.\w-])")])
           [(holding #"softland\.store\.door(?![.\w-])") (holding #"softland\.store\.read-path(?![.\w-])")])
    (check "The program that opens the door and the read path calls nothing of them but their opening."
           (= #{"door/open" "read-path/open"}
              (set (re-seq #"(?<![\w.-])(?:door|read-path)/[\w!?*-]+" (code-of (slurp "src-server/softland/server/main.clj")))))
           (set (re-seq #"(?<![\w.-])(?:door|read-path)/[\w!?*-]+" (code-of (slurp "src-server/softland/server/main.clj")))))
    (check "The seam calls the door's send-act in one place and its send-acts in one place: no act reaches a depot but through the two functions that write the actor's who, one act or a series."
           (= {"door/send-act" 1 "door/send-acts" 1} (frequencies (re-seq #"door/send-acts?\b" (code-of (slurp "src-server/softland/server/seam.clj")))))
           (frequencies (re-seq #"door/send-acts?\b" (code-of (slurp "src-server/softland/server/seam.clj")))))
    (check "The server opens no table and no depot of Rama's but in the client of its own module: what it reads of the store, it reads through the read path."
           (= ["src-server/softland/server/sessions.clj"]
              (holding #"foreign-pstate|foreign-select|foreign-proxy|foreign-depot|foreign-append|foreign-query|foreign-invoke"))
           (holding #"foreign-pstate|foreign-select|foreign-proxy|foreign-depot|foreign-append|foreign-query|foreign-invoke"))
    (check "No file of the server's tree requires Electric: the seam and the words are called by every face."
           (empty? (holding #"hyperfiddle")) (holding #"hyperfiddle"))
    (check "No file of the server's tree requires the rig, the old server or Inland."
           (empty? (holding #"\[rig\.|\[app\.|softland\.inland")) (holding #"\[rig\.|\[app\.|softland\.inland"))
    (check "The seam's actor of the system is asked for in one file beside the seam, the one that writes what sign-in writes: no face can make that actor or hand one to the seam. A face reaches the system's steps only by calling that file's functions."
           (= ["src-server/softland/server/sign_in.clj"]
              (remove #{"src-server/softland/server/seam.clj"} (holding #"seam/system|\(system ")))
           (holding #"seam/system|\(system "))
    (check "No file of the server's tree reads an environment file."
           (empty? (holding #"env\.clj|\.env\b")) (holding #"env\.clj|\.env\b"))))

(defn -main
  "Runs every check, prints the counts, and ends the process with status 0
  when none failed."
  [& _]
  (record-checks)
  (lately-checks)
  (credential-checks)
  (words-checks)
  (frame-checks)
  (watching-checks)
  (google-checks)
  (source-checks)
  (finish))
