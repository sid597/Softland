# Run 4 of 5: what crosses the wire

A review of 26 November 2026 with the skill `design-pitfalls`
(`.claude/skills/design-pitfalls/SKILL.md`). One reviewer on Fable at xhigh
effort wrote the report below, and it is kept word for word. The brief the
reviewer was given is at the end. The merge of the five runs is in
[merge.md](merge.md).

---

# Run 4: the words, the "could not" answers, the commands

**In short.** The wire's own table mostly holds: the act and the query cross whole, the check reads nothing inside them, and who is acting, how she signed in and what drives a `:session` sit in three separate places. What breaks is at the two ends of the wire: in `line.clj`, where three "could not" words are used for things their table entry does not say, and in the command and its library, where one exit status, one HTTP number, one kept `:session` and one secret each do more than one job. Ten findings; four reach what is stored.

Two words kept apart throughout: "an agent session" is a Claude or Codex session that wrote code; `:session` is the product's own thing.

## 1. What I read, and what I did not

**Read whole:**
- `.claude/skills/design-pitfalls/SKILL.md`, and the two paragraphs of the root `CLAUDE.md`.
- `src-wire/`: `README.md`, `softland/README.md`, `softland/wire/README.md`, `frame.clj`, `watching.clj`, `words.clj`, `credential.clj`.
- `src-command/`: the three `README.md`, `client.clj`, `main.clj`.
- `src-runtime/softland/runtime/line.clj`.

**Gatherer:** none. I ran the greps myself.

**Greps outside the reads.** `*.clj` of `src-store`, `src-wire`, `src-command`, `src-runtime`, `src-auth`, with `tables.clj` excluded; matched lines only, no file opened. Every hit marked "by grep" below is located, not reviewed. The patterns were:
- the two tables and their columns (`words/words`, `could-nots`, `:carries`, `:says`, `:anybody`, `:an-actor`, `:a-person`);
- each of the 16 words and each of the 14 "could not" words;
- `:kind`, `driven-by`, `own-lane`, `softland-session`, `cookie-name`;
- `:hello`, `:bye`, `:gives`, `watching/`, the wire's numbers;
- the library's and the command's own "could not" words, `sent.edn`, `:uuids`, `next-credential`, `client/bye`;
- `:asks` and the yes page's agent option, the result keys the command reads, the routes;
- the names of the seam's entry functions.

**Not grepped:** the test trees (`test-wire`, `test-runtime`), which are outside the allowance. A check there may hold the table's `:who` against the seam; I cannot say.

**Not opened, and what each would have settled:**
- `src-runtime/softland/runtime/http.clj` (in no run's list that I was given):
  - line 189: which status a "could not" outside the wire's table gets;
  - lines 267 and 612: when the runtime itself writes `:driven-by {:kind :person}`;
  - lines 455-492: what the yes page does with `--agent`;
  - line 542: a 413 where the table says 400;
  - lines 595-632: what a refused opening of the line sends back.
- `seam.clj`, `sign_in.clj`, `standing.clj` (run 1's): what `needs-the-person` tests, the form of `:results`, how a `:session` runs out, the poll.
- `jetty.clj`: what a too-large frame gets.
- `src-auth/*`: how `:driven-by`, "beneath" and `:asks` are stored.
- The store's door and read path: what a fact with `:v nil` is, and what one act-id with a different act gets.
- The record: not used, per bound 2. The skill's inventory line "what Sid was shown, and what he answered" and its question 9 are therefore not done: run 5.

## 2. The inventory

### The two tables, by column

Both are fixed in code and change only by an edit of `words.clj`. The runtime and the command hold them as the same file; any other program holds a copy.

| Column | Job | Read where |
|---|---|---|
| words: the key | Names the word in a frame's `:word`. | `words.clj:278-281`; `line.clj:158,161`; the seam's `case`, `seam.clj:677-756` (by grep) |
| words: `:says` | One sentence for a person. | No code in the five trees (by grep) |
| words: `:carries` | Names the part that holds a form of the store's, so the check leaves it unread. | `words.clj:282-283, 292-297` |
| words: `:who` | Says who may say the word. | No code in the five trees (by grep); see answer a |
| could-nots: the key | The closed set of the runtime's own "could not" words. | `words.clj:379`; `http.clj:189` (by grep) |
| could-nots: `:says` | One sentence for a person. | No code (by grep) |
| could-nots: `:status` | The HTTP status a face gives the word. | `http.clj:189` (by grep) |

### The words (`words.clj:80-111`, shapes `116-163`)

A word is decided by the system when it says it, does not change after, and there are many.

| Word | Parts | `:who` | Jobs | The command says it at |
|---|---|---|---|---|
| `:who-am-i` | none | `:anybody` | Gives again what hello gave. | never |
| `:send` | `:act`, `:act-id` (a UUID), `:wait-ms` | `:an-actor` | Puts one act into its depot. Waits up to `:wait-ms` for the gate's answer. While it waits, every later word of the connection waits behind it (`line.clj:30-35`). | `main.clj:396-398`, `428-434` |
| `:send-all` | `:acts` (up to 1024 of `{:act :act-id}`), `:wait-ms` | `:an-actor` | Sends acts in order and stops at the first that could not be sent. | `main.clj:379-383` |
| `:answer` | `:act-id` (the whole id), `:digest`, `:wait-ms` | `:an-actor` | Asks again for the answer of an act sent before. | never |
| `:read` | `:query` | `:anybody` | A read, once. | `main.clj:403` |
| `:watch` | `:query` | `:anybody` | A read kept live under the number of its word. | `client.clj:215` |
| `:leave` | `:watch` (a number) | `:anybody` | Leaves a watch; done in `line.clj:161-167`. | never |
| `:replace-secret` | `:hash` | `:an-actor` | Replaces the sign-in's secret; the `:session` stays. | `main.clj:234` |
| `:sign-out` | none | `:an-actor` | Ends this connection's sign-in. | `main.clj:520` |
| `:open-session` | `:driven-by` (optional) | `:an-actor` | Opens a new `:session`. Says once what drives it. | `main.clj:488-493` |
| `:close-session` | none | `:an-actor` | Closes the connection's `:session`. | `main.clj:499, 514` |
| `:invite` | `:uuid`, `:claim` | `:a-person` | Makes a person who waits for an account. | `main.clj:443` |
| `:make-agent` | `:uuid`, `:label`, `:hash` | `:an-actor` | Makes an agent beneath the caller. Makes its first sign-in. | `main.clj:453-454` |
| `:end-agent` | `:agent` | `:an-actor` | Ends one agent. Ends every actor it made. Ends every sign-in of each. | `main.clj:463` |
| `:under` | none | `:a-person` | Lists the actors of the caller's root and its live sign-ins. | `main.clj:466` |
| `:end-sign-in` | `:sign-in` | `:a-person` | Ends one sign-in of the caller's root. | `main.clj:468` |

Parts that are more than a plain value:
- **`:act-id`** has two forms under one name: the maker's bare UUID in `:send` and `:send-all` (`words.clj:125, 244`), and the whole id in `:answer` (`words.clj:132`). The id itself is run 2's.
- **`:driven-by`** is `{:kind :person}` or `{:kind :model :model text}` (`words.clj:206-213`). It is decided at the opening, no word of the table changes it, and there is one per `:session`.
- **`:claim`** is `{:by :email …}` or `{:by :code :hash …}` (`words.clj:215-224`).
- **`:uuid`** is the system's own name for one making; the runtime derives the id from it (`words.clj:23-26`).
- **`:wait-ms`** defaults to 10000 for a send and 0 for `:answer`, and is cut to 60000 (`words.clj:296-300`).

### The "could not" answers (`words.clj:325-373`)

| Word | Status | Made at (my reads) | Made at (by grep) |
|---|---|---|---|
| `:read-the-frame` | 400 | `line.clj:149`, with no number; `line.clj:222`, as `:bye`, `:why :too-many-waiting` | `http.clj:542` (`:too-large`, status set to 413), `545` |
| `:read-the-word` | 400 | `line.clj:152` (`:why :no-number`, with no number); `line.clj:156` | `seam.clj:410, 417, 756`; `http.clj:556, 578` |
| `:find-her-sign-in` | 401 | | `seam.clj:257, 593`; `http.clj:623, 627` |
| `:find-her-session` | 401 | | `seam.clj:289-291, 324, 395`; `http.clj:618` |
| `:send-as-another` | 403 | | `seam.clj:411-412, 422-427, 776, 786` |
| `:do-that-as-an-agent` | 403 | | `seam.clj:400`; `sign_in.clj:207` |
| `:find-the-agent` | 404 | | `seam.clj:667, 669` |
| `:find-the-sign-in` | 404 | the command's own use, `main.clj:270, 486` | `seam.clj:753` |
| `:find-the-watch` | 404 | `line.clj:116` (`:number-in-use`), `120-123` (`:too-many`), `167` (`:not-known`) | none |
| `:find-the-ask` | 404 | | `sign_in.clj:187-188` |
| `:take-that` | 409 | read at `main.clj:239` | `seam.clj:608`; `sign_in.clj:94, 123, 176, 221, 262` |
| `:take-more` | 429 | read at `main.clj:349` | `seam.clj:486, 498, 505, 534` |
| `:reach-auth` | 503 | `line.clj:207, 210` | 16 more places, most with `:got` |
| `:do-the-word` | 500 | `line.clj:172` | `seam.clj:989, 992` |

Parts of a "could not" answer:
- `:could-not` says which word.
- `:why` is a narrower reason. Its values are closed for some words; for `:take-that` it is whatever Auth said.
- `:at` is a part of a word, or of an act or query. By grep it is also a step, at `sign_in.clj:51`.
- `:most` is the limit that was reached.
- `:threw` is a class name, and it crosses the wire.
- `:got` is Auth's own answer, nested.
- `:said` is a text for a person.
- `:status` and `:closed` are added by the library only.

What else holds `:could-not` and crosses:
- The door's 7 words and the read path's 10, which pass through as they are (`words.clj:327-329`; by grep `door/words.clj:26-56`, `read_path/words.clj:19-55`).
- `:write-her-sign-in`, a runtime word outside the wire's table (by grep `sign_in.clj:49-51`).
- The library's `:reach-the-runtime` and `:hear-the-runtime` (`client.clj:7-11`).
- The command's `:read-the-command` and `:run-the-command`, and its own use of `:find-the-sign-in`.

### The commands and flags (`main.clj:385-523`)

Every command but `sign-in` does two more things through `with-line*`:
- it writes the kept `:session` or credential back to the file (`main.clj:284-293`);
- except `replace-secret`, it replaces the secret when under two days remain (`main.clj:294-297`).

So `who` and `read` can open a `:session` and change a secret.

| Command | Jobs |
|---|---|
| `sign-in` | Posts the hash and a text, shows a link and a code, polls each second, keeps `{:url :credential}` over whatever the file held, then connects, which opens the first `:session` (`main.clj:326-359`). |
| `who` | Prints hello. |
| `write e k v` | One act with one fact, under a fresh UUID that is not kept (`main.clj:393-398`). |
| `read e k`, `watch e k` | One place, through one lane (`main.clj:318-322`). |
| `send file` | Fills `:who` and `:session` into each act of the file; keeps UUIDs in `<file>.sent.edn` (`main.clj:361-383`). |
| `give who lane see\|act` | One act that writes a permission fact (`main.clj:418-434`): permission session. |
| `invite` | Makes a person; with no `--email`, makes a secret and prints it in a link (`main.clj:436-446`). |
| `agent new name`, `agent end uuid` | `:make-agent` and a file `{:url :credential}` (`main.clj:448-463`); `:end-agent`. |
| `under`, `end-sign-in`, `replace-secret`, `sign-out` | One word each. |
| `new-session` | Closes the `:session` and connects with none; with `--model`, opens one that says a model drives it, then closes the old one (`main.clj:478-518`). |

Flags:
- **`--as file`**: which sign-in file.
- **`--url`**: which runtime; read by `sign-in` only.
- **`--agent name`**: changes the text she is shown and prints a hint (`main.clj:329, 339`).
- **`--lane id`** does three jobs: the lane an act is written into (`main.clj:314`), the one lane a read looks through (`main.clj:322`), and the lane inside the default permission the act names (`main.clj:315`: permission session).
- **`--permission edn`**: permission session.
- **`--email`**: picks the form of the claim.
- **`--out`**: where the agent's file goes.
- **`--model name`**: the only source of `:driven-by` in the command.

Exit status (`main.clj:184-191, 532`): 0, 1, 2, 3. See finding 6.

### Others that cross

- **The frame** (`line.clj:15-28`): `{:hello about}`, `{:id n :word …}`, `{:id n :gives result}`, `{:id n :anew|:changed|:ended …}`, `{:bye word}`.
- **The number `:id`** does three jobs: it matches one result to its word, it names a watch for as long as it lives, and it is what `:leave` names.
- **Hello**: `{:who :root :sign-in :session :kind :own-lane :until}` (`client.clj:156`). By grep, `:kind` here is `:browser` or `:program` (`sign_in.clj:118`, `seam.clj:652`), and `:driven-by` is added when there is a `:session` (`seam.clj:176`).
- **A watch** (`watching.clj`): three message forms; what waits for a slow reader is merged by place.
- **The credential text** `sl2_<root>.<sign-in>.<secret>` (`credential.clj:7`): two parts say where the record is, one is the proof.
- **"A code"**: eight characters, shown `XXXX-XXXX` (`credential.clj:24-27`).
- **Carriers and routes**: cookie `softland-sign-in`; headers `Authorization: Bearer` and `softland-session` (`client.clj:149-150`); routes `/line`, `/sign-in/program`, `/sign-in/program/poll`, `/?invited=`.
- **Bounds**: frame bytes by caller (`words.clj:43-54`); token length, set equal to the store's bound on a value (`frame.clj:36-40`); 256 waiting words (`line.clj:54`).
- **Files**: `sign-in.edn` `{:url :credential :session :next-credential}`, `agent-<name>.edn`, `<file>.sent.edn` `{:uuids :session}`.

### What is fixed for good

- **Stored in an act:** what `act-of` builds (`main.clj:311-316`): `:version 1`, `:who`, `:session`, `:lane`, `:permission`, `:facts`; and the act's UUID.
- **Stored in Auth (by grep):** a `:session`'s `:driven-by` (`schema.clj:73, 94`), an agent's `:label`, the `:asks` text of a program's sign-in (`schema.clj:1553`), an invite's claim.
- **Shown to someone:** the `:asks` text and the code on the yes page; the invite link; the command's printed result and exit status.
- **Held by programs built against it:** the 16 words, the 14 "could not" words and their `:why` values, the frame, the credential's form, the routes, the three files, the commands and flags. Today those programs are the command and the runtime's checks (`src-command/README.md:21`). `line.clj:4-5` names two to come: an agent's harness and the browser's client.
- **Written on a page as decided:** see the pointers for run 5.

## 3. What holds

- **The wire fixes nothing of an act or a query** (questions 4, 5). The check tests only that the carried part is a plain map (`words.clj:226-230`).
- **"No word for a feature of the product"** (`words.clj:6-8`) (questions 1, 3). I looked for a word that is a feature: none. Five words carry acts and reads; eleven are about who is signed in.
- **Who is acting, how she signed in and what drives the `:session` are three separate places** (question 2): the act's `:who`, hello's `:kind`, and `:driven-by`. Their lifecycles differ and they are apart.
- **The credential keeps where-the-record-is apart from the proof** (question 2). `:replace-secret` changes only the proof, and the `:session` outlives it.
- **The command's default lane and default permission are values, not a fused form** (questions 1, 2). The act carries `:lane` and `:permission` as separate parts and each has its own flag.
- **"Could not" is kept apart from a gate's refusal on the wire** (`words.clj:328-330`), and a definite refusal (`:take-that`) apart from an uncertain failure (`:reach-auth`, `:do-the-word`). `replace-on-line!` depends on that difference and uses it correctly (`main.clj:235-240`).
- **Every limit is a number, not a form** (question 3). The token bound is set equal to the store's value bound by one `def`, which costs one line to part.
- **Merging by place loses nothing for good** (question 6). A watch is the present, not a log; the acts are kept whole elsewhere.
- **`:send` holding the line while it waits** is one thing doing two jobs, but the page says so and gives the way out (`:wait-ms 0`, then `:answer`), and nothing is stored. Under the brake it is not a finding.

## 4. What breaks

Ordered by what gets fixed for good soonest. Findings 1 to 4 reach what is stored; 5 to 10 reach programs built against the wire.

### 1. `write e k` with the value left out writes `:v nil`

- **The thing:** the value `nil` in a fact's `:v`.
- **Its jobs:** it says "no third argument was typed", and it says "she typed `nil`".
- **The code:** `main.clj:397`, `{:e (value-of a) :k (value-of b) :v (value-of c)}`. `value-of` gives nil for a missing argument and for the text `nil` (`main.clj:175-180`).
- **How they differ:** the first is a slip that nobody decided; the second is an act she means.
- **Cost now:** the command telling the two apart. **Later:** acts stored from slips.
- **Certainty:** certain for the command. What the store takes `:v nil` to be depends on what I did not read: run 2 (a fact with no value).

### 2. `send` decides "these are the same acts" from their count

- **The thing:** `<file>.sent.edn`.
- **Its job:** hold the UUIDs so that a run that was cut off sends the same acts under the same ids.
- **The code:** `main.clj:373-375`, `(if (= (count held) (count acts)) held …)`. The acts then get the present `:session` at `main.clj:382`.
- **What follows:** a file that was edited but kept its count, or a re-run after `new-session`, sends different acts under the old UUIDs.
- **A key with no job:** the file also holds `:session` (`main.clj:377`), which nothing reads (by grep).
- **How they differ:** the count changes only when acts are added or removed; the acts change with any edit, and with the `:session`.
- **Cost now:** one test in one function. **Later:** acts that were silently not written, or written under an id that named another act.
- **Certainty:** certain for the command. What the store does with one act-id and a different act depends on what I did not read: run 2.

### 3. The `:session` that the command keeps does three jobs, and what drove an act is recorded only there

- **The thing:** `:session` in `sign-in.edn`, put into every act (`main.clj:313, 382`).
- **Its jobs:**
  1. It groups every command of one sign-in file until `new-session` (`main.clj:53-55`).
  2. It is the only place that says what drove an act. `:driven-by` is said once, at the opening, and only by `new-session --model` (`main.clj:488`).
  3. For `send`, it is part of what makes a re-sent act the same act.
- **How they differ:** job 1 changes when she types `new-session`. Job 2 changes whenever the hands at the keys change, which no command sees. Job 3 must not change between a run that was cut off and its re-run.
- **An example:** she signs in, an agent runs commands in the same sitting, and every act of both carries one `:session` with one `:driven-by` or none.
- **Cost now:** the command and one README line marked "a session's, kept and not decided" (`command/README.md:14-17`). **Later:** every stored act points at a `:session` whose driver may not be what drove it, and acts cannot be told apart below the `:session`.
- **Certainty:** certain for what the command does. What a `:session` and `:driven-by` are meant to be is run 1's and Sid's. `http.clj:267, 612` (by grep) also writes `{:kind :person}`; I did not read under what condition.

### 4. `sign-in --agent name`: the name crosses only inside a text for a person

- **The thing:** `:asks`.
- **Its jobs:** it is what the program says of itself, shown to her; with `--agent`, it is also the only carrier of the agent's name.
- **The code:** `main.clj:329`, `(str (if agent (str "the agent \"" agent "\"") "the command line") " on " …)`.
- **What the page says of the flag:** "the yes page offers to make this program an agent of that name" (`main.clj:10-12`). No part carries a name. By grep the page has its own input for the label (`http.clj:463`) and nothing reads the name out of the text.
- **How they differ:** a text for a person can be anything; a name is a value that something later uses.
- **Cost now:** one flag. **Later:** the text is stored with the sign-in (by grep, `schema.clj:1553`).
- **Certainty:** depends on `http.clj:455-492`, not read.

### 5. The program's secret crosses the wire before her yes, against two docstrings

- **The thing:** the secret of a program's sign-in.
- **Its jobs:** it proves that a poll comes from the program that asked; after her yes it is the proof of the sign-in.
- **What the pages say:**
  - `credential.clj:19-20`: "sends the runtime its hash, so the secret crosses no wire before her yes".
  - `client.clj:65-66`: "The secret never leaves the program before a person has said yes".
- **The code:** `client.clj:78`, `(post url "/sign-in/program/poll" {:code code :secret secret})`, called each second while the state is `:waiting` (`main.clj:341-344`). The default address is plain `http` (`main.clj:73-74`).
- **How they differ:** the first job lasts minutes and ends with the ask; the second lasts until the secret is replaced.
- **Cost now:** one route and one function. **Later:** every program that signs in is built on the poll's form.
- **Certainty:** certain from what I read. It sits at the edge with run 1 (sign-in).

### 6. The exit status says two things with 3 and four with 0

- **The thing:** the exit status.
- **Its stated reason:** "so that a script can tell an act that was refused from a command that could not be done" (`command/README.md:23-24`).
- **Status 3:** `main.clj:189-190` gives 3 when a result of `send` holds `:could-not`, and also when one was refused.
- **Status 0:** "when the thing was done" (`main.clj:57-58`). It is also given for:
  - `sign-in` when she said no or the ask ran out (`main.clj:359`, `:else got`);
  - a `watch` that ended with a "could not" nested under `:ended` (`main.clj:414`);
  - `:nothing-yet` (`words.clj:82`).
- **How they differ:** a refusal is final and kept; a "could not" and a "nothing yet" can change when asked again.
- **Cost now:** one function of eight lines. **Later:** scripts and harnesses built on the statuses.
- **Certainty:** certain from what I read.

### 7. The library's two own words cover what the runtime did say

- **The thing:** `:reach-the-runtime` and `:hear-the-runtime`.
- **A refused opening:** `client.clj:34-36` keeps only the status number and names it `:reach-the-runtime`. The table gives 401 to both `:find-her-sign-in` and `:find-her-session` (`words.clj:340, 343`).
- **Three decisions rest on that one number:**
  - fall back to the old credential (`main.clj:253`);
  - tell her "Run: sign-in" (`main.clj:274`);
  - drop the `:session` and try again, in `new-session` only (`main.clj:502-504, 515-518`).
- **Two more cases:**
  - The runtime's `:bye` word is kept and never shown (`client.clj:96`; `main.clj` does not call `bye`).
  - An answer with no number is kept under `:said` and never read (`client.clj:104-106`). The caller waits 90 seconds and gets `:hear-the-runtime`, where the library's page says "A word of the runtime's comes back as it is" (`client.clj:9-10`). A value with one token longer than the bound takes this road (`line.clj:149`).
- **How they differ:** a `:session` closes many times under one sign-in, and a sign-in ends once. A failure of the network can pass; a refusal with a reason does not.
- **Cost now:** the library. **Later:** every program built on the library's words.
- **Certainty:** certain for the library and the command. Whether the refused opening carries the word in its body depends on `http.clj:595-632`, not read; by grep it does (`http.clj:189, 618`).

### 8. `:find-the-watch` says three things, and "a limit was reached" is said by three words

- **The thing:** `:find-the-watch`.
- **What its table entry says:** "this connection holds no watch under that number" (`words.clj:357`).
- **Where it is made:**
  - `line.clj:116`, `:why :number-in-use`, where the connection does hold one;
  - `line.clj:120-123`, `:why :too-many`, a limit;
  - `line.clj:167`, `:why :not-known`, the only one that fits.
- **The same kind of answer under other words:** `:take-more`, status 429, is the table's word for a limit, and by grep it also says `:too-many-watches` (`seam.clj:534`). `line.clj:222` says a third limit as `:read-the-frame {:why :too-many-waiting}`, status 400.
- **How they differ:** a limit passes with time, so the same word sent later is done; a watch that is not known, or a frame that is no EDN, never is.
- **Cost now:** three lines of `line.clj`. **Later:** the retry rule of every client.
- **Certainty:** certain from what I read.

### 9. A second `:watch` under a live number is refused in the first watch's voice

- **The thing:** the number `:id`.
- **Its jobs:** it matches one result to one word; it names a watch for as long as it lives.
- **The code:** `line.clj:115-116`, `(say! connection {:id id :ended (words/could-not :find-the-watch {:why :number-in-use})})`.
- **What follows:** a reader cannot tell this from the end of the live watch. This library drops the live watch on it (`client.clj:100-103`) while the runtime keeps it. The library's own counter avoids the case; another client would not.
- **A related case:** a refused `:watch` comes back as `:gives` when the check refuses it (`line.clj:156`) and as `:ended` otherwise. The library hears `:gives` for a watch nowhere (`client.clj:97-99`).
- **How they differ:** one result against many messages; the life of one word against the life of a watch.
- **Cost now:** `line.clj`. **Later:** the browser's client and any harness.
- **Certainty:** certain from what I read.

### 10. The command says `:find-the-sign-in` for "this program is not signed in"

- **The thing:** `:find-the-sign-in`.
- **Its jobs:** in the wire's table it is "the sign-in that was named is not one of the caller's root" (`words.clj:354`). The command uses it for "no credential in the file" (`main.clj:270, 486`).
- **What follows:** `end-sign-in uuid` can give either under one word and one exit status.
- **How they differ:** in who says it and in what she does next: run `sign-in`, or check the uuid.
- **Cost now:** two lines. **Later:** scripts.
- **Certainty:** certain from what I read.

**Seen on the way, not a two-jobs finding:** `watch` ignores what `client/watch` returns (`main.clj:409-414`). If the watch could not be sent, the command waits for an end that never comes.

## 5. What is still open

### Answers to a to d

**a. The table's `:who`.**
- **Values:**
  - `:anybody`: `:who-am-i`, `:read`, `:watch`, `:leave`.
  - `:an-actor`: `:send`, `:send-all`, `:answer`, `:replace-secret`, `:sign-out`, `:open-session`, `:close-session`, `:make-agent`, `:end-agent`.
  - `:a-person`: `:invite`, `:under`, `:end-sign-in`.
- **Its job by its docstring** (`words.clj:78-79`): one, "who may say the word".
- **Its job in code:** none in the five source trees. Nothing reads the column or matches its three values (by grep).
- **Where the rule is made:** in `seam.clj` by `needs-a-who` (381), `needs-a-session` (391) and `needs-the-person` (397), called word by word at 677-756 (by grep, not reviewed).
- **Do the two agree?** The matched lines agree with the column for all 16 words. They add a requirement the column has no value for: a `:session`, for `:send`, `:send-all` and `:close-session` (`seam.clj:679, 681, 725`).
- **So:** the column is a second statement of the rule that nothing is built on. Whose: Sid's; run 1 for the seam. A check in the test trees may read it; not grepped.
- **Not the act's `:who`:** that is one actor's id; this is a class of callers. Same key, different thing, never in one map.

**b. "Agent" and "person" on the wire.**
- **`:make-agent`:** `{:word :make-agent :uuid U :label "indexer" :hash H}` gives `{:made A :root R :sign-in G :until T}` (`seam.clj:656`, by grep). A is a new actor with a who of its own beneath the caller, with one sign-in. Nothing says what runs it.
- **`:end-agent`:** `{:word :end-agent :agent A}` ends A, every actor A made, and every sign-in of each.
- **`:find-the-agent`:** A is not beneath the caller.
- **`:do-that-as-an-agent`:** the caller is an agent and said `:invite`, `:under` or `:end-sign-in`.
- **`:an-actor`:** anybody signed in, person or agent.
- **`:a-person`:** the caller's actor is a person's own.
- **`:invite`:** `{:word :invite :uuid U :claim {:by :email :email "a@b.c"}}` makes a person with no account yet.
- **`{:kind :person}` and `{:kind :model :model "x"}`:** what drives a `:session`.
- **Where the words stand:** "agent" is always an actor; "model" is always a driver. "Person" is on both. An example: she signs in the command line as herself and an agent runs `new-session --model x`. The actor is a person and the driver is a model, and `:invite` passes as "a person herself and no agent" (`words.clj:79`).
- **Open:** which of the two those three words are meant to test is Sid's. What `needs-the-person` tests is run 1's (`seam.clj:397-400`).

**c. `:driven-by` in the command.**
- **Where:** `main.clj:487-488` and nowhere else.
- **From what:** the flag `--model` alone, as typed; nothing checks it. `--model` with no name sends `""`, which the check refuses.
- **Where it is not said:** `sign-in`, any command that connects with no kept `:session`, and `new-session` without the flag all open a `:session` and say nothing of what drives it. The command never sends `{:kind :person}`.
- **Open:** by grep the runtime writes `{:kind :person}` itself at `http.clj:267, 612`; under what condition is not read. If it is drawn from how the credential arrived, a browser driven by a model would be recorded as a person.

**d. The default lane.**
- **Where:** `main.clj:310` for acts and `main.clj:322` for reads.
- **From what:** `(:own-lane hello)`, which the runtime says at each connection (`line.clj:183`). It is never kept in the file (`command/README.md:18-19`).
- **Open:** for an agent's sign-in, by grep the value comes from the root's record (`schema.clj:1491`), so "her own lane" would be the lane of the person at its root. Not reviewed: run 1.

### Other open points, by whose they are

**Sid's:**
- "A code" names two things: the eight characters of a program's ask (`credential.clj:24`), and the 43-character secret of an invite (`main.clj:439-446`; `:by :code`, `words.clj:222`).
- "Signs in" names two things: the ask with her yes, and an agent presenting a file (`main.clj:24-25`).
- `:end-agent` ends everything the agent made. "Beneath" records who made an agent, and by the words it also decides what ends with it and who may end it. On the wire this is a default: `:make-agent` names no parent, so a later part could.
- An agent can end the agents it made but cannot list them (`:under` is `:a-person`). This sits at the edge with the permission session.

**Run 1's:**
- Whether "beneath" is one stored slot for all of those jobs: Auth's shape, not read.
- `:write-her-sign-in`, a runtime "could not" outside the wire's table (`sign_in.clj:49-51`, by grep).
- Two `:why` values of `:take-more` that its table entry does not name: `:polls-paced` and `:sessions-paced` (`seam.clj:498, 505`, by grep).

**Run 2's:**
- The two forms of `:act-id`.
- `:nothing-yet` and `:not-mine` both exiting 0.
- Findings 1 and 2.

**Run 3's:**
- The command can name only one lane for a read, where the query takes several (`main.clj:322`).

**Nobody's that I was told:**
- `http.clj`. Line 189 would give status 200 to a door's or read path's "could not"; line 542 gives 413 where the table says 400.
- `src-auth`. `:driven-by` is a record type for each kind (`schema.clj:36-37`, by grep).

## 6. Pointers

### Not checked against the record: run 5

- **`src-wire/README.md:8-12`:** "That the whole act goes on the wire is Sid's decision (entry 7 of that page)". Standing on it: `words.clj:13-21, 82-85`; `main.clj:306-322, 382`; by grep `seam.clj:411-412`.
- **`words.clj:9-11`:** "(the runtime's page, entry 5)", that a `:session`'s trail is read by `:read` and `:watch`. Standing on it: `words.clj:88-91`.
- **`words.clj:32-34`:** "(src-store/README.md, 12 October 2026, entry 7)". Standing on it: the check leaving the carried part unread, `words.clj:226-230, 282-300`.
- **`src-command/README.md:8-12`:** "That the command line is Clojure is entry 10 of that page", and "until Sid names where it goes". Standing on it: all of `src-command`.
- **Pages cited as a base, not as his decision:** `credential.clj:14-15` and `words.clj:60-62` (Auth's spec); `frame.clj:5-6` (`src-store/MODEL.md`).
- **Pages that mark themselves as an agent session's and his to turn:** `src-wire/README.md:11-12`, `wire/README.md:21-25`, `command/README.md:12-14`. Nothing here is written as his.

### Permission session

- `main.clj:18-20, 418-434`: `give`, the permission fact, `:see` and `:see-and-act`.
- `main.clj:35-38, 306-316`: `--permission` and the default `[(:who hello) lane lane]`.
- `main.clj:3-4`: "what it may do is what it was given".
- `main.clj:58, 188`: a gate's refusal.
- `watching.clj:60-63`: "a lane that she may see".
- At the edge:
  - `words.clj:344-346`, "Nobody acts for another";
  - the ends of Auth: `words.clj:96, 106, 110`;
  - the yes of a program's sign-in: `credential.clj:20`, `client.clj:65, 75`, `main.clj:9-12, 354`.

### Run 1

- `credential.clj:7-13`: root and sign-in inside a credential.
- `client.clj:75-80`: `:root :actor :sign-in`.
- `client.clj:140-159`: the `:session` header, and hello.
- `main.clj:311-313, 382`: `:who` and `:session` on an act.
- `words.clj:14, 23-26`.
- `line.clj:9-13`: one caller in one `:session`; standing.
- `main.clj:294-297`: `:until`.

### Run 2

- `main.clj:311`: `:version 1`.
- `words.clj:18-21, 125, 132, 244` and `main.clj:398, 434, 372-378`: act-id.
- `main.clj:397, 432`: facts.
- `watching.clj:67, 73`: `:no-fact`.
- `words.clj:86, 133`: digest.
- `main.clj:296, 351`: the command's clock held against the runtime's times.

### Run 3

- `words.clj:19, 82` and `line.clj:32`: depot.
- `main.clj:318-322` and `words.clj:15-16`: the words of a query.
- `watching.clj:6-9, 59-78`: the words of an answer.

## Forks I took

- **Defects of the command.** Where a defect is not strictly one thing with two jobs (findings 1, 2, 6), I still wrote it in the skill's form, because each reaches what is stored or what scripts read.
- **The kept `:session`.** I listed what the command does with it, since the command sends it, and left what a `:session` is to run 1.
- **"End" and "yes".** I put the sign-in's yes and Auth's ends at the edge rather than under the permission session outright.
- **Grep.** I quoted matched lines only and opened none of those files, so each "by grep" statement is a location and not a reading.

---

# Appendix: the brief this reviewer was given

You are doing run 4 of a five-run review of the Softland repository (/mnt/data/projects/Softland) with the skill `design-pitfalls`. Run 4 only. Runs 1, 2, 3 and 5 are other reviewers' and the merge is mine; do not do any part of them.

## Why this review exists

Sid, the founder, has to find out whether what earlier agent sessions built is right. The case that started it: the store's unit carried his word "layer" while meaning something narrower, and one id did several jobs; that was corrected to "lane". He now wants the same adversarial check on **what crosses the wire**: the words a system says to the runtime and hears back, the "could not" answers, and the commands of the command-line program. The job is to break the thing, not to confirm it, and not to redesign it. If it holds, say that it holds and what you asked of it.

## Read these first

- `.claude/skills/design-pitfalls/SKILL.md`, whole. Follow it: the inventory first, then the ten questions, the brake, and "How to answer".
- In the root `CLAUDE.md`: the paragraph "Defaults, not conflations" and the one after it ("The same holds between Sid and a session"). That is the rule being checked.

## What you review (the whole of it)

- `src-wire/`, every file: `README.md`, `softland/README.md`, `softland/wire/README.md`, `frame.clj` (83 lines), `watching.clj` (78), `words.clj` (380), `credential.clj` (124)
- `src-command/`, every file: three `README.md`, `softland/command/client.clj` (236), `softland/command/main.clj` (532)
- `src-runtime/softland/runtime/line.clj` (233): this one file only, not the rest of that directory

Run 4 owns these: **the words, the "could not" answers, the commands**. For each one you list every job it does: each word of the wire's table and each column of that table, each "could not" answer and its parts, each command and each flag. Also take into the inventory, under "others", anything else in these files that crosses the wire and that no run owns (the frame, a watch).

`credential.clj`: run 1 read it for how a credential becomes who is acting, and the jobs of root and sign-in are run 1's. You read it as a thing that crosses the wire.

Run 1 left these for this run. They are questions, not findings to confirm; answer each from your reads with file:line, or say what you did not read:
- a. In `words.clj` (near line 78) the table of words has a key `:who`. On an act, `:who` is the actor that sent it. What are the jobs of the table's `:who`, and what are its values?
- b. The words and values that carry "agent" and "person" (`:make-agent`, `:end-agent`, `:a-person`, `:an-actor` and any other): say in an example what each names on the wire.
- c. Where does the command decide a session's `:driven-by`, and from what?
- d. Where does the command take its default lane from?

On the edge with the permission session: the wire's own table of who may say a word is yours to list. The store's permissions are not. Where you are unsure on which side something falls, list it, and say that it sits at the edge so that Sid can turn it.

A note on one word: in the skill and the root page, "a session" usually means an agent session that wrote code or pages. `:session` in the code is the product's own thing. Keep the two apart in your report.

## Bounds. These are strict; Sid asked for them in so many words

1. **Read only.** Write or edit no file anywhere: no notes on disk, no memory, no docs. No git command that changes anything. Start no process: no REPL, no tests, no proofs, and do not run the command-line program.
2. **The record is not yours.** Do not use memtool, the softland-memory tools, or recall, at all, for any reason. The skill's question 9 says to open the line Sid typed; in this run you do not. Where a docstring or page in your reads says Sid decided or called something, or cites a "Decisions of …" entry or an entry of "the runtime's page", write "not checked against the record: run 5" beside it with file:line, and list which code in your reads stands on it. Write nothing as Sid's decision yourself.
3. **Never read** `src/app/server/env.clj` or any `.env` file. Never open a `raw-` file under any `runs/` directory.
4. **Stay inside the reads above.** One allowance, so that the jobs of a word you own can be listed: you may *grep* the `*.clj` files of `src-store/`, `src-wire/`, `src-command/`, `src-runtime/` and `src-auth/` for where a word, a "could not" answer or a command you own is made or read, and report each hit as "located by grep, not reviewed" with file:line and the matched line. Keep `src-store/softland/store/record/tables.clj` out of every grep (the store's tables are not in these runs). Do not open those files to read them through, and raise no finding on code outside your reads. Where a finding would need such a file, write it under "still open" and name the run that reads that file. If a file outside your reads looks necessary (for example `http.clj` or `seam.clj` in the runtime), do not open it: name it in the list of what you did not read and say what it would have settled. `seam.clj`, `sign_in.clj` and `standing.clj` are run 1's reads.
5. **Never open**: `src-server/`, `src-cli/`, `src-server-browser/`, `src/app/` (the old server, the client, the renderer), `src-inland/`, the rig, `history/`, the store's tables, the plan for pieces 5 to 8.
6. **Permissions belong to a separate session.** It owns: who may act, who may see, who may read a lane's acts, end, yes, and what a permission covers. ("End" and "yes" are read here as the store's permission words.) Where you meet one of these, write one line: where you met it (file:line) and "permission session". Do not list its jobs and do not judge it.
7. **Ids of the other runs.** Where you meet one, write one line with file:line and the run number, and do not list its jobs:
   - run 1: person, root, agent, sign-in, session and run, `:who` on an act
   - run 2: version, act-id, fact, a fact with no value, stamp, time
   - run 3: the depot (streaming or microbatch), builds-on, marked keys, the words of a query and of an answer
   - run 5: the pages that say "Sid decided"

## Models, on Sid's instruction

You are the reviewer and the judgment is yours alone; it is never handed down. Where you need collection (every place a word is made or read, with file:line and the quoted line), you may hand that to a read-only gatherer on a lower model and effort: the Agent tool with `subagent_type` `Explore` or `code-hunter`, `model: "sonnet"`, `effort: "low"` or `"medium"`. Its brief must carry bounds 1 to 5 above, since it loads neither this brief nor the root page. A gatherer returns facts with file:line and never a verdict. Read for yourself every passage a finding rests on, and quote only what you yourself read. Your files are about 1,800 lines, so you may well need no gatherer; if you use none, say so.

## What to send back

Your final message is the whole deliverable, and it is all I will see, so make it complete. Plain words. Use Sid's word for a thing he has named (lane, act) and the standard term otherwise; add no vocabulary of your own. Give your view and its grounds. Name no fix beyond the split: the design is its author's and Sid's.

1. **What I read, and what I did not.** Each file, whole or in part. Any gatherer used, for what, on which model. Each grep outside the reads. Files you would have needed and did not open.
2. **The inventory.** For each word, each column of the words' table, each "could not" answer, each command and flag, and each other thing that crosses the wire: every job, each in one plain sentence, with file:line for where it is written and where it is read. For each job: decided when (at write, at read), whether it can change later, one or many, who changes it. Then: what is fixed for good (stored in an act, shown to someone, held by a program outside that was built against it, or written on a page as decided). A job you cannot say in one plain sentence is a finding by itself.
3. **What holds.** What you tested that came through, and which questions you asked of it.
4. **What breaks.** Each finding as the skill's "How to answer" has it: the thing, its jobs, how they differ (in when decided, in whether they change, or in how many), what it costs to separate now, what it costs later. Quote the line with file:line. Order them by what gets fixed for good soonest. Mark each one "certain from what I read" or "depends on something I did not read", and say what. Hold to the brake: "these could be separate" is not a finding. A line on a page that nothing is built on, and that the page itself marks as a default for Sid to turn, goes under "still open", not here.
5. **What is still open.** What the reads cannot settle, and whose it is: Sid's, run N's, or the permission session's. Include the answers to questions a to d above, or why they could not be answered.
6. **Pointers.** The one-line entries from bounds 2, 6 and 7.

Where you took a fork in how to read the task, say which way you went.
