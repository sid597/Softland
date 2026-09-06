(ns app.client.path.frame
  "Decide what a path frame depends on, level by level.

   Input: ordered draw items ({:path/material record :container group-id}),
   the view ({:zoom :pan}) and the caller's world transforms (group id →
   {:affine :flags :buffer-index}). Output: pure keys and the view a
   record's construction sees. No retained state.

   Four things change at four rates, and each has its own key:
   - the run (path and regions): the values a construction read, kept by
     the renderer from the executor's report and compared by
     component/rerun?; this file only derives the view those reads see;
   - the pack: a region's content, and the scale bucket only when the
     region has cubics to lower;
   - the instance row: the pack's slot, the cover, the colour, the group;
   - the frame: the early-out over all items, one entry per item carrying
     its identity, its group's slot, its scale bucket, and the exact scale
     only when its stroke width is in device pixels and the pan only when
     it snaps, because those are the declarations that make a run read the
     view. A group's translation is not in the key: the group buffer moves
     the picture. A group's scale is, through the bucket and the item's
     scale.

   Folder map: README.md."
  (:require [app.client.engine.transform :as transform]
            [app.client.path.pack :as pack]))

(defn device-width?
  "Record → true when its stroke width is declared in device pixels."
  [record]
  (= :device (get-in record [:path/paint :stroke :unit])))

(defn group-scale
  "World transform → the larger axis scale of its affine, 1 for none."
  [{:keys [affine]}]
  (let [[a b c d] (or affine transform/identity-affine)]
    (max (Math/hypot a b) (Math/hypot c d))))

(defn item-view
  "View and a group's world transform → the view a record's construction
   sees: device pixels per local unit and the device offset of the group's
   origin. A screen-space group ignores the camera."
  [view {:keys [affine flags] :as world-transform}]
  (let [[_ _ _ _ tx ty] (or affine transform/identity-affine)
        screen? (= 1 flags)
        zoom (if screen? 1.0 (or (:zoom view) 1.0))
        [px py] (if screen? [0.0 0.0] (or (:pan view) [0.0 0.0]))]
    {:scale (* zoom (group-scale world-transform))
     :pan [(+ (* tx zoom) px) (+ (* ty zoom) py)]}))

(defn item-key
  "Draw item, view, world transforms → [material-id revision container
   buffer-index bucket scale-or-nil pan-or-nil]: what this item's frame
   entry depends on. The scale is carried only for a device-unit width and
   the pan only for a snapped record."
  [item view world-transforms]
  (let [record (:path/material item)
        wt (get world-transforms (:container item))
        {:keys [scale pan]} (item-view view wt)]
    [(:path/material-id record) (:path/revision record) (:container item) (:buffer-index wt)
     (pack/scale-bucket scale)
     (when (device-width? record) scale)
     (when (:path/snap? record) pan)]))

(defn frame-key
  "Draw items, view, world transforms → one item-key per item, in order.
   Equal keys mean the frame's inputs are unchanged at every level."
  [draw-items view world-transforms]
  (mapv (fn [item] (item-key item view world-transforms)) (or draw-items [])))

(defn region-key
  "Region → the key its packs are cached under: the outline's content and
   the rule. The bucket sits inside the entry, because a region without
   cubics packs once for every bucket."
  [region]
  [(hash (:path region)) (:rule region)])
