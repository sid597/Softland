(ns softland.runtime.ids
  "The ids that sign-in uses: the store's two known ids, the ids of the
  system's layers, an account's id, and a name's rule and entity. Pure
  code. It needs no cluster.

  Every person, agent and program is an id that says nothing, and so are
  the layers, sessions, runs and invitations that sign-in makes: random
  UUIDs that a caller makes and hands in, so that nothing here takes a
  random source (BUILD.md, default 1). The ids made here are of two other
  kinds. The system's own layers have fixed keyword ids, so the runtime
  finds them at each start. And an account's id is worked out from the
  account, so that one account is always one place in `ways-in`.

  An account's id is a name-based UUID, version 5 of RFC 4122 (SHA-1), of
  its kind and its number under `account-namespace`, a UUID made once for
  this code. Google's account number is the `sub` of its ID token. The
  number itself is kept beside the id as given, never in place of it
  (Sid's decision 3 of 15 October 2026). Which hash and which namespace are
  a session's choices; the rule that one account is one id is the plan's
  (src/proposal/sign-in-2026-10-15/PROGRESS.md, \"Ids\").

  A name is a changeable shorthand for an identity (Sid's decision 6). Its
  fact sits on the name's own entity, `(keyword \"name\" n)`, in the
  system's names layer, so that a name is found with one read and claimed
  with one write. The rule of a name is a session's, Sid's to change.

  Inputs and results, by call:

    account-id        a kind and an account's number -> a UUID, or nil
    name?             anything -> true for a name that keeps the rule
    name-entity       a name -> the entity of its fact, or nil
    provisional-name  8 random bytes -> a name that keeps the rule

  State and effects. None. Each function is pure: the SHA-1 of a UUID
  version 5 is a pure function of its bytes. Nothing here reads a clock or
  a random source.

  Callers. `softland.runtime.records`, `softland.runtime.start`,
  `softland.runtime.sign-in`, and the pure proof."
  (:import [java.nio ByteBuffer]
           [java.nio.charset StandardCharsets]
           [java.security MessageDigest]
           [java.util UUID]))

;; ------------------------------------------------------------ known ids

(def system
  "The system's id, as the store knows it
  (`softland.store.gate.first-act/system`). The runtime's own steps act as
  the system, and nothing else does (BUILD.md, default 17)."
  :system)

(def base
  "The base's id, as the store knows it (`softland.store.gate.first-act/base`)."
  :base)

(def identities
  "The system's layer of identities, found by identity id: each identity's
  kind, name, record layer, own layer, maker, origin and invitation."
  :system/identities)

(def ways-in
  "The system's layer of ways in, found by account id: the person an
  account points at, and the account as given."
  :system/ways-in)

(def names
  "The system's layer of names, found by name: the identity a name points
  at."
  :system/names)

(def invitations
  "The system's layer of invitations, found by invitation id: the hash of
  the code, the rights it gives, who made it, who used it."
  :system/invitations)

(def rules
  "The system's layer of rules: the limit for each kind of actor, the
  system's blocks of a sender, the wait before a freed name is taken again,
  and how many blocked attempts are kept whole."
  :system/rules)

(def system-record
  "The system's own record layer: each start of the runtime is a session of
  the system, recorded here."
  :system/record)

(def system-layers
  "The six layers that the system makes at the runtime's start, in the
  order of their making. Each is a streaming layer whose first permission
  is the system's. PROGRESS.md names six where BUILD.md named two (the
  links and the names); the other four hold what the plan put in the
  system's care."
  [identities ways-in names invitations rules system-record])

;; ---------------------------------------------------------- account ids

(def account-namespace
  "The namespace UUID under which an account's id is made. Made once, at
  random, for this code, and written here so that it never changes: a
  change would give every account a new id."
  #uuid "1574d801-360e-488c-ac25-20dabbb03e63")

(defn- uuid-bytes
  "Takes a UUID. Gives its sixteen bytes, most significant first, as RFC
  4122 lays them out."
  ^bytes [^UUID u]
  (.array (doto (ByteBuffer/allocate 16)
            (.putLong (.getMostSignificantBits u))
            (.putLong (.getLeastSignificantBits u)))))

(defn uuid-v5
  "Takes a namespace UUID and a name, a string. Gives the name-based UUID of
  RFC 4122, version 5: the first sixteen bytes of the SHA-1 of the
  namespace's bytes and the name's UTF-8, with the version set to 5 and the
  variant to RFC 4122's. The same two give the same UUID, always."
  [^UUID namespace-uuid ^String the-name]
  (let [md (MessageDigest/getInstance "SHA-1")
        _ (.update md (uuid-bytes namespace-uuid))
        _ (.update md (.getBytes the-name StandardCharsets/UTF_8))
        h (.digest md)]
    (aset-byte h 6 (unchecked-byte (bit-or (bit-and (aget h 6) 0x0f) 0x50)))
    (aset-byte h 8 (unchecked-byte (bit-or (bit-and (aget h 8) 0x3f) 0x80)))
    (let [buf (ByteBuffer/wrap h 0 16)]
      (UUID. (.getLong buf) (.getLong buf)))))

(def max-account-number-length
  "How many characters an account's number can have. Google's `sub` is at
  most 255 ASCII characters, by Google's page on its ID token. A number of
  the build."
  255)

(defn account-id
  "Takes the kind of an account, a keyword such as `:google`, and its number
  as the provider gave it, a string. Gives the account's id: the UUID
  version 5 of the text `<kind>:<number>` under `account-namespace`. Gives
  nil for a kind that is no keyword with no namespace, or a number that is
  no string of 1 to `max-account-number-length` characters. The number is
  taken as given: no case is changed and no space is trimmed."
  [kind number]
  (when (and (keyword? kind) (nil? (namespace kind))
             (string? number) (<= 1 (count number) max-account-number-length))
    (uuid-v5 account-namespace (str (name kind) ":" number))))

;; ----------------------------------------------------------------- names

(def ^:private name-pattern
  "The rule of a name: 1 to 32 characters, each a lowercase letter a to z,
  a digit, `-` or `_`, and the first a letter."
  #"[a-z][a-z0-9_-]{0,31}")

(defn name?
  "Takes anything. Gives true for a string that keeps the rule of a name:
  1 to 32 characters of lowercase letters a to z, digits, `-` and `_`, the
  first a letter. Such a name makes a keyword that reads back as itself, so
  its entity is an id of the store."
  [x]
  (boolean (and (string? x) (re-matches name-pattern x))))

(defn name-entity
  "Takes a name. Gives the entity on which its fact sits in the names layer:
  `(keyword \"name\" n)`, so `:name/sid` for \"sid\". Gives nil for a thing
  that is no name (`name?`)."
  [n]
  (when (name? n) (keyword "name" n)))

(defn provisional-name
  "Takes 8 random bytes, which the caller makes. Gives the name that a first
  sign-in claims for its person until she chooses one: `new-` and the 16
  lowercase hex digits of the bytes, 20 characters. Gives nil when the
  thing is not 8 bytes. That a first sign-in gets a random provisional name
  is a session's choice (PROGRESS.md, \"Choices to tell Sid\")."
  [random-bytes]
  (when (and (bytes? random-bytes) (= 8 (alength ^bytes random-bytes)))
    (apply str "new-" (map #(format "%02x" (bit-and (long %) 0xff)) random-bytes))))
