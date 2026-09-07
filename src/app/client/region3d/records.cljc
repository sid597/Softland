(ns app.client.region3d.records
  "The definer's sphere, retained bindings and tool records as fixtures.
   Takes no runtime input. Gives ordinary records to JVM and browser callers;
   production-authored tools use the same grammar and remain data above the
   waist. Holds immutable fixture values. Evidence: brush_test.clj."
  (:require [app.client.region3d.support :as support]))

(def host
  (let [R 200.0 piR (* Math/PI R)
        phi {:id "φ" :from :S0 :to :S1 :map [0.5 0 0 1 -100 0] :work 0}
        chi {:id "χ" :from :S1 :to :S2 :map [1 0 0.25 1 60 0] :work 0}
        beta {:id "β" :from :B0 :to :S0 :map [1 0 0 1 piR 200] :work 0}]
    {:kind :sphere :id "G" :R R :piR piR :revisions [0 1 2] :record-keys [:A :B]
     :charts [{:id :S0 :revision 0 :period (* 2 piR) :to-root [1 0 0 1 0 0]}
              {:id :S1 :revision 1 :period piR :to-root [2 0 0 1 200 0]}
              {:id :S2 :revision 2 :period piR :to-root [2 0 -0.5 1 80 0]}]
     :A {:id "A@0" :mark "kA" :kind :arc :curve "kA/arc-AB" :seed [Math/PI (/ Math/PI 3)]
         :tangent [0 0 -1] :length 80.0 :radius 16.0 :rgba [0.5 0 0 0.5]
         :domain [(- piR 160) 120 (+ piR 160) 290]}
     :B {:id "B@0" :mark "kB" :kind :tap :on-curve "kA/arc-AB" :at 0.65 :radius 24.0
         :rgba [0 0 0.5 0.5] :domain [-10 -80 200 90]}
     :bindings
     {0 [{:id "binding-A" :record :A :root :S0 :chain []}
         {:id "binding-B" :record :B :root :B0 :chain [beta]}]
      1 [{:id "binding-A" :record :A :root :S0 :chain [phi]}
         {:id "binding-B" :record :B :root :B0 :chain [beta phi]}]
      2 [{:id "binding-A" :record :A :root :S0 :chain [phi chi]}
         {:id "binding-B" :record :B :root :B0 :chain [beta phi (assoc chi :work 1)]}]}}))

(defn program-record "Authored fixture → executor record; identity and prose are not roots."
  [record] {:program (:program record) :roots (dissoc record :id :what :program)})

(defn- step [out op args]
  {:out out :op op :args (cond-> args
                         (contains? #{:curve-point :surface-region :read-surface :paint :sample} op)
                         (assoc :host [:get :host]))})

(defn- painting [id width height rect]
  {:surface/id id :width width :height height :domain {:kind :chart :chart :S0 :rect rect}
   :color :linear-premultiplied-rgba :filter :nearest :initial [0 0 0 0]})

(def reach
  {:id "reach@0" :support {:id "G" :revision 0}
   :tool {:seed [Math/PI (/ Math/PI 3)] :radius 150 :distance :surface}
   :program {:steps [(step :region :surface-region {:support [:get :support] :seed [:get :tool :seed]
                                                   :radius [:get :tool :radius] :distance [:get :tool :distance]})]
             :return {:region [:get :region]}}})

(def sequence-record
  {:id "sequence@0" :support {:id "G"} :coating {:layers ["A@0" "B@0"] :order ["kA" "kB"]}
   :tool {:curve "kA/arc-AB" :t 0.65}
   :events [{:id "G@0" :revision 0} {:id "G@1" :revision 1} {:id "G@2" :revision 2}]
   :program {:each {:items [:get :events] :item :event :fields [:revision] :state {:last nil}
                    :steps [(step :at :curve-point {:curve [:get :tool :curve] :t [:get :tool :t]})
                            (step :read :read-surface {:support [:get :support] :revision [:get :event :revision]
                                                      :point [:get :at] :layers ["coating"] :order [:get :coating :order]})]
                    :next {:last [:get :read]}}
             :return {:last [:get :state :last]}}})

(def pickup
  {:id "coating-pickup@0" :support {:id "G" :revision 2}
   :coating {:layers ["A@0" "B@0"] :bindings ["binding-A@2" "binding-B@2"] :order ["kA" "kB"]
             :interpretation "linear-light premultiplied rgba; no lighting, no screen lease"}
   :painting (painting "pickup-G" 64 32 [588.3185307179587 194 80 30])
   :tool {:carry [1 0 0 1] :pickup 0.25 :opacity 0.5 :radius 8.75}
   :events (mapv (fn [i t] {:id (str "dab-" i) :curve "kA/arc-AB" :t t}) (range 4) [0.65 0.7 0.41 0.65])
   :program
   {:steps [(step :fresh :surface/new {:declaration [:get :painting]})]
    :each {:items [:get :events] :item :event :fields [:curve :t]
           :state {:carry [:get :tool :carry] :painting [:get :fresh]}
           :steps [(step :at :curve-point {:curve [:get :event :curve] :t [:get :event :t]})
                   (step :picked :read-surface {:support [:get :support] :point [:get :at]
                                                :layers ["coating" [:get :state :painting]]
                                                :order [:get :coating :order] :filter [:get :painting :filter]})
                   (step :carry :mix {:a [:get :state :carry] :b [:get :picked :color] :amount [:get :tool :pickup]})
                   (step :footprint :surface-region {:support [:get :support] :point [:get :at]
                                                     :radius [:get :tool :radius] :distance :surface})
                   (step :painted :paint {:surface [:get :state :painting] :region [:get :footprint]
                                          :rgba [:get :carry] :opacity [:get :tool :opacity] :blend :source-over})]
           :next {:carry [:get :carry] :painting [:get :painted]}}
    :return {:painting [:get :state :painting] :carry [:get :state :carry]}}})

(def clip-record
  {:id "clip-dab@0" :support {:id "G" :revision 0}
   :inputs {:clip {:kind :surface-region :from {:record "reach@0" :output :region}}}
   :painting (painting "clip-G" 64 32 [900 180 100 60])
   :tool {:seed [(* 1.5 Math/PI) (/ Math/PI 3)] :radius 4 :rgba [0.5 0 0 0.5] :opacity 1}
   :events [{:id "dab-q1"}]
   :program {:steps [(step :fresh :surface/new {:declaration [:get :painting]})]
             :each {:items [:get :events] :item :event :fields [] :state {:painting [:get :fresh]}
                    :steps [(step :footprint :surface-region {:support [:get :support] :seed [:get :tool :seed]
                                                              :radius [:get :tool :radius] :distance :surface})
                            (step :painted :paint {:surface [:get :state :painting] :region [:get :footprint]
                                                   :rgba [:get :tool :rgba] :opacity [:get :tool :opacity]
                                                   :clips [[:get :inputs :clip]]})]
                    :next {:painting [:get :painted]}}
             :return {:painting [:get :state :painting]}}})

(def cold-reach reach)
(def cold-dab
  {:id "reach-dab@0" :support {:id "G" :revision 0}
   :inputs {:clip {:kind :surface-region :from {:record "reach@0" :output :region}}}
   :painting (painting "reach-paint" 128 64 [308.3185307179587 50 640 250])
   :tool {:curve "kA/arc-AB" :t 0.65 :radius 200 :rgba [0 0.5 0 0.5] :opacity 1}
   :program {:steps [(step :fresh :surface/new {:declaration [:get :painting]})
                     (step :at :curve-point {:curve [:get :tool :curve] :t [:get :tool :t]})
                     (step :disc :surface-region {:support [:get :support] :point [:get :at] :radius [:get :tool :radius] :distance :surface})
                     (step :painted :paint {:surface [:get :fresh] :region [:get :disc]
                                            :rgba [:get :tool :rgba] :opacity [:get :tool :opacity] :clips [[:get :inputs :clip]]})]
             :return {:painting [:get :painted]}}})
