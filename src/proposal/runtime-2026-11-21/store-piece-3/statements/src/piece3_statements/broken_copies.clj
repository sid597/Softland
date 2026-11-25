(ns piece3-statements.broken-copies
  "Sixteen broken copies of the Piece 3 target, each breaking one statement of
  `SPEC.md`, `IMPLICIT_SPEC.md`, or Sid's 32 MiB / 256 KiB call on purpose.
  The generated histories must catch every one of them."
  (:require [piece3-statements.model :as model]
            [piece3-statements.statements :as st]))

(def broken-copies
  [{:name :stray-gets-a-stamp
    :breaks "L1: gives a stray a stamp in the lane's line"
    :make #(model/make-target {:stray-gets-stamp? true})}

   {:name :other-gate-mixed-in
    :breaks "L2: mixes a thing sent on the other gate's depot into L's line"
    :make #(model/make-target {:other-gate-mixed-in? true})}

   {:name :resend-overwrites-thing
    :breaks "L3 / L6: a second arrival under a used act-id overwrites the kept thing"
    :make #(model/make-target {:resend-overwrites-thing? true})}

   {:name :standing-refusal-capped-at-256kib
    :breaks "K2 (Sid's call): caps a refused thing from a sender with standing at 256 KiB instead of 32 MiB"
    :make #(model/make-target
            {:kept-whole-fn
             (fn [{:keys [verdict thing-bytes]}]
               (or (= :accepted verdict)
                   (<= (long thing-bytes) st/max-no-standing-or-declaration-bytes)))})}

   {:name :no-standing-refusal-kept-past-256kib
    :breaks "K3 (Sid's call): keeps a >256 KiB refused thing from a sender with no standing whole"
    :make #(model/make-target
            {:kept-whole-fn
             (fn [{:keys [verdict refused-stage thing-bytes]}]
               (case verdict
                 :accepted true
                 :refused (case refused-stage
                            (:places :standing) (<= (long thing-bytes) st/max-with-standing-bytes)
                            :declaration (<= (long thing-bytes) st/max-no-standing-or-declaration-bytes))))})}

   {:name :declaration-refusal-kept-past-256kib
    :breaks "K3 (Sid's call): keeps a >256 KiB declaration-refused thing whole"
    :make #(model/make-target
            {:kept-whole-fn
             (fn [{:keys [verdict refused-stage thing-bytes]}]
               (case verdict
                 :accepted true
                 :refused (case refused-stage
                            (:places :declaration) (<= (long thing-bytes) st/max-with-standing-bytes)
                            :standing (<= (long thing-bytes) st/max-no-standing-or-declaration-bytes))))})}

   {:name :base-acts-open-by-default
    :breaks "S1: defaults :base's :who-may-read-acts to :everyone instead of :members"
    :make #(model/make-target {:default-acts-setting-fn (fn [lane] (if (= :base lane) :everyone :members))})}

   {:name :given-holder-writes-acts-setting
    :breaks "S2: allows a non-first holder to write :softland.store/who-may-read-acts"
    :make #(model/make-target {:given-holder-writes-acts-setting? true})}

   {:name :visitor-reads-taken-act-whole
    :breaks "R3: gives a visitor of an :everyone lane another sender's taken act whole while acts setting is :members"
    :make #(model/make-target {:visitor-reads-taken-act-whole? true})}

   {:name :withheld-leaks-answer
    :breaks "R3: includes :act-id and :answer on a :withheld decision"
    :make #(model/make-target {:withheld-leaks-answer? true})}

   {:name :open-lane-skips-permission-read
    :breaks "R1 / R4: skips reading the asker's permission in an :everyone lane, treating holders as visitors"
    :make #(model/make-target {:open-lane-skips-permission? true})}

   {:name :act-permission-grants-line-read
    :breaks "R1: treats an :act-permission holder as holding a permission to read the line"
    :make #(model/make-target {:act-permission-grants-read? true})}

   {:name :ended-chain-still-reads-whole
    :breaks "R2: checks only the asker's own permission row without checking that permissions above it still stand"
    :make #(model/make-target {:ended-chain-still-reads-whole? true})}

   {:name :closed-lane-hides-own-things
    :breaks "R5 / R6: omits a signed-in asker's own taken and refused things in a :may-not-see or :not-made lane"
    :make #(model/make-target {:closed-lane-hides-own-things? true})}

   {:name :closed-lane-leaks-stamps
    :breaks "R5: leaks :last-stamp and withheld stamps of others' decisions in a :may-not-see lane"
    :make #(model/make-target {:closed-lane-leaks-stamps? true})}

   {:name :too-large-stops-line
    :breaks "R8: stops returning entries when encountering a decision whose thing is :too-large"
    :make #(model/make-target {:too-large-stops-line? true})}])
