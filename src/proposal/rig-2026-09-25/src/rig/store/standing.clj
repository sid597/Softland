;; IMPORTANT: Before modifying this file, re-read PLAN-reads-rest.md, "Standing
;; reads" (its fixes F2, F3 and F18), and adhere to its decisions.
(ns rig.store.standing
  "Standing reads (CONCLUSION R6, a default; PLAN-reads-rest.md 'Standing
  reads'), client side beside the one exit: a read held open by its caller,
  delivered at the caller's rate (the renderer's refresh, the agent's model
  call) through the exit's order: query, append the line, then show.

  One entry per standing read, `:read-<uuid>` from its opening act's name
  (or, when its lines share an act with other entries', from a name made
  for it alone: the pointer screen's `offer-lines!`, one act per cause);
  every line a fact about it in the reader's working layer, through the
  ordinary offer path, marked `:own-row` (FRR1 to FRR4, first-record): the
  opening `:read/standing`, a `:read/delivery` for each delivery that shows
  something new (none for nothing new), the closing `:read/closed`. A
  delivery line carries the running closing fingerprint, count and mark
  (`:so-far`, F3), computed in the module under the fingerprint secret, so
  the close reads one line.

  The handle lives in the caller's process and is not durable: the record
  has everything a close needs (`close-open!` closes what a crashed door
  left). One caller per handle.

  Test hooks (R3): the exit's `:exit-after-entry` and `:exit-shown`, named
  by each line's act name.

  The spec fixes (H-1): every line act is named with the working layer's
  class as the layer has it when the line is made, and offered through the
  door of the gate that orders the layer (`micro-client/offer-into!`), as
  the exit's entries are; a session's close also finds the entries left
  open in a re-classed layer's micro era."
  (:require [com.rpl.rama :refer :all]
            [rig.store.client :as c]
            [rig.store.envelope :as env]
            [rig.store.inject :as inject]
            [rig.store.micro-client :as mc]
            [rig.store.read-exit :as rx]
            [rig.store.reads :as reads]))

(defn- fresh-line-name
  "A fresh name for a line act, tagged with the working layer's class now
  (the spec fixes, H-1)."
  [store working]
  (env/make-name working (mc/layer-class store working)))

(defn- line-offer
  "One act of a standing read's lines, into the reader's working layer, as
  the exit's entries are: `:who` the reader, its permission and session
  there, the class its name carries, facts about the entry entity, each
  marked `:own-row` (FRR6)."
  [{:keys [reader working permission session]} nm facts]
  (c/build {:name nm :who reader :layer working :class (nth nm 1) :permission permission
            :stood-on {} :because-of nil :subjects #{} :session session
            :facts (mapv #(assoc % :mark #{:own-row}) facts)}))

(defn- offer-line!
  "Offer a line act until answered, through the door of the gate its name
  is tagged for and the exit's hooks: nothing is shown unless it is
  answered yes."
  [store spec nm facts]
  (let [a (mc/offer-into! store (line-offer spec nm facts))]
    (inject/point! :exit-after-entry nm)
    a))

(defn- delivery-value
  "FRR2: a delivery line's value, from the delta's answer."
  [spec answer after]
  (cond-> {:layer (:layer answer) :moment (:moment answer) :after after :role (:role spec)
           :count (count (:matched answer)) :mark (:mark answer)
           :fingerprint (:fingerprint answer) :fp-secret (:fp-secret answer)
           :so-far (:so-far answer)}
    (reads/exact? (:reader-kind spec) (:rows? spec)) (assoc :exact (vec (:matched answer)))
    (contains? answer :max-stamp) (assoc :max-stamp (:max-stamp answer))))

(defn- delta
  "The delta query (RS3) on the read layer's task."
  [store {:keys [layer for pattern limit]} scan prev]
  (foreign-invoke-query (:read-delta store) layer for pattern limit scan prev))

;; Each of the three moves below is two halves (the pointer screen): the first
;; runs the module's query and gives the lines and the handle's next value
;; without offering anything; the second offers them. `subscribe!`, `deliver!`
;; and `close-entry!` offer one move's lines in one act, as they always have;
;; `offer-lines!` offers the lines of many standing reads of one reader in one
;; act, one record per cause (a gesture, a push).

(defn open-lines
  "The first half of `subscribe!`: the opening delta for `spec` (the exit's,
  with `:pattern` and `:limit`) on entry entity `ent`, and the facts the
  opening act carries: `{:facts [opening line?] :rows [...] :state s}`, `s`
  the handle's value once an act carrying the facts is answered yes; or
  `{:refused r}`, nothing opened."
  [store spec ent]
  (let [spec (cond-> (merge {:role :shown :limit reads/default-limit} spec)
               (and (= :person (:reader-kind spec)) (nil? (:for spec))) (assoc :for (:reader spec)))
        a (delta store spec nil nil)]
    (if (contains? a :refused)
      {:refused (:refused a)}
      (let [opening {:e ent :k :read/standing
                     ;; R-2: the pattern as recorded, a [:kv] value keyed, never its text
                     :v (cond-> {:layer (:layer spec) :pattern (:recorded-pattern a) :role (:role spec)
                                 :limit (:limit spec) :moment (:moment a)}
                          (contains? a :max-stamp) (assoc :max-stamp (:max-stamp a)))}
            line (when-not (:nothing-new a) {:e ent :k :read/delivery :v (delivery-value spec a nil)})]
        {:facts (cond-> [opening] line (conj line))
         :rows (:rows a)
         :state {:ent ent :spec spec :scan (:next-scan a) :line (:moment a)
                 :opening (:moment a) :so-far (:so-far a)}}))))

(defn subscribe!
  "Open a standing read (FRR1): the opening read through the exit's order
  (query; the opening act, with a first delivery line when it matched
  anything; then the rows, only on the act's yes). `spec` is the exit's
  (`:reader :for :reader-kind :rows? :working :permission :session :role
  :layer`) with `:pattern` and `:limit`. `{:handle h :rows [...]}`, or
  `{:refused r}` with nothing shown and nothing opened."
  [store spec]
  (let [nm (fresh-line-name store (:working spec))
        o (open-lines store spec (reads/entry-entity nm))]
    (if (contains? o :refused)
      {:refused (:refused o)}
      (let [r (offer-line! store (:spec (:state o)) nm (:facts o))]
        (if (= :yes (:answer r))
          (do (inject/point! :exit-shown nm)
              {:handle (atom (:state o)) :rows (:rows o) :entry nm})
          {:refused (:reason r) :entry nm})))))

(declare reopen!)

(defn delivery-lines
  "The first half of `deliver!`, on a handle's value `state`: the delta
  since its cursor. `{:nothing-new true :state s}`, the cursor moved (F2);
  `{:facts [line] :rows [...] :state s}`, `s` the handle's value once an act
  carrying the line is answered yes; `{:reclassed true}` for a handle held
  across its layer's re-class (F18: close it `:reclass` and open it again);
  or `{:refused r}`."
  [store state]
  (let [{:keys [ent spec scan line so-far]} state
        a (delta store spec scan so-far)]
    (cond
      (= :reclassed (:refused a)) {:reclassed true}
      (contains? a :refused) {:refused (:refused a)}
      (:nothing-new a) {:nothing-new true :state (assoc state :scan (:next-scan a))}
      :else {:facts [{:e ent :k :read/delivery :v (delivery-value spec a line)}]
             :rows (:rows a)
             :state (assoc state :scan (:next-scan a) :line (:moment a) :so-far (:so-far a))})))

(defn deliver!
  "One delivery at the caller's rate (FRR2): the delta since the handle's
  cursor; nothing new gives `:nothing-new` with nothing offered and the
  cursor moved (F2: to where the scan budget stopped, never past an
  unscanned entry); something new gives one line act, and only on its yes
  the rows, both of the handle's moments moving; a refused line shows
  nothing and moves nothing, so the same facts are new again. A handle held
  across its layer's re-class is closed `:reclass` and reopened on a
  frontier moment, its first read's rows the delivery (F18). `:line-name`
  names the line's act (tests arm hooks on it); else it is made fresh."
  [store h & {:keys [line-name]}]
  (let [d (delivery-lines store @h)]
    (cond
      (:reclassed d) (reopen! store h)
      (contains? d :refused) {:refused (:refused d)}
      (:nothing-new d) (do (reset! h (:state d)) :nothing-new)
      :else
      (let [spec (:spec @h)
            nm (or line-name (fresh-line-name store (:working spec)))
            r (offer-line! store spec nm (:facts d))]
        (if (= :yes (:answer r))
          (do (reset! h (:state d))
              (inject/point! :exit-shown nm)
              {:rows (:rows d) :entry nm})
          {:refused (:reason r) :entry nm})))))

(defn close-lines
  "The first half of `close-entry!`: the closing line of entry `ent`, its
  fingerprint, count and mark from the module (`standing-close`, the last
  admitted line's running value), with how it closed. The facts, one."
  [store spec ent closed-by]
  (let [c (foreign-invoke-query (:standing-close store) (:working spec) ent)]
    [{:e ent :k :read/closed
      :v (cond-> {:layer (:layer c) :moment (:moment c) :deliveries (:deliveries c)
                  :fingerprint (:fingerprint c) :fp-secret reads/fp-secret-id
                  :mark (:mark c) :closed-by closed-by}
           (nil? (:layer c)) (assoc :layer (:layer spec)))}]))

(defn close-entry!
  "FRR3: an entry's closing act, its fingerprint, count and mark from the
  module (`standing-close`, the last line's running value), with how it
  closed. The act's answer."
  [store spec ent closed-by]
  (let [facts (close-lines store spec ent closed-by)
        nm (fresh-line-name store (:working spec))]
    (offer-line! store spec nm facts)))

(defn fresh-entry
  "An entry entity for one standing read whose lines share an act with
  others' (the pointer screen): FR5's entity, fresh per entry, taken from a
  name made for this entry alone, since the act's own name is shared."
  [store working]
  (reads/entry-entity (fresh-line-name store working)))

(defn offer-lines!
  "The pointer screen: one act carrying the lines of many standing reads of
  one reader (one record per cause), as each move's own act would carry
  them: `:who` the reader, its permission and session in the working layer,
  each fact marked `:own-row`, through the door of the gate that orders the
  layer. `spec` names the reader (`:reader :working :permission
  :session`); `facts` are the halves' lines, at most one move per entry (a
  closing line reads the entry's admitted lines, so an entry's delivery and
  its close never share an act). The answer, with `:entry` the act's name;
  nothing a half gave may be shown unless it is yes."
  [store spec facts]
  (let [nm (fresh-line-name store (:working spec))]
    (assoc (offer-line! store spec nm facts) :entry nm)))

(defn unsubscribe!
  "Close a standing read at unsubscribe (FRR3, `:closed-by :unsubscribe`)."
  [store h]
  (let [{:keys [ent spec]} @h]
    (close-entry! store spec ent :unsubscribe)))

(defn- reopen!
  "F18: close the entry `:reclass` and open a new one on the same pattern,
  role and limit, a frontier moment; its opening rows are this delivery."
  [store h]
  (let [{:keys [ent spec]} @h
        closed (close-entry! store spec ent :reclass)
        s (subscribe! store spec)]
    (if (:handle s)
      (do (reset! h @(:handle s)) {:rows (:rows s) :reopened true :closed (:answer closed)})
      {:refused (:refused s)})))

(defn close-session!
  "At a session's close: every live handle closed `:session-close`, then
  every entry of the session left open in the working layer (a crashed
  door's) closed `:crash`, found from the record (`standing-open`). `spec`
  names the closer (`:reader`, `:working`, `:permission`, `:session`).
  Returns `{:closed n :crash n}`.

  The spec fixes (H-1): for a working layer re-classed by entity the record
  has two eras. An entry the stream era opened and the micro era closed is
  not open; an entry the micro era opened and did not close is
  (`micro-standing-open`, the micro era's opens of the session and every
  entry it closed)."
  ([store spec] (close-session! store spec []))
  ([store spec handles]
   (let [live (count (mapv #(close-entry! store (:spec @%) (:ent @%) :session-close) handles))
         W (:working spec)
         sopen (foreign-invoke-query (:standing-open store) W (:session spec))
         sopen (if (sequential? sopen) sopen [])
         m (when (= :by-entity (mc/layer-class store W))
             (foreign-invoke-query (:micro-standing-open store) W (:session spec)))
         open (if (map? m)
                (into [] (distinct) (concat (remove (set (:closed m)) sopen) (:open m)))
                sopen)
         crash (count (mapv #(close-entry! store spec % :crash) open))]
     {:closed live :crash crash})))
