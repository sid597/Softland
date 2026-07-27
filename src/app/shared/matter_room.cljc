(ns app.shared.matter-room
  "matter-room P1 — deterministic addresses for facet-master rooms.

   The JVM derives room ids. CLJS consumes the served `:portal/room` mapping and
   must never mint an address independently (matter-room R2-7)."
  (:require [app.shared.facet-masters :as facet-masters])
  #?(:clj
     (:import [java.nio.charset StandardCharsets]
              [java.util UUID])))

(def registered-master-ids
  "The finite registry input from which both directions are derived."
  facet-masters/master-ids)

(defn room-id
  "A UUID-shaped, byte-stable conversation id derived from exactly the UTF-8
   bytes of `master-id` with JDK nameUUIDFromBytes (UUID v3 / MD5)."
  [master-id]
  #?(:clj
     (str (UUID/nameUUIDFromBytes
           (.getBytes (str master-id) StandardCharsets/UTF_8)))
     :cljs
     (throw
      (ex-info "Room ids are server-derived; read :portal/room"
               {:master-id master-id
                :type :matter-room/client-derivation-forbidden}))))

(def room-id-by-master
  "The finite registered master -> room table. Unknown anchors are still total
   and serve their directly-derived mapping, but are not added to this registry."
  #?(:clj
     (into (sorted-map)
           (map (fn [master-id] [master-id (room-id master-id)]))
           registered-master-ids)
     :cljs {}))

(def master-id-by-room
  "The server-authoritative reverse table used by the later room briefing lane."
  #?(:clj
     (into (sorted-map)
           (map (fn [[master-id derived-room-id]]
                  [derived-room-id master-id]))
           room-id-by-master)
     :cljs {}))

(defn master-id-for-room
  [room-id]
  (get master-id-by-room (str room-id)))
