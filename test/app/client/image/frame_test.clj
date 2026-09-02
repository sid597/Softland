(ns app.client.image.frame-test
  (:require [clojure.test :refer [deftest is]]
            [app.client.image.frame :as frame]))

(deftest i3-revision-group-residency-key-tripwire
  (let [draw-items [{:image/component {:image/component-id :image/a
                               :image/revision :rev-1}
              :container 0}
             {:image/component {:image/component-id :image/b
                               :image/revision :rev-4}
              :container 17}]
        key (frame/frame-key draw-items 8)]
    (is (= [[[:image/a :rev-1 0]
             [:image/b :rev-4 17]]
            8]
           key))
    (is (not= key
              (frame/frame-key
               [(update-in (first draw-items) [:image/component :image/revision]
                           (constantly :rev-2))
                (second draw-items)]
               8)))
    (is (not= key (frame/frame-key draw-items 9)))
    (is (= key
           (frame/frame-key
            [(assoc-in (first draw-items) [:image/component :image/provenance]
                       {:changed-without-revision true})
             (second draw-items)]
            8)))))
