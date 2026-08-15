(ns app.client.workspace.agent
  "Agent streaming: SSE stream management (stream-agent-run!)."
  (:require [clojure.string :as str]
            [cljs.reader :as reader]))

(defn stream-agent-run!
  "Streaming fetch: POST to url, read SSE events via ReadableStream.
   Calls (on-event edn-map) for each parsed SSE event.
   Calls (on-error err) on failure. Returns nil."
  [url body on-event on-error]
  (-> (js/fetch url
                (clj->js {:method "POST"
                          :headers {"Content-Type" "application/edn"}
                          :body (pr-str body)}))
      (.then
        (fn [resp]
          (if-not (.-ok resp)
            (on-error (js/Error. (str "HTTP " (.-status resp))))
            (let [rdr    (.getReader (.-body resp))
                  !buf   (atom "")]
              (letfn [(pump []
                        (-> (.read rdr)
                            (.then
                              (fn [result]
                                (if (.-done result)
                                  ;; Stream ended — flush any remaining buffer
                                  (let [remaining @!buf]
                                    (when (seq remaining)
                                      (doseq [chunk (str/split remaining #"\n\n")]
                                        (let [trimmed (str/trim chunk)]
                                          (when (str/starts-with? trimmed "data: ")
                                            (try
                                              (on-event (reader/read-string (subs trimmed 6)))
                                              (catch :default _ nil)))))))
                                  ;; Got a chunk — decode + split on SSE boundary
                                  (let [text  (.decode (js/TextDecoder.) (.-value result))
                                        buf   (swap! !buf str text)
                                        parts (str/split buf #"\n\n" -1)]
                                    ;; All parts except the last are complete events
                                    (reset! !buf (peek parts))
                                    (doseq [part (pop parts)]
                                      (let [trimmed (str/trim part)]
                                        (when (str/starts-with? trimmed "data: ")
                                          (try
                                            (on-event (reader/read-string (subs trimmed 6)))
                                            (catch :default _ nil)))))
                                    (pump)))))
                            (.catch (fn [e] (on-error e)))))]
                (pump))))))
      (.catch (fn [e] (on-error e))))
  nil)


