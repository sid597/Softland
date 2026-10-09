# Full-Spec Review

Verdict: pass

Summary: One session (Opus 5.5, 10 October 2026) held the door and its proof against the whole spec:
the contract `BUILD-door.md`, Sid's decisions in `src-store/README.md`, the rulings on P1 to P22 with
rules A and B, the rulings 1 to 18 on the plan, and `src-store/CLAUDE.md`. Three hunts were made. The
first, written down before the builder's three validation papers were read, found four faults of the
door's code and three of its explanations; the second, after those papers, found two more faults of
explanations, one check that the proof lacked and one fault that the review itself had just put in;
the third found nothing new. The four faults of code: one maker's act could hold the seat of a stock
for good; a call threw for a `:who` that cannot be compared; a read was started after a call's wait
had ended, against three of the door's own docstrings; and a word named a request as open that the
call had just ended. Each is fixed in `door.clj` and its pure parts, each has a case in the door's
proof that fails for the door as committed, and each fix was held against a door made wrong again.
The proofs gained 5 groups and 31 checks on the cluster and 2 cases without one, among them four
things that the builder's own papers named as not reached. The four proofs pass: act 150, pure 540,
module 227, door 193, the door's five times in its final shape. One sentence outside the paths that
this session may touch is untrue of the door in one corner and still stands; it is the first point
for the build session, and the verdict is given with it named and not with it passed in silence.

## Items found and fixed

"Before" is the tree at `b3d59cc1` (the door `f33177cb`, its proof `1626bab5`). Line numbers are of
the files as this session left them. `review/` is
`/mnt/data/projects/research/softland/door-build-2026-10-09/review/`.

| Location | Why it violated the spec | Fix applied | Evidence it holds |
|---|---|---|---|
| `door.clj`, `ask` (the digest of the door's request, taken in the seat) and `locks-for` | Implied spec O3, "Concurrency": "a maker never waits without a bound on a request that is not its own"; O4, "Throughput": "two sends wait on each other only where they take locks from one stock, and that taking is short"; `locks-for`: "The seat is given up [...] on every way out." The request holds the maker's `:version`, `:who`, `:permission` and `:session` as given, and its digest was taken while the call sat in the seat. A part that does not come back when it is read held the seat for good: no later call of that layer and who could ask for locks, and the door cannot check who names a who (part 6), so any maker could do it to any who. | `locks-for` walks those four parts once before it goes for the seat (`door.clj:670`). A reading now holds them as `:carried` (`additions.clj:274-307`), and `request` is made of them and reads nothing of the maker's thing again (`:413-439`), so it cannot throw in the seat. Docstrings of `locks-for`, `send-act` ("Limits"), `reading`, `request`; `door/README.md`. | Before, probe part 2: "map [[\"[:p2 :streaming :p2]\" :calls 1 :open false :unused 0 :seat-free 0]]", and for another maker of that who, twice, "{:could-not :get-locks-within-its-wait, :waited-ms 400}". After: ":seat-free 1", and "{:ack :gate-ran}" twice. Proof, "the seat and the wait", three checks; the door as committed fails them, and so does the wrong door r01 (the walk taken out). |
| `door.clj`, `locks-for` and `enter!` | Implied spec O4.11: "The door's calls give the maker a result and do not throw for what a maker gave"; the namespace docstring: "No call throws for what a maker gave." The stock's key was tested for its hash and not for whether it can be compared, and the count into the door's map was outside any guard. | The count into the map is guarded and ends in `:read-the-act` at `:who`; the hash is still tested first, since a small map does not hash a key and a who that cannot be hashed would get in (`door.clj:659-660`; `enter!`'s docstring says why). `leave!` never throws (`:402-410`). | Before, probe part 8: `send-act` threw "java.lang.ClassCastException: class clojure.lang.Keyword cannot be cast to class java.lang.String". After: "{:could-not :read-the-act, :at :who}". Proof, "a who that cannot be a key", five checks; the door as committed gives an error there; wrong doors r04 (no guard) and r08 (no test of the hash) fail it. |
| `door.clj`, `settle`, entered from `ask` after the append | `settle`: "No step is started after the call's wait has ended"; `in-the-seat`: "A step is started only while the call's wait has not ended"; the builder's own validation, divergence 6: "The code follows the general sentence". It did not on one way: after an append that outlasted the wait, the first read of the answer was made. `locks-for` and `door/README.md` said "one append and one query", which is what the code did and not what the other three said. | `settle` starts no read when the wait has ended (`door.clj:482-486`); the request stays open as it was. `locks-for`, `settle` and `door/README.md` now say one thing: the call in the seat outlasts its wait by the one call of Rama's client that was on its way, an append or a query. | Before, probe part 1 (wait 300 ms, the append made to take 500 ms more): "calls [[:append-begins 1 :request] [:append-returns 527] [:query-begins 528 2] [:query-returns 529]]". After: "calls [[:append-begins 1 :request] [:append-returns 512]]". Proof, "the seat and the wait": "the one call of Rama's client in that send is the append, and no read follows it"; fails for the door as committed and for r02. |
| `door.clj`, `in-the-seat`, the branch `(passed? ends)`; `words.clj`, `:get-locks-within-its-wait` | Implied spec O6.1: "A word about a request says what became of the request". When the wait ended after the call had read an earlier call's request to its end (refused, or its locks read and the stock still short), the word carried that request's act-id, and its `:says` said that a request "stays open". | The word names no request there (`door.clj:594`); `waited-word`'s docstring and the word's `:says` say when it names one and that the one named stays open. | Before, probe part 6: "carries :request? true the same request? true | open requests after {... :open-requests 0}". After: "carries :request? false". Proof, "the seat and the wait": "The word names no request, since none of this call stays open"; fails for the door as committed and for r03. |
| `words.clj`, `:read-its-requests-locks` | The same clause of O6.1. The word has two fates of its request, open after a throw and ended after a read that came back, and its `:says` gave neither. | The `:says` gives both, and how a maker tells them (the word names the class of the throw, or not). | Read against `settle` (`door.clj:499-518`) and `ask` (`:556-557`). The proof's group 11 holds both branches. |
| `stock.clj`, `take-locks`; `door/README.md`, "The stock" | `src-store/CLAUDE.md`: "Does the code do what its explanation says?" Both said that an act names one request, or two at a border. An act names three where the stock held a rest, the door read an earlier call's open request, and then asked for its own; the plan says so and the tree did not. | Both say when it is more than two, and that it is then one request for each run that the act crosses. | Probe part 3: "requests that its act names: 3 | indexes by request: [[3] [0 1 2 3] [0 1 2 3 4]]", accepted. Proof, new group "an act over three runs", four checks. |
| `door.clj`, `locks-for` and the namespace docstring; `door/README.md` | `src-store/CLAUDE.md`: "say how far the claim reaches". "A who that is no id gets a stock that never fills, because a gate refuses its request" is untrue of a who that Rama's client cannot turn into bytes: no gate sees its request, and its stock stays with a request in doubt; a plain Java object leaves one stock for each call. The plan says it ("Resource usage analysis"); no docstring did. | The three places say it. | Probe part 7: "5 sends [...] stocks in the door's map 5 {:unused-locks 0, :stocks 0, :open-requests 5}". No change of code: the cost is the plan's, accepted by the rulings. |
| `door.clj`, namespace docstring and `act-ids-for`; `additions.clj`, `series-word`; `stock.clj`, `numbers`; `words.clj`, `:make-an-act-id` | "No call throws for what a maker gave" had no limit, and `act-ids-for` makes its whole vector for any count. "An integer of 1 or more" was untrue of a big integer, which the code does not take. | The namespace docstring says how far the sentence reaches; `act-ids-for` says that a count beyond the memory ends in the JVM's own error; the four places say "fixed-width integer". No bound was added: see point 5. | Read. Pure proof: the case of a count that is no count takes `3N` too. |
| `door/README.md`, "Words a session made" | D7: "Every word the door makes [...] is listed for Sid". The parts of a reading, of a stock in the door's map and of a lock as a stock gives it were not listed. | Three rows: a reading with `:thing`, `:carried`, `:entries`, `:values` and an entry's `:fact`, `:plain`, `:place`; `:calls`, `:cell`; `:lock`. | Read against `additions.clj`, `door.clj:349-376`, `stock.clj:214-256`. |
| `door_proof.clj`: no case for the digest of the door's own request | D4: "The door reads the record by act-id, and compares digests." Part 3: "it reads the request's answer by act-id and then its locks". A door that takes any answer under its request's act-id for its own passed. | New group "a request's answer with another digest", four checks: the word with "not mine", no read of locks, the record's own answer an accept with 4 locks made, and the send after it asks anew. | Run, `mutants/gaps.py`: that wrong door against the proof as committed, "exit 0 {:test 21, :pass 162, :fail 0, :error 0}". Against the new proof (r05): "{:test 26, :pass 189, :fail 3, :error 0}". |
| `door_proof.clj`: nothing read the door's map of stocks | The plan, "The memory of the door's process": a stock that holds nothing "is taken out of the map", "so a maker that sends value acts under whos that do not stand grows no memory in the door"; the build session's ruling: "that a seat is given up on every way out of a call". The proofs' README listed it as not reached. | The proof reads the map from the door's own field (`stocks-of`, `stock-told`): a refused who's stock has left; a stock with an open request stays; and at the end, over every stock of the four doors that hold locks, no call is counted in, no seat is taken, no request is open. Three checks more in groups 9, 11 and 26. | Run, `gaps.py`: a door whose stocks never leave, against the proof as committed, "exit 0 {:test 21, :pass 162, :fail 0, :error 0}". Against the new proof (r06): three checks fail; a door whose calls are never counted out (r07): ten fail. |
| `door_proof.clj`: nothing held a maker that waits for a seat to its own wait | Implied spec O3, "Concurrency": "a maker never waits without a bound on a request that is not its own". The one case of a second maker (the paused gate) passes for a door whose wait for the seat has no time limit, because the first maker leaves the seat in time: by reading. | In "the seat and the wait": a second maker waits for the seat while the first is inside an append of 600 ms; it is back at its own 400 ms, inside 540 ms, with a word that names no request and with no call of Rama's client made. | r13 (the wait for the seat has no time limit): "{:test 26, :pass 192, :fail 1, :error 0}", that check. |
| `door_proof.clj`, `pure_proof.clj`: four things that the builder's papers named as not reached | The stage's doc: "a suite that enumerates a class of invalid inputs and omits one member of that class is a gap". | A request in doubt whose append truly failed, met 200 ms after the throw ("the seat and the wait"). The opener's numbers other than the wait, and a wait beyond the longest ("the opener's numbers", three checks; one pure case). A bound greater than any wait ("round trips are counted"). A who that cannot be hashed ("a who that cannot be a key"). | r09, r10, r11 and r12 each fail the new checks. By reading, no check of the proof as committed fails for r09, r10 or r12: the cluster was run with the defaults and one other wait, and with no bound over the longest wait. Probe part 10 for the first: "took 484 calls [[:query-begins 0 1] [:query-begins 1 2] [:query-begins 202 2] [:query-begins 403 2] [:query-begins 450 2] [:append-begins 451 :request] ...". |
| `pure_proof.clj`, the case of a fact whose `:e` is no id | The table of things that are no act, row 16: "A fact with a value whose `:e` or `:k` is no id or no key". The proofs had `:e` only. | The case takes a fact whose `:k` is a string too. The count of cases is the same. | The pure proof passes with it. |
| `test-store/softland/store/README.md` | `src-store/CLAUDE.md`: "The map of a check says how to run it, what a pass shows, what state it changes outside the code that it checks, and what it does not reach." | The door's proof: five doors; groups 1 to 26; six kinds of stand-in; the two objects of the proof's own that a maker's act holds; that it reads the door's own field; what it still does not reach; and a list of the checks that rest on a time. | Read against the proof. |

Found by this session in its own change, before any run, and fixed: with the count into the map
guarded, the first version dropped the test of the hash. A map of up to eight entries does not hash
its keys, so a who that cannot be hashed would have got in, and a later call of another who would
have met the throw when the map grew. The test is back (`door.clj:659`), `enter!`'s docstring says
why, and the wrong door r08 fails two checks.

## Points for the build session

None is fixed against a ruling, and none decides a thing of Sid's.

1. **A sentence outside this session's paths that is untrue of the door in one corner.**
   `src-store/softland/store/README.md:214-216`: "The door's own acts name the locks of one request,
   or of two, in runs of neighbours". The plan's table of sentences gave that wording (row 7), and the
   ruling on P9 says that the two sentences are "made true". The plan's own design says when it is
   more: "It names more than two only when the door had fewer locks left than the act needs and read
   the locks of an earlier, open request first." The proof's group 14 shows three. The clause that is
   then not met is P15's, "Each sentence that the door makes untrue is changed". The fix is one
   clause; the door's own files now carry it (`stock.clj:225-230`, `door/README.md:147-149`).
2. **The walk before the seat is a new choice of the builder's, and it is a bound on one kind of
   thing only.** It shows that the four parts came back once. A part that comes back at one read and
   not at the next can still hold the seat, in the digest of the request or in Rama's own writing of
   it; `locks-for` says so. The other answer, a request that carries no part of the maker's as given,
   is against part 3 ("whose `:who` and `:permission` are the maker's"). For the receipt, as a
   provisional choice, with `:carried` as a word that the review made.
3. **A request in doubt whose append truly failed, met at once.** As ruling 12 has it, the request
   gets the whole of the door's wait from the throw. A call that comes at once after the throw reads
   until that moment, which is the moment when its own wait ends: it forgets the request and ends in
   `:get-locks-within-its-wait`, and the call after it asks anew. Ran, probe part 10: "the same with
   the next send at once: {:could-not :get-locks-within-its-wait, :waited-ms 601} [...] | open requests
   0 | the send after it: {:ack :gate-ran}". The plan's table of a request's life has the two outcomes
   apart ("forgotten, and this call goes to D0 with its own"; "the word; the request stays in doubt")
   and not this third. No ruling is broken: it is what the ruling costs a maker that comes back at
   once, two calls and one whole wait. With the defaults that is 10 seconds. For the receipt.
4. **The proof now reads one field of the door's type**, the map of stocks, beside the logs and the
   record. It is a third way round the door, said in the proof's docstring and in the proofs' README.
   A maker has no such way through the six calls; code of the same process has, as the door's own
   docstring says of any object.
5. **`act-ids-for` has no bound on its count.** The check of the plan found it (F12) and the plan
   keeps it as a limit that the docstring says. The ruling on P9 names two bounds and no third, so
   this session added none; the namespace docstring's "No call throws" now carries the limit.
6. **A who that is a plain Java object leaves one stock for each call** whose request's append threw
   (the plan's F11). Known and accepted; now in the docstrings. What would bound it is a stock's key
   made of the one text of the who, which is another design of the key that ruling "accepted as the
   plan has them" covers; not built.
7. **`IMPLEMENTATION_VALIDATION.md`, divergence 6**, says that the code starts no step after the
   call's wait has ended. On one way it did; it does now.
8. **`door/PROGRESS.md` and the receipt** hold the counts 538 and 162 and "three doors"; they are the
   build session's pages. After this session: pure 540, door 193 in 26 groups, five doors.

## Rulings of the build session on the review

By the build session, 10 October 2026. It read every line that the review changed in the door's four
source files (the diff against `1626bab5`) and ran the four proofs itself on the tree as the review
left it; the count lines are on the build's page. Each ruling is the build session's and is Sid's to
strike.

Before these, one thing that the build's page must not hide. The build session read the door's four
source files whole before it committed them, and wrote that it found no fault. The review then found
four faults in that code: a maker's act could hold a stock's seat for good; `send-act` could throw
for a who that throws when it is compared; a read was started after a call's wait had ended; and a
word named an ended request as open. It also found two wrong doors that the committed proof passed:
one that ignores the digest of the door's own request, and one whose stocks never leave its memory.
The first two faults are of things that a maker gives, which the build session's read had followed
only as far as the tests of the act, and not into the seat and the door's map.

1. **Point 1: the store's map is changed**, by the build session, in the commit that holds the
   review's fixes: the door's own acts name the locks of one request, of two at a border, and of
   more only where a call first read a request that an earlier call had left open.
2. **Point 2: the walk before the seat stands as the review built it.** A call walks the parts that
   its request will carry before it goes for the seat, so that a part with no end holds its own
   maker's thread and no seat. It bounds one kind of thing: a part that reads differently the second
   time can still hold the seat, and the docstring says so. For the receipt, with the word
   `:carried`.
3. **Point 3: ruling 12 stands, and its cost is said.** After an append of a request that truly
   failed, a call that comes at once waits the door's whole bound and ends in
   `:get-locks-within-its-wait`; the request is forgotten there, and the call after it asks. A call
   that comes later than the bound pays one read. The plan's row D5 now holds this way out. For the
   receipt.
4. **Point 4: the door's proof may read the door's field `stocks`.** It is the one way to hold a
   stock's leaving of the door's memory to a check, and a wrong door passed without it. The proofs'
   map says that the proof looks inside the door there.
5. **Point 5: `act-ids-for` keeps no bound on its count.** The ruling on P9 names two bounds and no
   third; a count that the memory does not hold ends in the JVM's own error, and the docstring says
   so. For the receipt.
6. **Point 6: the two papers of the build stage stay as that stage wrote them**, with a line at the
   head of each that says which tree they are of.

Carried to the receipt as not reached, by the review's own list: the seat under slowness that Rama
itself causes, since each slow call of the proof is a stand-in; two takes, and the step of the
door's map, forced to one instant; an act of the base that names the locks of two or three requests,
which no run made; and the checks that rest on a time, on one machine.

## The three hunts

### The first hunt, as it was written down before the three validation papers were read

It is kept here as it was written, in the first version of this file, when the session had read the
contract, `src-store/CLAUDE.md`, `src-store/README.md`, the implied spec whole with its rulings, the
plan's design and its rulings 1 to 18, `DECOMPOSITION.json`, the whole source of the door and of what
it stands on, the whole of `door_proof.clj`, the form and groups 14 to 16 of `pure_proof.clj`, the
hand in `module_proof.clj` and the three READMEs under `test-store/`, and had run the four proofs on
the tree as committed (the door's three times) and one probe of eight parts. It had not read
`IMPLEMENTATION_VALIDATION.md`, `TEST_VALIDATION.md` or `PLAN_VALIDATION.md`.

- **H1. One maker's act can hold the seat of a stock for good.** The first row of the table.
- **H2. A call of the door throws for what a maker gave.** The second row.
- **H3. A step is started after the call's wait has ended.** The third row.
- **H4. A word names a request as open that the call has just ended.** The fourth row.
- **H5. "One request, or two" is not how far the claim reaches.** The sixth row, and point 1.
- **H6. Two sentences on a who that is no id.** The seventh row.
- **H7. "No call throws for what a maker gave", and a count that the memory does not hold.** The
  eighth row. Read, not run.
- **H8. Gaps of the proofs**: no case for the digest of the door's own request; no case reads the
  door's map of stocks; no case for H1 to H4; row 16 has `:e` only; no act that names three requests.
- **Traced and found to hold**, by reading and by the probe: no lock goes to two facts or two acts; a
  stock that leaves the map against a call that enters it (part 4: 12 threads, 320 refused requests,
  "errors 0", one stock left, "locks asked for by the who that stands 378 = named in the log 316 +
  unused in the stock 62 : true"); a thread interrupted while it waits for the seat (part 5: the word
  after 300 ms, its mark set, the seat not taken, the call counted out); a maker is handed the
  refusal only of its own request; no path of a lock's bytes to a word, a printed thing, a result or
  a depot.

### What the three papers named, and what became of each

| The lead | Where | What this session did |
|---|---|---|
| "the step of `leave!` that lets an empty stock go, when it races a call that enters" rests on a trace alone | `IMPLEMENTATION_VALIDATION.md`, "What the runs found"; `TEST_VALIDATION.md`, "does not reach" | Forced by many threads, probe part 4, twice: no error, no lock lost, the map right. In the proof the map is now read between calls; the instant itself is still not forced, and the proofs' README says so. |
| "a request in doubt whose append truly failed, met before the door's wait has passed since the throw" | both papers | Probe part 10, and a case 200 ms after the throw. Met at once: point 3. |
| "the cut of a wait to `longest-wait-ms`, and a clock whose numbers wrap" | `IMPLEMENTATION_VALIDATION.md` | The cut: a pure case, a check of six bounds through the door, and a door opened with a wait of `Long/MAX_VALUE`; the wrong door r10 gives an error. A clock that wraps: read only (`after`, `left`, `kept-long-enough?` compare by a difference); not run. |
| "a `:who` that cannot be hashed" | `IMPLEMENTATION_VALIDATION.md` | A case; and the fault H2 beside it, which no paper had. |
| "the door's five numbers at `open` beyond `:request-wait-ms`" | `TEST_VALIDATION.md` | The group "the opener's numbers"; r09 and r12. |
| "two takes of one lock at the same instant, forced" | `TEST_VALIDATION.md` | Not forced. A take is one `compare-and-set!` of the whole value (`door.clj:413-426`); 16 threads on one stock in each of 11 runs of the proof, and 12 threads twice in the probe, named no lock twice. |
| Three checks that rest on a time | `TEST_VALIDATION.md`, "Synchronization" | The proof was run 11 times in all against the right door (3 as committed, 8 after), and no check that rests on a time failed in any. The numbers that vary are under "The runs". The proofs' README now lists each such check. |
| Divergence 6, "No step of a request is started after the call's wait has ended" | `IMPLEMENTATION_VALIDATION.md` | H3. |
| "Wrong doors that were listed and not run [...] a door that never lets a stock that holds nothing leave its map" | `TEST_VALIDATION.md` | Run against the proof as committed: it passes. A check now fails for it (r06). |
| F11, F12 of the check of the plan | `PLAN_VALIDATION.md` | Points 6 and 5; the docstrings now say both. |
| "one handle on many threads rests on one probe run"; "what the microbatch gate does with an act that names the locks of two requests was read [...] and not run" | the rulings on the check of the plan | The first: each run of the proof sends from 16 threads on one set of handles, 11 runs. The second: the base's 4 acts of one request name one request each; an act of the base over two runs is still not run. Under "checked least well". |

### The second hunt, after the fixes of the first

Each fix was read again as new code, and the whole spec was walked again.

- The fault of the review's own, on the hash (above).
- `:read-its-requests-locks` said nothing of its request's fate (fifth row).
- "An integer of 1 or more" (eighth row).
- No check held a waiting maker to its own wait (the row on r13).
- The walk before the seat was claimed too widely; its reach is now said (point 2).
- Two reflection warnings in a helper of the proof; taken out, since the proof loaded with none.

### The third hunt

The whole spec again, the changed lines first: each sentence of parts 1 to 7, D1 to D8, the five
store rules, each ruling, the eighteen rows, each docstring of the four source files and each
sentence of the door's README and of the proofs' README against the code as it then stood. Nothing
new was found. The four proofs were then run once more for the receipt.

## The spec, clause by clause

`D` is `src-store/softland/store/door.clj`, `A` `door/additions.clj`, `S` `door/stock.clj`, `W`
`door/words.clj`; "g" and a number is a group of the door's proof as the proofs' README numbers
them; "pure" is a case of groups 14 to 16 of the pure proof.

### The contract's "What to build"

| Clause | The code | The case that fails without it | Holds |
|---|---|---|---|
| "requiring Rama's client API, and nothing of the gates' or the record's internals: it uses the module's surface only, two depots and three queries" | `D:118-127`: four calls of Rama; `first-act/depot-at-making` and four functions of `record.rows`, by the ruling on P6 | The pure namespaces load under `:store`; g1 opens five handles | Yes |
| Part 1, "holds the two depots' handles and the three queries' handles. It owns no durable state. Its one memory is a stock of locks" | `D:141-148`, `D:683-710`, `open` starts with `(atom {})` | g1 "a door that was just opened holds no lock"; g25 "a door that is opened holds nothing, whatever the record holds" | Yes |
| Part 2, "`act-id-for` a layer gives `[layer depot uuid]`: the layer's depot as the module gives it [...] or the depot that the layer starts with when it is not made [...] and a random UUID [...] the door keeps none of them" | `D:248-267`, `D:712-734` | g2 (not made, then made, another uuid); g20 (the base); g3 (a second send that makes its act-id makes one query again) | Yes |
| Part 3, "a lock-request act whose `:who` and `:permission` are the maker's, whose `:layer` is the act's, and whose one fact is `{:e <the uuid of the request's own act-id> :k :softland.store/lock-request :v n}`" | `A:413-439` | g4 "one request in the log, for 4 locks, under her who, her permission, her session and her version"; pure, three cases of the request | Yes |
| "it reads the request's answer by act-id and then its locks with `locks-of-request`" | `D:440-518` | g20, the calls in their order; g3, 3 queries; g13, the digest | Yes |
| "a lock is used only for values of acts that carry the same `:who` and `:layer` as the request that made it" | `D:658`, the stock's key | g18; g26 over every record of both logs | Yes |
| "A lock's bytes never go into a depot" | `A:413-439` carries a count; `A:345-382` puts `:sealed` and `:lock-id` | g26, 1,419 locks against every record | Yes |
| "A refused request is handed to the maker as the gate's refusal; the door retries nothing on its own" | `D:559-561`; one `append!` of a request on one path | g9; g11 (an earlier call's refusal is handed to nobody) | Yes, as the ruling on P5 reads it |
| "the door never destroys one [...] the receipt counts the rows it left" | No call of the door writes a row | g25, the count held against its sources | Yes |
| Part 4, "reads `:layer` and `:facts` and no more" | `A:106-126` reads six parts | g4 (the request carries four of them) | As the ruling on P4 has it |
| "it takes a lock from the stock and seals the value to its place with `seal.clj`, giving `:sealed` bytes and a `:lock-id`; a fact under a store key keeps its plain value; a retract has nothing to seal" | `A:153-232`, `A:345-382` | g4, g5; pure, the kinds of fact | Yes |
| "appends the thing to the depot the act-id names — the streaming depot with the ack level `:ack`, the microbatch depot with the ack the module's docstring describes" | `D:190-211` | g2 (`:gate-ran`, an answer at the first read), g20 (`:depot-holds`), g23 | Yes; `:append-ack` by the ruling on P17 |
| "gives the maker the act-id, the digest of what it sent [...] and the ack as a signal, never as the answer" | `D:859-866` | g2, g6, g17 | Yes |
| "never fills a missing part, never reorders facts, never names a permission for the maker, and changes the act only by the three additions" | `A:384-409` | g2, g5; pure, "each part that the maker gave is the maker's own object" | Yes; `:facts` goes as a vector by the ruling on P20 |
| "it tells the maker in the door's own words [...] and it appends nothing for that act" | `W:43-121` | g7: no record more in either log after each word | Yes, but for `:append-cleanly`, by the ruling on P1 |
| Part 5, "gives one of four: accepted, refused with the gate's reason, nothing yet, or not mine" | `W:162-188`, `D:868-932` | g8; pure | Yes |
| "the door waits as the maker asks, in a bound the maker gives, reading by act-id until an answer is there" | `D:304-345` | g3 (4 queries in 600 ms), g15, g20 | Yes |
| "The door keeps nothing of what it sent once the call returns" | `send-act` holds locals only | g8, a maker's own second send is another thing | Yes |
| Part 6, "It cannot check that the sender is the who it names, and does not [...] The door's docstring says the same in plain words" | `D:77-80` | Read | Yes |
| Part 7, its own cases | | g9 to g17 and g21 (each word; g26 holds that all fourteen were given); g8 (not mine); g18, g26; g26; g5 | Yes |

### The builder's rules

| Rule | Held by | Holds |
|---|---|---|
| D1 "The door checks nothing of the act [...] The door's own words name only what the door could not do" | `A:292-309` tests what the door's additions need, as rules A and B have it; g7, twelve things that go in and come back as a gate's refusal | Yes, narrowed by rule B |
| D2 "An act-id is made once and never reused; the door gives ids on request and keeps none" | `UUID/randomUUID` at `D:734`, `D:760`, `D:848`, `D:543`; no field holds an id | Yes |
| D3 "A lock is requested under the maker's who and permission, and used only for that who in that layer [...] a restart asks again" | g4, g18, g25 | Yes |
| D4 "An ack is never the answer. The door reads the record by act-id, and compares digests" | g8; g13 for the door's own request | Yes; the second half had no case for a request before this session |
| D5 "A wait for an answer is bounded by the maker's word and is one question about one act-id" | g3; no handle of a proxy or a PState | Yes |
| D6 "never opens a sealed value, never reads a fact of the record for a maker, and reads locks only of the requests it made itself" | No call of `seal/open` in the four files; `read-locks` is called from `settle` alone, with the stock's own open request | Yes |
| D7 "listed for Sid in the door's README" | `door/README.md`, "Words a session made"; three rows added | Yes |
| D8 "the door is the module's only client in the runtime [...] the proofs' README says so" | The proofs' README, "The door as the module's one client, and two proofs" | Yes |

The store's rules: 5 (a request carries a count: g26); 11 (the digest: g6, g8, g13); 15 ("The door
takes a fresh random part for each seal": `seal.clj:146`; g5, "the two runs of sealed bytes" differ);
16 (the line on who reads a lock is made true in `module/door_reads.clj:73-78` and in the store's
map, by the ruling on P12); 20 (the door's two bounds are read from the declaration:
`A:74-85`, `A:225-229`, `A:372-376`; pure, at the edge).

"Not in this build, by decision": no maker, no sign-in, no read of facts, no network edge, no switch
of a layer, no forget, no change of the gates, the record or the declaration is in the four files.
The closed files changed in docstrings alone since before the build (`git diff f33177cb^ HEAD` over
them, read line by line), and `seal.clj` gained `sealed-length` and nothing else. This session
changed none of them. The room that the door leaves is said in the namespace docstring (`D:97-113`).

"Explanations": each namespace and each function of the four files has a docstring (the `deftype`
and its two print methods take none; `printed` and the constructor's var hold theirs); each folder
has a README with a parent link; no README lists functions; no docstring or README of the tree cites
a paper of this build by its marks (searched again after the changes); the module's proof's row in
the proofs' README is as it was before the build (compared by `git show`).

### The rulings

Rules A and B: `A:292-309`, `A:153-232`; pure, the 23 cases of things that are no act; g7.
P1: `D:862-863`, g17 both cases; of a request, `D:550-552`, g10, g22. P2: `A:137-151`; an act-id with
the other depot word goes as given, g23. P3: `A:128-135`, `A:211-221`; a fact with a seal and no
value goes as given, g7. P4: `A:106-126`. P5: `D:559-561`, `send-act`'s docstring on the two
outcomes, g9. P6: the requires. P7: no step puts a lock back; g25. P8: `S:90-130`, g21. P9: the two
bounds; the docstring of the namespace on the cost. P10: `D:929-932`, the stamp in `W:184-188`, one
wait for any act-id. P11: `answer-of`'s docstring; g8. P12, P15: the sentences, with point 1. P13:
the proofs' README; each word has a case. P14: `:make-an-act-id`. P16: no thread. P17: `D:209`. P18:
under "A lock's bytes". P19: the docstrings of the namespace and of `additions`; g7. P20: g6. P21:
`D:264-266`, g11. P22: no closing; g25; `stock-count`.

The rulings on the plan. 1, 16: `S:90-130`. 2: `depot-of` gives a local. 3: `settle`. 4: `S:214-256`.
5: `seal.clj:107-116`, asked at `A:225`. 6 as 12 amends it: `S:296-314`, `D:342`; g10, g12, g22.
7: `door/README.md`. 8 as 11 amends it: `S:346-366`, `D:565-597`; g11, g14. 9: `A:208-209` before
`A:211-215`. 10: `A:87-93`. 13: an open request holds no answer (`S:260-269`); g11, 2 queries. 14:
the build session's. 15: `door/README.md`, the row of "a stock". 17: `A:74-85`. 18: `D:169-179`;
and by the probe, part 9, for an interrupt inside the call: "[:append-cleanly
\"java.lang.InterruptedException\" true] 215" of 240 appends, and "[:read-the-answer
\"java.lang.InterruptedException\" true] 54" of 200 reads, the third slot being the thread's mark
after the call. Rama's client throws the class itself, with no wrapping, so the test of the class
holds.

### The eighteen things that are no act

Each was run through `send-act` by reading `A:292-309` and `A:153-272`, and held to a case.

| Row | What comes out | The case |
|---|---|---|
| 1 no map | `:read-the-act` | g7, pure |
| 2 no `:layer`, or nil | `:make-an-act-id` at `:layer` | g7 (absent), pure (nil too) |
| 3 a `:layer` that cannot be read | `:read-the-act` at `:layer` | g7, pure |
| 4 a record with a `:layer` | `:read-the-act` | g7, pure |
| 5, 6 a `:layer` that is no id, or of 257 characters | `:make-an-act-id` at `:layer` | g7, pure |
| 7 `:facts` absent or nil | goes in; `:malformed` | g7 (absent), pure (nil too) |
| 8 `:facts` no sequence | goes in; `:malformed`; the value inside plain in the depot | g7 (one fact map, a set), pure (a string too) |
| 9 facts with no end | `:seal-that-many-values` when values keep coming; else it does not come back | g7, pure |
| 10 a fact that is no map | as given; `:malformed` | g7, pure |
| 11 a fact that is a record, with a value | `:seal-a-value` at `[:facts i]` | g7, pure |
| 12 `:sealed` and `:lock-id`, no `:v` | as given; the gate judges | g7 (`:lock-not-made-for-this-layer`), pure |
| 13 `:v` beside `:sealed` or `:lock-id` | `:add-its-parts` | g7 (`:sealed`), pure (both) |
| 14 `:act-id` or `:depot` on the act | `:add-its-parts` | g7, pure |
| 15 a value that is no value | `:seal-a-value` at `[:facts i :v]` | g7 (three kinds), pure (five) |
| 16 `:e` or `:k` no id or no key | sealed to the place as given; `:malformed`; one lock | g7 (`:e`), pure (`:e` and now `:k`) |
| 17 a part that the form does not know | as given; `:unknown-part` | g7, pure |
| 18 over a limit | goes in whole; the gate's word | g7 (three), pure |

## Concurrency, written out

The mechanism as the code has it. A stock's value changes by `compare-and-set!` or `swap!` of one
atom (`:cell`). A take is one such step and needs no seat. Only the call in the seat, a fair
semaphore of one permit, reads or changes `:open` or adds a run. A call is counted into its stock
before it reads it and out in a `finally`.

**Two makers, the stock empty.** A and B take short (`D:666`); each walks its four parts; A gets the
seat, B waits for at most what is left of its own wait (`D:671`). A takes again, finds no open
request, writes its request as in doubt, appends, marks it sent, reads the answer and then the
locks, and in one `compare-and-set!` puts the run in, forgets the request and takes its own
(`D:513-518`). A gives the seat up in a `finally`. B sits, and its first step is a take (`D:585`).
Ran: g20, 4 threads and one request.

**Three makers, the stock short, and a take racing the step that puts a run in.** A take by C while
A's step is being made either lands first, and A's `compare-and-set!` fails and is made again on the
new value, or lands after and takes from the new run. The run and A's own take are one value, so no
take comes between them, and A's request never asks for fewer than A needs (`S:151-187`). No lock can
be in two takes: two takes that read one value cannot both write it.

**The stock full.** No call goes near the seat.

**Every way out of the seat.** A word: a returned value, so both `finally`s run. A throw of Rama's
client: `attempt` turns it into a value (`D:181-188`). An interrupt before or in the wait for the
seat: `tryAcquire` throws without the permit, the mark is set again, and the seat is not released
because it was not taken (`D:671-678`; ran, probe part 5). An interrupt in a sleep: `sleep-for` gives
false (`D:292-302`) and the wait ends as `[:nothing]`. An interrupt inside the request's append:
Rama's client throws at once, the request stays in doubt, and it went through each time it was tried
(ran, part 9: all 240 things were on the depot). An interrupt inside a read: the word of that read;
the request stays open. A throw of the door's own code in the seat: after this session, the request
is made of parts that the reading already holds, so nothing of the maker's is read there; the two
`finally`s would run all the same.

**A seat that stays taken.** Before: H1. After: the seat is held for the door's own steps, for sleeps
that end at the call's wait, and for calls of Rama's client, each of which Rama bounds (10,000 ms for
an append by default; a query by the module's option). The reach of the walk is point 2.

**A call counted in for ever.** A call that does not come back because a part has no end stays
counted in; its stock then cannot leave the map. It holds no seat and no other call waits for it.
`leave!` can fail to count a call out for one kind of who (its docstring); the stock stays, and no
lock is lost.

**A stock leaving the map against a call that enters it.** `leave!` reads "holds nothing" inside the
function that `swap!` applies, after the count has come to none (`D:402-410`). If another call
entered meanwhile, the map is another object, the step is made again, and the count is not none. If
the step succeeds with the count at none, no call entered, and only a call that is counted in
changes a cell. Ran, part 4, twice.

**An open request met by the next call, with the call's wait ending at each point.**

| The request | The call's wait ends | What comes |
|---|---|---|
| sent | before it sits | `:get-locks-within-its-wait`, no request named; the request stays |
| sent | while it reads the answer and finds none | the same word, naming the request; it stays |
| sent, accepted | after the answer and before the read of locks | the same; it stays, and the next call reads the answer again |
| sent, accepted | the read of locks threw | `:read-its-requests-locks` with the class; it stays |
| sent, accepted | not yet | the locks; or, still short, its own request |
| sent, refused or of another digest | not yet | forgotten, handed to nobody; its own request |
| sent, refused, or its locks read and still short | while that was read | the word, naming no request: it is ended |
| in doubt, its age under the door's wait | before that age is reached | the word, naming it; it stays in doubt |
| in doubt, its age at the door's wait or over, and a read finds nothing | not yet | forgotten; its own request (g10, g12) |
| in doubt | at the same moment as its age | forgotten, and the word: point 3 |

**A stock that never asks again.** A stock asks unless it holds an open request. One that is sent is
on a depot and is no stray, so a gate answers it. One in doubt is forgotten at the first read that
finds nothing after its age has reached the door's wait. One whose reads throw stays while they
throw. Before this session one more: a seat held for good.

**A refusal that the maker's own call did not send.** Only `ask` makes `:have-its-request-accepted`,
from the request that it sent in this call (`D:559-561`); g11 and g12 hold that an earlier call's
refusal is handed to nobody.

**A wait with no bound.** The wait for the seat: bounded (g12, r13). The reads of a request: end at
the call's wait. A call of Rama's client on its way: Rama's limit. A thing with no end: no bound,
and said.

## Failure: each call of Rama's client

| The call | It throws | The stock after | The maker gets | The case |
|---|---|---|---|---|
| `open`, five fetches | | no door | `:find-the-module` | g1, a real throw |
| the read of a layer's depot | before any stock | untouched | `:read-the-layers-depot` | g11, by a stand-in |
| the request's append | | the request, in doubt from the throw | `:append-its-request-cleanly`, nothing appended for the act | g10, g12, a real throw; g10, g22, one that went through |
| a read of the request's answer | | the request, as it was | `:read-its-requests-answer` | g11 |
| the read of its locks | | the request, as it was | `:read-its-requests-locks`, with the class and no message | g11 |
| the act's append | | the act's locks are gone | `:append-cleanly`, with the act-id and the digest, "not known" | g17, both |
| a read of `answer-of` | | untouched | `:read-the-answer` | g11 |

An append that threw and went through: of an act, g17 (the log holds the record, its digest is the
word's, the answer is accepted); of a request, g10 and g22 (the next send uses its locks and sends no
second request).

## A lock's bytes

Each path was read. They come into the process by one call, `read-locks` (`D:231-246`). If it
throws, the word carries the class and not the message (`D:510-512`). If it comes back, the arrays go
into a run (`S:328-344`), with no copy. A take hands them to the one call that seals with them
(`S:214-256`), and nothing of the stock refers to them after. `thing-to-append` puts sealed bytes and
a lock id into the thing, never a lock (`A:345-382`). A word carries what `W:123-138` lists.
`stock-count` gives three numbers. A door prints as its module's name (`D:131-165`); the field
`stocks` is reachable by code of the same process, which the docstring says. The door logs and
prints nothing. The request carries a count. Held by g26: every lock's bytes against every record
of both logs; no result of 1,500 and more holds a run of bytes; the door prints as before with locks
held.

## Boundary and malformed input on the six calls

| Call | Given | What it does |
|---|---|---|
| `open` | nil, a wrong name, numbers that are no map, no numbers, or huge | A word by a real throw; the defaults; a wait cut to the longest (g1, g15, pure) |
| `act-id-for` | nil, a string, a number, a keyword of 257 characters | `:make-an-act-id` at `:layer`, no round trip (g7 by `send-act`; pure, the heads of the reads) |
| `act-ids-for` | 0, a negative, 1.5, a string, nil, `3N` | `:make-an-act-id` at `:count` (pure). A count beyond the memory: the JVM's own error, said; point 5 |
| `send-act` | the eighteen rows; an act-id of another layer, of no shape, with the other depot word; a `:who` that cannot be hashed or compared; a part whose read blocks | Above; g7, g16, g12, g23 |
| `answer-of` | a thing that is no act-id; a digest that is nil; a bound of 0, below 0, no number, nil, `Long/MAX_VALUE`, a big integer, `1e300`, infinity, a ratio, a fraction | "Nothing yet" with no query; "not mine"; one read; no throw (g3, g8) |
| `stock-count` | | Three numbers |

"Find one that does": two were found. The `:who` that cannot be compared, fixed. The count that the
memory does not hold, which is the plan's own limit and stays one. A third was looked for and not
found: a thing that throws at `find` and not at `get`, in `request`; after the change `request` reads
nothing of the thing.

## The proof against wrong doors

`review/mutants/run.py` and `gaps.py`; `results.txt` has each line. A wrong door is a changed copy
of one file, first on the classpath; no file of the repository was changed.

| The wrong door | Against the door's proof as this session left it |
|---|---|
| the door as committed | 5 fails, 1 error: "the seat and the wait", "a who that cannot be a key" |
| r01 no walk before the seat | 7 fails |
| r02 a read after the wait has ended | 1 |
| r03 the word names a request that was ended | 1 |
| r04 the count into a stock is not guarded | 1 error |
| r05 the request's answer is not held to its digest | 3; against the proof as committed: none |
| r06 a stock never leaves the map | 3; against the proof as committed: none |
| r07 a call is never counted out | 10 |
| r08 the hash of the who is not tested | 2 |
| r09 the opener's numbers are not read by the size rule | 1 |
| r10 a maker's bound is not cut | 1 error |
| r11 a request in doubt is never forgotten | 7 |
| r12 the opener's interval is not read by a wait | 1 |
| r13 the wait for the seat has no time limit | 1 |
| nine of the builder's own, made again on the door as it now is (who dropped; forgotten at once; a short read ends the call; the first read in the base at once; the mark not set again; a stock leaves whenever no call is counted in; no second take in the seat; no seat; an earlier refusal handed on) | each fails: 23, 13, 4, 1, 3, 43, 1, 20, 2 |

So no check was weakened, and each new check fails for the door that it is for.

## The runs

Each from the repository root; outputs under `review/`.

| Run | The count line |
|---|---|
| The tree as committed: act, pure, module | `{:test 8, :pass 150, :fail 0, :error 0}`; `{:test 16, :pass 538, :fail 0, :error 0}`; `{:test 22, :pass 227, :fail 0, :error 0}` |
| The door's proof as committed, three times (`base-door-1` to `-3`) | `{:test 21, :pass 162, :fail 0, :error 0}`, three times |
| After the fixes, with the new groups as first written (`fix1-door-1`) | `{:test 26, :pass 191, :fail 1, :error 1}`: the two faults of the proof's own writing that the next paragraph names |
| With those two mended, before the last check was added (`fix1-door-2`, `fix2-door-1`) | `{:test 26, :pass 192, :fail 0, :error 0}`, twice |
| In its final shape (`fix3-door-1`, `pass2-door-1` to `-3`, `final-door`) | `{:test 26, :pass 193, :fail 0, :error 0}`, five times |
| The final four (`final-act.txt`, `final-pure.txt`, `final-module.txt`, `final-door.txt`) | `{:test 8, :pass 150, :fail 0, :error 0}`; `{:test 16, :pass 540, :fail 0, :error 0}`; `{:test 22, :pass 227, :fail 0, :error 0}`; `{:test 26, :pass 193, :fail 0, :error 0}` |

No run of a proof against the right door failed after a change, but the first run of the new groups,
which had two faults of the proof's own writing (a reading of the door's map taken after the call
that changed it; a printed line that took a pair apart wrongly); both were in one new group and
were fixed there.

What varied between runs of the door's proof, and what did not. The same in each: 4 queries in a
wait of 600 ms; 4 in a wait of 250 ms at 100 ms; 2 appends and 3 queries in 15 ms after the door's
wait; 2 appends and 5 queries for the request in doubt met after 200 ms; the first read in the base
202 or 203 ms after the append; 14, 3 and 1 unused locks in three of the doors; 6 locks taken for
acts that were not accepted; 13 outside every stock. Varying: the sizes of the requests of 16
threads (third to sixth: 7 or 8, 13 to 15, 28 or 29, 55 to 59); 1 or 2 acts over two requests there;
4 or 5 reads of the request in the base; 261 to 267 rows that seal nothing of 1,417 to 1,423.

The counts, by case. Pure, 538 to 540: two cases more, "a reading holds the four parts that a
request for locks carries [...]" in group 15 and "a wait that the opener gives is cut to the longest
wait that the door counts [...]" in group 16; two cases take one thing more with the same count (a
fact whose `:k` is no key; a count that is a big integer). Door, 162 to 193, 21 groups to 26: "the
seat and the wait" 11; "a request's answer with another digest" 4; "an act over three runs" 4; "the
opener's numbers" 3; "a who that cannot be a key" 5; and one check more in each of "round trips are
counted", "a refused request", "reads that throw" and the last group. Two checks changed with the
fourth door that holds locks: the count of open requests at the end is of four doors, and the sum of
the rows that seal nothing takes the fourth door's.

## What was read, what was run, what was not done

Read whole: the stage's doc, `foreign-client.md`, `testing.md`; the contract; `src-store/CLAUDE.md`;
`src-store/README.md`; the implied spec; of the plan, everything but "Partitioning efficiency"
(lines 987 to 1095), which was not read; `DECOMPOSITION.json`; `door.clj`, `additions.clj`,
`stock.clj`, `words.clj`, `seal.clj`, `declaration.clj`, `text.clj`, `digest.clj`, `door/README.md`;
`act.clj`, `module.clj`, `module/door_reads.clj`, `record/rows.clj`, `gate/first_act.clj`,
`gate/decision.clj`; the store's map and the READMEs of `softland/`, `module/`, `gate/`, `record/`;
`door_proof.clj`; the three READMEs under `test-store/`; and, after the first hunt was written,
`IMPLEMENTATION_VALIDATION.md` and `TEST_VALIDATION.md`.

Read in part: `pure_proof.clj`, lines 1 to 150 and 2040 to 2933; `module_proof.clj`, lines 1 to 505;
`BUILD-store-module.md`, its builder's rules; `PLAN_VALIDATION.md`, its fails, its points,
"Concurrency", "Failure", "The door's proof" and "The explanations"; the two gates, by a search for
what they say of the door; the builder's `mutants/run.py`.

Not read: `env.clj` or any `.env` file; the rig, the server, Inland; `gate/batch.clj`,
`gate/stamp.clj`, `record/tables.clj` and the two gates' dataflow.

Probes, under `review/probes/src/probe/`: `review1` (eight parts, run before and after the fixes:
`probe-review1-before.txt`, `-after.txt`) and `review2` (parts 9 and 10: `probe-review2.txt`). The
lines that matter are quoted where they are used.

Asked for and not done. A door made wrong was run on the classpath before the tree, and never in the
tree. No interleaving of two takes at one instant was forced. The builder's 27 wrong doors were not
all run again: nine were. The store's rule 21 and the aims of speed were held against nothing: no
speed is measured. `door/PROGRESS.md` and the receipt were not touched.

## What was checked least well, in the order of how much a pass rests on it

1. **The seat under real slowness.** Every slow call of the proof is one that the proof made slow in
   its own process. A call that Rama's client itself holds for seconds, with makers standing behind
   it, was not run.
2. **The reach of the walk before the seat** (point 2). The proof's part of that kind blocks the same
   way at each read. One that reads differently the second time was reasoned and not run.
3. **Two takes at one instant, and the map's step at one instant.** Many threads, many runs, no
   forced interleaving.
4. **The base.** An act of the base that names the locks of two or three requests was not run; the
   microbatch gate's code for it was not read by this session.
5. **The checks that rest on a time**, eleven runs on one machine.
6. **`leave!`'s guard**: the who that throws only when it is compared with one that came after it.
   Read, and not reached by any run.
7. **A clock whose numbers wrap.** Read.
8. **The plan's section on what each call costs at 1, 16 and 128 tasks.** Not read; no number of it
   is held here.

PHASE_VALIDATION:pass
