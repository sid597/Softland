(ns rig.store.promote-unit-test
  "Promotion's pure parts, no cluster (PLAN-promotion.md, 'Namespaces and
  tests', U1 to U4): the sealed box, the store-made offers, the status over
  three answers, the request's and the landing lease's checks, the
  landing's face rules, the landing body, and the read-out's verdict.

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key."
  (:require [clojure.test :refer [deftest is testing]]
            [rig.store.box :as box]
            [rig.store.envelope :as env]
            [rig.store.gate :as gate]
            [rig.store.locks :as locks]
            [rig.store.promote :as promote]
            [rig.store.promote-shape :as ps])
  (:import [java.util Arrays]))

(defn- same? [a b] (and (bytes? a) (bytes? b) (Arrays/equals ^bytes a ^bytes b)))

(def ^:private spki-prefix
  "The 12-byte X.509 SubjectPublicKeyInfo prefix of an X25519 public key."
  (byte-array (map unchecked-byte [0x30 0x2a 0x30 0x05 0x06 0x03 0x2b 0x65 0x6e 0x03 0x21 0x00])))

(defn- spki-of-u
  "A well-formed 44-byte X25519 public key whose u-coordinate is `u` (a
  byte, little-endian): u = 0 and u = 1 are small-order points."
  [u]
  (let [out (byte-array 44)]
    (System/arraycopy spki-prefix 0 out 0 12)
    (aset-byte out 12 (unchecked-byte u))
    out))

(defn- flip [^bytes b i] (let [c (aclone b)] (aset-byte c i (unchecked-byte (bit-xor (aget c i) 1))) c))

;; ------------------------------------------------------------------- U1

(deftest u1-the-box
  (let [lease (box/keypair)
        other (box/keypair)
        K (locks/fresh-lock)
        lid [[:group :by-entity :offer (env/uuid7)] 0]
        lname [:group :by-entity :landing (nth (first lid) 3)]
        aad (ps/aad lid lname)
        b (box/box (:public lease) K (box/keypair) (box/fresh-nonce) aad)]
    (testing "sizes: 44-byte public, 48-byte private, a 104-byte box"
      (is (= 44 (alength ^bytes (:public lease))))
      (is (= 48 (alength ^bytes (:private lease))))
      (is (= 104 (+ (alength ^bytes (:eph b)) (alength ^bytes (:nonce b)) (alength ^bytes (:wrapped b)))))
      (is (box/box? b)))
    (testing "the box opens with the lease's private key, to the lock"
      (is (same? K (box/unbox (:private lease) (:public lease) b aad))))
    (testing "and with nothing else, never throwing"
      (is (nil? (box/unbox (:private other) (:public other) b aad)) "another key pair")
      (is (nil? (box/unbox (:private other) (:public lease) b aad)) "another private key")
      (is (nil? (box/unbox (:private lease) (:public lease) (update b :wrapped flip 5) aad)) "a changed byte")
      (is (nil? (box/unbox (:private lease) (:public lease) (update b :eph flip 20) aad)) "a changed ephemeral key")
      (is (nil? (box/unbox (:private lease) (byte-array 43) b aad)) "a public key of the wrong length")
      (is (nil? (box/unbox (:private lease) (:public lease) {:eph 1} aad)) "not a box")
      (is (nil? (box/unbox nil nil nil nil)) "nothing"))
    (testing "[F2] the box is bound to its landing: another lock id or landing name does not open it"
      (let [lname2 [:group :by-entity :landing (env/uuid7)]]
        (is (nil? (box/unbox (:private lease) (:public lease) b (ps/aad lid lname2))))
        (is (nil? (box/unbox (:private lease) (:public lease) b (ps/aad [(first lid) 1] lname))))))
    (testing "[F2] boxing to what is no X25519 key is nil, never a throw"
      (let [rand44 (locks/fresh-bytes 44)]
        (is (nil? (box/public-key rand44)) "44 random bytes do not decode")
        (is (nil? (box/box rand44 K (box/keypair) (box/fresh-nonce) aad))))
      (doseq [u [0 1]]
        (is (some? (box/public-key (spki-of-u u))) (str "the small-order point u = " u " decodes"))
        (is (nil? (box/box (spki-of-u u) K (box/keypair) (box/fresh-nonce) aad))
            (str "and the agreement refuses it: no box, u = " u)))
      (is (nil? (box/box (:public lease) (byte-array 31) (box/keypair) (box/fresh-nonce) aad)) "a lock of 31 bytes")
      (is (nil? (box/box (:public lease) K {:public 1} (box/fresh-nonce) aad)) "no ephemeral pair"))
    (testing "the public key's text form round-trips; anything else decodes to nil"
      (is (same? (:public lease) (box/decode-public (box/encode-public (:public lease)))))
      (is (nil? (box/decode-public "not base64 !")))
      (is (nil? (box/public-key (box/decode-public (box/encode-public (byte-array 44))))) "44 zero bytes are text of the right length, and no key")
      (is (nil? (box/decode-public 42))))))

;; ------------------------------------------------------ a request, pure

(def ^:private u (env/uuid7))
(def ^:private src-name [:alice :by-layer :offer (env/uuid7)])
(def ^:private src [src-name 0])
(def ^:private lease-pair (box/keypair))

(defn- request
  "A well-formed request as the gate parses it, with overrides of its value."
  ([] (request {}))
  ([vmore & {:as omore}]
   (merge {:version 1
           :name [:alice :by-layer :offer u]
           :who :alice :layer :alice :class :by-layer
           :permission [:alice :alice :alice] :session :door/alice
           :stood-on {src 1000}
           :because-of nil :claimed-when 5 :subjects #{}
           :facts [{:e :e0 :k :promote-request
                    :v (merge {:source src :target :group :class :by-entity
                               :lease [[:group :by-entity :offer u] 0]
                               :public (box/encode-public (:public lease-pair))
                               :permission [:alice :group :group [:group :group :group]]
                               :replaces nil
                               :subjects #{}}
                              vmore)
                    :replaces nil :mark #{}}]}
          omore)))

(defn- ok? [o] (ps/request-value-ok? o (:v (first (:facts o)))))

;; ------------------------------------------------------------------- U4

(deftest u4-the-request-check
  (testing "a well-formed request passes"
    (is (ok? (request))))
  (testing "each malformed shape is refused (the gate's :malformed-control)"
    (doseq [[what o] [["an extra key" (request {:extra 1})]
                      ["a missing key" (update-in (request) [:facts 0 :v] dissoc :replaces)]
                      ["[F1] a lease tagged for another layer" (request {:lease [[:base :by-entity :offer u] 0]})]
                      ["[F1] a lease tagged for another class" (request {:lease [[:group :by-layer :offer u] 0]})]
                      ["[F1] a lease whose uuid is not the request's own" (request {:lease [[:group :by-entity :offer (env/uuid7)] 0]})]
                      ["[F1] a lease id other than the lease's first lock" (request {:lease [[:group :by-entity :offer u] 1]})]
                      ["[F2] a 43-byte public key" (request {:public (.encodeToString (java.util.Base64/getEncoder) (byte-array 43))})]
                      ["[F2] a 44-byte blob that is no X25519 key" (request {:public (.encodeToString (java.util.Base64/getEncoder) (locks/fresh-bytes 44))})]
                      ["a public key that is not text" (request {:public (:public lease-pair)})]
                      ["a permission for another layer" (request {:permission [:alice :base :base [:operator :base :base]]})]
                      ["257 subjects" (request {:subjects (set (map #(keyword (str "p" %)) (range 257)))})]
                      ["subjects not a set" (request {:subjects [:bob]})]
                      ["replaces in another layer" (request {:replaces [[:base :by-layer :offer (env/uuid7)] 0]})]
                      ["the target is the owner's own layer" (request {:target :alice :lease [[:alice :by-entity :offer u] 0]})]
                      ["a source in another layer" (request {:source [[:bob :by-layer :offer (env/uuid7)] 0]})]
                      ["a source the act does not stand on" (request {} :stood-on {})]
                      ["a source index past a row vector's range" (request {:source [src-name (long Integer/MAX_VALUE)]} :stood-on {[src-name (long Integer/MAX_VALUE)] 1000})]
                      ["no session" (request {} :session nil)]]]
      (is (not (ok? o)) what)))
  (testing "a small-order point decodes, so the request's check lets it through; the read-out refuses it ([F2])"
    (is (ok? (request {:public (box/encode-public (spki-of-u 0))})))))

(deftest the-landing-lease-and-crossing-checks
  (let [lease-act {:name [:group :by-entity :offer u] :layer :group :class :by-entity :session :door/alice}]
    (testing "[F1] a landing lease is exactly {:count 1 :landing L*}, L* its own name's"
      (is (ps/landing-lease-value? lease-act {:count 1 :landing [:group :by-entity :landing u]}))
      (is (not (ps/landing-lease-value? lease-act {:count 2 :landing [:group :by-entity :landing u]})) "count 2")
      (is (not (ps/landing-lease-value? lease-act {:count 1 :landing [:group :by-entity :landing (env/uuid7)]})) "another uuid: another's landing")
      (is (not (ps/landing-lease-value? lease-act {:count 1 :landing [:group :by-layer :landing u]})) "another class")
      (is (not (ps/landing-lease-value? lease-act {:count 1 :landing [:base :by-entity :landing u]})) "another layer")
      (is (not (ps/landing-lease-value? lease-act {:count 1 :landing [:group :by-entity :landing u] :x 1})) "an extra key")
      (is (not (ps/landing-lease-value? (assoc lease-act :name [:group :by-entity :landing u]) {:count 1 :landing [:group :by-entity :landing u]}))
          "a lease act under a landing name"))
    (testing "a crossing fact's value is exactly {:request name :source fid}"
      (is (ps/crossed-value-ok? {:request [:alice :by-layer :offer u] :source src}))
      (is (not (ps/crossed-value-ok? {:request [:alice :by-layer :offer u]})))
      (is (not (ps/crossed-value-ok? {:request :x :source src}))))))

;; ------------------------------------------------------------------- U2

(defn- source-row [v]
  (let [K (locks/fresh-lock)]
    {:K K :row {:e :e0 :k :note :v nil :replaces nil :mark #{}
                :sealed (locks/seal K (locks/canonical-bytes v)) :lock-id [[:alice :by-layer :offer (env/uuid7)] 0]}}))

(deftest u2-the-store-made-offers
  (let [req (request {:subjects #{:carol}} :subjects #{:dave})
        {:keys [row]} (source-row "hello")
        crossing (promote/crossing-offer req 2000 row :by-layer)
        fresh (promote/fresh-read-out)
        ro (promote/read-out-verdict req row {:value "hello" :stamp 1000} fresh)
        landing (promote/landing-offer req 3000 row (:sealed ro) (:box ro))]
    (testing "PR2: the crossing, named from the request, the store's, standing on the source and the request"
      (is (= [:alice nil :crossing u] (:name crossing)))
      (is (= (env/crossing-name (:name req)) (:name crossing)))
      (is (= :store (:who crossing)))
      (is (nil? (:permission crossing)))
      (is (= {src 1000 [(:name req) 0] 2000} (:stood-on crossing)))
      (is (= [{:e :e0 :k :crossed :v {:request (:name req) :source src} :replaces nil :mark #{}}] (:facts crossing)))
      (is (not (env/mis-tagged? crossing)) "placed by the store: its name carries no class")
      (is (= :by-entity (:class (promote/crossing-offer req 2000 row :by-entity))) "the class in force: it claims none of its own"))
    (testing "PR3: the landing, [T C :landing u], the request's who, session, landing permission and subjects"
      (is (nil? (:reason ro)))
      (is (= [:group :by-entity :landing u] (:name landing)))
      (is (= (env/landing-name (:name req) :group :by-entity) (:name landing)))
      (is (= [:group :by-layer :landing u] (env/landing-name (:name req) :group :by-layer)) "the class the request carries names the gate")
      (is (= [:group :by-entity :landing u] (env/landing-name (:name req) :group)) "the 2-arity keeps the micro gate's class")
      (is (= [:alice :door/alice [:alice :group :group [:group :group :group]] (:name req)]
             ((juxt :who :session :permission :because-of) landing)))
      (is (= #{:carol} (:subjects landing)) "the request's subjects, never the source act's")
      (is (= {src 1000 [[:alice nil :crossing u] 0] 3000} (:stood-on landing)) "standing on the source and the crossing")
      (is (= 1 (count (:facts landing))))
      (is (= [:e0 :note [[:group :by-entity :offer u] 0]] ((juxt :e :k :lock-id) (first (:facts landing)))))
      (is (not (contains? (first (:facts landing)) :replaces)) "no nil part: the stored envelope reads back as sent"))
    (testing "the copy opens under the lock in the box, and only the landing lease opens the box"
      (let [f (first (:facts landing))
            K (box/unbox (:private lease-pair) (:public lease-pair) (:box f) (ps/aad (:lock-id f) (:name landing)))]
        (is (some? K))
        (is (= "hello" (:value (locks/decode-plain (locks/open K (:sealed f))))))
        (is (nil? (ps/open-landing {:under nil :sealed (:private lease-pair) :public (:public lease-pair)
                                    :for [:group :by-entity :landing (env/uuid7)]}
                                   (:name landing) (:lock-id f) (:box f)))
            "a lease row :for another landing opens nothing")
        (is (same? K (ps/open-landing (ps/landing-row lease-pair (:name landing)) (:name landing) (:lock-id f) (:box f)))
            "its own lease row opens it")
        (is (nil? (locks/unlease (ps/landing-row lease-pair (:name landing)) nil))
            "a landing row is never an ordinary lock (PR10)")))
    (testing "the parts digest leaves the box and the sealed bytes out: two sealings of one landing digest the same"
      (let [ro2 (promote/read-out-verdict req row {:value "hello" :stamp 1000} (promote/fresh-read-out))
            landing2 (promote/landing-offer req 3000 row (:sealed ro2) (:box ro2))]
        (is (not (same? (:wrapped (:box (first (:facts landing)))) (:wrapped (:box (first (:facts landing2)))))))
        (is (= (env/digest (promote/landing-parsed landing)) (env/digest (promote/landing-parsed landing2))))
        (is (= (env/digest (promote/landing-parsed landing)) (env/digest (promote/landing-parsed landing)))
            "and one landing digests the same every time (a byte array would print by identity)")))
    (testing "the forward: the landing as sent, routed by the lease name to a micro target, by nothing to a stream one"
      (let [d {:kind :decide :record {:answer :yes :stamp 3000}}
            fwd (promote/forward req d ro row)]
        (is (= landing (:landing fwd)))
        (is (= [:group :by-entity :offer u] (:route fwd)))
        (is (nil? (promote/forward req {:kind :decide :record {:answer :no}} ro row)) "no forward for a no")
        (is (nil? (promote/forward req d {:reason :source-erased} row)) "none for a read-out refusal")
        (let [sreq (request {:target :base :class :by-layer :lease [[:base :by-layer :offer u] 0]
                             :permission [:alice :base :base [:operator :base :base]]})
              sro (promote/read-out-verdict sreq row {:value "hello" :stamp 1000} (promote/fresh-read-out))]
          (is (nil? (:route (promote/forward sreq d sro row))) "a stream target goes by a hop")
          (is (= [:base :by-layer :landing u] (get-in (promote/forward sreq d sro row) [:landing :name]))))))))

(deftest the-read-out-verdict
  (let [req (request)
        {:keys [row]} (source-row "hello")
        fresh (promote/fresh-read-out)]
    (is (= {:reason :source-erased} (promote/read-out-verdict req row {:erased-at 5} fresh)) "a forgotten source")
    (is (= {:reason :source-erased} (promote/read-out-verdict req row {:unreadable :does-not-open} fresh)))
    (is (= {:reason :source-has-no-value} (promote/read-out-verdict req nil {:unreadable :no-such-fact} fresh)) "no such fact")
    (is (= {:reason :source-has-no-value}
           (promote/read-out-verdict req {:e :e0 :k :note :v "\"x\"" :replaces nil :mark #{}} {:value "x" :stamp 1} fresh))
        "a control fact (its row is not sealed)")
    (is (= {:reason :source-has-no-value}
           (promote/read-out-verdict req {:e :e0 :k :note :v nil :replaces nil :mark #{}} {:value nil :stamp 1} fresh))
        "a retract")
    (is (= {:reason :malformed-control}
           (promote/read-out-verdict (request {:public (box/encode-public (spki-of-u 1))}) row {:value "hello" :stamp 1} fresh))
        "[F2] a small-order public key: no box can be made, a recorded no, never a throw")
    (is (promote/fresh-ok? fresh))
    (is (not (promote/fresh-ok? {})) "a failed draw is not sealed with")))

(deftest the-landing-face
  (let [lname [:group :by-entity :landing u]
        good {:e :e0 :k :note :sealed (byte-array 40) :lock-id [[:group :by-entity :offer u] 0]
              :box {:eph (byte-array 44) :nonce (byte-array 12) :wrapped (byte-array 48)}}]
    (is (nil? (ps/landing-face lname [good])))
    (is (= :malformed (ps/landing-face lname [good good])) "two facts")
    (is (= :malformed (ps/landing-face lname [(dissoc good :box)])) "no box")
    (is (= :malformed (ps/landing-face lname [(assoc-in good [:box :nonce] (byte-array 11))])) "a malformed box")
    (is (= :malformed (ps/landing-face lname [(dissoc good :sealed)])) "not sealed")
    (is (= :no-such-lock (ps/landing-face lname [(assoc good :lock-id [[:group :by-entity :offer (env/uuid7)] 0])]))
        "[F1] a lock id other than the one its name binds")
    (is (= :malformed (ps/landing-face lname :x)) "not a vector of facts")))

;; ------------------------------------------------------------------- U3

(deftest u3-the-status
  (let [yes (fn [s & {:as m}] (merge {:answer :yes :stamp s} m))
        no (fn [s r & {:as m}] (merge {:answer :no :reason r :stamp s} m))
        st (fn [& args] (:status (apply promote/status args)))]
    (testing "none: no request, or none at or before T ([F5]), or a refused request"
      (is (= :none (st nil nil nil nil nil)))
      (is (= :none (st (yes 10) nil nil nil 9)) "a request stamped after T is not shown")
      (is (= {:status :none :request-refused :no-permission :request-stamp 10}
             (promote/status (no 10 :no-permission) nil nil nil nil))))
    (testing "pending: admitted, no crossing at or before T"
      (is (= :pending (st (yes 10) nil nil nil nil)))
      (is (= :pending (st (yes 10) (yes 20) nil nil 15)) "the crossing after T: still pending as of T"))
    (testing "refused at the read-out"
      (is (= {:status :refused :at :read-out :reason :source-erased :request-stamp 10 :crossing-stamp 20}
             (promote/status (yes 10) (no 20 :source-erased) nil nil nil)))
      (is (= :pending (st (yes 10) (no 20 :source-erased) nil nil 19))))
    (testing "crossed: read out, not landed, or landed after T, or above the frontier (R5)"
      (is (= :crossed (st (yes 10) (yes 20) nil nil nil)))
      (is (= :crossed (st (yes 10) (yes 20) (yes 30 :batch 7) 6 nil)) "a batch above the frontier is not yet settled")
      (is (= :crossed (st (yes 10) (yes 20) (yes 30 :batch 7) nil nil)) "no frontier read: not settled")
      (is (= :crossed (st (yes 10) (yes 20) (yes 30 :batch 7) 7 25)) "landed after T"))
    (testing "done: a settled yes at or before T; a stream landing is settled once committed"
      (is (= :done (st (yes 10) (yes 20) (yes 30 :batch 7) 7 nil)))
      (is (= :done (st (yes 10) (yes 20) (yes 30 :batch 7) 9 30)))
      (is (= :done (st (yes 10) (yes 20) (yes 30) nil nil)) "no batch: the stream gate's answer"))
    (testing "refused at the landing, with its reason"
      (is (= {:status :refused :at :landing :reason :permission-revoked :request-stamp 10 :crossing-stamp 20 :landing-stamp 30}
             (promote/status (yes 10) (yes 20) (no 30 :permission-revoked :batch 7) 7 nil)))
      (is (= :crossed (st (yes 10) (yes 20) (no 30 :landing-lock-gone :batch 8) 7 nil)) "not yet settled"))
    (testing "the statements, as data, for the state found"
      (let [say (fn [s] (:key (promote/statement s :group)))]
        (is (= :pending (say {:status :pending})))
        (is (re-find #"refuses this promotion" (:text (promote/statement {:status :pending} :group))))
        (is (re-find #"does not recall the copy" (:text (promote/statement {:status :crossed :crossing-stamp (bit-shift-left 1000 16)} :group))))
        (is (re-find #"Crossed does not promise done" (:text (promote/statement {:status :crossed :crossing-stamp 0} :group))))
        (is (re-find #"Landed in group" (:text (promote/statement {:status :done :landing-stamp 0} :group))))
        (is (re-find #"forgotten before it was read out" (:text (promote/statement {:status :refused :reason :source-erased} :group))))
        (is (nil? (promote/statement {:status :none} :group)))))))

;; ------------------------------------ the stream gate's decision, R-1

(deftest the-gate-admits-promotion-facts-only-on-their-own-path
  (let [settings {:kind :personal :owner :alice :class :by-layer :grain :per-value}
        rows {[:alice :alice :alice] {:granted [[:alice :by-layer :offer (env/uuid7)] 0] :revoked nil}}
        decide (fn [raw]
                 (let [p (env/parse raw)]
                   (if-let [r (:refuse p)]
                     {:face r}
                     (let [o (:ok p)
                           d (gate/decide o settings rows {} 0 1000 (env/digest o))]
                       (if (= :decide (:kind d))
                         (or (get-in d [:record :reason]) :yes)
                         {:face (get-in d [:ack :reason])})))))
        act (fn [who facts & {:as more}]
              (let [raw (merge {:version 1 :who who :layer :alice :class :by-layer
                                :permission (when-not (= :operator who) [:alice :alice :alice])
                                :stood-on {} :subjects #{} :facts facts}
                               more)]
                (assoc raw :name (env/name-for raw))))
        crossed {:e :e0 :k :crossed :v {:request [:alice :by-layer :offer (env/uuid7)] :source src}}]
    (testing "a client's :crossed fact is refused: the crossing is the store's read-out's alone"
      (is (= :control-not-allowed (decide (act :alice [crossed]))) "by the layer's owner")
      (is (= :control-not-allowed (decide (act :operator [crossed]))) "by the operator")
      (is (= :malformed-control (decide (act :alice [(assoc crossed :v {:request :x})]))) "a malformed one is malformed first")
      (is (= {:face :reserved-who} (decide (act :store [crossed]))) "and no depot record may claim to be the store"))
    (testing "a :promote-request fact is admitted only as a request, whose yes goes on to its read-out"
      (let [req (request)]
        (is (= :yes (decide req)) "a well-formed request, its act's one fact")
        (is (= :malformed-control (decide (update req :facts conj {:e :e1 :k :tag :v nil})))
            "beside another fact: never a free-standing control fact in a larger act")
        (is (= :malformed-control (decide (assoc-in req [:facts 0 :v] {}))) "a value of another shape")
        (is (= :malformed-control (decide (assoc-in req [:facts 0 :v :lease] [[:group :by-entity :offer (env/uuid7)] 0])))
            "[F1] a lease not bound to the request's own uuid")
        (is (true? (promote/continues? (:ok (env/parse req)) {:answer :yes})) "a yes continues to the read-out")
        (is (false? (promote/continues? (:ok (env/parse (update req :facts conj {:e :e1 :k :tag :v nil}))) {:answer :no})))))))
