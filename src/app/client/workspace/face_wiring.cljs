(ns app.client.workspace.face-wiring
  "Faces-as-assemblies · the client glue for the ONE generic face artery (CONTRACT §7)
   + the W2 arsenal surfaces (CONTRACT §16).

   Sibling of trail_face/wiring.cljs, same S2 exemption, same seam law (S1): this file
   touches NO server names — the Electric pulls happen in electric_flow/file_viewer;
   this glue only moves data between client atoms. It derives the requests the server
   watches, and mirrors the data-contexts back into client atoms.

   No face-keyword dispatch anywhere on the client (trap T8): requests carry the
   face name and the server projection registry routes them. The Electric surface is
   generic and CONSTANT — the W2 additions (assembly-source pull, face-list pull,
   wear outbox) are one-of-each, not per-face growth.

   W2 wear path (§16): the W1 HTTP fetch of /faces/<name>.edn is RETIRED — the
   assembly source is served FROM RAMA by the :assembly projection and compiled
   client-side with the SAME .cljc compiler the server validates with (trap T18).
   Wear events stream INTO Rama through the outbox → RecordFaceWear (back-arrow;
   trap T15: never through the read artery)."
  (:require [clojure.string :as string]
            [cljs.reader :as reader]
            [app.client.workspace.face-assembly :as fa]
            [app.client.workspace.face-primitives :as prims]
            [app.client.workspace.scene-runtime :as scene-rt]
            [app.shared.material-inspector :as material-inspector]))

(defn parse-face-command
  "\"/face ...\" command text -> a face-state op, or nil when it is not a face
   command (the parse-trail-command shape). Forms:
     /face <name> [<address-edn>]  -> {:op :set :state {:face <kw>
                                       :address <addr|:default> :params {}}}
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
                            ;; no :limit — the server clamps to its own max page;
                            ;; a client-pinned limit silently narrows the window
                            :params {}}}

          :else nil)))))

;; The Electric refs, published by install-face-wiring! so the command path
;; (wear-face!, called from agent_flow with only the runtime atoms map) can
;; reach the request/outbox atoms. Client-glue module state, S2-contained;
;; nil until the wiring installs.
(defonce ^:private !electric-refs (atom nil))

;; Wear-time compile sentinel (trap T13: compare the source VALUE, never a
;; hash). ::none on wear entry so an identical re-wear recompiles (the W1
;; re-entry law).
(defonce ^:private !last-compiled-source (atom ::none))

(defonce ^:private !material-inspector-nonce (atom 0))

(defn- drill-mode?
  []
  (boolean
   (re-find #"[?&]drill=" (str (.-search js/window.location)))))

(defn wear-face!
  "Wear-time entry (the §5 two-stage law, trap T3), W2 form: arm the
   :assembly-source request — the server serves the assembly FROM RAMA
   (:assembly projection; the W1 static-resource fetch is retired) — and
   queue ONE wear event (§16: the wear-id is minted HERE, before the call;
   the SERVER stamps worn-at-ms). The compile happens in the
   !assembly-data watch when the source arrives."
  [atoms face-kw address]
  (let [{:keys [!assembly-request !face-wear-outbox !last-pull-epoch]} @!electric-refs]
    (if (nil? !assembly-request)
      (js/console.error "[FACE] wiring not installed; cannot wear" (name face-kw))
      (do
        (reset! !last-compiled-source ::none)
        (reset! !assembly-request {:face :assembly
                                   :address (name face-kw)
                                   :epoch (if !last-pull-epoch @!last-pull-epoch 0)})
        (when !face-wear-outbox
          (reset! !face-wear-outbox {:wear-id (str (random-uuid))
                                     :face-name (name face-kw)
                                     :wearer "human:local"
                                     :address (pr-str address)}))))))

(defn- compile-served-source!
  "Compile the :assembly data-context into :!face-compiled. Total (trap T4):
   an unreadable/absent source compiles a root-less envelope → visible error
   card, never a black screen. Publish guard (the F4 lesson): only the
   still-current face's data may write !face-compiled."
  [atoms data]
  (let [{:keys [!face-state !face-compiled]} atoms
        current (some-> @!face-state :face name)]
    (when (and current (= current (:assembly/name data)))
      (let [src (:assembly/source data)]
        (when (not= src @!last-compiled-source)
          (reset! !last-compiled-source src)
          (let [assembly (if (string? src)
                           (try (reader/read-string src)
                                (catch :default e
                                  (js/console.error "[FACE] served source unreadable" e)
                                  {:assembly/name current :assembly/grammar 0}))
                           ;; absent source (unknown face / arsenal lack):
                           ;; root-less envelope → error card
                           {:assembly/name current :assembly/grammar 0})
                compiled (fa/compile-assembly prims/registry assembly)]
            (when (fa/error? compiled)
              (js/console.warn "[FACE] assembly compiled with errors"
                               (clj->js (fa/compile-errors compiled))))
            (reset! !face-compiled compiled)))))))

(defn- picked-entity-id
  []
  (let [address (:address (scene-rt/last-pick))]
    (when (string? address) address)))

(defn- current-material-wearers
  []
  (material-inspector/wearers-from-scene-store
   (scene-rt/store-snapshot)))

(defn- material-inspection!
  [!request !data entity-id]
  (if-not (string? entity-id)
    (js/Promise.reject
     (js/Error.
      "Pick a block first, or pass its string entity id to __material.inspect(id)."))
    (let [token (str "material-inspector-"
                     (swap! !material-inspector-nonce inc))
          watch-key (str token "-watch")
          !timeout-id (atom nil)
          cleanup! (fn []
                     (remove-watch !data watch-key)
                     (when-let [timeout-id @!timeout-id]
                       (js/clearTimeout timeout-id)
                       (reset! !timeout-id nil)))]
      (js/Promise.
       (fn [resolve reject]
         (reset! !data nil)
         (add-watch
          !data watch-key
          (fn [_ _ _ response]
            (when response
              (if (= token (:material-inspector/request-token response))
                (do
                  (cleanup!)
                  (resolve response))
                (when (:conversation/error response)
                  (cleanup!)
                  (reject
                   (js/Error.
                    (str "Material inspector projection failed: "
                         (name (:conversation/error response))))))))))
         (reset! !timeout-id
                 (js/setTimeout
                  (fn []
                    (cleanup!)
                    (reject
                     (js/Error.
                      "Material inspector projection timed out after 10 seconds.")))
                  10000))
         (reset! !request
                 {:face :material-inspector
                  :params {:request-token token
                           :entity-id entity-id
                           :wearers (current-material-wearers)}}))))))

(defn- install-material-inspector-api!
  [!request !data]
  (when (and !request !data)
    (letfn [(request! [entity-id]
              (material-inspection! !request !data
                                    (or entity-id (picked-entity-id))))
            (inspect
              ([] (inspect nil))
              ([entity-id]
               (.then (request! entity-id)
                      (fn [response]
                        (clj->js
                         (:material-inspector/result response))))))
            (edn
              ([] (edn nil))
              ([entity-id]
               (.then (request! entity-id)
                      (fn [response]
                        (:material-inspector/edn response)))))]
      (set! (.-__material js/window)
            #js {:picked (fn [] (picked-entity-id))
                 :wearers (fn [] (clj->js (current-material-wearers)))
                 :inspect inspect
                 :edn edn})
      (js/console.log
       "[MATERIAL] window.__material installed — click a block, then await __material.inspect() / await __material.edn()"))))

(defn- install-interaction-table-api!
  "editable-material P5: the SERVED interaction table, on the console.

   `window.__bindings.served()` resolves the server's own answer to \"what does
   each gesture mean, and which master revision decided it\" — the projection,
   not a client re-derivation. `window.__bindings.table()` (ground.cljs) shows
   the client's tiers; the two agreeing is the seam's proof. Read-only: this
   pull cannot write, because the projection layer physically cannot (G12)."
  [!request !data]
  (when (and !request !data)
    (letfn [(served []
              (js/Promise.
               (fn [resolve reject]
                 (let [watch-key (str "interaction-table-"
                                      (swap! !material-inspector-nonce inc))
                       !timeout (atom nil)
                       cleanup! (fn []
                                  (remove-watch !data watch-key)
                                  (when-let [t @!timeout]
                                    (js/clearTimeout t)))]
                   (reset! !data nil)
                   (add-watch
                    !data watch-key
                    (fn [_ _ _ response]
                      (when response
                        (cleanup!)
                        (if (:conversation/error response)
                          (reject (js/Error.
                                   (str "Interaction table projection failed: "
                                        (name (:conversation/error response)))))
                          (resolve (clj->js response))))))
                   (reset! !timeout
                           (js/setTimeout
                            (fn []
                              (cleanup!)
                              (reject
                               (js/Error.
                                "Interaction table projection timed out.")))
                            10000))
                   (reset! !request
                           {:face :interaction-table
                            :params {:drill? (drill-mode?)}})))))]
      (when-let [existing (.-__bindings js/window)]
        (set! (.-served existing) served))
      (set! (.-__softland_served_bindings js/window) served))))

(defn install-face-wiring!
  "Wire the generic face pulls + the wear write path:
   - !face-state (set by the /face command + address/scrub selection) derives
     !face-request — the CONTRACT §7 value Electric watches and pulls against:
     {:face :address :params {:limit :until-ms} :epoch}.
   - !face-data (Electric pull result) is the ONE data-context atom, consumed whole
     by the §5 flow (the data-context arrives whole — no split, unlike the trail face).
   - W2: !assembly-request/!assembly-data carry the wear-time source serve
     (compiled here, value-compared — trap T13); !face-list-request/!face-list-data
     carry the sidebar roster (mirrored into :!face-list — from Rama, trap T14);
     !face-wear-outbox/!face-wear-result carry wear events (cleared on ack;
     depth-1 queue by design, recorded in W2-INT).
   - remote ingest epoch mirrors into !ingest-epoch and, after a client-side debounce
     >= 1s (INV-19 — the epoch PUSH is the only re-pull trigger; no polling anywhere),
     re-stamps and re-arms every armed request so faces + roster fill in near-live
     (the Step-7A save→re-render loop closes here: changed source → recompile;
     unchanged source → the value compare skips)."
  [atoms {:keys [!face-request !face-data !ingest-epoch-remote
                 !assembly-request !assembly-data
                 !face-list-request !face-list-data
                 !facet-materials-request !facet-materials-data
                 !material-inspector-request !material-inspector-data
                 !interaction-table-request !interaction-table-data
                 !face-wear-outbox !face-wear-result]}]
  (let [{:keys [!face-state !ingest-epoch]} atoms
        !last-pull-epoch (atom 0)
        !debounce (atom nil)
        list-request! (fn [epoch nonce]
                        (when !face-list-request
                          (reset! !face-list-request
                                  (cond-> {:face :face-list :epoch epoch}
                                    nonce (assoc :refresh nonce)))))
        material-request! (fn [epoch]
                            (when !facet-materials-request
                              (reset! !facet-materials-request
                                      {:face :facet-materials
                                       :params {:drill? (drill-mode?)}
                                       :epoch epoch})))
        request! (fn []
                   (let [s @!face-state]
                     (reset! !face-request
                             (when (:face s)
                               {:face    (:face s)
                                :address (:address s)
                                :params  (:params s {})
                                :epoch   @!last-pull-epoch}))
                     ;; face off → the wear-time source pull retires with it
                     (when (and (nil? (:face s)) !assembly-request)
                       (reset! !assembly-request nil))))]
    ;; publish the refs for the command path (wear-face!)
    (reset! !electric-refs {:!assembly-request !assembly-request
                            :!face-wear-outbox !face-wear-outbox
                            :!last-pull-epoch !last-pull-epoch})
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
    ;; W2: wear-time source arrives → compile (guarded, value-compared, total)
    (when !assembly-data
      (add-watch !assembly-data :assembly-compile
                 (fn [_ _ _ data]
                   (when data (compile-served-source! atoms data)))))
    ;; W2: the roster mirrors into :!face-list (sidebar reads Rama, trap T14)
    (when (and !face-list-data (:!face-list atoms))
      (add-watch !face-list-data :face-list-mirror
                 (fn [_ _ _ data]
                   (when data (reset! (:!face-list atoms) data)))))
    ;; P3: both masters arrive in one confirmed, batched projection. Ground
    ;; watches this atom; candidate writes and activation acks never touch it.
    (when (and !facet-materials-data (:!facet-materials atoms))
      (add-watch !facet-materials-data :facet-materials-mirror
                 (fn [_ _ _ data]
                   (when data
                     (reset! (:!facet-materials atoms) data)))))
    ;; P2: console-only, read-only inspector. One request carries the picked
    ;; entity plus the complete current scene wearer snapshot; the server
    ;; registry performs the durable joins in one FacePull.
    (install-material-inspector-api!
     !material-inspector-request !material-inspector-data)
    ;; P5: the served interaction table, same registry, same artery. Installed
    ;; before the ground exists, so the ground's __bindings seam reaches for it.
    (install-interaction-table-api!
     !interaction-table-request !interaction-table-data)
    ;; W2: wear ack → clear the outbox + refresh the roster (the count bump is
    ;; visible without waiting for an ingest epoch; the :refresh nonce changes
    ;; the request VALUE so Electric re-pulls)
    (when (and !face-wear-result !face-wear-outbox)
      (add-watch !face-wear-result :face-wear-ack
                 (fn [_ _ _ res]
                   (when res
                     (when-not (:wear/recorded? res)
                       (js/console.warn "[FACE] wear not recorded" (clj->js res)))
                     ;; G26 fix (client-wiring MEDIUM): clear ONLY the wear this
                     ;; ack answers — a late/stale ack must not nil a NEWER wear
                     ;; sitting in the outbox before its write mounts (that loss
                     ;; would be an undercount the depth-1 design did not accept)
                     (swap! !face-wear-outbox
                            (fn [w] (if (= (:wear-id w) (:wear/id res)) nil w)))
                     (list-request! @!last-pull-epoch (:wear/id res))))))
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
                                 (request!)
                                 ;; W2 (INV-19): re-stamp the constant pulls too
                                 (list-request! new-epoch nil)
                                 (material-request! new-epoch)
                                 (when-let [s @!face-state]
                                   (when (and (:face s) !assembly-request)
                                     (reset! !assembly-request
                                             {:face :assembly
                                              :address (name (:face s))
                                              :epoch new-epoch}))))
                               1000))))))
    (request!)
    ;; arm the roster pull at install (the sidebar lists faces from first light)
    (list-request! 0 nil)
    ;; arm the one material pull at install; later changes use this same epoch
    ;; debounce and therefore cannot render ahead of durable activation.
    (material-request! 0)))
