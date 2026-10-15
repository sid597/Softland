(ns softland.runtime.sign-in
  "Sign-in as steps that are safe to run again: a first sign-in by
  invitation, a later one, and a second account linked while signed in;
  and the making of an invitation. Pure code. It needs no cluster.

  The account comes from a check that was made before the steps: for
  Google, `softland.runtime.google/check-id-token`, whose claims the caller
  gives here with `:kind :google`. The steps work out what is missing from
  reads, asked as the system, and give one act at a time, as the system
  (`softland.runtime.steps`). Every later sign-in finishes whatever is
  missing, so a crash halfway never makes a second person (BUILD.md, \"The
  first sign-in\").

  A sign-in, in order (PROGRESS.md, \"The first sign-in, as steps\"):

    :link          the account's link in `ways-in`. A link says who she is,
                   and the invitation she came in by
    :invitation    with no link, an invitation is needed: its code is
                   checked against the kept hash, then `:invitation/used`
                   is claimed for a fresh person and this account (a
                   one-write key). An invitation already used by this
                   account goes on with its person; by another, refused
    :link-claim    with no link, the link is claimed: `:way-in/person`
                   (one-write), the account as given, and the invitation.
                   Taken by another person: that person is hers
    :identity      her identity in `identities`: kind, record layer, own
                   layer, invitation (each one-write), and a provisional
                   name (`softland.runtime.ids/provisional-name`)
    :name-free     the name is free or hers; when another holds it, her
                   identity takes the fresh name instead
    :name-claim    the name claimed in `names` (`:name/of`, one-write)
    :own-layer     her own layer made, by the system, with her first
                   permission and no right of the system's
    :record-layer  her record layer made, with the system as its top
    :record-facts  `:see` given to her in the record layer, beneath the
                   system; the way in recorded there, as Google gave it
    :rights        what her invitation gives: each right beneath the
                   system's first permission in its layer
    :session       the sitting, with its token's hash, in her record layer,
                   and when it ends when the input gives that time

  A place whose nearest fact is a retract counts as done for every step but
  the link: a right that was ended is not given again by a later sign-in.
  Unlinking an account is not designed: no code retracts a link. Were a link
  retracted, the account could not come in again by an invitation, since
  the link's `:way-in/invitation` takes one write and would still hold its
  first invitation; Sid's command line links an account to a person again
  (`softland.runtime.service/link-by-admin!`).

  Linking a second account while signed in: the account's link claimed for
  her, or refused when it is another's; then the way in recorded in her
  record layer.

  Inputs and results, by call:

    sign-in-step       the input, what is known -> the next thing to do
    link-account-step  the input, what is known -> the next thing to do
    invitation         an invitation's input -> {:ok {:code :act}} or
                       {:refuse word}

  The input of a sign-in:

    {:account        {:kind :google :sub … :email … :email-verified …
                      :name …}, the claims of the check, with the kind
     :code           an invitation's code, or nil
     :fresh          {:person :record-layer :own-layer :session uuid,
                      :secret 32 random bytes, :name a provisional name},
                      made by the caller, used only where nothing is there
     :system-session the id of this start of the runtime
     :sitting-ends-at  when the sitting ends, in milliseconds; optional.
                     Written with the session as `:session/ends-at`; the
                     caller works it out from the rule `:rule/sitting-ms`}

  Done is {:do :done :person :session :token :first-sign-in?}: the person,
  the sitting's id, its token's text, and whether this run made the link.

  The words: `words`, beside the steps' own (`:read-failed`,
  `:step-refused`, `:act-not-answered`).

  State and effects. None. Each function is pure.

  Callers. `softland.runtime.service`, at a sign-in, at a second account
  linked, at an account linked by Sid's command line, and at an invitation
  made; the service's proof, which drives a sign-in that it cuts after each
  act; and the pure proof, on a model of the store."
  (:require [softland.runtime.ids :as ids]
            [softland.runtime.records :as records]
            [softland.runtime.steps :as steps]
            [softland.runtime.token :as token]))

(def words
  "The words with which a sign-in or a linking stops, beside those of the
  steps:

    :input-not-whole              the input misses a part, or one is not of
                                  its form
    :runtime-not-started          a system layer is not made: the start has
                                  not run
    :no-invitation                no link and no code: a stranger
    :invitation-not-valid         the code is not in its form, names no
                                  invitation, or its hash is not the kept one
    :invitation-used              the invitation was used by another account
    :identity-not-a-person        the link points at an identity of another
                                  kind
    :provisional-name-taken       the provisional name and the fresh one are
                                  both held by others: run again with
                                  another fresh name
    :own-layer-not-hers           her own layer is made, and she does not
                                  hold its first permission
    :record-layer-not-the-systems her record layer is made, and its top is
                                  not the system
    :invitation-right-not-givable a layer that her invitation gives a right
                                  in is not made, or the system cannot see it
    :only-a-person-links-an-account  the actor who links is no person
    :account-linked-to-another    the account is linked to another person
    :identity-not-found           the signed-in person has no identity"
  [:input-not-whole :runtime-not-started :no-invitation :invitation-not-valid
   :invitation-used :identity-not-a-person :provisional-name-taken
   :own-layer-not-hers :record-layer-not-the-systems :invitation-right-not-givable
   :only-a-person-links-an-account :account-linked-to-another :identity-not-found])

;; ----------------------------------------------------------------- helpers

(defn- reading-system
  "Reads queries of the system's layers, as `softland.runtime.steps/reading`
  does, and stops with `:runtime-not-started` when a system layer read is
  left out: the start has not made it."
  [ctx queries f]
  (steps/reading ctx queries
    (fn [answers]
      (if-let [layer (some (fn [[q a]]
                             (let [layer (first (:layers q))]
                               (when (and (some #{layer} ids/system-layers) (steps/left-out-why a layer))
                                 layer)))
                           (map vector queries answers))]
        [:refuse :runtime-not-started {:layer layer}]
        (f answers)))))

(defn- account-as-given
  "Takes the account of the input. Gives what the link keeps of it: its
  kind and its number."
  [account]
  (select-keys account [:kind :sub]))

(defn- account-as-google-gave-it
  "Takes the account of the input. Gives what the record layer keeps of it:
  its kind, its number, and the email, whether it is verified, and the name,
  each when it is there."
  [account]
  (into {} (remove (comp nil? val)) (select-keys account [:kind :sub :email :email-verified :name])))

(defn- facts
  "Takes a record's name, an entity and a record. Gives the record's facts
  (`softland.runtime.records/facts-of`). What must hold: the record keeps
  its shape, which the input's check made sure of."
  [record-name entity record]
  (:ok (records/facts-of record-name entity record)))

(defn- link-facts
  "The facts of a link from the account to a person, with the invitation
  when there is one."
  [ctx person invitation]
  (facts :link (:account-id ctx)
         (cond-> {:way-in/person person
                  :way-in/account (account-as-given (get-in ctx [:in :account]))}
           invitation (assoc :way-in/invitation invitation))))

(defn- read-link
  "The step that reads the account's link. With a person there, the
  context takes `:person` and `:invitation` from it, and `:linked? true`."
  [ctx]
  (let [a (:account-id ctx)
        q (steps/entity-query ids/system ids/ways-in a false)]
    (reading-system ctx [q]
      (fn [[answer]]
        (let [link (steps/record-of answer a)]
          (if-let [person (:way-in/person link)]
            [:go (assoc ctx :person person :invitation (:way-in/invitation link) :linked? true)]
            [:go (assoc ctx :linked? false)]))))))

;; --------------------------------------------------------- a first sign-in

(defn- invitation-step
  "The step of the invitation, for an account with no link."
  [ctx]
  (if (:linked? ctx)
    [:go ctx]
    (let [code (get-in ctx [:in :code])
          parsed (token/parse-code code)]
      (cond
        (nil? code) [:refuse :no-invitation]
        (nil? parsed) [:refuse :invitation-not-valid]
        :else
        (let [inv (:invitation parsed)
              q (steps/entity-query ids/system ids/invitations inv false)]
          (reading-system ctx [q]
            (fn [[answer]]
              (let [held (steps/record-of answer inv)
                    used (:invitation/used held)
                    a (:account-id ctx)
                    fresh-person (get-in ctx [:in :fresh :person])]
                (cond
                  (not (token/matches? code (:invitation/code-hash held))) [:refuse :invitation-not-valid]
                  (and used (not= a (:account used))) [:refuse :invitation-used]
                  used [:go (assoc ctx :person (:person used) :invitation inv)]
                  :else
                  (steps/acting ctx
                                (steps/system-act ctx ids/invitations
                                                  (:ok (records/part-facts :invitation inv {:invitation/used {:person fresh-person :account a}})))
                                (assoc ctx :person fresh-person :invitation inv)))))))))))

(defn- link-claim
  "The step that claims the link for the person, when there is none."
  [ctx]
  (if (:linked? ctx)
    [:go ctx]
    (steps/acting ctx
                  (steps/system-act ctx ids/ways-in (link-facts ctx (:person ctx) (:invitation ctx)))
                  (assoc ctx :linked? true))))

(defn- identity-step
  "The step of her identity: read it, or write it with the fresh ids."
  [ctx]
  (let [p (:person ctx)
        q (steps/entity-query ids/system ids/identities p false)]
    (reading-system ctx [q]
      (fn [[answer]]
        (let [held (steps/record-of answer p)
              fresh (get-in ctx [:in :fresh])]
          (cond
            (= :person (:identity/kind held))
            [:go (assoc ctx
                        :record-layer (:identity/record-layer held)
                        :own-layer (:identity/own-layer held)
                        :name (:identity/name held)
                        :name-fact (steps/fact-at answer [p :identity/name])
                        :invited-by (:identity/invited-by held))]

            (some? (:identity/kind held)) [:refuse :identity-not-a-person]

            :else
            (let [record (cond-> {:identity/kind :person
                                  :identity/name (:name fresh)
                                  :identity/record-layer (:record-layer fresh)
                                  :identity/own-layer (:own-layer fresh)}
                           (:invitation ctx) (assoc :identity/invited-by (:invitation ctx)))]
              (steps/acting ctx
                            (steps/system-act ctx ids/identities (facts :person p record))
                            (assoc ctx
                                   :record-layer (:record-layer fresh)
                                   :own-layer (:own-layer fresh)
                                   :name (:name fresh)
                                   :name-fact nil
                                   :invited-by (:invitation ctx))))))))))

(defn- name-query
  "The query of a name's place in `names`."
  [n]
  (steps/place-query ids/system ids/names (ids/name-entity n) :name/of))

(defn- name-free
  "The step that makes sure her name is free or hers. When another holds it,
  or held it and freed it, her identity takes the fresh name, replacing the
  fact of the name it held."
  [ctx]
  (let [n (:name ctx)
        p (:person ctx)]
    (reading-system ctx [(name-query n)]
      (fn [[answer]]
        (let [fresh-name (get-in ctx [:in :fresh :name])]
          (cond
            (or (= p (steps/value-at answer)) (= :no-fact (steps/holds-at answer))) [:go ctx]
            (and (not= fresh-name n) (:name-fact ctx))
            (steps/acting ctx
                          (steps/system-act ctx ids/identities
                                            [{:e p :k :identity/name :v fresh-name :replaces (:name-fact ctx)}])
                          (assoc ctx :name fresh-name :name-fact nil))
            :else [:refuse :provisional-name-taken {:name n}]))))))

(defn- name-claim
  "The step that claims her name in `names`."
  [ctx]
  (let [n (:name ctx)
        p (:person ctx)]
    (reading-system ctx [(name-query n)]
      (fn [[answer]]
        (cond
          (= p (steps/value-at answer)) [:go ctx]
          (= :no-fact (steps/holds-at answer))
          (steps/acting ctx (steps/system-act ctx ids/names (facts :name (ids/name-entity n) {:name/of p})) ctx)
          :else [:refuse :provisional-name-taken {:name n}])))))

(defn- own-layer
  "The step of her own layer. It reads her permission in it as she would:
  the system keeps no right there, so it could not tell a layer made for
  her from one that another made."
  [ctx]
  (let [p (:person ctx)
        o (:own-layer ctx)
        q (steps/place-query p o p :softland.store/permission)]
    (steps/reading ctx [q]
      (fn [[answer]]
        (cond
          (= [p o o] (steps/value-at answer)) [:go ctx]
          (= :not-made (steps/left-out-why answer o)) (steps/acting ctx (steps/making-act ctx o p) ctx)
          :else [:refuse :own-layer-not-hers {:layer o}])))))

(defn- record-layer
  "The step of her record layer: made with the system as its top."
  [ctx]
  (let [r (:record-layer ctx)
        q (steps/place-query ids/system r ids/system :softland.store/permission)]
    (steps/reading ctx [q]
      (fn [[answer]]
        (cond
          (= [ids/system r r] (steps/value-at answer)) [:go ctx]
          (= :not-made (steps/left-out-why answer r)) (steps/acting ctx (steps/making-act ctx r ids/system) ctx)
          :else [:refuse :record-layer-not-the-systems {:layer r}])))))

(defn- see-in-record-layer
  "The permission that her record layer gives her: `:see`, beneath the
  system's first permission there."
  [p r]
  [p r r [ids/system r r] :see])

(defn- record-facts
  "The step of what her record layer holds at a sign-in: `:see` given to
  her, and the way in, as Google gave it. What is missing goes in one act."
  [ctx]
  (let [p (:person ctx)
        r (:record-layer ctx)
        a (:account-id ctx)
        q-see (steps/place-query ids/system r p :softland.store/permission)
        q-way (steps/place-query ids/system r a :way-in/account)]
    (steps/reading ctx [q-see q-way]
      (fn [[see way]]
        (let [needed (concat (when (= :no-fact (steps/holds-at see))
                               [{:e p :k :softland.store/permission :v (see-in-record-layer p r)}])
                             (when (= :no-fact (steps/holds-at way))
                               (facts :way-in a {:way-in/account (account-as-google-gave-it (get-in ctx [:in :account]))})))]
          (if (empty? needed)
            [:go ctx]
            (steps/acting ctx (steps/system-act ctx r needed) ctx)))))))

(defn- give-act
  "The act that gives a right of an invitation: in its layer, as the
  system, a permission for her beneath the system's first permission
  there, allowing what the invitation says."
  [ctx p {:keys [layer allows]}]
  (steps/system-act ctx layer
                    [{:e p :k :softland.store/permission
                      :v [p layer layer [ids/system layer layer] allows]}]))

(defn- rights
  "The step of what her invitation gives: each right that her place in its
  layer does not hold yet, one act at a time. A place that holds any fact,
  a right that was ended among them, counts as done."
  [ctx]
  (if-let [inv (:invited-by ctx)]
    (let [q (steps/entity-query ids/system ids/invitations inv false)]
      (reading-system ctx [q]
        (fn [[answer]]
          (let [gives (:invitation/gives (steps/record-of answer inv))
                p (:person ctx)
                queries (mapv (fn [{:keys [layer]}] (steps/place-query ids/system layer p :softland.store/permission)) gives)]
            (steps/reading ctx queries
              (fn [answers]
                (let [pairs (map vector gives answers)]
                  (if-let [[give] (first (filter (fn [[give a]] (steps/left-out-why a (:layer give))) pairs))]
                    [:refuse :invitation-right-not-givable {:layer (:layer give)}]
                    (let [outcomes (get-in ctx [:known :outcomes])
                          pending (keep (fn [[give a]]
                                          (let [the-act (give-act ctx p give)]
                                            (when (and (= :no-fact (steps/holds-at a))
                                                       (not (steps/accepted? (get outcomes the-act))))
                                              the-act)))
                                        pairs)]
                      (if-let [the-act (first pending)]
                        (steps/acting ctx the-act ctx)
                        [:go ctx]))))))))))
    [:go ctx]))

(defn- session-step
  "The step of the sitting: its record in her record layer, with the hash
  of its token, and when it ends when the input says. The session is fresh,
  so there is nothing to read."
  [ctx]
  (let [p (:person ctx)
        {:keys [session secret]} (get-in ctx [:in :fresh])
        the-token (token/make-token p session secret)
        ends-at (get-in ctx [:in :sitting-ends-at])
        record (cond-> {:session/of p
                        :session/kind :sitting
                        :session/token-hash (token/text-hash the-token)
                        :session/made-by p
                        :session/way-in (:account-id ctx)}
                 (some? ends-at) (assoc :session/ends-at ends-at))]
    (steps/acting ctx
                  (steps/system-act ctx (:record-layer ctx) (facts :session session record))
                  (assoc ctx :session session :token the-token))))

(def sign-in-steps
  "The steps of a sign-in, first or later, in order."
  [[:link read-link]
   [:invitation invitation-step]
   [:link-claim link-claim]
   [:identity identity-step]
   [:name-free name-free]
   [:name-claim name-claim]
   [:own-layer own-layer]
   [:record-layer record-layer]
   [:record-facts record-facts]
   [:rights rights]
   [:session session-step]])

;; ------------------------------------------------------------- the input

(defn- whole-account?
  "True for an account that the steps can use: kind `:google`, and claims
  that the record of a way in takes."
  [account]
  (and (map? account) (= :google (:kind account))
       (contains? (records/check :way-in {:way-in/account (account-as-google-gave-it account)}) :ok)))

(defn- whole-fresh?
  "True for fresh ids that the steps can use: four UUIDs, 32 bytes, a
  name."
  [{:keys [person record-layer own-layer session secret] :as fresh}]
  (and (every? uuid? [person record-layer own-layer session])
       (bytes? secret) (= 32 (alength ^bytes secret))
       (ids/name? (:name fresh))))

(defn sign-in-step
  "A sign-in, first or later. Takes the input (the namespace docstring) and
  what is known (`softland.runtime.steps`). Gives the next thing to do:
  reads, one act as the system, done, or a refusal. Done is
  {:do :done :person :session :token :first-sign-in?}."
  [input known]
  (let [account (:account input)]
    (if-not (and (whole-account? account) (whole-fresh? (:fresh input)) (uuid? (:system-session input))
                 (let [e (:sitting-ends-at input)] (or (nil? e) (and (int? e) (<= 0 e)))))
      {:do :refuse :word :input-not-whole}
      (steps/run-steps sign-in-steps
                       {:in input :known known :account-id (ids/account-id (:kind account) (:sub account))}
                       (fn [ctx]
                         {:do :done
                          :person (:person ctx)
                          :session (:session ctx)
                          :token (:token ctx)
                          :first-sign-in? (boolean (some (fn [[the-act o]] (and (= ids/ways-in (:layer the-act)) (steps/accepted? o)))
                                                         (:outcomes known)))})))))

;; ------------------------------------------------- a second account linked

(defn- link-own-account
  "The step of the account's link, for a person who is signed in: hers
  already, claimed for her, or refused when another's."
  [ctx]
  (let [a (:account-id ctx)
        p (:person ctx)
        q (steps/entity-query ids/system ids/ways-in a false)]
    (reading-system ctx [q]
      (fn [[answer]]
        (let [linked-to (:way-in/person (steps/record-of answer a))]
          (cond
            (= p linked-to) [:go ctx]
            (some? linked-to) [:refuse :account-linked-to-another]
            :else (steps/acting ctx (steps/system-act ctx ids/ways-in (link-facts ctx p nil)) ctx)))))))

(defn- her-record-layer
  "The step that finds her record layer, from her identity."
  [ctx]
  (let [p (:person ctx)
        q (steps/entity-query ids/system ids/identities p false)]
    (reading-system ctx [q]
      (fn [[answer]]
        (if-let [r (:identity/record-layer (steps/record-of answer p))]
          [:go (assoc ctx :record-layer r)]
          [:refuse :identity-not-found])))))

(defn- way-in-recorded
  "The step that records the way in, as Google gave it, in her record
  layer, when it is not there."
  [ctx]
  (let [r (:record-layer ctx)
        a (:account-id ctx)
        q (steps/place-query ids/system r a :way-in/account)]
    (steps/reading ctx [q]
      (fn [[answer]]
        (if (= :no-fact (steps/holds-at answer))
          (steps/acting ctx (steps/system-act ctx r (facts :way-in a {:way-in/account (account-as-google-gave-it (get-in ctx [:in :account]))})) ctx)
          [:go ctx])))))

(def link-account-steps
  "The steps of linking a second account, in order."
  [[:link link-own-account]
   [:identity her-record-layer]
   [:way-in way-in-recorded]])

(defn link-account-step
  "Linking another account to a person who is signed in. Takes the input,
  {:actor {:id :kind :session} :account {:kind :google :sub …}
  :system-session uuid}, and what is known. Gives the next thing to do, or
  done: {:do :done :person :account}, the account's id. Refused
  `:only-a-person-links-an-account` for an actor of another kind, and
  `:account-linked-to-another` for an account that is another person's."
  [input known]
  (let [{:keys [actor account]} input]
    (cond
      (not (and (whole-account? account) (uuid? (:system-session input)) (uuid? (:id actor))))
      {:do :refuse :word :input-not-whole}

      (not= :person (:kind actor))
      {:do :refuse :word :only-a-person-links-an-account}

      :else
      (steps/run-steps link-account-steps
                       {:in input :known known :person (:id actor)
                        :account-id (ids/account-id (:kind account) (:sub account))}
                       (fn [ctx] {:do :done :person (:person ctx) :account (:account-id ctx)})))))

;; --------------------------------------------------------- an invitation

(defn invitation
  "The making of an invitation: by a command on Sid's machine for the
  first, which gives him full rights in the base beneath the system
  (BUILD.md, default 4). Takes {:invitation uuid :secret 32 random bytes
  :gives [{:layer :allows}] :made-by id :system-session uuid}. Gives
  {:ok {:code text :act act}}: the code to hand to the person, which the
  store never holds, and the act of the system that records the
  invitation with the code's hash; or {:refuse word :at where}, a word of
  `softland.runtime.records/check`, or `:input-not-whole`.

  Each give names a layer whose top is the system: the right is given
  beneath the system's first permission there. That is not read here; a
  give into another layer is refused at the sign-in
  (`:step-refused`)."
  [{:keys [invitation secret gives made-by system-session]}]
  (let [code (token/make-code invitation secret)]
    (if-not (and code (uuid? system-session))
      {:refuse :input-not-whole}
      (let [got (records/facts-of :invitation invitation
                                  {:invitation/code-hash (token/text-hash code)
                                   :invitation/gives gives
                                   :invitation/made-by made-by})]
        (if-let [fs (:ok got)]
          {:ok {:code code
                :act (steps/system-act {:in {:system-session system-session}} ids/invitations fs)}}
          got)))))
