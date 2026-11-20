# Session — the pure parts of a session

[Parent: server](../README.md) · [Server source entry](../../../README.md).

This folder holds the rules of a session that need no cluster. The Rama module
that keeps a session's row, [module.clj](../module.clj), and the client of that
module, [sessions.clj](../sessions.clj), are one folder up; they hold no rule of
their own and call these.

All of this folder is pure code, but for one call that reads the JVM's secure
random numbers.

| File | Responsibility | State and lifetime |
|---|---|---|
| [record.clj](record.clj) | A session's row, the two events that change it, what an event does to a row, the one order of a hash's rows, and what a credential stands for at a moment. | None. |
| [lately.clj](lately.clj) | The feed of the rows that changed lately: its value, what a change does to it, and what one copy of the server takes from a value. | None. |
| [credential.clj](credential.clj) | A token, its hash, the short code that a person compares, and the reading of a token from a header and from a cookie. | None; `make` reads the JVM's secure random numbers. |

## How the files relate

[record.clj](record.clj) requires nothing. [lately.clj](lately.clj) requires
nothing: it carries a row and reads nothing inside it.
[credential.clj](credential.clj) requires nothing of this tree; the form of a
hash that it makes is the one that [record.clj](record.clj) tests.

[module.clj](../module.clj) runs `after` of the record and `after` of the feed
in its topology. [sessions.clj](../sessions.clj) runs `news` of the feed in the
callback of its one push. [seam.clj](../seam.clj) reads a row's `standing` and
takes the `join` of what a read and the feed gave it.

## What holds, and why it is built so

**A hash is bound once, and an end is for good.** The rows of one hash go one
way: none, then live, then ended. So no step anywhere rests on the order of two
events, of two callbacks, or of a read and a callback: the join of two rows is
the one further on. A session that begins twice under one hash stands for the
first who, so a link that somebody else sent her cannot turn a credential in
use into hers.

**No event marks the passing of time.** A row holds the time until which its
session lasts. Whether that time has passed is worked out from the server's
clock where a standing is asked for, and is written nowhere.

**The feed is state and not a line of events.** A push of Rama's tells the
present. The feed holds the last changes with a number each, so a copy that was
left out of a callback finds what it missed in the next value, and a copy that
fell further behind is told that it missed changes.

## Not decided, kept for now

| The thing | Built as | Where the other answer would be made |
|---|---|---|
| The form of a token, its length and its hash | `sl1_` and 32 random bytes; SHA-256 as hex | [credential.clj](credential.clj) |
| The name of the browser's cookie | `softland` | `cookie-name` in [credential.clj](credential.clj) |
| How many changes the feed holds | 32 | `kept` in [lately.clj](lately.clj) |
| What a row holds beside who and when | Nothing: no kind of actor, no label of what asked | `after` in [record.clj](record.clj) |
