(ns softland.inland.seed
  "Explicit development refresh of accepted genesis material.
   Takes the packaged seed and default workbench rows; gives revision-checked put
   admissions only for missing or still-genesis-owned records. Borrows the process
   store connection; owns no maintained world. This is a launcher-invoked refresh,
   not a migration of arbitrary workspaces or an overwrite of user-authored rows."
  (:require [softland.inland.module :as module]
            [softland.inland.store :as store]
            [softland.inland.total :as total]
            [clojure.string :as str]))
(defn -main
  "Packaged seed → accepted updates in workbench, then process exit 0.
   Skips rows whose accepted request is not a known seed/genesis request. Compares
   content without admission metadata and submits expected revisions; rejection
   throws. Deleted seed entries are not removed from existing accepted workspaces."
  [& _]
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
