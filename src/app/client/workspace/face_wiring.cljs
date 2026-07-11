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

;; The Electric refs, published by install-face-wiring! so the command path
;; (wear-face!, called from agent_flow with only the runtime atoms map) can
;; reach the request/outbox atoms. Client-glue module state, S2-contained;
;; nil until the wiring installs.
(defonce ^:private !electric-refs (atom nil))

;; Wear-time compile sentinel (trap T13: compare the source VALUE, never a
;; hash). ::none on wear entry so an identical re-wear recompiles (the W1
;; re-entry law).
(defonce ^:private !last-compiled-source (atom ::none))

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
                 !face-wear-outbox !face-wear-result]}]
  (let [{:keys [!face-state !ingest-epoch]} atoms
        !last-pull-epoch (atom 0)
        !debounce (atom nil)
        list-request! (fn [epoch nonce]
                        (when !face-list-request
                          (reset! !face-list-request
                                  (cond-> {:face :face-list :epoch epoch}
                                    nonce (assoc :refresh nonce)))))
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
                                 (when-let [s @!face-state]
                                   (when (and (:face s) !assembly-request)
                                     (reset! !assembly-request
                                             {:face :assembly
                                              :address (name (:face s))
                                              :epoch new-epoch}))))
                               1000))))))
    (request!)
    ;; arm the roster pull at install (the sidebar lists faces from first light)
    (list-request! 0 nil)))
