(ns softland.runtime.attacks.numbers
  "The numbers that the runtime's two new guards need, named as keys of the
  seam's numbers (`softland.runtime.seam/default-numbers` and `:numbers` at
  `softland.runtime.main/start` and `softland.runtime.seam/open`), with the
  defaults that this session takes.

  Each name and each default here is this session's choice, marked for Sid to
  turn; the builder reads these names.

    :sign-in-window-ms
      The window, in milliseconds, over which posts to `/sign-in/*` from one
      remote address are counted. Default: 10000 (ten seconds).

    :max-sign-in-posts-per-address
      The most posts to `/sign-in/*` that one remote address may make in
      `:sign-in-window-ms` before further posts from that address are turned
      away before reaching Google or Auth. Default: 30 (lets up to three
      programs behind one address poll `/sign-in/program/poll` once a second,
      beside a browser sign-in and name confirmation, while holding a flood
      from one address to thirty posts per ten seconds).

    :max-watches-per-root
      The most live watches that one root — a person and every agent beneath
      her, across all of their connections — may hold open at once in one
      copy of the runtime. Default: 256 (matches `line/max-watches` of one
      connection while holding a root's connections and agents in one copy
      well under `read-path`'s 900 pushes per routing id and Rama's 1,000
      proxies per task).

    :max-visitor-watches
      The most live watches that visitors (connections with no credential)
      may hold open together at once in one copy of the runtime. Default: 64.")

(def default-guard-numbers
  "The four guard numbers and the defaults that this session takes, marked
  mine for Sid to turn."
  {:sign-in-window-ms 10000
   :max-sign-in-posts-per-address 30
   :max-watches-per-root 256
   :max-visitor-watches 64})
