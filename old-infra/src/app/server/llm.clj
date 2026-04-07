(ns app.server.llm
  (:require  [app.server.env :as env :refer [oai-key]]
             [wkok.openai-clojure.api :as api]
             [clojure.core.async :as a :refer [<! >! go]]))


(def !nodes (atom {}))

(defn chat-complete [{:keys [messages render-uid]}]
  (println "render uid " render-uid)
  (let [events (api/create-chat-completion
                 {:model "gpt-3.5-turbo"
                  :messages messages
                  :stream true}
                 {:api-key oai-key})]

    (go
      (loop []
        (let [event (<! events)]
          (when (not= :done event)
            (let [res (-> event
                        :choices
                        first
                        :delta
                        :content)
                  cur-val (-> @!nodes
                            render-uid
                            :text)]
              ;(println "res" res cur-val)
              (swap!
                !nodes
                update-in
                [render-uid :text]
                (constantly
                  (str
                   cur-val
                   res))))

            (recur)))))))
