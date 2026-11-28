(ns softland.wire.pure-proof
  "The proof of the wire's forms. It needs no cluster. One command, from the
  repository root:

    clojure -M:wire -m softland.wire.pure-proof

  It checks, each by cases: a credential (`softland.wire.credential`), the
  words and their check (`softland.wire.words`), one frame
  (`softland.wire.frame`), and what waits for a slow reader
  (`softland.wire.watching`). The checks of a frame and of what waits are
  the ones of the server's pure proof of 20 November 2026, carried as they
  were: the two namespaces came over unchanged but for their names.

  State. It changes nothing outside its own process."
  (:require [clojure.string :as str]
            [softland.wire.credential :as credential]
            [softland.wire.frame :as frame]
            [softland.wire.proof :refer [check finish]]
            [softland.wire.watching :as watching]
            [softland.wire.words :as words])
  (:import [java.util Random UUID]))

;; ------------------------------------------------------------------ a credential

(defn- credential-checks []
  (let [root (UUID/randomUUID)
        sign-in (UUID/randomUUID)
        secret (credential/make-secret)
        text (credential/text root sign-in secret)
        hash (credential/hash-of secret)]
    (check "A secret that is made has the form of a secret, and two are not the same."
           (and (credential/secret? secret) (not= secret (credential/make-secret))))
    (check "A secret's hash is 64 lower-case hex characters, and a text of another form has none."
           (and (credential/hash? hash) (nil? (credential/hash-of "short")) (nil? (credential/hash-of nil))))
    (check "A credential's text gives back its three parts, the two ids as UUIDs."
           (= {:root root :sign-in sign-in :secret secret} (credential/parts text)))
    (check "A text that has not the form of a credential has no parts: a wrong prefix, a part missing, an id that is no UUID, a secret of another length, and a thing that is no text."
           (every? nil? [(credential/parts (str "sl1_" (subs text 4)))
                         (credential/parts (str "sl2_" root "." secret))
                         (credential/parts (str "sl2_nobody." sign-in "." secret))
                         (credential/parts (str text "x"))
                         (credential/parts 7)]))
    (check "A credential is put together only from two UUIDs and a secret."
           (and (nil? (credential/text "root" sign-in secret)) (nil? (credential/text root sign-in "secret"))))
    (check "Two hashes are the same only when both are hashes and equal."
           (and (credential/same-hash? hash hash)
                (not (credential/same-hash? hash (credential/hash-of (credential/make-secret))))
                (not (credential/same-hash? hash nil))
                (not (credential/same-hash? "x" "x"))))
    (check "A code is eight characters of an alphabet without 0, O, 1, I and L, and is shown in two fours."
           (let [code (credential/make-code)]
             (and (credential/code? code)
                  (not-any? (set "0O1IL") code)
                  (= 9 (count (credential/shown code)))
                  (= code (credential/of-shown (credential/shown code))))))
    (check "What a person typed is read as a code when its letters are small and it holds blank space or a dash; a text that is no code gives none."
           (and (= "ABCD2345" (credential/of-shown " abcd-2345 "))
                (nil? (credential/of-shown "ABCD-234"))
                (nil? (credential/of-shown "ABCD-2O45"))))
    (check "A credential is read from an Authorization header that says Bearer, in any case of the word, and from nothing else in a header."
           (and (= text (credential/of-header (str "Bearer " text)))
                (= text (credential/of-header (str "bearer   " text)))
                (nil? (credential/of-header (str "Basic " text)))
                (nil? (credential/of-header "Bearer nothing"))))
    (check "A credential is read from the runtime's cookie among others, and a header that holds the cookie twice gives none."
           (and (= text (credential/of-cookie (str "a=1; " credential/cookie-name "=" text "; b=2")))
                (nil? (credential/of-cookie (str credential/cookie-name "=" text "; " credential/cookie-name "=" text)))
                (nil? (credential/of-cookie "a=1"))))))

;; ------------------------------------------------------------------- the words

(def ^:private an-act
  {:version 1 :who :person.asha :session (UUID/randomUUID) :lane :notes :permission (UUID/randomUUID)
   :facts [{:e :chair-7 :k :colour :v :green}]})

(def ^:private a-query {:asks [:place :chair-7 :colour] :asker :person.asha :lanes [:notes]})

(def ^:private a-hash (apply str (repeat 64 "a")))

(defn- words-checks []
  (let [uuid (UUID/randomUUID)
        each {:who-am-i {:word :who-am-i}
              :send {:word :send :act an-act :act-id uuid :wait-ms 5}
              :send-all {:word :send-all :acts [{:act an-act :act-id uuid}] :wait-ms 5}
              :answer {:word :answer :act-id [:notes :streaming uuid] :digest a-hash :wait-ms 5}
              :read {:word :read :query a-query}
              :watch {:word :watch :query a-query}
              :leave {:word :leave :watch 3}
              :replace-secret {:word :replace-secret :hash a-hash}
              :sign-out {:word :sign-out}
              :open-session {:word :open-session :driven-by {:kind :person}}
              :close-session {:word :close-session}
              :invite {:word :invite :uuid uuid :claim {:by :email :email "asha@example.com"} :herself false}
              :take-back-invite {:word :take-back-invite :claim {:by :code :hash a-hash}}
              :accounts {:word :accounts}
              :unlink-account {:word :unlink-account :account [:google "1082"] :link uuid}
              :make-agent {:word :make-agent :uuid uuid :label "importer" :hash a-hash}
              :end-agent {:word :end-agent :agent uuid}
              :under {:word :under}
              :end-sign-in {:word :end-sign-in :sign-in uuid}}
        refused (fn [thing] (select-keys (words/check thing) [:refuse :at]))]
    (check "Every declared word has a case here, and each in its form passes the check and comes back as it went in."
           (and (= (set (keys words/words)) (set (keys each)))
                (every? (fn [[_ word]] (= {:ok word} (words/check word))) each)))
    (check "A wait that is left out is the default for a send and nothing for an answer, and a wait that is longer than the longest is cut to it."
           (= [words/default-wait-ms 0 words/longest-wait-ms]
              [(:wait-ms (:ok (words/check (dissoc (:send each) :wait-ms))))
               (:wait-ms (:ok (words/check (dissoc (:answer each) :wait-ms))))
               (:wait-ms (:ok (words/check (assoc (:send each) :wait-ms 999999999))))]))
    (check "The act and the query come through the check as they arrived, with their who, their session and their asker: the check reads nothing inside them."
           (and (= an-act (:act (:ok (words/check (:send each)))))
                (= a-query (:query (:ok (words/check (:read each)))))))
    (check "An act of a wrong form passes the check: a gate refuses it, and the log keeps it."
           (contains? (words/check {:word :send :act {:nothing :of-an-act} :act-id uuid}) :ok))
    (check "A send names its act's UUID: one that names none, or a thing that is no UUID, is turned away at that part."
           (= [{:refuse :act-id-not-given :at :act-id} {:refuse :act-id-is-no-uuid :at :act-id}]
              [(refused (dissoc (:send each) :act-id)) (refused (assoc (:send each) :act-id "one"))]))
    (check "A thing that is no word is turned away with a reason and the part: no map, an unknown word, a part that the word has not, a part that carries the store's form absent or no map."
           (= [{:refuse :not-a-word :at nil} {:refuse :word-not-known :at :word}
               {:refuse :part-not-known :at :more} {:refuse :not-given :at :query}
               {:refuse :not-a-plain-map :at :act}]
              [(refused [:send]) (refused {:word :fly})
               (refused {:word :sign-out :more 1}) (refused {:word :read})
               (refused {:word :send :act [1] :act-id uuid})]))
    (check "A series of acts is a vector of 1 to the most of {:act :act-id} maps, each with a UUID: an empty one, one that is too long, an entry of another form and an act-id that is no UUID are turned away."
           (= [{:refuse :acts-not-a-series :at :acts} {:refuse :too-many-acts :at :acts}
               {:refuse :acts-not-a-series :at :acts} {:refuse :act-id-is-no-uuid :at :acts}]
              [(refused {:word :send-all :acts []})
               (refused {:word :send-all :acts (vec (repeat (inc words/max-acts-of-one-word) {:act an-act :act-id uuid}))})
               (refused {:word :send-all :acts [[uuid an-act]]})
               (refused {:word :send-all :acts [{:act an-act :act-id 7}]})]))
    (check "A hash that is not 64 hex characters, a label that is empty or too long, and a digest that is no text are turned away at their parts."
           (= [{:refuse :hash-has-no-form :at :hash} {:refuse :label-is-no-text :at :label}
               {:refuse :label-is-no-text :at :label} {:refuse :digest-is-no-text :at :digest}]
              [(refused (assoc (:replace-secret each) :hash "abc"))
               (refused (assoc (:make-agent each) :label ""))
               (refused (assoc (:make-agent each) :label (apply str (repeat (inc words/max-text) "x"))))
               (refused (assoc (:answer each) :digest 7))]))
    (check "A claim is an email with an at-sign in it or the hash of a code, and nothing else: another way, a part too many, an email with none, and a hash of another form are turned away."
           (and (words/claim? {:by :email :email "a@b"})
                (words/claim? {:by :code :hash a-hash})
                (every? (fn [claim] (= {:refuse :claim-has-no-form :at :claim}
                                       (refused (assoc (:invite each) :claim claim))))
                        [{:by :phone :phone "1"} {:by :email :email "a@b" :more 1}
                         {:by :email :email "nobody"} {:by :code :hash "abc"}])))
    (check "An invitation names the caller herself only with `:herself true`, which is a yes or a no; left out, it is no; and a take-back names its claim in the invitation's own form."
           (and (nil? (:herself (:ok (words/check (dissoc (:invite each) :herself)))))
                (true? (:herself (:ok (words/check (assoc (:invite each) :herself true)))))
                (= {:refuse :herself-is-no-yes :at :herself} (refused (assoc (:invite each) :herself :yes)))
                (= {:refuse :claim-not-given :at :claim} (refused (dissoc (:take-back-invite each) :claim)))
                (= {:refuse :claim-has-no-form :at :claim}
                   (refused (assoc (:take-back-invite each) :claim {:by :email :email "nobody"}))))
           [(words/check (assoc (:invite each) :herself :yes)) (words/check (dissoc (:take-back-invite each) :claim))])
    (check "An account is unlinked by its [provider subject] and its link: a provider that is no keyword, a subject that is no text or is too long, a vector of another length, and a link that is no UUID or is left out are turned away at their parts."
           (and (every? (fn [account] (= {:refuse :account-has-no-form :at :account}
                                         (refused (assoc (:unlink-account each) :account account))))
                        [["google" "1082"] [:google 1082] [:google ""] [:google (apply str (repeat (inc words/max-text) "x"))]
                         [:google] [:google "1082" "more"]])
                (= {:refuse :link-is-no-uuid :at :link} (refused (assoc (:unlink-account each) :link "one")))
                (= {:refuse :link-not-named :at :link} (refused (dissoc (:unlink-account each) :link))))
           (mapv (fn [account] (refused (assoc (:unlink-account each) :account account))) [["google" "1082"] [:google 1082]]))
    (check "An :open-session word takes {:kind :person}, {:kind :model :model text} up to 256 characters, or no :driven-by, and turns away any other :driven-by."
           (and (= {:ok {:word :open-session :driven-by nil}} (words/check {:word :open-session}))
                (= {:ok {:word :open-session :driven-by nil}} (words/check {:word :open-session :driven-by nil}))
                (= {:ok {:word :open-session :driven-by {:kind :person}}}
                   (words/check {:word :open-session :driven-by {:kind :person}}))
                (= {:ok {:word :open-session :driven-by {:kind :model :model "claude-opus-4-6"}}}
                   (words/check {:word :open-session :driven-by {:kind :model :model "claude-opus-4-6"}}))
                (every? (fn [d] (= {:refuse :driven-by-has-no-form :at :driven-by}
                                   (refused {:word :open-session :driven-by d})))
                        [{:kind :robot} {:kind :person :extra 1} {:kind :model}
                         {:kind :model :model ""}
                         {:kind :model :model (apply str (repeat (inc words/max-text) "x"))}
                         "person"])))
    (check "Every reason that the check gave above is one of the declared reasons."
           (every? words/reasons
                   (keep :refuse [(words/check [:send]) (words/check {:word :fly})
                                  (words/check {:word :sign-out :more 1}) (words/check {:word :read})
                                  (words/check (dissoc (:send each) :act-id))
                                  (words/check (assoc (:replace-secret each) :hash "abc"))
                                  (words/check (assoc (:invite each) :claim {:by :phone}))
                                  (words/check (assoc (:invite each) :herself 1))
                                  (words/check (assoc (:unlink-account each) :account [:google]))
                                  (words/check (dissoc (:unlink-account each) :link))
                                  (words/check {:word :send-all :acts []})])))
    (check "A word of the runtime's own is made only when it is declared, and each has what it says and a status."
           (and (= {:could-not :find-her-sign-in :why :over} (words/could-not :find-her-sign-in {:why :over}))
                (nil? (words/could-not :make-tea nil))
                (every? (fn [[_ {:keys [says status]}]] (and (string? says) (integer? status))) words/could-nots)))))

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


(defn -main
  "Runs every check, prints the counts, and ends the process with a status."
  [& _]
  (credential-checks)
  (words-checks)
  (frame-checks)
  (watching-checks)
  (finish))
