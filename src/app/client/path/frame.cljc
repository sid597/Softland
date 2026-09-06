(ns app.client.path.frame
  "Decide what a path frame depends on, level by level.

   Input: ordered draw items ({:path/material record :container group-id})
   and the view ({:zoom :pan}). Output: pure keys. No retained state.

   Four things change at four rates, and each has its own key:
   - the run (path and regions): the values a construction read, kept by
     the renderer from the executor's report and compared by
     component/rerun?; this file only says which view fields to carry;
   - the pack: a region's content and the scale bucket;
   - the instance row: the pack's slot, the cover, the colour, the group;
   - the frame: the early-out over all items, which carries the exact
     scale only when some stroke width is in device pixels and the pan only
     when some record snaps, because those are the declarations that make
     a run read the view.

   Folder map: README.md."
  (:require [app.client.path.pack :as pack]))

(defn scale-of
  "View and a group's scale → device pixels per local unit."
  [view group-scale]
  (* (or (:zoom view) 1.0) (or group-scale 1.0)))

(defn device-width?
  "Record → true when its stroke width is declared in device pixels."
  [record]
  (= :device (get-in record [:path/paint :stroke :unit])))

(defn item-view
  "View, record, group scale → the view a record's construction sees: the
   scale it runs at and the pan, both in device pixels."
  [view record group-scale]
  {:scale (scale-of view group-scale)
   :pan (or (:pan view) [0.0 0.0])})

(defn frame-key
  "Draw items and view → [[material-id revision container] ...] plus the
   scale bucket, the exact scale when any width is in device pixels, and
   the pan when any record snaps. Equal keys mean the frame's inputs are
   unchanged at every level."
  [draw-items view]
  (let [draw-items (or draw-items [])
        records (map :path/material draw-items)
        scale (scale-of view 1.0)]
    [(mapv (fn [item]
             (let [record (:path/material item)]
               [(:path/material-id record) (:path/revision record) (:container item)]))
           draw-items)
     (pack/scale-bucket scale)
     (when (some device-width? records) scale)
     (when (some :path/snap? records) (:pan view))]))

(defn region-key
  "Region → the key its pack is cached under, before the bucket: the
   outline's content and the rule."
  [region]
  [(hash (:path region)) (:rule region)])

(defn pack-key
  "Region and scale bucket → the pack cache key."
  [region bucket]
  [(region-key region) bucket])
