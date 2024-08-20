(ns app.client.shapes.line
  (:require [hyperfiddle.electric-svg :as svg]
            [hyperfiddle.electric :as e]
            [app.client.flow-calc :as fc]
            [hyperfiddle.electric-dom2 :as dom]
            [app.client.utils :refer [ ui-mode edges nodes
                                      is-dragging?  zoom-level last-position subscribe
                                      viewbox  context-menu? reset-global-vals]]
            [app.client.shapes.rect :refer [watch-server-update]]
            #?@(:cljs
                [[global-flow :refer [current-time-ms node-pos-flow]]
                 [missionary.core :as m]]
                :clj [[com.rpl.rama.path :as path :refer [subselect ALL FIRST keypath select MAP-VALS]]
                      [app.server.rama.util-fns :refer [roam-query-request !subscribe get-path-data nodes-pstate
                                                        update-node get-event-id node-ids-pstate
                                                        add-new-node
                                                        dg-node-ids-pstate
                                                        dg-pages-pstate
                                                        dg-nodes-pstate
                                                        dg-edges-pstate]]])))


(defn attributes [x y height width a b]
  (let [xmin x
        ymin y
        xmax (+ x width)
        ymax (+ y height)
        rx (atom nil)
        ry (atom nil)]
    ;(println "xmin" xmin "xmax" xmax "ymin" ymin "ymax" ymax "a " a "b" b)
    (cond
      (<= a xmin) (reset! rx xmin)
      (>= a xmax) (reset! rx xmax)
      :else       (reset! rx a))
    (cond
      (<= b ymin) (reset! ry ymin)
      (>= b ymax) (reset! ry ymax)
      :else       (reset! ry b))
    [@rx @ry]))

(comment
  (attributes 0 0 100 100 -51 51)
  (attributes 0 0 100 100 51 51)
  (attributes 0 0 100 100 -51 -51)
  (attributes 0 0 100 100 51 -51)
  (attributes 0 0 100 100 51 -21))


#?(:cljs (defn edge-update []
           (->> (node-pos-flow)
             (e/throttle 10)
             (m/reductions {} {:id 0
                               :x 0
                               :y 0})
             (m/relieve {})
             (m/latest (fn [new-data]
                         ;(println "----- EDGE NEW DATA -----" new-data)
                         new-data))
             (m/signal))))

(e/defn draw-edges [suid all-nodes]
  (println "draw edges")
  (e/server
    (let [edges (first (get-path-data [(keypath :main) suid] dg-edges-pstate))]
      (when (some? edges)
        (e/for-by identity [edge edges]
          (e/client
            (let [tuid (keyword (-> edge :to :uid))]
              (println "^^^ Drawing edge: "
                suid
                tuid
                (contains? @all-nodes suid)
                (contains? @all-nodes tuid)
                #_@all-nodes)
              (when (and
                      (contains? @all-nodes suid)
                      (contains? @all-nodes tuid))
                (let [to (e/server (first (get-path-data
                                            [(keypath :main) (keyword tuid)]
                                            nodes-pstate)))
                      from (e/server (first (get-path-data
                                              [(keypath :main) (keyword suid)]
                                              nodes-pstate)))
                      tw (int (-> to :type-specific-data :width))
                      th (int (-> to :type-specific-data :height))
                      fw (int (-> from :type-specific-data :width))
                      fh (int (-> from :type-specific-data :height))
                      !tx (atom (-> to :x :pos))
                      tx (int (e/watch !tx))
                      !ty (atom (-> to :y :pos))
                      ty (int (e/watch !ty))
                      !fx (atom (-> from :x :pos))
                      fx (int (e/watch !fx))
                      !fy (atom (-> from :y :pos))
                      fy (int (e/watch !fy))
                      [xx yy] (attributes tx ty th tw fx fy)
                      [fxx fyy] (attributes fx fy fh fw xx yy)]
                  (svg/line
                    (dom/props {:style {:z-index -1}
                                :id (str suid "-" tuid)
                                :x1  xx
                                :y1  yy
                                :x2  fxx
                                :y2  fyy
                                :stroke "black"
                                :stroke-width 0.1})))))))))))


(e/defn line [from to edge]
  (e/client
    (let [tw (int (-> to :type-specific-data :width))
          th (int (-> to :type-specific-data :height))
          fw (int (-> from :type-specific-data :width))
          fh (int (-> from :type-specific-data :height))
          !tx (atom (-> to :x :pos))
          tx (int (e/watch !tx))
          !ty (atom (-> to :y :pos))
          ty (int (e/watch !ty))
          !fx (atom (-> from :x :pos))
          fx (int (e/watch !fx))
          !fy (atom (-> from :y :pos))
          fy (int (e/watch !fy))
          [xx yy] (attributes tx ty th tw fx fy)
          [fxx fyy] (attributes fx fy fh fw xx yy)]
      (println "fxx fyy" fxx fyy xx yy)
      #_(let [sd (new (edge-update))
              nid (:id sd)
              nx  (-> sd :x :pos)
              ny  (-> sd :y :pos)]
          (if (= (:id from) nid)
            (do
              (when (some? nx)
               (reset! !fx nx))
              (when (some? ny)
                (reset! !fy ny)))
            (do
              (when (some? nx)
                (reset! !tx nx))
              (when (some? ny)
                (reset! !ty ny)))))
      (when-not (some zero? [fxx fyy xx yy])
        (svg/line
          (dom/props {:style {:z-index -1}
                      :id (:id edge)
                      :x1  xx #_(if  tw
                                  (+  tx) (/ tw 2)
                                  tx)
                      :y1  yy #_(if th
                                  (+ ty (/ th 2))
                                  ty)
                      :x2  fxx
                      :y2  fyy
                      :stroke "black"
                      :stroke-width 0.1}))))))


#_(e/defn line [[k {:keys [id color to from]}]]
    (println "--->" k id color to from)
    (println "--"[to :y])
    (e/client
      (let [tw (int (subscribe. [ to :type-specific-data :width]))
            th (int (subscribe. [ to :type-specific-data :height]))
            fw (int (subscribe. [ from :type-specific-data :width]))
            fh (int (subscribe. [ from :type-specific-data :height]))
            tx (int (subscribe. [to :x]))
            ty (int (subscribe. [to :y]))
            fx (int (subscribe. [from :x]))
            fy (int (subscribe. [from :y]))
            [xx yy] (attributes tx ty th tw fx fy)
            [fxx fyy] (attributes fx fy fh fw xx yy)]
        (svg/line
          (dom/props {:style {:z-index -1}
                      :id id
                      :x1  xx #_(if  tw
                                  (+  tx) (/ tw 2)
                                  tx)
                      :y1  yy #_(if th
                                  (+ ty (/ th 2))
                                  ty)
                      :x2  fxx
                      :y2  fyy
                      :stroke color
                      :stroke-width 4})))))
