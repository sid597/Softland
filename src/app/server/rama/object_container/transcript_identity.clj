(ns app.server.rama.object-container.transcript-identity
  "Deterministic identities for transcript conversations, sources, tools, and audits.
   Takes: transcript source names, session ids, file paths, line numbers, and event data.
   Gives: object keys, container ids, source ids, tool-call ids, and completion statuses.
   Holds nothing."
  (:require [app.server.rama.core :as core]))

(def transcript-source-line-complete-statuses #{:import-complete :parse-error-complete})

(defn transcript-object-key
  [source conversation-id]
  (str "chat:" (core/sha-256 (str (name source) ":" conversation-id))))

(defn transcript-source-id
  [object-key source-line-key]
  (str "src:tr:" object-key ":" (core/sha-256 source-line-key)))

(defn chat-conversation-id
  [object-key]
  (str "oc:chat-conversation:" object-key))

(defn chat-message-id
  [object-key message-key]
  (str "oc:chat-message:" object-key ":" message-key))

(defn tool-call-id
  [object-key tool-use-key]
  (str "oc:tool-call:" object-key ":" tool-use-key))

(defn tool-result-id
  [object-key result-key]
  (str "oc:tool-result:" object-key ":" result-key))

(defn transcript-source-file-key
  ([m]
   (or (:source/file-key m)
       (when-let [file-id (:source/file-id m)]
         (transcript-source-file-key (:transcript/source m) file-id))))
  ([source file-id]
   (str (name source) ":" (pr-str file-id))))

(defn transcript-source-line-key
  [obs]
  (str (transcript-source-file-key (:transcript/source obs) (:source/file-id obs))
       ":" (:source/byte-offset obs)
       ":" (:source/line-hash obs)))

(defn transcript-file-generation-key
  [m]
  (or (:source/file-generation-key m)
      (:source/file-generation m)
      (when-let [file-key (transcript-source-file-key m)]
        (str file-key ":" (core/sha-256 (pr-str {:file-id (:source/file-id m)
                                                 :path (:source/file-path m)}))))))
