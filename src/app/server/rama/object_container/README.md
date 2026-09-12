# Object-container support — foreign access and shared identity

This child folder supports the modules defined in
[object_container.clj](../object_container.clj). It owns no depot or PState.
Return to the [Rama map](../README.md) for the common store's relationship to
relations, transcript operations, and trail queries.

| File | Role |
|---|---|
| [runtime.clj](runtime.clj) | Acquires an IPC and foreign handles for focused use; wraps request appends, decision polling, and material reads for either IPC or durable-cluster handle maps. Also contains optional block-edit file logging and explicit replay. |
| [transcript_identity.clj](transcript_identity.clj) | Derives common conversation, message, tool, source, physical-line, and file-generation identities from supplied values. Performs no file reads or state writes. |

## Handles and lifecycle

`start-object-container-runtime!` creates one IPC, launches the common material
module, then launches transcript operations, whose mirror depends on the common
module. It returns depot, PState, and query handles plus `:ipc`.
`close-object-container-runtime!` closes that IPC when present and suppresses
close errors. The caller is responsible for stopping any acquisition/watch
resources using those handles first.

[door/cluster.clj](../../door/cluster.clj) builds equivalent foreign access to
already deployed modules, without an `:ipc` or local edit-log path. Manager
lifetime belongs there. The map entries here are process handles into stored
state, not the stored state itself. Different keys intentionally address either
the common module or the transcript-operations module.

## Requests and reads

The ordinary object request and file-state append wrappers default to
`:append-ack`; the control and block-edit wrappers default to `:ack`.
For these stream modules, `:ack` waits for the colocated stream topologies to
process that append, including replication of updated PStates. See the
[Rama depot acknowledgement rules](https://redplanetlabs.com/docs/~/depots.html#_appends).
Neither the returned acknowledgement nor the submitted request is
itself an accepted decision. `read-decision` returns either accepted or rejected
rows, and `await-object-container-decision` polls until a row is visible or
returns nil on timeout. It does not poll downstream source-line completion or
operational resume indexes.

Point readers return one row or nil. Collection readers preserve the PState's
index keys/order where applicable; paged wrappers use inclusive order-key cursors
and the common default page size. `read-common-material-for-source` resolves
source-index references into the requested material categories through a query.
`read-unit` follows a stored graduation to authored content, while
`read-current-revision` follows the current container pointer. These calls read
current state and do not share a snapshot across multiple invocations.

The two source-line readers refer to different facts:
`read-transcript-source-lines` reads completions owned by the common module;
`read-transcript-file-source-lines` reads observations owned by operations.
`read-transcript-file-offset` reads the resulting operational resume state.

## Optional file replay and identity

When a runtime supplies `:block-edit-log-path`, the edit wrapper first appends
one UTF-8 EDN line, converting records into maps, then appends to the depot.
Missing/nil path disables this logging. The writer uses a process-local lock,
creates parent directories, and closes the file; it does not fsync or atomically
couple the two writes. `replay-block-edit-log!` reads the configured or default
path and appends only edit requests, never writing them back to the file. Its
replayed count includes already-decided or rejected requests when their append
succeeds. Per-line failures are skipped and counted.

`transcript_identity.clj` constructs the common `chat:*` object keys and
`oc:chat-*`/`oc:tool-*` container IDs consumed by adapters and the kernel's routing
logic. These differ from the standalone `transcript_ingest.clj` store's `tc:*`
IDs. A physical source-line key combines source/file identity, byte offset, and
line hash. A generation key prefers explicit metadata; its fallback hashes file
identity and path, so it is not itself a freshness check on file content.
