(ns app.server.rama.testing
  (:use [com.rpl.rama]
        [com.rpl.rama.path]))


(comment
  (do
    (def ipc
      (let [c (create-ipc)]
        (println "--R--: Start ipc, launch module")
        (reset! !rama-ipc c)
        (launch-module! c node-events-module {:tasks 4 :threads 2})))

    ;; Define clj defs

    (def event-depot (foreign-depot @!rama-ipc (get-module-name node-events-module) "*node-events-depot"))
    (def nodes-pstate (foreign-pstate @!rama-ipc (get-module-name node-events-module) "$$nodes-pstate")))

  (close! @!rama-ipc)

  (foreign-select [] event-id-pstate)

  (foreign-append! event-depot (->node-events
                                 :update-event-id
                                 {}
                                 {}))


  (foreign-append! event-depot (->node-events
                                 :new-node
                                 {:rect11 {:id :rect11
                                           :x 50.99
                                           :y 60.0
                                           :type-specific-data {:text "GM Hello"
                                                                :width 400
                                                                :height 800}
                                           :type "rect"
                                           :fill  "lightblue"}}
                                 {:graph-name :main})
    :append-ack)

  (foreign-select [(keypath :main) ALL FIRST ] nodes-pstate)
  (foreign-select [(keypath :main) ] nodes-pstate)
  (foreign-select-one [(keypath :main) ALL FIRST ] nodes-pstate)

  (foreign-select  ALL  nodes-pstate {:pkey 4})
  (foreign-select  [(keypath :main) ALL]  nodes-pstate {:pkey :rect})
  (foreign-select [:main :6f818232-b41d-405f-9e2f-df2c66c9181f :x] nodes-pstate {:pkey :rect})
  (foreign-select-one [:main ] nodes-pstate {:pkey :rect})
  (foreign-select-one [:main FIRST ] nodes-pstate {:pkey :rect})
  ;(foreign-select-one [:main :cc61a1a9-663e-4d70-9347-bb9571588eb6 FIRST ] nodes-pstate {:pkey :rect})
  (foreign-select-one [:main ALL] nodes-pstate {:pkey :rect})


  (do
    (foreign-append! event-depot (->node-events
                                   :new-node
                                   {:rect3 {:id :rect3
                                            :x 500.032452345
                                            :y 600.989070
                                            :type-specific-data {:text "GM Hello"
                                                                 :width 400
                                                                 :height 800}
                                            :type "rect"
                                            :fill  "lightblue"}}
                                   {:graph-name :main})
      :append-ack)
    (foreign-append! event-depot (->node-events
                                   :new-node
                                   {:rect4 {:id :rect4
                                            :x 500.2
                                            :y 600.3
                                            :type-specific-data {:text "GM Hello"
                                                                 :width 400
                                                                 :height 800}
                                            :type "rect"
                                            :fill  "lightblue"}}
                                   {:graph-name :level-1})
      :append-ack)
    (foreign-append! event-depot (->node-events
                                   :new-node
                                   {:rect5 {:id :rect5
                                            :x 500.44
                                            :y 600.223
                                            :type-specific-data {:text "GM Hello"
                                                                 :width 400
                                                                 :height 800}
                                            :type "rect"
                                            :fill  "lightblue"}}
                                   {:graph-name :level-1})
      :append-ack))
  foreign-select (keypath ["main" ALL]) nodes-pstate {:pkey :rect})
