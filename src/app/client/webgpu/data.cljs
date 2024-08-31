(ns app.client.webgpu.data)


(defonce vertices (js/Float32Array.
                    #_(clj->js [-1, -1,
                                1, -1,
                                1,  1,
                                -1, -1,
                                1,  1,
                                -1,  1,])
                    #_(clj->js [-0.5, -0.5,  0.5, -0.5,  0.5, 0.5,
                                -0.5, -0.5,  0.5, 0.5, -0.5, 0.5])
                    (clj->js [-0.8, -0.8,
                              0.8, -0.8,
                              0.8,  0.8,
                              -0.8, -0.8,
                              0.8,  0.8,
                              -0.8,  0.8,])))
(defonce grid-size 16)
(defonce uniform-array (js/Float32Array. (clj->js [grid-size grid-size])))
(def !rects (atom []))