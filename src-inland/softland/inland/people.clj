(ns softland.inland.people
  "Who is at the screen, and under whose permission they act.
  Takes a login at the host and the rig's store; gives a known person, a
  session of theirs in the store, and the permissions that session writes
  under. Owns the host's passphrase file and its login tokens; borrows the
  store's doors as operator code, as the rig's own doors are (trusted:
  RIG.md default, PROGRESS.md \"Operator trusted at launch\").

  - Login: a person gives their passphrase at the host; the host gives the
    browser a token (an HttpOnly cookie). Passphrases live at the host, not
    in the store: `.inland-runtime/people.edn` holds salted PBKDF2 hashes,
    and `.inland-runtime/passphrases.txt` the words, made at first launch.
  - Every browser session is a session in the store, `S`, granted `[S L L
    p]` beneath the person's own permission `p` in each layer it writes:
    the person's hand layer, their own layer, the base and each group they
    belong to. Every act the screen writes for them names `:who` the
    person, `:session S` and that permission (rig.store.permit: a session
    holds a permission beneath its person's).
  - Groups: the operator makes one when a person asks, with them as its
    first member (the first group re-classes the base, the rig's default
    6), and adds a member when a member asks; the store accepts a member
    after the group is made (rig.store.micro-client `add-members!`).
  - Forget: the operator's act, from the host's command line
    (`bin/inland forget <person>`), never a screen gesture.

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [rig.store.client :as c]
            [rig.store.gate :as gate]
            [rig.store.locks :as locks]
            [rig.store.micro-client :as mc]
            [softland.inland.facts :as f])
  (:import [java.security MessageDigest SecureRandom]
           [java.util Base64]
           [javax.crypto SecretKeyFactory]
           [javax.crypto.spec PBEKeySpec]))

(def people
  "The persons the store makes at its first launch (placeholder: named
  people, keyword ids). Carol logs in nowhere; a note can name her."
  [:alice :bob :carol])

(def logins "Who may log in at the screen." [:alice :bob])

(defn hand-of "A person's hand layer." [p] (keyword (str (name p) "-hand")))
(defn own-of "A person's own layer." [p] p)

(def base-root "The base's root permission, held by the root actor." [locks/root-actor :base :base])

(defn person-permission
  "Person `p`'s own permission in layer `L` of `kind`: `[p L L]` in a layer
  they own, beneath the base's root in the base, beneath the group's root
  (held by the group) in a group."
  [p L kind]
  (case kind
    :base [p :base :base base-root]
    :group [p L L [L L L]]
    [p L L]))

(defn session-permission
  "The permission session `S` of `p` writes `L` under: `[S L L p-perm]`."
  [p S L kind]
  [S L L (person-permission p L kind)])

;; ================================================================ login

(def ^:private runtime (io/file ".inland-runtime"))
(def ^:private hashes-file (io/file runtime "people.edn"))
(def ^:private words-file (io/file runtime "passphrases.txt"))

(defonce ^:private random (SecureRandom.))

(defn- b64 [^bytes bs] (.encodeToString (Base64/getUrlEncoder) bs))
(defn- unb64 [^String s] (.decode (Base64/getUrlDecoder) s))

(defn- random-bytes [n] (let [bs (byte-array n)] (.nextBytes ^SecureRandom random bs) bs))

(defn- pbkdf2 [^bytes salt ^String pass]
  (let [spec (PBEKeySpec. (.toCharArray pass) salt 120000 256)]
    (.getEncoded (.generateSecret (SecretKeyFactory/getInstance "PBKDF2WithHmacSHA256") spec))))

(defn- passphrase
  "Three words from a small list and two digits: easy to type, not easy to guess
  offline for a local screen (placeholder: the host's own login)."
  []
  (let [words ["amber" "birch" "cedar" "delta" "ember" "fjord" "grove" "harbor" "indigo" "juniper"
               "kestrel" "lantern" "meadow" "nectar" "orchard" "pebble" "quartz" "river" "saffron" "tundra"
               "umber" "violet" "willow" "yarrow" "zephyr" "copper" "falcon" "garnet" "hollow" "iris"]
        pick #(nth words (.nextInt ^SecureRandom random (count words)))]
    (str (pick) "-" (pick) "-" (pick) "-" (format "%02d" (.nextInt ^SecureRandom random 100)))))

(defn ensure-passphrases!
  "The host's login file, made at first launch with a passphrase per login,
  whose words go to `passphrases.txt` beside it (read it, then keep it where
  you keep such things). Returns the hashes, `{name {:salt :hash :person}}`."
  []
  (if (.exists hashes-file)
    (edn/read-string (slurp hashes-file))
    (let [made (into {} (for [p logins] [(name p) (passphrase)]))
          hashes (into {} (for [[n w] made]
                            (let [salt (random-bytes 16)]
                              [n {:salt (b64 salt) :hash (b64 (pbkdf2 salt w)) :person (keyword n)}])))]
      (.mkdirs runtime)
      (spit hashes-file (pr-str hashes))
      (spit words-file (str (str/join "\n" (for [[n w] (sort made)] (str n "  " w))) "\n"))
      (.setReadable words-file false false)
      (.setReadable words-file true true)
      hashes)))

(defonce ^:private tokens (atom {}))

(defn login!
  "A name and passphrase → a fresh token for that person, or nil."
  [name pass]
  (let [entry (get (ensure-passphrases!) (str/lower-case (str name)))]
    (when (and entry (string? pass)
               (MessageDigest/isEqual (unb64 (:hash entry)) (pbkdf2 (unb64 (:salt entry)) pass)))
      (let [t (b64 (random-bytes 32))]
        (swap! tokens assoc t {:person (:person entry) :at (System/currentTimeMillis)})
        t))))

(defn person-of-token "The person a token was given to, or nil." [t] (:person (get @tokens t)))

(defn logout! "Forget a token." [t] (swap! tokens dissoc t))

;; ================================================================ the store's people

(defn world
  "The first facts' one-owner side: the persons, each one's own layer, a
  hand layer for each login, and their own permissions."
  []
  {:persons people
   :layers (vec (concat (for [p people] [(own-of p) {:kind :personal :owner p}])
                        (for [p logins] [(hand-of p) {:kind :hand :owner p}])))
   :permissions (vec (concat (for [p people] [p (own-of p) (own-of p)])
                             (for [p logins] [p (hand-of p) (hand-of p)])))})

(defn- operator-act!
  "An operator act into layer `L` through the door of the gate that orders it."
  [store L facts]
  (mc/offer-into! store (c/build {:who :operator :layer L :class (mc/layer-class store L) :facts facts})))

(defn made? "Whether the store's first facts are in." [store] (some? (c/settings store c/people-layer)))

(defn ensure-world!
  "The first facts, once: the store layer and its persons, their layers and
  permissions (rig.store.client `seed!`), the base with a permission for
  each login beneath its root, and the grammars each layer's keys need:
  Inland's record keys in the base and every own layer, the note's in
  every own layer, the stored cells' in every hand layer (a grammar lives in
  the layer it governs). Returns nil when they were in already."
  [store]
  (when-not (made? store)
    (let [answers (concat (c/seed! store (world))
                          (mc/make-base! store {:root base-root :grants (for [p logins] (person-permission p :base :base))})
                          [(operator-act! store :base f/record-grammars)]
                          (for [p people] (operator-act! store (own-of p) (conj f/record-grammars f/note-grammar)))
                          (for [p logins] (operator-act! store (hand-of p) f/cell-grammars)))
          refused (remove #(= :yes (:answer %)) answers)]
      (when (seq refused)
        (throw (ex-info "The first facts were refused." {:refused (vec refused)})))
      (count answers))))

(defn layer-kind
  "A layer's kind as the store holds it: `:base`, `:group`, `:personal`,
  `:hand` (the operator's view of settings, for choosing a permission)."
  [store L]
  (or (:kind (c/settings store L)) (:kind (mc/settings-of store L))))

(defn groups-of
  "The groups person `p` belongs to, among `candidates`, from the members
  the store holds (the operator's view)."
  [store p candidates]
  (vec (filter #(contains? (mc/members-of store %) p) candidates)))

(defn grant-session!
  "Grant session `S` of `p` its permission in layer `L`, beneath `p`'s own,
  by the operator through the gate that orders `L`. The answer."
  [store p S L]
  (let [kind (layer-kind store L)
        pid (session-permission p S L kind)]
    (mc/offer-into! store (c/build {:who :operator :layer L :class (mc/layer-class store L)
                                    :facts [{:e (gate/perm-entity S) :k :permission :v {:id pid}}]}))))

(defn open-session!
  "A browser session of person `p` in the store: a fresh session id, and
  its permission in each layer it writes. `{:S :who :writes {L permission}}`
  where `:who` is the read exit's reader part."
  [store p groups]
  (let [S (keyword (str (name p) "-s-" (subs (str (random-uuid)) 0 8)))
        hand (hand-of p)
        layers (vec (distinct (concat [hand (own-of p) :base] groups)))
        answers (into {} (for [L layers] [L (grant-session! store p S L)]))
        refused (remove (comp #(= :yes (:answer %)) val) answers)]
    (when (seq refused)
      (throw (ex-info "The session's permissions were refused." {:refused (into {} refused)})))
    {:S S :person p :hand hand :own (own-of p)
     :writes (into {} (for [L layers] [L (session-permission p S L (layer-kind store L))]))
     :who {:reader p :reader-kind :person :working hand :session S
           :permission (session-permission p S hand :hand)}}))

(defn make-group!
  "A group `L` made by the operator on `creator`'s asking, with them its
  first member and their permission beneath its root; then the grammars
  the group's keys need, by its own gate. The first group re-classes the
  base on the way (rig.store.micro-client `make-group!`). Returns the
  making act's answer."
  [store L creator]
  (let [{:keys [made]} (mc/make-group! store L {:members #{creator} :root [L L L]
                                                :grants [(person-permission creator L :group)]})]
    (when (= :yes (:answer made))
      (mc/offer! store (mc/build {:who :operator :layer L :facts (conj f/record-grammars f/note-grammar)})))
    made))

(defn add-member!
  "Person `p` accepted into group `L` on member `asker`'s asking: refused
  here, before any act, when the asker is not a member or `p` already is
  (the fold would move an existing member's batch). The answer."
  [store L asker p]
  (let [members (mc/members-of store L)]
    (cond (not (contains? members asker)) {:answer :no :reason :not-a-member}
          (contains? members p) {:answer :no :reason :already-a-member}
          (not (some #{p} people)) {:answer :no :reason :no-such-person}
          :else (mc/add-members! store L [p]))))

(defn forget!
  "The operator forgets person `p`: their person lock is destroyed on every
  task before the answer returns (rig.store.client `forget-person!`)."
  [store p]
  (mc/forget-person! store p))
