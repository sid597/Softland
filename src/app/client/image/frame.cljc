(ns app.client.image.frame
  "Key the image frame by content and residency changes.

   Input: ordered image items plus residency revision. Output: a pure key.
   It owns no state.

   Folder map: README.md.")

(defn frame-key
  "Items and residency revision → ordered component-ID/revision/container
   rows plus residency revision.

   Encodes declared invalidation dependencies. Trusts component revisions
   and stable compact indexes; raw component fields and index values are
   absent."
  [draw-items residency-rev]
  [(mapv (fn [draw-item]
           (let [component (:image/component draw-item)]
             [(:image/component-id component)
              (:image/revision component)
              (:container draw-item)]))
         (or draw-items []))
   residency-rev])
