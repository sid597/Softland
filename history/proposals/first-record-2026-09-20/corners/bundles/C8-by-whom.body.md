# C8 by-whom (8): body

Copied by line number by bundle.py, never retyped. Tags and notes are Codex's, under
yardstick.prompt.md. L-numbers are lines in the named research file. Zone: the team-by-team body of the research files.

---
**datalog L203-203 · BODY · DISAGREES C8,C7**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.4 Question by question*

- INSTITUTIONAL. So: the first facts are written by the system, not by an actor. They are identical in every database. They carry a time that is openly not a real time. Room below 1000 is reserved so the system's own vocabulary can grow. The `:db` namespace is reserved for the system for ever: "The :db namespace, and all :db.* namespaces, are reserved for use by Datomic." [D-SCHEMA]. The vocabulary for forgetting (`:db/excise`) is among the first facts.

---
**datalog L229-229 · BODY · NEW-REASON C8,A2**

- REPORTED. Datomic has no built-in actor. It is an attribute the application puts on the transaction: "the purpose of the transaction, the application that executed it, the provenance of the data it added, or the user who caused it to execute." [D-TXDATA]. Nubank does exactly this: "we attach that with the Git version of the service, we attach the credentials of the user that is making that transaction, and several other things." [N-QCON]

---
**datalog L230-230 · BODY · DISAGREES C8,E4**

- REPORTED. Nobody checks it. Datomic trusts its peers. The docs warn only that "database functions are deployed via transactions, so you should prevent arbitrary transactions from untrusted users." [D-TXFN]

---
**datalog L231-231 · BODY · ABOVE C8,C7,E4**

- INFERRED. This is a real gap between the camp and Sid. Datomic's writers are a company's own servers. Sid's writers are people, agents and models that do not trust each other. The camp has no answer to "who checks by-whom" because it never had to. What it does offer is the *place*: actor, the principal acted for, and the grant that allows it are three attributes on the saying, each pointing at an entity. "Acts for" is then a fact like any other, with its own history. Since "transactions can have an open set of attributes", agent and person need not be squeezed into one slot. A slot forces the choice Sid is asking about. An open set does not.

---
**datalog L232-233 · BODY · NEW-REASON C8**

- Missing voice: Fluree signs transactions, so by-whom is verified by cryptography. Noted in section 6.


---
**datalog L342-342 · BODY · DISAGREES C8**
*2. Rich Hickey and Datomic (with Stuart Halloway, the Datomi › 2.5 What resembles Sid's situation, and what differs*

- **Trusted writers versus strangers.** Datomic never verifies an actor.

---
**datalog L402-402 · BODY · NEW-REASON C8,A2**
*3. Nubank: Datomic lived with, at scale › 3.4 Which questions they speak to*

- **(8) by whom and (12) runtime version.** REPORTED. Both are on every transaction: the user's credentials and the git version of the service. This is practice since the early years, and they name no regret about it.

---
**datalog L411-412 · BODY · ABOVE C8,C3**
*3. Nubank: Datomic lived with, at scale › 3.5 Resembles and differs*

Resembles: years of operation, audit permanence by law, very many writers, real scale, provenance on every saying. Differs: a closed world with one owner, trusted writers, each database belonging to one service, no shared layer anyone may write.


---
**datalog L576-576 · BODY · NEW-REASON C8,X4**
*6. The group: who matters most, who I dropped, who is missin*

- **Fluree.** Datomic-shaped facts in an append-only ledger, with signed transactions and policy stored as data. I checked one page. It answers (16) the opposite way to Instant: "default-allow: false — Fail-closed. A flake with no targeting policies is denied. Recommended for production." and "default-allow: true — Fail-open… Useful in development or in deployments where an application layer handles authorization and Fluree is recording signed transactions for provenance." [FLU]. Fluree is the one system here that verifies by-whom with cryptography. It deserves a proper read for (8), (15) and (16).

---
**log L411-412 · BODY · DISAGREES C8**
*Question by question: what this camp would say › (8) By whom*

This camp says little. CT does not record the submitter at all. In the AT Protocol the account signs the root of its own repository, so the signature is the "who". Helland: "Managing the requester's identity, the target's identity, and the identity of the work in question are some of the hardest problems in scalable systems that need idempotence", and across trust boundaries systems "provide an alias". Trillian's Claimant Model asks of any logged statement who claims it, who relies on it, who can check it, and who acts when it is false.


---
**meaning L112-128 · BODY · NEW-REASON C8,C4**
*Part one — the exchange › 1.2 The exchange, in order, in their words*

**Turn 4 — Hickey, 11946764, 16:11**

> Data without an interpreter is certainly subject to (multiple) interpretation
> :) For instance, the implications of your sentence weren't clear to me, in
> spite of it being in English (evidently, not indicated otherwise). Some
> metadata indicated to me that you said it (should I trust that?), and when.
> But these seem to be questions of quality of
> representation/conveyance/provenance (agreed, important) rather than critiques
> of data as an idea. Yes, there is a notion of sufficiency ('42' isn't data).
>
> Data is an old and fundamental idea. Machine interpretation of un- or
> under-structured data is fueling a ton of utility for society. None of the
> inputs to our sensory systems are accompanied by explanations of their
> meaning. Data - something given, seems the raw material of pretty much
> everything else interesting, and interpreters are secondary, and perhaps
> essentially, varied.


---
**meaning L492-498 · BODY · NEW-REASON C8,E4**
*Part one — the exchange › 1.6 My reading: is this the axis under Sid's questions?*

**Fourth, the thread's two loose ends are Sid's authority rows.** Hickey, in
passing: "Some metadata indicated to me that you said it (should I trust
that?)". That is question (8). Kay's closing question on the safety of a
transmitted meaning, with Hickey's "I don't trust you enough to run it", is
question (15). Neither of them picks these up. The capability camp did. In that
sense Part two continues the thread.


---
**meaning L667-675 · BODY · NEW-REASON C8**
*Part two — the people › 2.1 The capability camp: Hardy, Miller, Yee, Shapiro, Donnel*

**How they keep "who did it" without identity-based access: Horton.** Miller,
Donnelley, Karp, "Delegating Responsibility in Digital Systems: Horton's 'Who
Done It?'" (2007),
https://www.usenix.org/legacy/event/hotsec07/tech/full_papers/miller/miller.pdf.

- They state the old objection fairly. REPORTED: "Because ocaps operate on an
  anonymous "bearer right" basis, they seem to make reactive control
  impossible. [...] a remaining unrefuted criticism is that they cannot record
  who to blame for which action".

---
**meaning L676-680 · BODY · NEW-REASON C8**

- The mechanism: each side logs, locally, who it holds responsible. Delegation
  passes authority and responsibility together. REPORTED: "today we have no
  means for delegating responsibility, that is, delegating authority coupled
  with assigning responsibility for using that authority." And: "Carol tags S3
  with Bob's Who, so Carol can blame Bob for messages sent to S3."

---
**meaning L681-684 · BODY · NEW-REASON C8**

- A deliberate limit. REPORTED: "To avoid non-repudiation [1], we accept that
  Bob can log bad data fooling himself into blaming the wrong party." In this
  camp "by whom" is a belief held by the party keeping the log. It is not a
  proof to the world.

---
**meaning L685-693 · BODY · DISAGREES C8**

- Varda gives the same shape in plain terms (Hacker News, 2018, item 16098698).
  REPORTED: "If you have a table mapping user identities to roles, that's an
  ACL, not a capability system." Then: actions through a delegated capability
  are logged "via Bob". If Bob delegates to Carol, "you might see "via Bob; via
  Carol". This means: "Bob claims that Carol performed this action." No one
  other than Bob actually needs to know who "Carol" is [...] Since the assertion
  in the audit log says "via Bob" first, we know to hold Bob responsible first.
  We only care about Carol to the extent that we trust Bob."


---
**meaning L699-700 · BODY · NEW-REASON C8**

- The accountability gap stood for about twenty years before Horton answered
  it. The authors say so.

---
**meaning L715-724 · BODY · DISAGREES C8**

**What they would say to Sid's questions.**

- (8) by whom. REPORTED: identity is needed to know who said a thing; it must
  not be what decides whether a thing may be done. INFERRED: "by whom" should be
  a chain, not a name. The gate can vouch only for the head of the chain: the
  party that presented the offer over an authenticated channel. Every later
  link is a claim by the link before it. A person's agent is itself, with its
  own id, and never the person. "Acts for" is a grant: a fact written by the
  person, which the agent's offers cite. Both the person and the instrument are
  then on every fact.

---
**meaning L1304-1312 · BODY · NEW-REASON C8**
*Part two — the people › 2.6 RDF, W3C PROV, nanopublications, trusty URIs*

**PROV: "acts for" is its own identified relation.** "Delegation is the
assignment of authority and responsibility to an agent (by itself or by another
agent) to carry out a specific activity as a delegate or representative, while
the agent it acts on behalf of retains some responsibility for the outcome of
the delegated work." It may be scoped to one activity. And they stop short on
purpose: "we do not say explicitly who bears responsibility and to what
degree." This matches the capability camp: delegation is a separate fact with
its own id, not a slot filled in on every record.


---
**meaning L1367-1367 · BODY · NEW-REASON C8**

- Key loss and key compromise are still open, by their own account.

---
**meaning L1406-1415 · BODY · ABOVE C8,E5**

**Transfer.** PROV is a vocabulary, not a store; nobody had to pay for writing
it all down at machine rate. Nanopublications are the nearest small-scale twin
of Sid's base layer: append-only, attributed, provenance on every record. They
differ by having no gate: ids are made by authors, validity is judged by
readers. Disagreements: Halpin and Hayes against linked-data practice on
sameness; Hogan and others against RDF's local ids; Kuhn against blockchains
("identity is inseparably linked to private key access"). I could not open
"The Rationale of PROV" (2015), the paper most likely to say what PROV's authors
would change. Every mirror refused. Nothing here is drawn from it.


---
**meaning L1817-1827 · BODY · DISAGREES C8,X4**
*Part three — substrates › 3.1 Dynamicland's Realtalk, and Folk Computer*

**Who said it.** In Realtalk a page claims things about itself (`Claim (you) is
geomap of ...`). In Folk, attribution is a convention, not a slot. INSTITUTIONAL
(Folk README): "Notice that you should scope your claim: it's `$this has a
ball`, not `there is a ball`, so different programs with different values of
`$this` will not stomp over each other." And any program can overwrite
another's held state: "`Hold! -on 852 { ... }`". There is no protection. The
Dynamicland FAQ explains why that is fine for them: "Realtalk has no user
accounts — there's no concept whatsoever of a "user" within the system. There's
just stuff on the table. The stuff doesn't know whose stuff it is." The room is
the trust boundary.


---
**meaning L2278-2291 · BODY · DISAGREES C8**
*Part four — Sid's questions, hung on the parts of the fact › (8) By whom*

### (8) By whom

**Camp.** Identity is for knowing who said a thing. It must not be what decides
whether a thing may be done (Lemmer-Webber, 2.1). Attribution in a capability
system is a chain. Each link is vouched for by the one before: "Bob claims that
Carol performed this action" (Varda, 2.1). Delegation hands over authority and
responsibility together (Horton, 2.1). PROV makes "acts for" its own identified
relation, scoped to an activity if wanted (2.6). Xanadu: "no one may endorse with
the identity of another"; a club's signature right is the recorded "acts for"
(3.5). Wikidata shows the cost of getting it wrong: mass edits by tools under
people's accounts made the bot policy "ineffective" (2.5). Folk shows that
attribution by convention is not attribution (3.1). Hickey saw the question and
left it: "(should I trust that?)" (1.2).


---
**meaning L2292-2297 · BODY · NEW-REASON C8**

**Split.** Sign every offer, or not. Nanopublications and AT Protocol sign. Signed
records authenticate themselves anywhere, so a second store and a credible exit
become possible. But key loss and key compromise are, by Kuhn's account, still
open (2.6). Horton refuses signatures on purpose: "To avoid non-repudiation"
(2.1).


---
**rama L104-105 · BODY · NEW-REASON C8,E4**
*Part one — What Rama's own reference says › Question by question › (8) By whom: who checks it? Is a person's agent itself, or t*

**NOT IN THE REFERENCE.** I searched every page for authentication, authorization, permission, credential, access control, encryption, TLS. Nothing, apart from an app-level password hash in the tutorial.


---
**rama L106-106 · BODY · NEW-REASON C8**

- **CHECKED** `skill/depot-design.md:132-134` — "Depot partitioners run on the appending client." The client is trusted code.

---
**rama L107-107 · BODY · NEW-REASON C8**

- **CHECKED** `docs/24-rest-api.md:5-20` — the REST API takes plain HTTP POSTs for appends and reads. No credential appears in it.

---
**rama L317-318 · BODY · NEW-REASON C8,E4**
*Part two — Nathan Marz › 4. Which questions he speaks to, and what he would say*

**(8) By whom.** Almost nothing. REPORTED, one sentence: "Because the events store is immutable and constantly growing, redundant checks, like permissions, can be put in to make it highly unlikely for a mistake to trample over the events store." (*Big Data*, chapter 1). Permissions, for him, protect the log from mistakes. They sit in front of it. His writers were a handful of trusted pipelines inside one company. INFERRED: he would treat the actor as a field in the record.


---
**rama L343-343 · BODY · NEW-REASON C8,E1**
*Part two — Nathan Marz › 5. What resembles Sid's situation, and what differs*

- His facts described the world (tweets, pageviews), written by a few trusted pipelines in one company. Sid's facts are acts by many writers who must be told apart, checked, and sometimes refused. Marz never had a gate, a policy, or a compare-and-set on the master dataset.

---
**rama L421-422 · BODY · NEW-REASON C8,E1,E6**
*Part three — The dissent: Jay Kreps › 5. Resemblance and difference*

Differs: Kafka's records are a transport between a company's systems. Writers are trusted services. No per-record attribution, no admission control, no reads recorded. Kafka does not care what a record means.


---
**skeptics L40-41 · BODY · DISAGREES C8,E4**
*1. The ordinary default, question by question*

**(8) By whom.** An application-set column (`created_by`). The database sees one service account and verifies nothing about the person. Delegation ("acts for") lives in access tokens and in the cloud audit log, not in the data (COMMON PRACTICE). Three voices call this default weak. Hamilton, REPORTED: "Audit all operations." and then: "this won't do much good if everyone is using the same account to administer the systems. A very bad idea but not all that rare." Andy Pavlo, REPORTED: "lazy practices like giving admin privileges to every account or using the same account for every service are going to get wrecked when the LLM starts popping off." ([Databases in 2025: A Year in Review, 4 Jan 2026](https://www.cs.cmu.edu/~pavlo/blog/2026/01/2025-databases-retrospective.html)). Meta's FlightTracker team, REPORTED: "An early lesson was that identifying the appropriate user for a web request was much more difficult than we originally expected. Request endpoints may be invoked before login or after logout; internal applications may track user contexts using bespoke mechanisms; and applications may involve multiple identities, such as when a user manages a business account." ([FlightTracker, OSDI 2020](https://www.usenix.org/system/files/osdi20-shi.pdf))


---
**skeptics L237-237 · BODY · DISAGREES C8**
*4. Question by question: the default beside what these voice*

| Sid's question | Ordinary default | Skeptics and big tech |
| (8) by whom | App-set column; one shared DB account | Hamilton, Pavlo: shared accounts defeat audit (REPORTED). FlightTracker: finding the right user "much more difficult than we originally expected" (REPORTED) |

---
**sync L179-182 · BODY · DISAGREES C8**
*2. Section one: sync and multiplayer › 2.1 Bayou (Doug Terry, with Theimer, Petersen, Demers, Sprei*

- "Acts for" is explicit: "Client applications and Bayou servers operate on
  behalf of users and obtain the key pair and access control certificates from
  the corresponding user at start-up time." Certificates "grant, delegate and
  revoke access". (SOSP 1995)

---
**sync L233-235 · BODY · DISAGREES C8**

- (8), (15) Delegation is a signed certificate; software acts "on behalf of"
  the user with the user's credentials. REPORTED. Note this is impersonation:
  the record shows the user, not the program. INFERRED.

---
**sync L859-864 · BODY · NEW-CASE C8**
*2. Section one: sync and multiplayer › 2.8 Automerge (Martin Kleppmann, Peter van Hardenberg, Orion*

- *Actor ids are a footgun at scale.* Alex Good: "I would like to get rid of
  actor IDs, I think they are currently a bit of a footgun… in practice users
  generate a new actor ID for each run of a program. The problem with this is
  that for long lived documents (or documents with many editors) this can lead
  to an unexpected explosion in the size of the encoded document". (#546) This
  is Sid's "tens of agents per person", already felt.

---
**sync L892-898 · BODY · NEW-CASE C8**

- *Authorship is only now arriving.* "Alex Good has also begun initial work
  adding author provenance to Automerge. Automerge will be able to tell you who
  wrote which commit… which has historically been a surprisingly thorny issue
  for CRDTs." ("This Month in Automerge: July '26",
  https://automerge.org/blog/2026-july/) A 2025 user question on how to tie
  actor ids to users got the answer every such system gives: keep a table
  outside.

---
**sync L916-919 · BODY · NEW-REASON C8**

- (2) A fresh random actor id per process is cheap to make and expensive to
  keep, once actors number in the thousands. REPORTED. For Sid: an agent run
  should not mint a new long-lived actor each time; the run is a session of a
  standing actor. INFERRED.

---
**sync L934-934 · BODY · NEW-CASE C8**

- (8) Actor is not author. Eight years in, authorship is being added. REPORTED.

---
**sync L1046-1048 · BODY · NEW-REASON C8**
*2. Section one: sync and multiplayer › 2.9 Ink & Switch essays and lab notebooks (van Hardenberg, L*

- Identity is left out on purpose: "Keyhive deliberately excludes user identity
  (i.e. the binding of a human identity to an application's identifier like a
  public key)."

---
**sync L1049-1054 · BODY · NEW-REASON C8**

- A person is a group: "An individual is identified by a single Ed25519 public
  key - which is immutable - whilst a group is a collection of other principals
  … One way we intend to use this is to represent a person (or more
  specifically their authority) as a group". Devices sit "behind a proxy
  ('Alice'). Documents in this scenario only need to know about Alice, not
  every device."

---
**sync L1127-1130 · BODY · NEW-REASON C8,E4**

- (8), (15) An agent is its own collaborator with its own branch; a person
  accepts or rejects; the timeline shows which edits were the agent's; the
  agent's prompt is a versioned record. REPORTED. A person's authority is a
  group of keys, so records name "Alice" and not each device. REPORTED.

---
**sync L1170-1172 · BODY · DISAGREES C8,E3**
*2. Section one: sync and multiplayer › 2.10 Yjs (Kevin Jahns; 2015–now)*

- What a delete forgets: "Yjs does not record metadata about a deletion: — No
  data is kept on *when* an item was deleted, or which user deleted it."
  (INTERNALS.md)

---
**sync L1186-1197 · BODY · NEW-CASE C8,E3**

- Who and when are being retrofitted. The v14 design note: "In order to
  implement a Google Docs-like versioning feature, we want to be able to
  attribute content with additional information (who created the change, when
  was this change created, ..)." The mechanism is a side map keyed by id
  ranges, because the item id is the only hook left. The worked example needs
  `gc: false`, and the application must supply the author as a literal. The
  docs' own example of authors: `'Bob'` and `'OpenAI o3'`. The earlier add-on,
  `PermanentUserData`, kept a second per-user delete log and answered "who
  deleted this" by scanning every user's log; it is gone from v14. v14 has been
  in prerelease since 2022. (attributing-content.md,
  https://github.com/yjs/yjs/blob/main/attributing-content.md; FOSDEM 2026
  abstract)

---
**sync L1204-1213 · BODY · NEW-CASE C8,E3,C1**

**4. Which questions.** (8), (11 when) If who and when are not in the record at
birth, they come back years later as a side table that works only for records
made after the add-on, and only with garbage collection off. REPORTED. (2)
Random ids without an authority can clash, and a clash is found only after
damage. REPORTED. With a gate, a clash can be refused at admission. INFERRED.
(9) Dropping deleted content is easy; knowing who deleted what, or erasing on
demand, is not. REPORTED. (6), (7) A delete that records nothing about itself
is the cheapest design and the one he is now undoing. INFERRED from the v14
work.


---
**sync L1317-1321 · BODY · NEW-REASON C8**
*2. Section one: sync and multiplayer › 2.12 Matthew Weidner (CRDT survey, Fugue, Collabs, list-posi*

- "a replica is not synonymous with a device or a user… In previous posts, I
  often said 'user' out of laziness". And: "Avoid the temptation to reuse a
  replica ID across replicas on the same device… That can cause problems if the
  user opens multiple tabs, or if there is a crash failure". (same)


---
**sync L1344-1344 · BODY · NEW-REASON C8**

- (8) Replica is not user. REPORTED.

---
**sync L1423-1427 · BODY · DISAGREES C8**
*2. Section one: sync and multiplayer › 2.13 AT Protocol and Bluesky (Paul Frazee, Bryan Newbold, Da*

- Keys are held for people: "manual key management is not appropriate for most
  users… The Bluesky PDSes therefore hold these signing keys custodially on
  behalf of users". (Kleppmann et al., "Bluesky and the AT Protocol", 2024,
  https://arxiv.org/abs/2402.03239)


---
**sync L1512-1513 · BODY · DISAGREES C8**

- (8), (15) Custodial keys; the host acts for the person. INSTITUTIONAL. The
  record shows the person. INFERRED.

---
**sync L1534-1539 · BODY · NEW-REASON C8,C5**

**6. Who they disagree with.** With Secure Scuttlebutt (no deletion, no
multi-device, unrecoverable keys). With Nostr (manual keys, no rotation). With
blockchains (cost per user). With pure peer-to-peer: "it has no answer for the
governance of shared resources" (Frazee, "Practical Decentralization", 2026).
Inside the team: whether the spec follows the code or the code the spec.


---
**sync L1571-1579 · BODY · DISAGREES C8,E4**
*2. Section one: sync and multiplayer › 2.14 Nostr (fiatjaf; also hodlbod, and the NIP authors; 2020*

- *Delegation on every record was tried and withdrawn.* NIP-26 now carries the
  banner "`unrecommended`: adds unnecessary burden for little gain". fiatjaf's
  reason: "in a world in which most Nostr users are using NIP-26 for
  everything, clients that do not implement NIP-26 become completely useless,
  as all they will see is a constant stream of random keys." (2023,
  https://fiatjaf.com/4c79fd7b.html) What won is remote signing (NIP-46), where
  the signer "generally has control over these keys" and signs as the user. The
  record is then identical whoever acted. Scope lives in a connection string,
  never in a record.

---
**sync L1610-1613 · BODY · DISAGREES C8,E4**

- (8), (15) On-record delegation failed because every reader had to understand
  it. REPORTED. That burden is lighter with one store, where the gate resolves
  delegation once at admission. INFERRED. The replacement erased the agent from
  the record. REPORTED. Do not copy that. INFERRED.

---
**sync L1622-1627 · BODY · DISAGREES C8,E4**

**5–6.** Like Sid's: small records, kinds, an id per record, one envelope for
everything. Unlike: no authority, no order, no provenance. fiatjaf disagrees
with delegation schemes, with edits, with "self-sovereign" slogans ("these keys
don't do anything without the means of actual action in the world"), and with
p2panda-style designs that lean on one strong peer.


---
**sync L1663-1671 · BODY · NEW-CASE C8,E4**
*2. Section one: sync and multiplayer › 2.15 Secure Scuttlebutt and its successors (Dominic Tarr, An*

- *One identity, many devices, recorded on the record.* PPPPP messages carry
  `accountTips`, the tips of the account's own history, so each record states
  which version of the identity it was signed under. Delegation is recorded
  with scoped powers (`'add' | 'del' | 'internal-encryption' |
  'external-encryption'`) and a consent signature. SSB's own fix came late and
  partial: in "fusion identity", "Only new members can be added to a fusion,
  there is no removal of members only tombstoning", and the spec's list headed
  "Out of scope for v1" includes "Use of fusion identity for authorisation
  logic".

---
**sync L1878-1879 · BODY · NEW-REASON C8**
*3. Section two: versioning › 3.1 Git (Linus Torvalds, Junio Hamano, Jeff King, brian m. c*

- (8) Two actors per record (who made it, who admitted it). Identity fixes are
  applied at display time from a separate, versioned map. REPORTED.

---
**sync L2196-2201 · BODY · NEW-REASON C8,C5**
*3. Section two: versioning › 3.6 Pijul (Pierre-Étienne Meunier, Florent Becker; 2015–now)*

- Authors are keys, names are elsewhere: identities give "greater security and
  control than simply mapping authors to a name and email address. The
  inclusion of a name and email address can be spoofed in a way that a key
  signature cannot." And names may change later, because they are "no longer
  tied to their submitted patches". (manual, Pijul identities)


---
**sync L2218-2222 · BODY · NEW-CASE C8**

- *Authors.* "The 'author' field in patches now gives you the choice between a
  simple free-format string, or your public key, and a mapping between public
  keys and identities is stored in repositories". He adds that author names
  "weren't initially the main focus of the project". ("Two changes to changes",
  2021)

---
**sync L2503-2506 · BODY · NEW-CASE C8**
*4. Question by question › (8) By whom: who checks it? Is an agent itself, or the perso*

- A replica is not an author. Weidner said so of his own earlier posts. Yjs and
  Automerge recorded replicas only and are both adding authorship in 2026; Yjs's
  first bolt-on kept a parallel log per user and answered by scanning.
  REPORTED.

---
**sync L2507-2508 · BODY · NEW-REASON C8**

- Two actors on a record is normal: Git's author and committer; Bayou's
  accepting server and user. REPORTED (Bayou); background knowledge (Git).

---
**sync L2509-2511 · BODY · DISAGREES C8**

- Acting *as* the person loses the actor: Nostr remote signing, Bayou's
  applications holding the user's keys, AT Protocol's custodial hosts.
  REPORTED.

---
**sync L2512-2515 · BODY · DISAGREES C8,E4**

- Delegation written on every record failed in Nostr because every reader had
  to understand it. REPORTED. PPPPP writes the identity version and scoped
  powers instead. Matrix writes the authorising events onto each event.
  REPORTED and INSTITUTIONAL.

---
**sync L2516-2517 · BODY · NEW-REASON C8**

- A person is a group of keys, so records name the person and not the device:
  Keyhive. REPORTED.

---
**sync L2518-2520 · BODY · DISAGREES C8**

- The author on the record is a key; the name is a changeable mapping kept
  outside: Pijul. Fossil lets a later record override the user, and lets a user
  be erased while their commits remain. REPORTED.

---
**sync L2523-2525 · BODY · NEW-REASON C8,E4**

- The server authenticates; that is where rejection can live: Boodman.
  REPORTED. Signatures age: "With key rotation, verification of older commit
  signatures can become ambiguous" (AT Protocol). INSTITUTIONAL.

---
**sync L2917-2927 · BODY · DISAGREES C8**
*4. Question by question › (15) A click: which tools may act in a person's name?*

**What the camp says.** The record of a person's act is the named action and
its arguments; the server produces the effects: Zero ("a record of the mutator
having run with certain arguments"). REPORTED. An approval should bind to
exact content: Automerge's intent record. REPORTED. Agents propose on their own
branch; the person merges: Patchwork. REPORTED. Scopes people reached for
first: kind and time window (Nostr's delegation conditions; remote-signer
permissions per kind). REPORTED. Scoped powers with consent, recorded: PPPPP.
Attenuated delegation through groups: Keyhive. Signed delegation certificates:
Bayou. REPORTED. Acting as the person erases the tool from the record: Nostr
remote signing, Bayou, AT Protocol hosts. REPORTED.


---
**sync L3210-3211 · BODY · NEW-REASON C8**
*6. The second store: what each source implies*

- **People.** A permanent actor id apart from any name, with authority as a
  group of keys (Keyhive), so a person is one actor in both stores.

