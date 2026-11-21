(ns auth-blind.mutants
  "Broken variants of Auth, one rule broken at a time, that the blind property
  suite must catch."
  (:require [auth-blind.model :as model]))

(def broken-variants
  "Each entry breaks exactly one contract rule in `auth-blind.model/make-auth`."
  [{:id :throw-on-bad-input
    :breaks "Throws an exception on malformed input instead of returning {:could-not :read-the-event}"
    :auth (model/make-auth {:throw-on-bad-input? true})}

   {:id :allow-closed-client
    :breaks "Allows reads and writes on a client after `close!`"
    :auth (model/make-auth {:allow-closed-client? true})}

   {:id :call-watch-after-close
    :breaks "Continues calling `watch-root` callback `f` after `close!` on that client"
    :auth (model/make-auth {:call-watch-after-close? true})}

   {:id :call-watch-on-register
    :breaks "Calls `watch-root` callback `f` immediately at registration"
    :auth (model/make-auth {:call-watch-on-register? true})}

   {:id :leak-driven-by-in-watch
    :breaks "Includes `:driven-by` inside `watch-root`'s `:sessions` map"
    :auth (model/make-auth {:leak-driven-by-in-watch? true})}

   {:id :reuse-used-claim
    :breaks "Allows a claim already proven by `enter` to be invited for a second person"
    :auth (model/make-auth {:reuse-used-claim? true})}

   {:id :flip-invite-refusal-order
    :breaks "Checks `:person-in-use` before `:claim-in-use` when both apply to `invite`"
    :auth (model/make-auth {:flip-invite-refusal-order? true})}

   {:id :email-before-code-on-enter
    :breaks "Matches `:email` ahead of `:code-hash` when an unlinked `enter` carries both"
    :auth (model/make-auth {:email-before-code-on-enter? true})}

   {:id :overwrite-own-layer-on-reenter
    :breaks "Overwrites a person's fixed `:own-layer` and `:making` on a later `enter`"
    :auth (model/make-auth {:overwrite-own-layer-on-reenter? true})}

   {:id :refuse-repeat-after-end
    :breaks "Refuses a repeat of `begin-sign-in` with `:in-use` once the sign-in has ended"
    :auth (model/make-auth {:refuse-repeat-after-end? true})}

   {:id :drop-previous-secret
    :breaks "Drops `:previous` secret on `replace-secret` instead of keeping `{:hash H :until t2}`"
    :auth (model/make-auth {:drop-previous-secret? true})}

   {:id :accept-previous-secret-for-replace
    :breaks "Allows `replace-secret` to rotate from `:previous` hash instead of refusing `:wrong-secret`"
    :auth (model/make-auth {:accept-previous-secret-for-replace? true})}

   {:id :leave-sessions-open-on-end-sign-in
    :breaks "Ends the sign-in on `end-sign-in` without closing open sessions under it"
    :auth (model/make-auth {:leave-sessions-open-on-end-sign-in? true})}

   {:id :no-depth-bound
    :breaks "Allows `make-actor` chains deeper than 32 makers below root"
    :auth (model/make-auth {:no-depth-bound? true})}

   {:id :allow-ending-person
    :breaks "Allows `end-actor` to end the root person instead of refusing `:is-a-person`"
    :auth (model/make-auth {:allow-ending-person? true})}

   {:id :shallow-end-actor
    :breaks "Ends only the direct agent on `end-actor`, leaving grandchildren and their sessions live"
    :auth (model/make-auth {:shallow-end-actor? true})}

   {:id :overwrite-driven-by-on-repeat
    :breaks "Mutates `:driven-by` when a repeat `open-session` supplies a different `:driven-by`"
    :auth (model/make-auth {:overwrite-driven-by-on-repeat? true})}

   {:id :shrink-session-until-on-repeat
    :breaks "Moves an open session's `:until` backward when a repeat `open-session` has an earlier `:until`"
    :auth (model/make-auth {:shrink-session-until-on-repeat? true})}

   {:id :reopen-closed-session
    :breaks "Re-opens a closed session when `open-session` is called again with the same session id"
    :auth (model/make-auth {:reopen-closed-session? true})}

   {:id :refuse-over-on-already-answered-ask
    :breaks "Refuses a second `answer-ask` with `:over` instead of returning the first answer that stands"
    :auth (model/make-auth {:refuse-over-on-already-answered-ask? true})}

   {:id :check-sign-in-on-no-answer
    :breaks "Rejects a `no` `answer-ask` when `:sign-in` is `:in-use` instead of judging only that the person entered"
    :auth (model/make-auth {:check-sign-in-on-no-answer? true})}

   {:id :skip-sweep-on-refusal
    :breaks "Skips sweeping expired sign-ins/sessions under `:root` when the write itself is refused"
    :auth (model/make-auth {:skip-sweep-on-refusal? true})}

   {:id :sweep-on-non-root-write
    :breaks "Sweeps expired sign-ins/sessions on `own-layer-made` (which names no `:root`)"
    :auth (model/make-auth {:sweep-on-non-root-write? true})}

   {:id :expiry-strict-lt
    :breaks "Treats `until == at` as still live instead of expired (`at or before`)"
    :auth (model/make-auth {:expiry-strict-lt? true})}

   {:id :allow-expired-at-make
    :breaks "Makes a sign-in or session with `until <= at` live instead of taken and never live"
    :auth (model/make-auth {:allow-expired-at-make? true})}

   {:id :hide-ended-in-reads
    :breaks "Deletes ended sign-ins and closed sessions from `who-is`"
    :auth (model/make-auth {:hide-ended-in-reads? true})}

   {:id :drop-ended-actors-in-under
    :breaks "Omits ended agents from `under`'s `:actors` vector"
    :auth (model/make-auth {:drop-ended-actors-in-under? true})}])
