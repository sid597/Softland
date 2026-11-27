(ns softland.runtime.permissions
  "Party ids and permissions for the runtime's proofs (`CONTRACT.md` §2.1–2.3)."
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

;; --------------------------------------------------------------- permissions

(defn- id-from-text
  [^String text]
  (let [hash (.digest (MessageDigest/getInstance "SHA-256") (.getBytes text StandardCharsets/UTF_8))
        buffer (ByteBuffer/wrap hash 0 16)
        high (.getLong buffer)
        low (.getLong buffer)]
    (UUID. (bit-or (bit-and high (bit-not 0xF000)) 0x8000)
           (bit-or (bit-and low 0x3FFFFFFFFFFFFFFF) Long/MIN_VALUE))))

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

(defn first-permission
  "A lane's first permission (`CONTRACT.md` §2.1, §2.3).
  `opts` (optional): `{:given-by who, :granted-in lane, :id uuid}`."
  ([holder lane] (first-permission holder lane nil))
  ([holder lane {:keys [given-by granted-in id]}]
   {:id (or id (first-act/first-permission-id lane))
    :value {:holder holder
            :allows declaration/owner-allows
            :covers [:default-group (or granted-in lane)]
            :given-by (or given-by holder)}}))

(defn given-permission
  "A permission given beneath `parent` for `lane` (`CONTRACT.md` §2.3).
  `allows-or-opts` (optional) is either `:see` / `:see-and-act` or an options map
  `{:allows word-or-map, :granted-in lane, :given-by who, :id uuid, :lent-from uuid-or-nil, :salt any}`."
  ([holder lane parent] (given-permission holder lane parent nil))
  ([holder lane parent allows-or-opts]
   (let [opts (if (and (map? allows-or-opts)
                       (not-any? #(contains? allows-or-opts %) [:see :write :give :owner]))
                allows-or-opts
                {:allows allows-or-opts})
         raw-allows (:allows opts)
         allows (cond
                  (map? raw-allows) raw-allows
                  (= :see raw-allows) {:see true}
                  :else (if (act/borrower? holder)
                          {:see true :write :all}
                          {:see true :write :all :give #{:see :write :give}}))
         parent-id (permission-id parent)
         parent-holder (get (permission-value parent) :holder)
         given-by (or (:given-by opts) parent-holder)
         lent-id (if (contains? opts :lent-from) (permission-id (:lent-from opts)) parent-id)
         val (cond-> {:holder holder
                      :allows allows
                      :covers [:default-group (:granted-in opts lane)]
                      :given-by given-by}
               (some? parent-id) (assoc :under parent-id)
               (some? lent-id) (assoc :lent-from lent-id))
         perm-id (or (:id opts)
                     (id-from-text (str "softland/runtime-given-permission/" (pr-str [val (:salt opts)]))))]
     {:id perm-id :value val})))

(defn run-permission
  "A sitting-scoped permission beneath `parent` (`CONTRACT.md` §2.3 `:session`).
  `opts` (optional): `{:granted-in lane, :given-by who, :id uuid, :lent-from uuid-or-nil, :salt any}`."
  ([holder lane parent session]
   (run-permission holder lane parent session :see-and-act nil))
  ([holder lane parent session allows]
   (run-permission holder lane parent session allows nil))
  ([holder lane parent session allows opts]
   (let [r-allows (cond
                    (map? allows) allows
                    (= :see allows) {:see true}
                    :else {:see true :write :all})
         parent-id (permission-id parent)
         parent-holder (get (permission-value parent) :holder)
         given-by (or (:given-by opts) parent-holder)
         lent-id (if (contains? opts :lent-from) (permission-id (:lent-from opts)) parent-id)
         val (cond-> {:holder holder
                      :allows r-allows
                      :covers [:default-group (:granted-in opts lane)]
                      :session session
                      :given-by given-by}
               (some? parent-id) (assoc :under parent-id)
               (some? lent-id) (assoc :lent-from lent-id))
         perm-id (or (:id opts)
                     (id-from-text (str "softland/runtime-run-permission/" (pr-str [val (:salt opts)]))))]
     {:id perm-id :value val})))

;; ---------------------------------------------------- act slot & store facts

(defn permission-fact
  "The fact map that grants `permission` (`CONTRACT.md` §2.3).
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
