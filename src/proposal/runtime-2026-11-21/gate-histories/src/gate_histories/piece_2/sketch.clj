(ns gate-histories.piece-2.sketch
  "A sketch of a gate that takes the form of 22 November for a yes for one
  act (store-piece-2/SPEC.md), put in place of four functions of the
  store's gate for a run only: `clojure -M:piece-2 sketch` and `clojure -M:piece-2
  sketch mutants`. Pure code. No file of the store changes.

  Why. Before the store takes the form, every act under a yes is refused by
  the gate as committed, so a run cannot show that its statements can be
  held by any gate of the form, or that its broken copies that touch a yes
  can be caught. This is the judge's check of its own statements and
  steps, made from the spec's words. It is not the store's fix and binds
  nobody: where a gate writes the use of a yes, how it tells a used yes,
  and what words it gives, are the store's builders' to make. What a run
  under the sketch says is what a run says of a gate that keeps the form;
  what the store does is what the run says of the store.

  What it puts in place, each around the store's own function:

    `decision/hex-for-its-permission`
        the text of the act as its maker made it: SHA-256 of the one text
        of the one form that `softland.store.act/parse` gives of the
        maker's part of the thing, without its act-id, its depot, its time
        and its permission
    `decision/standing`
        the store's own, with the act-id playing no part in a yes: its
        check of the act-id's uuid against the yes is met by handing it an
        act whose act-id holds the yes's id
    `decision/writes`
        the store's own, and for an accept under a yes one row more, before
        the act's own rows: an end at the yes's place, which uses it up. So
        a used yes reads as ended (`:permission-ended`), and an end of it
        finds no permission to end
    `decision/store-key-fault`
        the store's own, and a give at a yes's place that holds a row, the
        yes itself, its end or its use, is refused `:a-yes-is-given-once`

  `redefs` is the map of the four, for `with-redefs-fn`."
  (:require [softland.store.act :as act]
            [softland.store.door.digest :as digest]
            [softland.store.door.text :as text]
            [softland.store.gate.decision :as decision]
            [softland.store.record.rows :as record]))

(defn- covers-an-act?
  "True when an act's permission is a yes for one act."
  [the-act]
  (= :act (first (act/covers (:permission the-act)))))

(def redefs
  "The four functions of the sketch, by the var of the store's function that
  each takes the place of, for `with-redefs-fn`."
  (let [standing @#'softland.store.gate.decision/standing
        writes @#'softland.store.gate.decision/writes
        store-key-fault @#'softland.store.gate.decision/store-key-fault]
    {#'softland.store.gate.decision/hex-for-its-permission
     (fn [thing the-act]
       (when (covers-an-act? the-act)
         (when-let [one-form (:ok (act/parse (dissoc thing :act-id :depot :door-time)))]
           (digest/hex-of (dissoc one-form :permission)))))

     #'softland.store.gate.decision/standing
     (fn [gate-depot the-act permission-row depot-row chain-rows hex]
       (standing gate-depot
                 (if (covers-an-act? the-act)
                   (assoc the-act :act-id (assoc (:act-id the-act) 2 (second (act/covers (:permission the-act)))))
                   the-act)
                 permission-row depot-row chain-rows hex))

     #'softland.store.gate.decision/writes
     (fn [act-id the-act refused stamp time-millis thing-digest in-hand]
       (let [rows (writes act-id the-act refused stamp time-millis thing-digest in-hand)]
         (if (and the-act (nil? refused) (covers-an-act? the-act))
           (let [yes (second (act/covers (:permission the-act)))
                 use (record/fact-row {:e yes :k :softland.store/act-permission :v nil :text nil}
                                      (nth act-id 2) (count (:facts the-act)) stamp time-millis
                                      (text/text (:who the-act)) (text/text (:permission the-act)))]
             (into [[:fact (decision/permission-place the-act) use]] rows))
           rows)))

     #'softland.store.gate.decision/store-key-fault
     (fn [gate-depot the-act rows fact]
       (if (and (= :softland.store/act-permission (:k fact))
                (some? (:v fact))
                (get rows (record/place-key (:lane the-act) (:e fact) (:k fact))))
         :a-yes-is-given-once
         (store-key-fault gate-depot the-act rows fact)))}))
