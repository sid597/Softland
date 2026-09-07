(ns app.client.path.frame
  "Derive complete input values for path preparation.
   Takes path-value draw items, view and world transforms. Gives keys and
   explicit projected scale/fractional pan. Holds no state. Screen groups
   ignore the world camera. Evidence: frame_test.clj and the browser's
   production border and snapping checks."
  (:require [app.client.engine.transform :as transform]
            [app.client.path.pack :as pack]
            [app.client.path.component :as component]))

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
  "View and a group's world transform → geometry's declared view: device pixels per local unit and the device offset of the group's
   origin. A screen-space group ignores the camera."
  [view {:keys [affine flags] :as world-transform}]
  (let [[_ _ _ _ tx ty] (or affine transform/identity-affine)
        screen? (= 1 flags)
        zoom (if screen? 1.0 (or (:zoom view) 1.0))
        [px py] (if screen? [0.0 0.0] (or (:pan view) [0.0 0.0]))]
    {:scale (* zoom (group-scale world-transform))
     :pan-fraction (mapv #(- % (Math/floor %)) [(+ (* tx zoom) px) (+ (* ty zoom) py)])}))

(defn item-key
  "Draw item, view and world transforms → complete preparation input.
   Source records and recipes never enter this key. Integer device pans
   leave snapped geometry unchanged; the group buffer moves the picture."
  [item view world-transforms]
  (let [record (:path/material item)
        wt (get world-transforms (:container item))
        iv (item-view view wt)]
    [(:path/material-id record) (:path/revision record) (:container item) (:buffer-index wt)
     (component/geometry-inputs record iv) (:path/paint record)
     (pack/scale-bucket (:scale iv))]))

(defn region-key
  "Region → the key its packs are cached under: the outline's content and
   the rule. The bucket sits inside the entry, because a region without
   cubics packs once for every bucket."
  [region]
  [(:path region) (:rule region)])
