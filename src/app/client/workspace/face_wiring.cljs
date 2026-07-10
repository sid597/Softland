(ns app.client.workspace.face-wiring
  "Faces-as-assemblies · the client glue for the ONE generic face artery (CONTRACT §7).

   Sibling of trail_face/wiring.cljs, same S2 exemption, same seam law (S1): this file
   touches NO server names — the Electric pull happens in electric_flow/file_viewer;
   this glue only moves data between client atoms. It derives the request the server
   watches, and mirrors the whole data-context back into ONE client atom.

   No face-keyword dispatch anywhere on the client (trap T8): the request carries the
   face name and the server projection registry routes it. The Electric surface is
   generic — every face reuses THIS glue and THIS request/data pair; nothing here grows
   per face."
  (:require [clojure.string :as string]
            [cljs.reader :as reader]
            [app.client.workspace.face-assembly :as fa]
            [app.client.workspace.face-primitives :as prims]))

(defn parse-face-command
  "\"/face ...\" command text -> a face-state op, or nil when it is not a face
   command (the parse-trail-command shape). Forms:
     /face <name> [<address-edn>]  -> {:op :set :state {:face <kw>
                                       :address <addr|:default> :params {:limit 64}}}
     /face until <ms>|off          -> {:op :until :until-ms <n|nil>}
                                      (the :until-ms replay/scrub cut, CONTRACT §7)
     /face off                     -> {:op :off}"
  [s read-edn]
  (let [s (string/trim (or s ""))
        s (if (string/starts-with? s "/") (subs s 1) s)]
    (when (or (= s "face") (string/starts-with? s "face "))
      (let [rest-s (string/trim (subs s 4))
            i (string/index-of rest-s " ")
            word (if i (subs rest-s 0 i) rest-s)
            tail (if i (string/trim (subs rest-s (inc i))) "")]
        (cond
          (= word "off")
          {:op :off}

          (= word "until")
          (if (= tail "off")
            {:op :until :until-ms nil}
            (let [ms (js/parseInt tail 10)]
              (when-not (js/isNaN ms)
                {:op :until :until-ms ms})))

          (seq word)
          {:op :set :state {:face (keyword word)
                            :address (if (seq tail) (read-edn tail) :default)
                            :params {:limit 64}}}

          :else nil)))))

(defn wear-face!
  "Wear-time compile (the §5 two-stage law, trap T3): fetch the face's assembly
   EDN — a static resource for W1; assemblies become kernel objects in W2
   (CONTRACT §8) — compile ONCE against the real registry, publish the compiled
   builder into :!face-compiled. `compile-assembly` is TOTAL (trap T4): a
   malformed assembly (or a failed fetch, rendered via a root-less envelope)
   compiles to an error-card builder and still renders beside the land —
   never a black screen."
  [atoms face-kw]
  (let [{:keys [!face-compiled !face-state]} atoms
        url (str "/faces/" (name face-kw) ".edn")
        ;; G16 falsification fix: fetches resolve in ANY order — a stale
        ;; resolution (e.g. a typo'd face's 404 landing after the corrected
        ;; face's 200) must not overwrite the CURRENT face's builder with a
        ;; wrong-face compile. Only the still-current face may publish.
        publish! (fn [compiled]
                   (when (= face-kw (:face @!face-state))
                     (reset! !face-compiled compiled)))]
    (-> (js/fetch url)
        (.then (fn [resp]
                 (if (.-ok resp)
                   (.text resp)
                   (throw (js/Error. (str "HTTP " (.-status resp)))))))
        (.then (fn [txt]
                 (let [assembly (reader/read-string txt)
                       compiled (fa/compile-assembly prims/registry assembly)]
                   (when (fa/error? compiled)
                     (js/console.warn "[FACE] assembly compiled with errors"
                                      (clj->js (fa/compile-errors compiled))))
                   (publish! compiled))))
        (.catch (fn [e]
                  (js/console.error "[FACE] assembly fetch FAILED" url e)
                  (publish!
                    (fa/compile-assembly prims/registry
                                         {:assembly/name (name face-kw)
                                          :assembly/grammar 0})))))))

(defn install-face-wiring!
  "Wire the generic face pull loop:
   - !face-state (set by the /face command + address/scrub selection) derives
     !face-request — the CONTRACT §7 value Electric watches and pulls against:
     {:face :address :params {:limit :until-ms} :epoch}.
   - !face-data (Electric pull result) is the ONE data-context atom, consumed whole
     by the §5 flow (the data-context arrives whole — no split, unlike the trail face).
   - remote ingest epoch mirrors into !ingest-epoch and, after a client-side debounce
     >= 1s (INV-19 — the epoch PUSH is the only re-pull trigger; no polling anywhere),
     re-stamps and re-arms the request so the face fills in near-live."
  [atoms {:keys [!face-request !face-data !ingest-epoch-remote]}]
  (let [{:keys [!face-state !ingest-epoch]} atoms
        !last-pull-epoch (atom 0)
        !debounce (atom nil)
        request! (fn []
                   (let [s @!face-state]
                     (reset! !face-request
                             (when (:face s)
                               {:face    (:face s)
                                :address (:address s)
                                :params  (:params s {})
                                :epoch   @!last-pull-epoch}))))]
    ;; face entry, address pick, and scrub (:until-ms in :params) all flow through
    ;; !face-state → request! → !face-request (the value Electric pulls against).
    (add-watch !face-state :face-request
               (fn [_ _ _ _] (request!)))
    ;; the data-context arrives WHOLE (no split, unlike the trail face): mirror
    ;; the Electric pull result into the ONE runtime atom the §5 flow watches
    ;; (the R4 mirror-atom pattern; :!face-context lives in state.cljs).
    (when-let [!face-context (:!face-context atoms)]
      (when !face-data
        (add-watch !face-data :face-data
                   (fn [_ _ _ data]
                     (when data (reset! !face-context data))))))
    (when !ingest-epoch-remote
      (add-watch !ingest-epoch-remote :face-epoch
                 (fn [_ _ _ new-epoch]
                   (when (number? new-epoch)
                     (when !ingest-epoch (reset! !ingest-epoch new-epoch))
                     (when-let [t @!debounce] (js/clearTimeout t))
                     (reset! !debounce
                             (js/setTimeout
                               (fn []
                                 (reset! !last-pull-epoch new-epoch)
                                 (request!))
                               1000))))))
    (request!)))
