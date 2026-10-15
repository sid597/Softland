(ns softland.runtime.admin
  "The admin token: the one secret between Sid's command line and the
  runtime's admin routes. Not pure: it writes and reads a file.

  Sid's command line reaches the runtime by an admin route whose token is in
  a file only his user can read (PROGRESS.md, \"Part 2\"). The runtime makes
  a new token at each start, from random bytes its caller hands in, and
  writes it to `default-file`, `~/.config/softland/runtime-admin-token`,
  with the permissions 0600; the folder is made when missing, with 0700. The
  command line reads the file and sends the token as `Authorization: Bearer
  <token>`; the runtime compares it in constant time. The token is never
  printed or logged, and it is not in the store: the store keeps no secret
  (BUILD.md, default 6), and the token lives as long as the process does:
  the file is written anew at each start, and removed when the runtime
  stops (`remove!`), so a token of an earlier start stops working with its
  process and does not stay on the disk.

  The text is `sla.<secret>`, the secret 32 random bytes in base64url with
  no padding. The prefix, the file's place and the permissions are a
  session's choices (the build's builder F), Sid's to change.

  Inputs and results, by call:

    make-token  32 random bytes -> the text, or nil for any other count
    write!      a path, the text -> {:ok path} or {:refuse word :why text}
    read-token  a path -> {:ok text} or {:refuse word :why text}
    remove!     a path, the text -> {:ok :removed}, {:ok :not-ours}, or
                {:refuse word :why text}
    matches?    a text given, the text kept -> true or false

  State and effects. `write!` makes a folder when missing and replaces one
  file; `read-token` reads one; `remove!` removes it. Nothing else here
  touches the disk. Nothing here reads a clock or takes a random source.

  Callers. `softland.runtime.main`, which makes and writes the token at its
  start and hands it to `softland.runtime.http`; `softland.runtime.http`,
  which asks `matches?` of each admin request; `softland.runtime.command`,
  which reads it; `softland.runtime.main`'s stop, which removes it."
  (:require [clojure.string :as str])
  (:import [java.nio ByteBuffer]
           [java.nio.charset StandardCharsets]
           [java.nio.file Files LinkOption Path Paths StandardOpenOption]
           [java.nio.file.attribute FileAttribute PosixFilePermissions]
           [java.security MessageDigest]
           [java.util Base64]))

(def default-file
  "The path of the admin token's file when no other is given:
  `~/.config/softland/runtime-admin-token`, under the user's home."
  (str (System/getProperty "user.home") "/.config/softland/runtime-admin-token"))

(def ^:private prefix "The admin token's prefix." "sla.")

(defn make-token
  "Takes 32 random bytes. Gives the admin token's text, `sla.` and the
  bytes in base64url with no padding; nil for a count of bytes that is not
  32 or a thing that is no byte array."
  [bs]
  (when (and (bytes? bs) (= 32 (alength ^bytes bs)))
    (str prefix (.encodeToString (.withoutPadding (Base64/getUrlEncoder)) ^bytes bs))))

(defn- token? [x]
  (and (string? x) (str/starts-with? x prefix) (= (+ (count prefix) 43) (count x))))

(defn- path-of ^Path [p] (Paths/get (str p) (make-array String 0)))

(defn write!
  "Takes a path and the token's text. Makes the folder when it is missing,
  with the permissions 0700, and removes a file that is there. Then makes
  the file anew, with the permissions 0600 and `CREATE_NEW`, and writes the
  text through the channel that made it: `CREATE_NEW` makes the file only
  where nothing is, a link among them, so a link put there between the
  removal and the making is refused and never followed, and the token is
  never in a file that others can read. Gives {:ok path}, or {:refuse
  :admin-token-not-written :why text} when the file system says no (the
  text names the fault, never the token). A folder that is there keeps its
  own permissions."
  [path text]
  (try
    (let [p (path-of path)
          dir (.getParent (.toAbsolutePath p))]
      (when-not (Files/isDirectory dir (make-array LinkOption 0))
        (Files/createDirectories dir (into-array FileAttribute
                                                 [(PosixFilePermissions/asFileAttribute
                                                   (PosixFilePermissions/fromString "rwx------"))])))
      (Files/deleteIfExists p)
      (with-open [ch (Files/newByteChannel
                      p
                      #{StandardOpenOption/CREATE_NEW StandardOpenOption/WRITE}
                      (into-array FileAttribute
                                  [(PosixFilePermissions/asFileAttribute
                                    (PosixFilePermissions/fromString "rw-------"))]))]
        (let [buf (ByteBuffer/wrap (.getBytes (str text) StandardCharsets/UTF_8))]
          (while (.hasRemaining buf) (.write ch buf))))
      {:ok (str p)})
    (catch Exception e
      {:refuse :admin-token-not-written :why (str (class e) " at " path)})))

(defn read-token
  "Takes a path. Gives {:ok text} with the token read from the file, its
  white space trimmed; {:refuse :admin-token-file-missing} when there is no
  file; {:refuse :admin-token-file-not-a-token} when its text is not an
  admin token; {:refuse :admin-token-file-unreadable :why text} when it
  cannot be read."
  [path]
  (try
    (let [p (path-of path)]
      (if-not (Files/exists p (make-array LinkOption 0))
        {:refuse :admin-token-file-missing :why (str path)}
        (let [text (str/trim (String. (Files/readAllBytes p) StandardCharsets/UTF_8))]
          (if (token? text)
            {:ok text}
            {:refuse :admin-token-file-not-a-token :why (str path)}))))
    (catch Exception e
      {:refuse :admin-token-file-unreadable :why (str (class e) " at " path)})))

(defn matches?
  "Takes the text a request gave and the text kept. True when both are
  admin tokens and equal, compared with `MessageDigest/isEqual`, which
  takes the same time wherever two texts of one length differ. False for
  nil or anything that is no token."
  [given kept]
  (boolean
   (and (token? given) (token? kept)
        (MessageDigest/isEqual (.getBytes ^String given StandardCharsets/UTF_8)
                               (.getBytes ^String kept StandardCharsets/UTF_8)))))

(defn remove!
  "Takes a path and the token's text that this runtime wrote there.
  Removes the file when it still holds that token, and gives {:ok
  :removed}; leaves it when it holds another, a later start's, or is gone,
  and gives {:ok :not-ours}; gives {:refuse :admin-token-not-removed :why
  text} when the file system says no. Called when the runtime stops."
  [path text]
  (try
    (let [r (read-token path)]
      (if (and (:ok r) (matches? (:ok r) text))
        (do (Files/deleteIfExists (path-of path)) {:ok :removed})
        {:ok :not-ours}))
    (catch Exception e
      {:refuse :admin-token-not-removed :why (str (class e) " at " path)})))
