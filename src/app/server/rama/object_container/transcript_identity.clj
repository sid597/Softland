(ns app.server.rama.object-container.transcript-identity
  "Pure identity construction shared by transcript adapters and operational tracking.
   Source family, native conversation/message/tool keys, file identity, byte offset,
   and line hash produce common object/container IDs and source-line keys. No files
   are read and no state is owned. These IDs belong to the common object-container
   path, distinct from the standalone transcript-ingest module's tc:* IDs."
  (:require [app.server.rama.envelope :as envelope]))

(def transcript-source-line-complete-statuses #{:import-complete :parse-error-complete})

(defn transcript-object-key
  "Hash source family and native conversation-id into a chat:<sha256> object key."
  [source conversation-id]
  (str "chat:" (envelope/sha-256 (str (name source) ":" conversation-id))))

(defn transcript-source-id
  "Build a transcript source ID from object-key and the hash of source-line-key."
  [object-key source-line-key]
  (str "src:tr:" object-key ":" (envelope/sha-256 source-line-key)))

(defn chat-conversation-id
  "Prefix object-key with oc:chat-conversation: for the common conversation container."
  [object-key]
  (str "oc:chat-conversation:" object-key))

(defn chat-message-id
  "Build the common message container ID from object-key and message-key."
  [object-key message-key]
  (str "oc:chat-message:" object-key ":" message-key))

(defn tool-call-id
  "Build the common tool-call container ID from object-key and tool-use-key."
  [object-key tool-use-key]
  (str "oc:tool-call:" object-key ":" tool-use-key))

(defn tool-result-id
  "Build the common tool-result container ID from object-key and result-key."
  [object-key result-key]
  (str "oc:tool-result:" object-key ":" result-key))

(defn transcript-source-file-key
  "Use an explicit :source/file-key, otherwise combine source family with pr-str
   file-id. The map arity returns nil without either an explicit key or file-id."
  ([m]
   (or (:source/file-key m)
       (when-let [file-id (:source/file-id m)]
         (transcript-source-file-key (:transcript/source m) file-id))))
  ([source file-id]
   (str (name source) ":" (pr-str file-id))))

(defn transcript-source-line-key
  "Combine the observation source family/file-id, byte offset, and line hash
   into a physical source-line identity; does not read the file."
  [obs]
  (str (transcript-source-file-key (:transcript/source obs) (:source/file-id obs))
       ":" (:source/byte-offset obs)
       ":" (:source/line-hash obs)))

(defn transcript-file-generation-key
  "Prefer explicit generation keys; otherwise hash file-id and path beneath
   the file key. This fallback is identity metadata, not a content/stat freshness check."
  [m]
  (or (:source/file-generation-key m)
      (:source/file-generation m)
      (when-let [file-key (transcript-source-file-key m)]
        (str file-key ":" (envelope/sha-256 (pr-str {:file-id (:source/file-id m)
                                                 :path (:source/file-path m)}))))))
