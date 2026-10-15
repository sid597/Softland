(ns softland.runtime.command
  "Sid's command line: what he does to a running runtime from his machine.
  Not pure: it reads the admin token's file and calls the runtime's admin
  routes over HTTP on localhost.

  From the repository root, while `softland.runtime.main` runs:

    clojure -M:runtime-module -m softland.runtime.command invite [--base see-and-act]
    clojure -M:runtime-module -m softland.runtime.command link <account-number> <person-id>
    clojure -M:runtime-module -m softland.runtime.command block <sender-id> [edn]
    clojure -M:runtime-module -m softland.runtime.command unblock <sender-id>
    clojure -M:runtime-module -m softland.runtime.command limit <kind-or-id> <edn>

  `invite` prints an invitation's link, `http://localhost:8400/#invitation=
  <code>`: the code is in the fragment, which a browser never sends, so it
  does not leave in the Referer of the page's requests to Google
  (`softland.runtime.page`). With `--base see-and-act` the invitation gives a right in the
  base, beneath the system: the first invitation, Sid's own, is made so
  (BUILD.md, default 4: it gives him full rights in the base). `--base
  see` gives seeing, and `--base` with an EDN set gives those key patterns
  (`softland.store.act`). With no `--base` it gives no right: a person gets
  her own layer and sees the base, which is set to everyone (decision 8).

  `link` links a Google account, by its number (the `sub`), to a person
  who has lost every account (BUILD.md, default 3). `block` sets the
  system's block on a sender (an identity's id), the EDN map
  `{:keys :layers :per-minute}`, each part optional; no map blocks
  everything (PROGRESS.md, `:rule/block`). `unblock` ends it. `limit`
  sets the limits of a kind of actor (`person`, `agent`, `program`) or of
  one identity (its id), the EDN map `{:acts-per-minute n
  :bytes-per-minute n}`, either or both; a limit on an id is read before
  its kind's, so a program that writes in bulk, such as the importer, can
  be given its own (`softland.runtime.service/set-limits!`).

  Two options, before or after the words: `--at <url>`, the runtime's
  address (`http://localhost:8400` when left out); `--admin-token-file
  <path>` (`softland.runtime.admin/default-file` when left out). The admin
  token goes only into the request's Authorization header; nothing here
  prints it.

  Inputs and results, by call:

    run    the words of the command, and {:at :admin-token-file} ->
           {:ok text} or {:refuse word …}
    -main  the words -> prints the text, or the refusal; exits 0 or 1

  What each admin route takes and gives is the runtime's service's
  (`softland.runtime.service`, `invite!`, `link-by-admin!`, `block!`,
  `unblock!`, `set-limits!`), reached through `softland.runtime.http`. This
  namespace does not load the service or Rama: it needs only Clojure and
  the JDK.

  State and effects. Reads one file; sends one request; prints. It keeps
  nothing.

  Callers. Sid, at his shell; and the HTTP proof, which makes its
  invitation with `run`."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [softland.runtime.admin :as admin])
  (:import [java.net URI URLEncoder]
           [java.net.http HttpClient HttpRequest HttpRequest$BodyPublishers HttpResponse$BodyHandlers]
           [java.nio.charset StandardCharsets]
           [java.time Duration]))

(def default-at "The runtime's address when no `--at` is given." "http://localhost:8400")

(defn- options
  "Takes the words. Gives [the words that are not options, {:at :admin-token-file
  :base}], or {:refuse :option-has-no-value :at option}."
  [words]
  (loop [ws words, plain [], opts {}]
    (if (empty? ws)
      [plain opts]
      (let [[w v & more] ws]
        (if (contains? #{"--at" "--admin-token-file" "--base"} w)
          (if (nil? v)
            {:refuse :option-has-no-value :at w}
            (recur more plain (assoc opts (keyword (subs w 2)) v)))
          (recur (rest ws) (conj plain w) opts))))))

(defn- post!
  "Takes the runtime's address, the admin token, a route and an EDN map.
  Posts the map as EDN with the admin token as its bearer. Gives the
  runtime's answer read as EDN, or {:refuse :runtime-not-reached :why
  text} when no answer came, or {:refuse :answer-not-edn :status n} when
  the answer is not EDN."
  [at admin-token route body]
  (try
    (let [client (HttpClient/newHttpClient)
          req (-> (HttpRequest/newBuilder (URI/create (str at route)))
                  (.timeout (Duration/ofSeconds 60))
                  (.header "Content-Type" "application/edn")
                  (.header "Authorization" (str "Bearer " admin-token))
                  (.POST (HttpRequest$BodyPublishers/ofString (pr-str body) StandardCharsets/UTF_8))
                  (.build))
          resp (.send client req (HttpResponse$BodyHandlers/ofString StandardCharsets/UTF_8))]
      (try (edn/read-string (.body resp))
           (catch Exception _ {:refuse :answer-not-edn :status (.statusCode resp)})))
    (catch Exception e
      {:refuse :runtime-not-reached :why (str (.getName (class e)) " " (.getMessage e) " at " at)})))

(defn- base-gives
  "Takes the text of `--base`, or nil. Gives the invitation's `:gives`: a
  right in the base allowing `:see`, `:see-and-act`, or an EDN set of key
  patterns; [] for nil. Gives {:refuse :base-not-read} for any other text."
  [s]
  (cond
    (nil? s) []
    (= s "see") [{:layer :base :allows :see}]
    (= s "see-and-act") [{:layer :base :allows :see-and-act}]
    :else (let [v (try (edn/read-string s) (catch Exception _ nil))]
            (if (and (set? v) (every? keyword? v))
              [{:layer :base :allows v}]
              {:refuse :base-not-read :at s}))))

(defn- sender-of
  "Takes a sender's text. Gives its UUID, or the EDN value it reads as
  (a keyword such as `:system`), or nil."
  [s]
  (or (parse-uuid (str s))
      (try (let [v (edn/read-string (str s))] (when (keyword? v) v)) (catch Exception _ nil))))

(defn- entity-of
  "Takes the text that names whose limit is set. Gives the kind `:person`,
  `:agent` or `:program` for its name, with or without a colon; else the
  identity's UUID; else nil."
  [s]
  (let [t (str/replace (str s) #"^:" "")]
    (or (get {"person" :person "agent" :agent "program" :program} t)
        (parse-uuid t))))

(defn- code-of
  "Takes what the invite route gave. Gives the invitation's code: the text
  itself, or the `:code` of a map."
  [ok]
  (if (string? ok) ok (:code ok)))

(defn run
  "Takes the command's words and {:at :admin-token-file}, each optional
  (the options in the words win). Reads the admin token, calls the admin
  route, and gives {:ok text}, the text to print, or {:refuse word …}: the
  runtime's refusal, or this namespace's (`:no-such-command`,
  `:option-has-no-value`, `:base-not-read`, `:sender-not-read`,
  `:person-not-read`, `:entity-not-read`, `:edn-not-read`, and the admin
  token file's words)."
  [words settings]
  (let [o (options words)]
    (if (:refuse o)
      o
      (let [[plain opts] o
            at (str/replace (or (:at opts) (:at settings) default-at) #"/+$" "")
            file (or (:admin-token-file opts) (:admin-token-file settings) admin/default-file)
            [command & args] plain
            token (admin/read-token file)]
        (cond
          (not (contains? #{"invite" "link" "block" "unblock" "limit"} command))
          {:refuse :no-such-command :at command
           :why "invite [--base see|see-and-act|<edn set>] | link <account-number> <person-id> | block <sender-id> [edn] | unblock <sender-id> | limit <kind-or-id> <edn>"}

          (:refuse token) token

          (= command "invite")
          (let [gives (base-gives (:base opts))]
            (if (:refuse gives)
              gives
              (let [r (post! at (:ok token) "/admin/invite" {:gives gives})]
                (if-let [code (and (:ok r) (code-of (:ok r)))]
                  {:ok (str at "/#invitation=" (URLEncoder/encode (str code) StandardCharsets/UTF_8)) :answer r}
                  r))))

          (= command "link")
          (let [[number person] args
                p (parse-uuid (str person))]
            (cond (str/blank? number) {:refuse :no-such-command :why "link <account-number> <person-id>"}
                  (nil? p) {:refuse :person-not-read :at person}
                  :else (let [r (post! at (:ok token) "/admin/link"
                                       {:account {:kind :google :sub number} :person p})]
                          (if (:ok r) {:ok (pr-str (:ok r)) :answer r} r))))

          (= command "block")
          (let [[sender edn-text] args
                s (sender-of sender)
                b (if edn-text (try (edn/read-string edn-text) (catch Exception _ ::no)) {})]
            (cond (nil? s) {:refuse :sender-not-read :at sender}
                  (not (map? b)) {:refuse :edn-not-read :at edn-text}
                  :else (let [r (post! at (:ok token) "/admin/block" {:sender s :block b})]
                          (if (:ok r) {:ok (pr-str (:ok r)) :answer r} r))))

          (= command "limit")
          (let [[who edn-text] args
                e (entity-of who)
                l (try (edn/read-string (str edn-text)) (catch Exception _ ::no))]
            (cond (nil? e) {:refuse :entity-not-read :at who}
                  (not (map? l)) {:refuse :edn-not-read :at edn-text}
                  :else (let [r (post! at (:ok token) "/admin/limit" {:entity e :limits l})]
                          (if (:ok r) {:ok (pr-str (:ok r)) :answer r} r))))

          (= command "unblock")
          (let [s (sender-of (first args))]
            (if (nil? s)
              {:refuse :sender-not-read :at (first args)}
              (let [r (post! at (:ok token) "/admin/unblock" {:sender s})]
                (if (:ok r) {:ok (pr-str (:ok r)) :answer r} r)))))))))

(defn -main
  "Runs one command (the namespace docstring) and exits: 0 when it was done,
  printing its text; 1 when it was refused, printing the refusal."
  [& words]
  (let [r (run words {})]
    (if (:ok r)
      (do (println (:ok r)) (flush) (shutdown-agents) (System/exit 0))
      (do (binding [*out* *err*] (println "refused:" (pr-str (dissoc r :answer))))
          (flush) (shutdown-agents) (System/exit 1)))))
