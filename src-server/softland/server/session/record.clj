(ns softland.server.session.record
  "A session's record: its row, the two events that change it, and what a
  credential's standing is at a moment. Pure code. It needs no cluster and
  reads no clock: a caller hands it the time.

  A session is the life of one credential: a sitting for a person at a
  browser, a run of a command-line program, a run of an agent's harness. It
  outlives a connection, so a maker that sends an act again after a break
  sends it under the same `:session`, and the store's digest of the act is
  the same (`softland.store.door.digest`). That a session is this, and not a
  connection, is a session's choice of 20 November 2026 (call 3 of
  src/proposal/server-2026-11-20/PROPOSAL.md), for Sid to turn.

  The row is kept under the hash of the credential and never under the
  credential: the log of the server's module is read by every program
  inside the network, and a hash gives nobody the token.

    {:state :live  :who id :session uuid :until ms :began ms}
    {:state :ended :who id :session uuid :until ms :began ms :ended ms}
    {:state :ended :ended ms}        a hash that was refused before it began

  Two events change a row, and each is a plain map on the module's depot:

    {:event :began :hash h :who id :session uuid :until ms :at ms}
    {:event :ended :hash h :at ms}

  A hash is bound once. The first `:began` for a hash makes its row, and no
  later `:began` changes who it stands for. `:ended` is for good. So the
  three standings of a credential are in one order, not known, then live,
  then ended, and a later word never goes back to an earlier one. `join`
  rests on that order: of two things that a program learned about one hash,
  in any order of arrival, the later standing is the true one.

  Inputs and results, by call:

    began, ended  the parts of an event -> the event
    event?        anything -> whether it is one of the two events
    after         a row or nil, and an event -> {:row :tell?}
    join          two rows, either nil -> the one that is further on
    standing      a row or nil, and the time now -> one of four standings

  State and effects. None.

  Callers. `softland.server.module`, which runs `after` in its topology;
  `softland.server.sessions` and `softland.server.seam`, which read
  standings; the pure proof."
  (:import [java.util UUID]))

(def hash-pattern
  "The text of a credential's hash: 64 lower-case hex characters, the
  SHA-256 of the token's text (`softland.server.session.credential`)."
  #"[0-9a-f]{64}")

(defn hash-text?
  "Takes anything. Gives true for the text of a credential's hash."
  [x]
  (boolean (and (string? x) (re-matches hash-pattern x))))

(defn- id?
  "Takes anything. Gives true for a thing that can be a who: a keyword or a
  UUID. The store's own test of an id is narrower
  (`softland.store.act/readable-keyword?`); the server makes each who that
  it writes here, so this test only keeps a wrong class out of a row."
  [x]
  (or (keyword? x) (uuid? x)))

(defn began
  "Makes the event that begins a session. Takes the credential's hash, the
  who that it stands for, the session's id, the time until which it lasts
  and the time now, both in milliseconds since 1970."
  [hash who session until at]
  {:event :began :hash hash :who who :session session :until until :at at})

(defn ended
  "Makes the event that ends a session, or refuses a hash for good before
  it began. Takes the credential's hash and the time now."
  [hash at]
  {:event :ended :hash hash :at at})

(defn event?
  "Takes anything. Gives true for one of the two events in its form. The
  topology drops a thing that is none: anything can append to a depot."
  [x]
  (boolean
   (and (map? x)
        (hash-text? (:hash x))
        (int? (:at x))
        (case (:event x)
          :began (and (id? (:who x))
                      (instance? UUID (:session x))
                      (int? (:until x)))
          :ended true
          false))))

(defn after
  "What an event does to a row. Takes the row under the event's hash, or
  nil for none, and an event that passed `event?`. Gives {:row :tell?}:
  the row from now on, and whether the change is told on the feed.

  A `:began` makes a live row only where there is none. On a live row of
  the same session it changes nothing and is told again: Rama can run an
  event a second time, and a second telling costs a listener one more look.
  On any other row it changes nothing and tells nothing: a hash is bound
  once, so nobody can turn a credential that is in use, or was refused,
  into her own.

  An `:ended` ends a live row, and makes an ended row where there is none.
  On an ended row it changes nothing and is told again."
  [row event]
  (case (:event event)
    :began (cond
             (nil? row)
             {:row {:state :live
                    :who (:who event)
                    :session (:session event)
                    :until (:until event)
                    :began (:at event)}
              :tell? true}

             (and (= :live (:state row)) (= (:session row) (:session event)))
             {:row row :tell? true}

             :else
             {:row row :tell? false})
    :ended (if (= :ended (:state row))
             {:row row :tell? true}
             {:row (assoc (or row {}) :state :ended :ended (:at event)) :tell? true})
    {:row row :tell? false}))

(defn- rank
  "The place of a row in the one order of a hash's rows: 0 for none, 1 for
  live, 2 for ended."
  [row]
  (case (:state row) :live 1 :ended 2 0))

(defn join
  "Takes two rows of one hash, either of which can be nil. Gives the one
  that is further on in the order none, live, ended; the first when they
  are level. A program that learned of a hash from a read and from the feed
  takes the join, so the order in which the two arrived does not matter."
  [a b]
  (if (< (rank a) (rank b)) b a))

(defn standing
  "What a credential stands for at a moment. Takes the row under its hash,
  or nil, and the time now in milliseconds since 1970. Gives one of:

    {:state :not-known}                              no row: nobody said yes
    {:state :live :who id :session uuid :until ms}   she is signed in
    {:state :over}                                   a live row whose time
                                                     has passed
    {:state :ended}                                  signed out, or refused

  `:over` is worked out here from the row's `:until` and is written
  nowhere: no event marks the passing of time."
  [row now]
  (case (:state row)
    :live (if (< now (:until row))
            {:state :live :who (:who row) :session (:session row) :until (:until row)}
            {:state :over})
    :ended {:state :ended}
    {:state :not-known}))
