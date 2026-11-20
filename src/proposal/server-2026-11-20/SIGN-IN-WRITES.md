# What sign-in writes

**20 November 2026.** Written by the session that built the server, for Sid's
redline. He has not seen this page. The proposal asked for it before any code,
and he said go while he was away, so the build took the least form that his
own lines leave and put each choice behind one function. Everything here runs
on a store that is thrown away. Nothing here is decided.

The code is `src-server/softland/server/sign_in.clj`.

## His lines that it stands on

- "sign-in gives us a bunch of data and we just store that data in rama"
  (15 October, ‹3165b020›).
- The server finds her id from Google's account id, or registers her, and
  writes that id as the who of each act that she makes (his go of 19 November,
  ‹afbff6f5›, to a session's answer).
- A person's id is a UUID that says nothing, and her sign-in accounts are
  joined to it by links (`src-store/README.md`, 9 October, entry 1).
- Google's number is kept as it is (15 October, ‹5325b353›: "1. A").
- A signed-in user has a personal layer and a permission in it, without asking;
  the system makes both at her first sign-in (the same page, 8 October, entry
  2).

## What is written

```text
at the server's start, once                       who: the system

  layer  :people                  made by its first act; only the system sees it
  fact   :people  :softland.store/marked-keys  #{:account/google}

at a first sign-in of a Google account            who: the system

  one act in :people, on a new random id P:
    P  :account/google       "1082…"      Google's number, as given
    P  :person/own-layer     L            a new random id
    P  :account/google-says  {…}          what the token said, under Google's names

  one act that makes her own layer L:
    L  :softland.store/depot       :streaming
    P  :softland.store/permission  [P L L]         her first permission

at each later sign-in                             who: the system

  one act in :people:
    P  :account/google-says  {…}          what the token said this time
```

And in the server's own module, not in the store: the session. One event,
`{:event :began :hash … :who P :session … :until … :at …}`, makes the row
under the hash of her credential.

## How she is found

`:people` marks `:account/google`, so the read path's "what points at a thing"
gives the entity that holds a number. That is the store's own way to ask it,
and it adds no table.

When two first sign-ins of one account run at the same moment, two ids hold the
number. The one whose fact has the lower stamp is the person, for every copy of
the server and for good. The other id is never read again. The seam's proof
runs that through two copies of the server and gets one person.

## What is not written

No name, no invitation, no kind of actor, no limit, no block, no agent, no
record of sessions in the store. The build of 15 October wrote those and was
taken off for it.

## The choices, each for your word

| # | The choice | Built as | The other side |
|---|---|---|---|
| 1 | The fact that holds the link | On the person, under a key for the kind of account. A second Google account for one person has no place; another kind of account is another key | An entity for each link, with the person and the account as its facts |
| 2 | Where the links live | In one layer of the system's, which only the system may see. A layer is one line on one task, so every sign-in of every person, and each `:who-am-i`, lands on that one task | In the server's own module, beside the sessions, spread by the number: one read. Your line of 9 October says that the store can erase a link, which reads as the store |
| 3 | What of Google's token is kept | Every claim, as given, written again at each sign-in. The line of acts so holds what each sign-in gave: her name, her email and her picture, each time, for ever. The review of 20 November held this against your line of 15 October, "no readback and no other thing created" | Only `sub`; or `sub`, `email` and `name`; or once, at the first sign-in |
| 4 | Who may become a person | Anybody whom Google proves | An invitation, or a list of accounts. Due before a stranger can reach the server |
| 5 | How she learns where her own layer is | The server reads it for her, as the system, at `:who-am-i`: one fact, and nothing that Google said | A fact that she may read herself; or the session's row carries it, which takes the read off the one task of row 2 |
| 6 | The words | `:people`, `:account/google`, `:account/google-says`, `:person/own-layer` | Yours |
| 7 | Whether the base is made here | No. Nothing of this build needs it | The server's start makes it, as the system |

## What stays open from before

- **How you come to hold rights in the system's layers.** The answer that you
  agreed to on 19 November: setup names your Google account, and the system
  gives your id rights at your first sign-in. Its form is not designed, and
  nothing here does it.
- **Who may act as the system.** Here: the server's own steps above, and
  nothing that a system sends, since a word has no place for a who.
- **The system's id:** `:system`, as today.
- **A visitor's trail,** and whether a sign-in is kept as an event of its own
  beside these facts.
