# Open questions: the queue

What is not yet decided, once each, by group, with where its source lines are and which session is writing its sheet. **Answers never live here.** An answer, with Sid's reason, goes to the README of the code it shapes — today `src-store/README.md` ("Decisions of 6 October 2026" for the act, "Decisions of 7 October 2026 — the store's module" for the store). A row marked answered only points there. Form and rules of a sheet: [METHOD.md](METHOD.md).
A session writes its name under "taken by" on a group before it writes that group's sheet, so two sessions never write one group.

## Keys

- States: **open** · **answered** · **superseded** · **moot**. **open?** marks a row where a session could not tell whether an answer covers it; Sid or the parent session settles it.
- Sources outside the repo are in the research depot `/mnt/data/projects/research/softland/`:
  **BK** = `decisions-booklet-2026-10-06/` (`g2-door.txt`, `g3-depots.txt`, `g4-gates.txt`, `part-1-zero.txt`, `part-2-A.txt`) ·
  **LP** = `decision-print-2026-10-06/make_decision_print.py`, the 27 ladder questions ·
  **MS** = `all-decisions-2026-10-06/master.html`, rows 0, A, B, C ·
  **RS** = `all-decisions-rest-2026-10-06/make_rest.py`, rows E to I.
- Sources in the repo: **A** = `src/proposal/backend-on-rama-2026-10-02/ARCHITECTURE-STE100.md` ·
  **R** = `src/proposal/frame-2026-09-15/PROGRESS.md` (the rulings file) · **SR** = `src-store/README.md`.
- An id in brackets is the same question under another paper's number. **S1–S6** are the six items that R lists as "Still Sid's" (R:203–209).
- The placement of the ladder questions, the A §9 items and the "Still Sid's" items into groups is a session's (7 Oct), not Sid's. Rows of RS were grouped on that print (E, F, G, H, I); their places here follow it.

## Words Sid changed (recorded in src-store/README.md; kept here for the sheets)

- 7 Oct 2026: the act's id is called **act-id**, not "name" (Sid: "why call it name when it can be act-uuid; everyone will understand"). It is `[layer class id]`: the layer, the layer's class, a random UUID — more than the UUID alone, because the layer and the class route the act and find its answer. The word for "class" (streaming / microbatch) is still Sid's to give.

## The groups

| group | name | sheet | taken by | question ids |
|---|---|---|---|---|
| 1 | the act | answered: SR, "Decisions of 6 October 2026" (SR:34–99) | — | act 1–12, act 6c, act-kept, act 12z, A7, B6, ?7 |
| 2 | the door | `groups/g-store.txt` (one sheet for groups 2, 3 and 4) | act implementation analysis (Fable), 7 Oct | C1, C2, Q20, Q22, F7, F8.1, F8.2, Q23 |
| 3 | the depots | `groups/g-store.txt` | act implementation analysis (Fable), 7 Oct | F3, F6, Q7 |
| 4 | the gates and the record | `groups/g-store.txt` | act implementation analysis (Fable), 7 Oct | F2, F1, F4, F5, Q25, F9a, F9b, F9c, ?9, ?10, ?11, ?15 |
| 5 | who an act acts for | sheet not yet written | act implementation analysis (Fable), 7 Oct | E1, E2, E3, E4, E5, E6, E7, Q16, Q19, ?4 |
| 6 | reads | sheet not yet written | | Q11, ?6, ?12, S3, ?14, ?16 |
| 7 | above the act | sheet not yet written | | Q1, Q2, Q3, Q4, Q5, Q6, Q12, Q13, Q14, Q17, Q18, ?2, ?8, G1, G2, G3 |
| 8 | what a kept record carries | sheet not yet written | act implementation analysis (Fable), 7 Oct | H1, H2, H3, H4, H5, H6, H7, H8, H9, H10, H11 |
| 9 | the sheet itself | sheet not yet written | | Q9, Q10, Q15, Q21, Q26, Q27, Q7-sheets, Q8, Q24 |
| — | outside the nine groups: where answers are kept | not a group of the booklet | | 0.2, 0.3, I3 |

## The questions

| id | group | title | state | source | answer, by whom, date | taken by |
|---|---|---|---|---|---|---|
| act 1 (MS A1, BK A1) | 1 | The scope: the form and its check | answered | SR:45–46; BK part-2-A.txt:1 | see src-store/README.md | — |
| act 2 (MS A2, BK A2) | 1 | Whose act is the one form | answered | SR:47–50; BK part-2-A.txt:70 | see src-store/README.md | — |
| act 3 (MS A3, BK A3) | 1 | The seal and the act's form | answered | SR:51–54; BK part-2-A.txt:151 | see src-store/README.md | — |
| act 4 (MS A4, BK A4) | 1 | Where the five parts sit | answered | SR:55–59; BK part-2-A.txt:220 | see src-store/README.md | — |
| act 5 (MS A5, BK A5) | 1 | The class, and the name | answered | SR:60–69; BK part-2-A.txt:282 | see src-store/README.md | — |
| act 6 (MS A6, BK A6) | 1 | The form of a fact | answered | SR:70–76; BK part-2-A.txt:366 | see src-store/README.md | — |
| act 6c | 1 | Where a fact that names another layer is refused | moot | SR:73–75 | moot while 6b stands (SR) | — |
| act 7 (MS 0.1, BK 0.1) | 1 | The rig's standing | answered | SR:77–78; BK part-1-zero.txt:1 | see src-store/README.md | — |
| act 8 (MS B1) | 1 | The permission's form | answered | SR:79–84 | see src-store/README.md | — |
| act 9 (MS B2) | 1 | What an id is | answered | SR:85–87 | see src-store/README.md | — |
| act 10 (MS B3) | 1 | The microbatch hash entity | answered | SR:88–89 | see src-store/README.md | — |
| act 11 (MS B4) | 1 | What based-on holds; the basis of an outside thing | answered | SR:90–94 | see src-store/README.md | — |
| act 12 (MS B5) | 1 | The stamp, as an act carries it | answered | SR:95–96 | see src-store/README.md | — |
| act 12z | 1 | A carried stamp is not below zero | open | SR:97–99 | closed by a session, waiting for Sid's word (SR) | — |
| act-kept | 1 | Values kept from today's code, "not decided": a fact with no value is a retract; a permission is 4 levels deep at most; based-on optional, empty when absent; a carried stamp below 2^62; 256 subjects; 32 collections | open? | SR:75–76, 84, 93–94, 96–97 | SR marks each "kept from today's code, not decided" | — |
| A7 (MS A7, BK A7) | 1 | The language that declares the act's shape | open? | BK part-2-A.txt:428; MS row A7 | none of the twelve decisions names it | — |
| B6 (MS B6) | 1 | The order of the reasons (shape before rules), and the four limits | open? | MS row B6 | the limits: see act-kept; the order: none of the twelve decisions names it | — |
| ?7 | 1 | The open parts of the act | open? | A:1208 | its parts are answered by decisions 4, 5, 10, 12; the stamp's form is H2, the fingerprint S3, its "new condition" F7 | — |
| C1 (MS C1) | 2 | What is built next | answered | BK g2-door.txt:1; MS row C1 | see src-store/README.md | act implementation analysis (Fable), 7 Oct |
| C2 (MS C2) | 2 | The comparison with the rig | answered | BK g2-door.txt:205; MS row C2 | see src-store/README.md | act implementation analysis (Fable), 7 Oct |
| Q20 | 2 | Which back arrow comes into the door | answered | LP:83 | see src-store/README.md | act implementation analysis (Fable), 7 Oct |
| Q22 | 2 | How a streaming act gets its answer | answered | BK g2-door.txt:57; LP:88 | see src-store/README.md | act implementation analysis (Fable), 7 Oct |
| F7 (?7's new condition) | 2 | A rule for all that leaves a decision | answered | BK g2-door.txt:108; RS:75; A:946–947, A:1208 | see src-store/README.md | act implementation analysis (Fable), 7 Oct |
| F8.1 | 2 | A setting key on an entity that is not the act's layer: whose check | answered | BK g2-door.txt:157; RS:78 | see src-store/README.md | act implementation analysis (Fable), 7 Oct |
| F8.2 | 2 | A fact with a value and no session on the act: whose check | moot | BK g2-door.txt:157 | moot: the session is a required part (SR decision 4); Sid, 6 Oct 2026 | act implementation analysis (Fable), 7 Oct |
| Q23 | 2 | Where the ladder shows the layer and its class | answered | BK g2-door.txt:275; LP:70 | see src-store/README.md | act implementation analysis (Fable), 7 Oct |
| F3 (?3, S1) | 3 | Must a value be sealed before a depot keeps it | answered | BK g3-depots.txt:1; A:1204; R:203–206 | see src-store/README.md | act implementation analysis (Fable), 7 Oct |
| F6 | 3 | Two depots, or one | answered | BK g3-depots.txt:50; RS:72 | see src-store/README.md | act implementation analysis (Fable), 7 Oct |
| Q7 | 3 | What the depots are called (the ladder's words for them) | answered | BK g3-depots.txt:103; LP:91 | see src-store/README.md | act implementation analysis (Fable), 7 Oct |
| F2 (D2) | 4 | Where the record is | answered | BK g4-gates.txt:1; A:1194–1197; RS:60 | see src-store/README.md | act implementation analysis (Fable), 7 Oct |
| F1 (D1) | 4 | Who sets the class of a layer | answered | BK g4-gates.txt:50; A:1179–1192; RS:57 | see src-store/README.md | act implementation analysis (Fable), 7 Oct |
| F4 (?5) | 4 | The class of each layer at the start | answered | BK g4-gates.txt:101; A:1206, A:1232–1239; RS:66 | see src-store/README.md | act implementation analysis (Fable), 7 Oct |
| F5 (?13) | 4 | Where a streaming gate finds a grammar | answered | BK g4-gates.txt:143; A:1214; RS:69 | see src-store/README.md | act implementation analysis (Fable), 7 Oct |
| Q25 | 4 | Locks, and the seal in the door | answered | BK g4-gates.txt:192; LP:75 | see src-store/README.md | act implementation analysis (Fable), 7 Oct |
| F9a (S4) | 4 | Can two nodes run here | answered | BK g4-gates.txt:228; R:208 | see src-store/README.md | act implementation analysis (Fable), 7 Oct |
| F9b (S5) | 4 | The three limits | answered | BK g4-gates.txt:228; R:208–209 | see src-store/README.md | act implementation analysis (Fable), 7 Oct |
| F9c (S6) | 4 | The size of a usual value in a hand session | answered | BK g4-gates.txt:228; R:209 | see src-store/README.md | act implementation analysis (Fable), 7 Oct |
| ?9 (F10) | 4 | A re-class with acts in flight | open | A:1210; BK g4-gates.txt:267 | listed, not asked: waits for a test | act implementation analysis (Fable), 7 Oct |
| ?10 (F10) | 4 | A promotion out of a microbatch layer | open | A:1211; BK g4-gates.txt:267 | listed, not asked: waits for a build | act implementation analysis (Fable), 7 Oct |
| ?11 (F10) | 4 | A record only the gates can read; bells apart | open | A:1212; BK g4-gates.txt:267 | listed, not asked: waits for a build | act implementation analysis (Fable), 7 Oct |
| ?15 (F10) | 4 | Task 0: all microbatch decisions, and some streaming layers | open | A:1216; BK g4-gates.txt:267 | listed, not asked: waits for an examination | act implementation analysis (Fable), 7 Oct |
| E1 | 5 | The roots of a permission: a person, a group, or the system | open | RS:33 | — | |
| E2 | 5 | Who may grant a narrower permission | open | RS:36 | — | |
| E3 | 5 | The top of the chain: the session, or a root above it | open | RS:39 | — | |
| E4 | 5 | One act, three names: who acts, who holds the permission, whose session | open | RS:42 | — | |
| E5 | 5 | Acting after the session closes | open | RS:45 | — | |
| E6 | 5 | An ingestor's session | open | RS:48 | — | |
| E7 | 5 | An agent's credentials: the word, and whether they say whose act it is | open | RS:51 | — | |
| Q16 | 5 | Sign-in: where it is on the map (its act side is act 9) | open | LP:30 | — | |
| Q19 | 5 | The session, permissions, policy: where drawn (its act side is act 8) | open | LP:59 | — | |
| ?4 | 5 | What identifies a person in the store | open? | A:1205 | act 9 says what an id is; whether that covers ?4 a session cannot tell | |
| Q11 | 6 | Where reads sit, and the read entry | open | LP:128 | deferred by Sid, 6 Oct 2026: "I will come to the read question when we have built the other system" (RS page 1) | |
| ?6 (G5) | 6 | Does one read line up the moments of the layers that it read | open | A:1207; RS:97 | — | |
| ?12 (G7) | 6 | The cost of a read entry for each read; does a reader ring itself | open | A:1213; RS:103 | waits with Q11 (RS G7) | |
| S3 (G7) | 6 | What a read entry's fingerprint covers | open | R:207; RS:103 | waits with Q11 (RS G7) | |
| ?14 (G6) | 6 | The maintain step across layers and tasks: what stands on this | open | A:1215; RS:100 | — | |
| ?16 (G8) | 6 | One query for a full context | open | A:1217; RS:105 | listed on RS as not for a ruling yet: not built | |
| Q1 | 7 | Is the screen an inside source, or a part of the outside | open | LP:23 | — | |
| Q2 | 7 | Are other instances an outside source | open | LP:27 | — | |
| Q3 | 7 | What is an inlet | open | LP:36 | — | |
| Q4 | 7 | Where an adapter ends; who makes identity, address and basis (the basis side is act 11) | open | LP:47 | — | |
| Q5 | 7 | Are the adapters above or under the outside inlet | open | LP:52 | — | |
| Q6 | 7 | Can a converter give no act | open | LP:56 | — | |
| Q12 | 7 | What RUN BY runs | open | LP:111 | — | |
| Q13 | 7 | Is paint an ask | open | LP:114 | waits with Q11 (RS G7) | |
| Q14 | 7 | The name of the AN ASK box, and the way back | open | LP:117 | — | |
| Q17 | 7 | Base functions, primitives, grants | open | LP:40 | — | |
| Q18 | 7 | May a body touch the outside | open | LP:124 | — | |
| ?2 (G4) | 7 | Where match, tools and the maintain step run | open | A:1203; RS:95 | — | |
| ?8 (G8) | 7 | A designed total language, so that converters and base functions can be data | open | A:1209; RS:105 | listed on RS as not for a ruling yet: needs a proof | |
| G1 | 7 | Does an inside thing ever pass through an adapter | open | RS:88 | — | |
| G2 | 7 | Is the one body language grown from Inland's | open | RS:90 | — | |
| G3 | 7 | Are adapters and converters data | open | RS:92 | — | |
| H1 | 8 | R1, forget: "Forget is immediate" | open | RS:110 | — | |
| H2 (S2) | 8 | R2, the stamp's form: a hybrid of wall milliseconds and a counter | open? | RS:112; R:206–207 | act 12 says a carried stamp is an integer; whether that settles the stamp's form a session cannot tell | |
| H3 | 8 | R3, read entries carry a frontier | open | RS:114 | — | |
| H4 | 8 | R4, based-on is fact ids with their stamps | open? | RS:116 | act 11 says the same of the act's form; whether it closes R4 a session cannot tell | |
| H5 | 8 | R5, reads go through a read gateway on the server | open | RS:118 | — | |
| H6 | 8 | R6, standing reads | open | RS:120 | — | |
| H7 | 8 | R7, how permissions reach a layer | open | RS:122 | — | |
| H8 | 8 | R8, the base starts as a one-owner layer owned by the root actor | open | RS:124 | — (goes with F4) | |
| H9 | 8 | Forget against a read-out: which comes first | open | RS:126 | — | |
| H10 | 8 | Forget after a revoke | open | RS:129 | — | |
| H11 | 8 | Writes about someone already forgotten | open | RS:132 | — | |
| Q9 | 9 | The loop, and the bells | open | LP:106 | — | |
| Q10 | 9 | A feed (goes with ?2) | open | LP:43 | — | |
| Q15 | 9 | The runtime and the module as bands | open | LP:136 | — | |
| Q21 | 9 | The door and the new tree in the data of sheets 2 and 3 (the booklet put it in group 2) | open | LP:63; BK g2-door.txt:235 | — | |
| Q26 | 9 | DATA AS CODE and PROBES | open | LP:139 | — | |
| Q27 | 9 | The word "store" | open | LP:142 | — | |
| Q7-sheets | 9 | Q7's second part: on sheets 2 and 3, is the code of the depots counted in a row of its own or with the gates | open | BK g3-depots.txt:103; LP:91 | — lifted off the store's sheet on 7 Oct (the coordinator): it changes no code | |
| Q8 | 9 | Is there a box under the depots on the ladder | open | BK g4-gates.txt:192; LP:99 | — lifted off the store's sheet on 7 Oct (the coordinator): it changes no code | |
| Q24 | 9 | What else the ladder shows below the depots | open | BK g4-gates.txt:192; LP:102 | — lifted off the store's sheet on 7 Oct (the coordinator): it changes no code | |
| 0.2 (MS 0.2) | — | What wins when a file, a session and the code disagree | open? | BK part-1-zero.txt:48; MS row 0.2 | none of the twelve decisions names it | |
| 0.3 (I1, I2) | — | Where answers are written; what is dead in docs/ | open? | BK part-1-zero.txt:98; RS:138, RS:141 | METHOD.md and this file now say where answers go; whether Sid ruled it a session cannot tell | |
| I3 | — | "Land it": the map page of the architecture-fix chat | open? | RS:144 | a session cannot tell whether it landed | |
