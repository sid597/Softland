(ns app.client.workspace.runtime.fonts
  "Font asset loading and font-change watch installation."
  (:require [app.client.workspace.settings-view :refer [font-defaults->settings]]))

(defn load-font-assets
  "Async-load MSDF atlas bitmap + metrics JSON for a font config. Returns a Promise."
  [font-config]
  (let [base-path "/fonts/"
        atlas-url (str base-path (:atlas font-config))
        metrics-url (str base-path (:metrics font-config))]
    (-> (js/Promise.all
          #js [(-> (js/fetch atlas-url) (.then #(.blob %)) (.then #(js/createImageBitmap %)))
               (-> (js/fetch metrics-url) (.then #(.json %)) (.then #(js->clj % :keywordize-keys true)))])
        (.then (fn [assets]
                 {:bitmap (aget assets 0)
                  :atlas (aget assets 1)
                  :id (:id font-config)})))))

(defn install-font-watch!
  "Watch !active-font for id changes; apply defaults and async-load new font assets."
  [{:keys [!active-font !font-manifest !font-assets !settings]}]
  (add-watch !active-font :font-loader
    (fn [_ _ old-val new-val]
      (when (not= (:id old-val) (:id new-val))
        (let [manifest @!font-manifest
              font-config (first (filter #(= (:id %) (:id new-val)) (:fonts manifest)))]
          (js/console.log "[FONT] Loading font:" (:id new-val) font-config)
          (when font-config
            (swap! !settings assoc :font-id (:id font-config))
            (when-let [defaults (font-defaults->settings font-config)]
              (swap! !settings merge defaults)))
          (when (and font-config (:atlas font-config))
            (-> (load-font-assets font-config)
                (.then (fn [assets]
                         (js/console.log "[FONT] Loaded assets for:" (:id new-val))
                         (reset! !font-assets assets)))
                (.catch (fn [err]
                          (js/console.error "[FONT] Failed to load:" err))))))))))
