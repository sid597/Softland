(ns app.client.shapes.line
  (:require [hyperfiddle.electric-svg :as svg]
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]))


(e/defn line [[k {:keys [id color to from]}]]
  (e/client
    (let [tw (subscribe. [ to :width])
          th (subscribe. [ to :height])
          fw (subscribe. [ from :width])
          fh (subscribe. [ from :height])
          tx (subscribe. [ to :x])
          ty (subscribe. [ to :y])
          fx (subscribe. [ from :x])
          fy (subscribe. [ from :y])]
      (svg/line
        (dom/props {:style {:z-index -1}
                    :id id
                    :x1  (if  tw
                           (+  tx) (/ tw 2)
                           tx)
                    :y1  (if th
                           (+ ty (/ th 2))
                           ty)
                    :x2  (if fw
                           (+ fx (/ fw 2))
                           fx)
                    :y2  (if fh
                           (+ fy (/ fh 2))
                           fy)
                    :stroke color
                    :stroke-width 4})))))
