(ns app.server.page.block-edit
  "Submit one object edit and summarize the stored decision.
   Borrow an ObjectContainer runtime and accept the transport envelope carrying
   target, content, scope and request identity. Build an :object/edit request,
   append through the runtime's durable edit helper, then read its decision.
   The helper may also write the runtime-configured edit log before the depot.
   Increment the supplied process-local ingest epoch only for an accepted edit
   not recognized as a replay. Rama owns the decision and materialized content;
   this wrapper acquires no runtime and owns no durable state."
  (:require [app.server.rama.object-container :as oc]
            [app.server.rama.object-container.runtime :as ocr]
            [app.server.rama.ingest-epoch :as ingest-epoch]))

(defn submit-block-edit!
  "Submit transport envelope `env` through the ObjectContainer edit stream.
   Payload carries content-text, object-key and document-container-id; target
   and request/edit identities are copied into the kernel request. Content is
   stringified. Read the decision before and after the acknowledged append,
   returning accepted?, replay?, reason/errors/status and correlation fields.

   The optional !epoch atom defaults to ingest-epoch/!ingest-epoch-atom and is
   incremented only for accepted, non-replay results. This is an invalidation
   hint, not durable truth or an exactly-once notification. Exceptions propagate;
   a missing read-back decision is not retried here and yields nil status."
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
