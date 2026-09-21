# Deferred from clocks-ids-determinism.md

Kept by the main session 142201 B (70.7%), promoted by Codex (gpt-6-astra, effort high) 42451 B (21.1%), deferred 16402 B (8.2%).
D1: everything in the block also appears in the kept range named. D2: bibliographic entry or heading only.

| lines | bytes | tag | see | subject | first words |
|---|---|---|---|---|---|
| L136-137 | 38 | D2 | - | Questions heading | ### 4. Which questions they speak to |
| L154-154 | 49 | D1 | 50-50,831-831 | Upgrade recorded as a log operation | - The upgrade is itself an operation in the log. |
| L234-235 | 38 | D2 | - | Questions heading | ### 4. Which questions they speak to |
| L236-236 | 106 | D1 | 77-77 | Strongest challenge to unstored running answers | **The premise: running answers are re-derivable, so never stored.** This is where my camp pushes har |
| L238-238 | 101 | D1 | 200-200,889-889 | Recorded outputs reveal replay divergence | - REPORTED: they can *see* the break only because the history holds the old outputs to compare with. |
| L239-239 | 221 | D1 | 77-77,218-226 | Replay remedies depend on histories ending | - REPORTED: every remedy they found leans on histories ending. Old branches are deleted when old run |
| L241-241 | 258 | D1 | 200-200,889-892 | Unstored answers conceal changed results after runtime rebuilds | - INFERRED: because a running answer is never stored, there is nothing to compare a later re-derivat |
| L242-243 | 505 | D1 | 77-77,198-198,839-839,889-892 | Named replay pair, crossing digests, content, and exact model inputs | - INFERRED: so the honest reading is that *re-derivable* is a claim *relative to a named tool versio |
| L244-244 | 63 | D2 | - | Runtime version and rebuilds heading | **(12) Runtime version at session start; are rebuilds facts?** |
| L245-245 | 177 | D1 | 202-202,224-224,831-831 | Build identity recorded per work unit | - REPORTED: they write the build into the history, and per unit of work, not per session (binary che |
| L246-246 | 138 | D1 | 833-833,1051-1052 | Deploy-spanning sessions require crossing and verdict stamps | - INFERRED: per session is too coarse if a session can outlive a deploy. The unit that needs the sta |
| L251-252 | 220 | D1 | 109-109,199-199,763-764,893-893 | Recorded time as an input to deterministic answers | **(11) When; time as an input.** REPORTED: replayed code never reads the machine clock; time comes f |
| L331-332 | 38 | D2 | - | Questions heading | ### 4. Which questions they speak to |
| L337-337 | 96 | D2 | - | Visibility before permissions and lagging policy heading | **(16) Who sees a fact before any permissions exist? Visibility under a lagging policy index.** |
| L338-338 | 94 | D1 | 297-301,768-768 | Visibility snapshot no older than displayed content | - REPORTED: visibility must be evaluated at a snapshot no older than the content being shown. |
| L340-340 | 403 | D1 | 827-827,1091-1091 | Recorded policy positions expose admission under withdrawn rules | - INFERRED: the same holds for the gate. A gate that admits a write under a policy read from a laggi |
| L343-343 | 62 | D2 | - | Policies as facts heading | **Policies as facts in the same store (the ALSO OPEN list).** |
| L344-344 | 105 | D1 | 309-309 | Space and bulk-change reasons against per-object rule records | - REPORTED against: Zanzibar chose not to make rules-about-rules records, for space and for bulk cha |
| L345-345 | 170 | D1 | 323-323 | External policy needs separate snapshot freshness discipline | - REPORTED for: a policy that lives outside the log has its own way of going stale (3e). Zanzibar ne |
| L409-410 | 38 | D2 | - | Questions heading | ### 4. Which questions they speak to |
| L411-411 | 58 | D2 | - | Pointer identity heading | **(1) Pointer: the gate's number, or the fact's own id?** |
| L413-413 | 226 | D1 | 396-399,812-812 | Incarnation prefixes survive moves without restamping old records | - REPORTED: a store-assigned number is not comparable across clusters, moves, or restores. It needs  |
| L418-418 | 36 | D2 | - | Order heading | **(10) Order; one number or many?** |
| L425-425 | 55 | D2 | - | Read dependencies and paths heading | **(11b) and (4) Recording reads; dependency or path?** |
| L428-428 | 104 | D1 | 403-403 | Manual dependency marking causes hard-to-find bugs | - REPORTED: letting each caller mark this by hand caused bugs that were "very hard to find" (3e abov |
| L429-430 | 277 | D1 | 793-793 | Tool-defined dependency marks prevent misleading staleness | - INFERRED for Sid: yes, mark each read as depends-on or only-looked. Without the mark, every glance |
| L486-487 | 38 | D2 | - | Questions heading | ### 4. Which questions they speak to |
| L564-565 | 38 | D2 | - | Questions heading | ### 4. Which questions they speak to |
| L566-567 | 331 | D1 | 528-528,548-548,558-558,759-760 | Store clocks, hardware-backed ordering, and production clock skew | **(11) When: whose clock, and is it ever used for order?** INSTITUTIONAL: the store's clock, never t |
| L568-568 | 29 | D2 | - | Order and as-of heading | **(10) Order, and *as of*.** |
| L644-645 | 38 | D2 | - | Questions heading | ### 4. Which questions they speak to |
| L667-667 | 99 | D2 | - | Checking conventions before building heading | **How to check Sid's conventions before building on them.** INFERRED, put together from the above: |
| L671-672 | 134 | D1 | 616-618,622-622 | Real fault tests complement models and rare-schedule checking | 4. Test the real thing under faults. The model cannot see the implementation, and the implementation |
| L922-923 | 14 | D2 | - | Sources heading | # 7. Sources |
| L928-928 | 16 | D2 | - | TigerBeetle sources heading | ### TigerBeetle |
| L929-929 | 348 | D2 | - | TigerBeetle documentation | - Docs: Data Modeling, Time, Reliable Transaction Submission, System Architecture, Correcting Transf |
| L930-930 | 295 | D2 | - | TigerBeetle repository source paths | - Repository, main branch, https://github.com/tigerbeetle/tigerbeetle : docs/ARCHITECTURE.md; docs/T |
| L931-931 | 135 | D2 | - | Greef clock article | - Joran Dirk Greef, "Three Clocks are Better than One", 2021. https://tigerbeetle.com/blog/2021-08-3 |
| L932-932 | 114 | D2 | - | Daly bitemporality article | - Lewis Daly, "One for the Treble, Two for the Time", 2026. https://tigerbeetle.com/blog/2026-01-14- |
| L933-933 | 103 | D2 | - | Matklad clockless-time article | - matklad, "Tracking Time Without Clock", 2025. https://tigerbeetle.com/blog/2025-10-21-clockless-ti |
| L934-934 | 113 | D2 | - | Fuzzer blind spots article | - "Fuzzer Blind Spots Meet Jepsen", 2025. https://tigerbeetle.com/blog/2025-06-06-fuzzer-blind-spots |
| L935-935 | 95 | D2 | - | Greef Changelog interview transcript | - Joran Dirk Greef on The Changelog #635, 2025 (transcript). https://changelog.com/podcast/635 |
| L936-936 | 157 | D2 | - | Greef QCon talk transcript | - Joran Dirk Greef, "A New Era for Database Design with TigerBeetle", QCon talk recorded 2023 (trans |
| L937-938 | 103 | D2 | - | Kingsbury TigerBeetle analysis | - Kyle Kingsbury, "Jepsen: TigerBeetle 0.16.11", 2025. https://jepsen.io/analyses/tigerbeetle-0.16.1 |
| L939-939 | 25 | D2 | - | Temporal and Restate sources heading | ### Temporal and Restate |
| L940-940 | 361 | D2 | - | Temporal documentation topics | - Temporal docs, https://docs.temporal.io/ : Workflow Definition (deterministic constraints); Events |
| L941-941 | 177 | D2 | - | Temporal Worker Versioning deployment article | - Temporal blog, "Safe deployments with Temporal Worker Versioning on Kubernetes", 2026. https://tem |
| L942-942 | 161 | D2 | - | Wang summary of Fateev talk | - Shawn Wang, "Designing a Workflow engine from first principles" (summary of a talk by Maxim Fateev |
| L943-943 | 189 | D2 | - | Fateev SE Radio transcript | - SE Radio 596, "Maxim Fateev on Durable Execution with Temporal", 2023 (transcript). https://se-rad |
| L944-944 | 93 | D2 | - | Ewen Restate origin article | - Stephan Ewen, "Why we built Restate", 2023. https://restate.dev/blog/why-we-built-restate/ |
| L945-945 | 148 | D2 | - | Kleeman immutability article | - Jack Kleeman, "Solving durable execution's immutability problem", 2024. https://restate.dev/blog/s |
| L946-946 | 212 | D2 | - | Ewen and Kleeman log article | - Stephan Ewen and Jack Kleeman, "Every System is a Log: Avoiding coordination in distributed applic |
| L947-947 | 210 | D2 | - | Durable execution engine article | - Stephan Ewen, Ahmed Farghal, Till Rohrmann, "Building a modern Durable Execution Engine from First |
| L948-948 | 167 | D2 | - | Agent versioning article | - Giselle van Dongen and Francesco Guardiani, "Updating AI Agents safely in production", 2026. https |
| L949-949 | 176 | D2 | - | Van Dongen checkpointing article | - Giselle van Dongen, "Agent checkpointing is far from production-grade resiliency", 2026. https://r |
| L950-951 | 158 | D2 | - | Restate versioning and retention documentation | - Restate docs: Versioning, https://docs.restate.dev/operate/versioning ; service configuration (ret |
| L952-952 | 32 | D2 | - | Zanzibar sources heading | ### Zanzibar and its rebuilders |
| L953-953 | 166 | D2 | - | Zanzibar paper | - Ruoming Pang and thirteen others, "Zanzibar: Google's Consistent, Global Authorization System", US |
| L954-954 | 105 | D2 | - | Kissner interview | - "Developer Den with Lea Kissner", Oso, 2021. https://www.osohq.com/post/developer-den-with-lea-kis |
| L955-955 | 161 | D2 | - | Moshenko permissions-ordering article | - Jake Moshenko, "Enforcing Causal Ordering in Distributed Systems: The Importance of Permissions Ch |
| L956-956 | 147 | D2 | - | Cordell Spanner and CockroachDB article | - Evan Cordell, "The One Crucial Difference Between Spanner and CockroachDB", AuthZed, 2021. https:/ |
| L957-957 | 123 | D2 | - | Zelinskie consistency-token article | - Jimmy Zelinskie, "Zed Tokens, Zookies, Consistency for Authorization", AuthZed, 2023. https://auth |
| L958-958 | 151 | D2 | - | AuthZed Zanzibar annotations | - AuthZed's annotations on the paper (source file). https://raw.githubusercontent.com/authzed/zanzib |
| L959-959 | 83 | D2 | - | SpiceDB consistency documentation | - SpiceDB docs, Consistency. https://authzed.com/docs/spicedb/concepts/consistency |
| L960-960 | 164 | D2 | - | Yao Himeji article and archive provenance | - Alan Yao, "Himeji: a scalable centralized system for authorization at Airbnb", 2021 (read via web. |
| L961-962 | 116 | D2 | - | OpenFGA consistency documentation | - OpenFGA docs, Query Consistency Modes (revised September 2026). https://openfga.dev/docs/interacti |
| L963-963 | 38 | D2 | - | FoundationDB and Record Layer sources heading | ### FoundationDB and the Record Layer |
| L964-964 | 164 | D2 | - | FoundationDB paper | - Jingyu Zhou and many others, "FoundationDB: A Distributed Unbundled Transactional Key Value Store" |
| L965-965 | 178 | D2 | - | Record Layer paper | - Christos Chrysafis and others (Apple), "FoundationDB Record Layer: A Multi-Tenant Structured Datas |
| L966-966 | 110 | D2 | - | Record Layer schema-evolution documentation | - Record Layer docs, "Schema evolution". https://foundationdb.github.io/fdb-record-layer/SchemaEvolu |
| L967-967 | 171 | D2 | - | FoundationDB documentation topics | - FoundationDB docs, https://apple.github.io/foundationdb/ : Developer Guide; Python API; Automatic  |
| L968-968 | 123 | D2 | - | Idempotency identifiers design document | - Design document for idempotency ids. https://raw.githubusercontent.com/apple/foundationdb/main/des |
| L969-969 | 243 | D2 | - | Versionstamp forum threads and GitHub issue | - FoundationDB forum threads: "Versionstamp uniqueness and monotonicity" (602), "Versionstamp vs com |
| L970-970 | 116 | D2 | - | Wilson Antithesis essay | - Will Wilson, "Is something bugging you?", Antithesis, 2024. https://antithesis.com/blog/is_somethi |
| L971-972 | 200 | D2 | - | Third-party notes on Wilson's talk | - Notes by a third party on Will Wilson's Strange Loop 2014 talk (a paraphrase, not Wilson's words). |
| L973-973 | 59 | D2 | - | Calvin and related sources heading | ### Calvin, Abadi, FaunaDB, and the reply from CockroachDB |
| L974-974 | 243 | D2 | - | Calvin paper | - Alexander Thomson, Thaddeus Diamond, Shu-Chun Weng, Kun Ren, Philip Shao, Daniel Abadi, "Calvin: F |
| L975-975 | 156 | D2 | - | Thomson and Abadi determinism paper | - Alexander Thomson and Daniel Abadi, "The Case for Determinism in Database Systems", VLDB 2010. htt |
| L976-976 | 149 | D2 | - | Abadi and Faleiro overview | - Daniel Abadi and Jose Faleiro, "An Overview of Deterministic Database Systems", CACM 2018. https:/ |
| L977-977 | 396 | D2 | - | Abadi blog bibliography | - Daniel Abadi's blog, https://dbmsmusings.blogspot.com/ : "Distributed consistency at scale: Spanne |
| L978-978 | 311 | D2 | - | FaunaDB protocol article and authorship metadata | - Daniel Abadi and Matt Freels, "Consistency without Clocks: The FaunaDB Distributed Transaction Pro |
| L979-979 | 90 | D2 | - | Kingsbury FaunaDB analysis | - Kyle Kingsbury, "Jepsen: FaunaDB 2.5.4", 2019. https://jepsen.io/analyses/faunadb-2.5.4 |
| L980-980 | 69 | D2 | - | Fauna closure article | - The Fauna Team, "The Future of Fauna", 2025 (via web.archive.org). |
| L981-981 | 149 | D2 | - | Kimball and Sharif clock article | - Spencer Kimball and Irfan Sharif, "Living without atomic clocks", Cockroach Labs. https://www.cock |
| L982-983 | 112 | D2 | - | Matei consistency-model article | - Andrei Matei, "CockroachDB's consistency model", 2019. https://www.cockroachlabs.com/blog/consiste |
| L984-984 | 21 | D2 | - | Google lineage sources heading | ### Google's lineage |
| L985-985 | 192 | D2 | - | Bigtable paper | - Fay Chang and others, "Bigtable: A Distributed Storage System for Structured Data", OSDI 2006. htt |
| L986-986 | 187 | D2 | - | Chubby paper | - Mike Burrows, "The Chubby Lock Service for Loosely-Coupled Distributed Systems", OSDI 2006. https: |
| L987-987 | 179 | D2 | - | Megastore paper | - Jason Baker and others, "Megastore: Providing Scalable, Highly Available Storage for Interactive S |
| L988-988 | 186 | D2 | - | Spanner paper | - James Corbett and others, "Spanner: Google's Globally-Distributed Database", OSDI 2012. https://st |
| L989-989 | 172 | D2 | - | F1 paper | - Jeff Shute and others, "F1: A Distributed SQL Database That Scales", VLDB 2013. https://static.goo |
| L990-990 | 215 | D2 | - | F1 schema-change paper | - Ian Rae, Eric Rollins, Jeff Shute, Sukhdeep Sodhi, Radek Vingralek, "Online, Asynchronous Schema C |
| L991-991 | 150 | D2 | - | Brewer TrueTime and CAP article | - Eric Brewer, "Spanner, TrueTime & The CAP Theorem", 2017. https://static.googleusercontent.com/med |
| L992-992 | 163 | D2 | - | Spanner SQL evolution paper | - David Bacon and others, "Spanner: Becoming a SQL System", SIGMOD 2017. https://static.googleuserco |
| L993-993 | 224 | D2 | - | Firestore paper | - Ram Kesavan, David Gay, Daniel Thevessen, Jimit Shah, C. Mohan, "Firestore: The NoSQL Serverless D |
| L994-994 | 209 | D2 | - | Cloud Spanner documentation topics | - Cloud Spanner docs: commit timestamps, schema design, TrueTime and external consistency, timestamp |
| L995-995 | 86 | D2 | - | Firestore best practices | - Firestore best practices. https://firebase.google.com/docs/firestore/best-practices |
| L996-996 | 208 | D2 | - | Datastore consistency article | - "Balancing Strong and Eventual Consistency with Datastore", Google Cloud article. https://docs.clo |
| L997-997 | 83 | D2 | - | Google Cloud deletion documentation | - "Data deletion on Google Cloud". https://cloud.google.com/docs/security/deletion |
| L998-998 | 113 | D2 | - | Google infrastructure security overview | - "Google infrastructure security design overview". https://cloud.google.com/docs/security/infrastru |
| L999-1000 | 76 | D2 | - | Cloud Audit Logs overview | - "Cloud Audit Logs overview". https://cloud.google.com/logging/docs/audit |
| L1001-1001 | 13 | D2 | - | Checking sources heading | ### Checking |
| L1002-1002 | 111 | D2 | - | Kingsbury timestamp article | - Kyle Kingsbury, "The trouble with timestamps", 2013. https://aphyr.com/posts/299-the-trouble-with- |
| L1003-1003 | 54 | D2 | - | Jepsen research ethics | - Jepsen, "Research Ethics". https://jepsen.io/ethics |
| L1004-1004 | 96 | D2 | - | Jepsen tutorial chapters | - Jepsen tutorial, chapters 3 and 6. https://github.com/jepsen-io/jepsen/tree/main/doc/tutorial |
| L1005-1005 | 283 | D2 | - | Jepsen database-analysis bibliography | - Kyle Kingsbury, Jepsen analyses, https://jepsen.io/analyses : Datomic Pro 1.0.7075 (2024); Cockroa |
| L1006-1006 | 149 | D2 | - | Elle paper | - Kyle Kingsbury and Peter Alvaro, "Elle: Inferring Isolation Anomalies from Experimental Observatio |
| L1007-1007 | 251 | D2 | - | AWS formal-methods report | - Chris Newcombe, Tim Rath, Fan Zhang, Bogdan Munteanu, Marc Brooker, Michael Deardeuff, "Use of For |
| L1008-1008 | 174 | D2 | - | AWS correctness-practices article | - Marc Brooker and Ankush Desai, "Systems Correctness Practices at AWS", ACM Queue, 2025 (read via w |
| L1009-1010 | 192 | D2 | - | S3 lightweight formal-methods paper | - James Bornholt and others, "Using Lightweight Formal Methods to Validate a Key-Value Storage Node  |
| L1011-1011 | 8 | D2 | - | Identifiers sources heading | ### Ids |
| L1012-1012 | 141 | D2 | - | RFC 9562 | - K. Davis, B. Peabody, P. Leach, RFC 9562, "Universally Unique IDentifiers (UUIDs)", IETF, 2024. ht |
| L1013-1014 | 188 | D2 | - | Snowflake announcement and project README | - Ryan King, "Announcing Snowflake", Twitter engineering blog, 1 June 2010 (via web.archive.org), an |
