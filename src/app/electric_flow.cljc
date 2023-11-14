(ns app.electric-flow
  (:require contrib.str
            #?(:cljs [clojure.string :as str])
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [app.data :as data]
            [hyperfiddle.electric-ui4 :as ui]))


#?(:clj (def node (atom "GM")))
(e/def nod (e/server (e/watch node)))

#?(:clj (def !msgs (atom [1 2 3])))

(e/def msgs (e/server (e/watch !msgs)))
(e/def nodes (e/server (e/watch data/!nodes)))

(e/defn TwoClocks []
  (println "nod " nod msgs)
  (e/client
      (dom/div (dom/text "TXT "
                 (e/server nod)))
    (e/server
      (e/for-by identity [msg msgs]
        (e/client
         (dom/div (dom/text msg)))))
    (e/server
      (e/for-by identity [node nodes]
        (e/client
          (dom/div (dom/text node)))))))

(e/defn main []
    (TwoClocks.))


