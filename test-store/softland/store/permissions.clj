(ns softland.store.permissions
  "Party ids and permissions for the store's proofs (`CONTRACT.md` §2.1–2.3)."
  (:refer-clojure :exclude [agent])
  (:require [clojure.string :as str]
            [softland.store.act :as act]
            [softland.store.door.declaration :as declaration]
            [softland.store.gate.first-act :as first-act])
  (:import [java.nio ByteBuffer]
           [java.nio.charset StandardCharsets]
           [java.security MessageDigest]
           [java.util UUID]))

;; ----------------------------------------------------------------- party ids

(defn- party
  [prefix id]
  (let [s (if (and (keyword? id) (nil? (namespace id)))
            (name id)
            (str id))]
    (if (str/starts-with? s (str prefix "."))
      (keyword s)
      (keyword (str prefix "." s)))))

(defn person
  "A person party id in the store (`CONTRACT.md` §2.1: `:person.<id>`)."
  [id]
  (party "person" id))

(defn agent
  "An agent party id in the store (`CONTRACT.md` §2.1: `:agent.<id>`)."
  [id]
  (party "agent" id))

(defn run
  "A run party id in the store (`CONTRACT.md` §2.1: `:run.<id>`)."
  [id]
  (party "run" id))

(defn long-person
  "A person party keyword whose printed text has `n` characters, colon counted:
  `:person.aaa...` of length `n`."
  [n]
  (keyword (str "person." (apply str (repeat (max 1 (- n 8)) "a")))))

;; --------------------------------------------------------------- permissions

(defn- id-from-text
  [^String text]
  (let [hash (.digest (MessageDigest/getInstance "SHA-256") (.getBytes text StandardCharsets/UTF_8))
        buffer (ByteBuffer/wrap hash 0 16)
        high (.getLong buffer)
        low (.getLong buffer)]
    (UUID. (bit-or (bit-and high (bit-not 0xF000)) 0x8000)
           (bit-or (bit-and low 0x3FFFFFFFFFFFFFFF) Long/MIN_VALUE))))

(def member-allows
  {:see true :write :all :give #{:see :write :give}})

(def publisher-allows
  {:see true :write :all :give #{:see :write :give :everyone}})

(def owner-allows
  declaration/owner-allows)

(defn permission-id
  "What an `:act` puts under `:permission` (`CONTRACT.md` §2.2): its `UUID`."
  [permission]
  (if (and (map? permission) (contains? permission :id))
    (:id permission)
    permission))

(defn permission-value
  "The stored `:v` map of `permission` (`CONTRACT.md` §2.3)."
  [permission]
  (if (and (map? permission) (contains? permission :id) (contains? permission :value))
    (:value permission)
    permission))

(defn- given-allows
  [holder allows]
  (cond
    (map? allows) allows
    (= :see allows) {:see true}
    (= :owner allows) declaration/owner-allows
    (= :publisher allows) publisher-allows
    (or (nil? allows) (= :see-and-act allows))
    (if (act/borrower? holder)
      {:see true :write :all}
      member-allows)
    :else allows))

(defn- opts-map?
  [x]
  (and (map? x)
       (not-any? #(contains? x %) [:see :write :give :owner])))

(defn first-permission
  "A lane's first permission (`CONTRACT.md` §2.1, §2.3).
  `opts` (optional): `{:given-by who, :granted-in lane, :id uuid, :allows map, :covers v, :session s, :under p, :lent-from p}`."
  ([holder lane] (first-permission holder lane nil))
  ([holder lane {:keys [given-by granted-in id allows covers session under lent-from]}]
   (let [perm-id (or id (first-act/first-permission-id lane))
         val (cond-> {:holder holder
                      :allows (or allows declaration/owner-allows)
                      :covers (or covers [:default-group (if (some? granted-in) granted-in lane)])
                      :given-by (or given-by holder)}
               (some? session) (assoc :session session)
               (some? under) (assoc :under (permission-id under))
               (some? lent-from) (assoc :lent-from (permission-id lent-from)))]
     {:id perm-id :value val})))

(defn given-permission
  "A permission given beneath `parent` for `lane` (`CONTRACT.md` §2.3).
  `allows-or-opts` (optional) is either an allows word/map or an options map
  `{:allows word-or-map, :granted-in lane, :covers v, :session s, :given-by who, :id uuid, :under p, :lent-from p-or-nil, :salt any}`.
  Defaults `:lent-from` to `(permission-id parent)` unless `:lent-from` is explicitly provided in `opts`."
  ([holder lane parent] (given-permission holder lane parent nil))
  ([holder lane parent allows-or-opts]
   (let [opts (if (opts-map? allows-or-opts) allows-or-opts {:allows allows-or-opts})
         parent-id (permission-id parent)
         parent-holder (get (permission-value parent) :holder)
         allows (given-allows holder (:allows opts))
         covers (or (:covers opts) [:default-group (:granted-in opts lane)])
         given-by (if (contains? opts :given-by) (:given-by opts) parent-holder)
         under-id (if (contains? opts :under) (permission-id (:under opts)) parent-id)
         lent-id (if (contains? opts :lent-from) (permission-id (:lent-from opts)) parent-id)
         val (cond-> {:holder holder
                      :allows allows
                      :covers covers
                      :given-by given-by}
               (some? (:session opts)) (assoc :session (:session opts))
               (some? under-id) (assoc :under under-id)
               (some? lent-id) (assoc :lent-from lent-id))
         perm-id (or (:id opts)
                     (id-from-text (str "softland/given-permission/" (pr-str [val (:salt opts)]))))]
     {:id perm-id :value val})))

(defn run-permission
  "A sitting-scoped permission beneath `parent` (`CONTRACT.md` §2.3 `:session`).
  `opts` (optional): `{:granted-in lane, :covers v, :given-by who, :id uuid, :under p, :lent-from p-or-nil, :salt any}`."
  ([holder lane parent session]
   (run-permission holder lane parent session :see-and-act nil))
  ([holder lane parent session allows]
   (run-permission holder lane parent session allows nil))
  ([holder lane parent session allows opts]
   (let [parent-id (permission-id parent)
         parent-holder (get (permission-value parent) :holder)
         r-allows (cond
                    (map? allows) allows
                    (= :see allows) {:see true}
                    (or (nil? allows) (= :see-and-act allows)) {:see true :write :all}
                    :else allows)
         covers (or (:covers opts) [:default-group (:granted-in opts lane)])
         given-by (if (contains? opts :given-by) (:given-by opts) parent-holder)
         under-id (if (contains? opts :under) (permission-id (:under opts)) parent-id)
         lent-id (if (contains? opts :lent-from) (permission-id (:lent-from opts)) parent-id)
         val (cond-> {:holder holder
                      :allows r-allows
                      :covers covers
                      :given-by given-by}
               (some? session) (assoc :session session)
               (some? under-id) (assoc :under under-id)
               (some? lent-id) (assoc :lent-from lent-id))
         perm-id (or (:id opts)
                     (id-from-text (str "softland/run-permission/" (pr-str [val (:salt opts)]))))]
     {:id perm-id :value val})))

(defn act-permission
  "An exact-act yes permission on `yes-uuid` for `digest` (`CONTRACT.md` §2.3 `[:exact-act digest]`).
  `opts` (optional): `{:allows word-or-map, :covers v, :session s, :given-by who, :under p, :lent-from p-or-nil}`."
  ([holder lane parent yes-uuid digest]
   (act-permission holder lane parent yes-uuid digest nil))
  ([holder _lane parent yes-uuid digest opts]
   (let [parent-id (permission-id parent)
         parent-holder (get (permission-value parent) :holder)
         raw-allows (:allows opts)
         a-allows (cond
                    (map? raw-allows) raw-allows
                    (= :see raw-allows) {:see true}
                    (or (nil? raw-allows) (= :see-and-act raw-allows)) {:write :all}
                    :else raw-allows)
         covers (or (:covers opts) [:exact-act digest])
         given-by (if (contains? opts :given-by) (:given-by opts) parent-holder)
         under-id (if (contains? opts :under) (permission-id (:under opts)) parent-id)
         lent-id (if (contains? opts :lent-from) (permission-id (:lent-from opts)) parent-id)
         val (cond-> {:holder holder
                      :allows a-allows
                      :covers covers
                      :given-by given-by}
               (some? (:session opts)) (assoc :session (:session opts))
               (some? under-id) (assoc :under under-id)
               (some? lent-id) (assoc :lent-from lent-id))]
     {:id yes-uuid :value val})))

(defn deep-permission
  "A chain of `n` permissions in `lane` held by `holder` (`n` >= 1, first counted),
  or `n` steps beneath `root` when `root` is given."
  ([holder lane n]
   (reduce (fn [parent _] (given-permission holder lane parent))
           (first-permission holder lane)
           (range (dec n))))
  ([holder lane root n]
   (reduce (fn [parent _] (given-permission holder lane parent))
           root
           (range n))))

;; ---------------------------------------------------- act slot & store facts

(defn permission-fact
  "The fact map that grants or changes `permission` (`CONTRACT.md` §2.3).
  `opts` (optional): `{:replaces fact-id}`."
  ([permission] (permission-fact permission nil))
  ([permission opts]
   (cond-> {:e (permission-id permission)
            :k declaration/permission-key
            :v (permission-value permission)}
     (contains? opts :replaces) (assoc :replaces (:replaces opts)))))

(defn end-fact
  "The fact map that ends `permission` (`CONTRACT.md` §2.3: `:v` absent).
  `opts` (optional): `{:replaces fact-id}`."
  ([permission] (end-fact permission nil))
  ([permission opts]
   (cond-> {:e (permission-id permission)
            :k declaration/permission-key}
     (contains? opts :replaces) (assoc :replaces (:replaces opts)))))

(defn end-act-id-fact
  "The fact map that ends an exact-act yes permission known by its `yes-uuid`."
  ([yes-uuid] (end-act-id-fact yes-uuid nil))
  ([yes-uuid opts]
   (cond-> {:e yes-uuid :k declaration/permission-key}
     (contains? opts :replaces) (assoc :replaces (:replaces opts)))))

(defn open-permission
  "The initial `:anyone` permission for `lane` opened at making (`CONTRACT.md` §2.1, §2.3)."
  ([lane given-by] (open-permission lane given-by (first-act/first-permission-id lane)))
  ([lane given-by first-perm]
   {:id (first-act/first-anyone-id lane)
    :value {:holder :anyone
            :allows {:see true}
            :covers [:default-group lane]
            :given-by given-by
            :under (permission-id first-perm)}}))

(defn open-fact
  "The fact map that opens `lane` to `:anyone` at making."
  ([lane given-by] (permission-fact (open-permission lane given-by)))
  ([lane given-by first-perm] (permission-fact (open-permission lane given-by first-perm))))

(defn making-facts
  "The facts of a lane's making act (`CONTRACT.md` §2.1–2.3)."
  ([lane holder] (making-facts lane holder holder nil))
  ([lane holder given-by] (making-facts lane holder given-by nil))
  ([lane holder given-by {:keys [open?]}]
   (let [own (first-permission holder lane {:given-by given-by})
         depot (first-act/depot-at-making lane)]
     (cond-> [{:e lane :k :softland.store/depot :v depot}
              (permission-fact own)]
       open? (conj (open-fact lane given-by own))))))

(defn making-act
  "A maker's making act for `lane` (`CONTRACT.md` §2.2: omits `:permission`)."
  ([lane who holder session] (making-act lane who holder session nil))
  ([lane who holder session opts]
   {:version 1
    :who who
    :lane lane
    :session session
    :facts (making-facts lane holder who opts)}))
