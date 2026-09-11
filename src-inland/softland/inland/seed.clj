(ns softland.inland.seed
  "Explicit development genesis refresh. Takes the checked-in seed. Gives
   admitted seed updates only where a person has not edited the accepted row."
  (:require [softland.inland.module :as module]
            [softland.inland.store :as store]
            [softland.inland.total :as total]
            [clojure.string :as str]))
(defn -main [& _]
  (store/connect!)
  (store/ensure-workspace! "workbench")
  (doseq [row (module/seed-rows)
          :let [current (store/read-one :rows ["workbench" (total/row-key "base" (:name row))])]
          :when (or (nil? current) (#{"seed-v1" "seed-v2"} (:accepted-request current))
                    (str/starts-with? (:accepted-request current "") "genesis/"))]
    (when-not (= row (dissoc current :revision :asserted-by :accepted-request :accepted-at))
      (let [result (store/submit! {:workspace "workbench" :name (:name row) :layer "base" :actor "sid"
                                  :kind :put :row row :expected-revision (:revision current 0)
                                  :request-id (str "genesis/" (:name row) "/" (:revision current 0) "/" (hash row))})]
        (println (:name row) (:status result))
        (when-not (= :accepted (:status result))
          (throw (ex-info "Seed admission failed" (select-keys result [:name :reason :status])))))))
  (System/exit 0))
