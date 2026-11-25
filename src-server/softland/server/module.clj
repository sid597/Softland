(ns softland.server.module
  "The Rama module `Sessions`: what the server keeps, and all that it keeps.
  It requires Rama.

  The server is one program in front of the store, and nothing true lives
  in its memory: a second copy of it shares nothing with the first but
  Rama. A who is proven at sign-in and then stands behind a credential, so
  one thing has to last between two requests and reach every copy: which
  credential stands for whom. That is this module. It is the server's own
  and not a part of the store's module (`softland.store.module`): a row
  here is found by the hash of a secret, which no reader of the store may
  be served, and the store's two depots of acts stay the store's whole
  ground. That a session's record lives here, and not as facts in a lane,
  is a session's choice of 20 November 2026 (call 1 of
  src/proposal/server-2026-11-20/PROPOSAL.md), for Sid to turn.

  What a client of the module opens, by name:

    *session-events    the depot: each event of a session, as it was sent
                       (`softland.server.session.record`)
    $$sessions         the row of each credential, under its hash: select,
                       with the hash as the partitioning key
    $$sessions-lately  the feed of the rows that changed lately
                       (`softland.server.session.lately`): one value, on
                       one task, for one push from each copy of the server

  The name of the module, for a client, is `(get-module-name Sessions)`.

  One event. The depot partitions by the hash, so the events of one
  credential are one line on one task, and that task holds its row. The
  topology is a stream topology: a sign-in appends with the ack level
  `:ack` and comes back when the row is written and the feed has it, so the
  next request under the credential finds the row. For each event, on the
  hash's task: read the row, ask `softland.server.session.record/after`
  what the event does to it, and write the row when it differs. Then, when
  the change is to be told, one hop to the feed's task and one write of the
  feed's value.

  When an event is run again. Rama runs a stream event again after a
  failure, so each step holds when it is done twice. `after` gives the same
  row for an event that the row already holds, and tells it again. A second
  telling gives a hash a new number on the feed with the row that it had,
  and a listener that is told a row twice does nothing the second time. The
  row is written before the hop, so a copy that the feed told of a change
  never has to read a row that is behind it: the feed's entry holds the
  row.

  Order. No step rests on the order of two events of one hash: the rows of
  a hash go one way, none then live then ended, and `after` never takes a
  row back. A `:began` that arrives after its `:ended` changes nothing.

  State. The depot and the two PStates, durable in the cluster. Nothing is
  deleted: an ended row stays under its hash, and the depot is not trimmed.
  Not decided: how long the events and the rows of ended sessions are kept.
  The store's rule that a depot of acts is never trimmed
  (src-store/README.md, 12 October 2026, entry 3) is of the store's depots;
  nothing here is one of them.

  What grows with the system, and where it would give first. The feed is
  one value on one task: every beginning and every end of a session, of
  every person, hops to that task, writes the feed's whole value, and is
  pushed whole to every copy of the server. Rama's own guidance keeps a
  global PState for a thing of which there is one, and warns against
  sending every write of a kind through one task. It is built so because a
  session begins and ends far more rarely than anything else happens, and
  one push for a copy needs one place to push from. When that task is the
  limit, the way on is several feeds, each under a key of its own and so
  on a task of its own, with one push for each from every copy. Not built,
  and not measured.

  What the module does not check. Who appends: Rama asks no name, and any
  program that can open Rama's ports can append a `:began` for any who.
  Only programs on machines that Sid runs may reach the ports
  (src-store/README.md, 11 and 12 October 2026, entry 4), and the server is
  the one program that is meant to append here."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [softland.server.session.lately :as lately]
            [softland.server.session.record :as record]))

(defn- change
  "Takes the row under an event's hash, or nil, and the event. Gives
  [the row from now on, whether the row differs, whether it is told], as a
  vector for the dataflow to take apart."
  [row event]
  (let [{after :row tell? :tell?} (record/after row event)]
    [after (not= row after) (boolean tell?)]))

;; The depot, the two PStates and the one topology. `defmodule` takes no
;; docstring; it is set below.
(defmodule Sessions [setup topologies]
  (declare-depot setup *session-events (hash-by :hash))
  (let [sessions (stream-topology topologies "sessions")]
    (declare-pstate sessions $$sessions {String Object})
    (declare-pstate sessions $$sessions-lately Object {:global? true :initial-value (lately/start)})
    (<<sources sessions
      (source> *session-events {:start-from :beginning} :> *event)
      (<<if (record/event? *event)
        (get *event :hash :> *hash)
        (local-select> (keypath *hash) $$sessions :> *row)
        (change *row *event :> [*after *differs? *tell?])
        (<<if *differs?
          (local-transform> [(keypath *hash) (termval *after)] $$sessions))
        (<<if *tell?
          (|global)
          (local-select> STAY $$sessions-lately :> *feed)
          (lately/after *feed *hash *after :> *feed-after)
          (local-transform> [(termval *feed-after)] $$sessions-lately))))))

(alter-meta! #'Sessions assoc
             :doc "The server's module: the depot `*session-events`, partitioned by the
  hash of a credential; the rows `$$sessions`; and the feed
  `$$sessions-lately`, one value on one task.")
