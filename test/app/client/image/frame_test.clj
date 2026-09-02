(ns app.client.image.frame-test
  (:require [clojure.test :refer [deftest is]]
            [app.client.image.frame :as frame]))

(deftest i3-revision-container-residency-key-tripwire
  (let [ops [{:image/material {:image/material-id :image/a
                               :image/revision :rev-1}
              :container 0}
             {:image/material {:image/material-id :image/b
                               :image/revision :rev-4}
              :container 17}]
        key (frame/frame-key ops 8)]
    (is (= [[[:image/a :rev-1 0]
             [:image/b :rev-4 17]]
            8]
           key))
    (is (not= key
              (frame/frame-key
               [(update-in (first ops) [:image/material :image/revision]
                           (constantly :rev-2))
                (second ops)]
               8)))
    (is (not= key (frame/frame-key ops 9)))
    (is (= key
           (frame/frame-key
            [(assoc-in (first ops) [:image/material :image/provenance]
                       {:changed-without-revision true})
             (second ops)]
            8)))))
