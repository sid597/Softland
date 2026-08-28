(ns app.server.page.block-edit
  "The server entry point for one :object/edit request.
   Takes: an object-container runtime and an edit request map.
   Gives: the accepted or rejected edit decision.
   Holds nothing."
  (:require [app.server.rama.object-container :as oc]
            [app.server.rama.object-container.runtime :as ocr]
            [app.server.rama.ingest-epoch :as ingest-epoch]))

(defn submit-block-edit!
  "Append one object edit through the existing ObjectContainer stream and return
   its durable accepted/rejected decision as a plain map."
  ([oc-rt env] (submit-block-edit! oc-rt env ingest-epoch/!ingest-epoch-atom))
  ([oc-rt env !epoch]
   (let [{:keys [request-id idempotency-key edit-client-id edit-seq actor time-ms
                 target payload]} env
         {:keys [document-container-id object-key content-text]} payload
         content-text (str content-text)
         request (oc/object-edit-request
                  (:target/kind target)
                  (:target/id target)
                  content-text
                  {:object-key object-key
                   :document-container-id document-container-id
                   :content-hash (oc/source-hash content-text)
                   :request-id request-id
                   :idempotency-key idempotency-key
                   :edit-client-id edit-client-id
                   :edit-seq edit-seq
                   :actor actor
                   :time-ms time-ms})
         already-decided? (some? (ocr/read-decision oc-rt request))]
     (ocr/append-block-edit-request-durably! oc-rt request :ack)
     (let [decision (ocr/read-decision oc-rt request)
           accepted? (oc/decision-accepted? decision)
           replay? (or already-decided?
                       (some? (:replayed-from-decision-id decision)))]
       (when (and accepted? (not replay?))
         (swap! !epoch inc))
       {:accepted? accepted?
        :replay? replay?
        :reason (:reason decision)
        :errors (:errors decision)
        :status (:status decision)
        :request-id request-id
        :object-key object-key
        :target-id (:target/id target)}))))
