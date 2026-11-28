(ns softland.runtime.attacks.yes-page
  "Attack 2: the yes page (`POST /yes`) when one ask is answered twice.

  Tests both remaining falsehoods of `POST /yes`:
  1. A second `yes` that names another actor than the one that stands (an
     agent after `as=me`, or `as=me` after an agent) must not say \"Done\".
  2. A person who says `no` after her own `yes` (as herself) must be told
     that the program is signed in as her, rather than only that it was
     answered before."
  (:require [clojure.string :as str]
            [softland.runtime.attacks.harness :as h]
            [softland.runtime.drive :as drive]
            [softland.wire.credential :as credential]
            [softland.wire.frame :as frame]
            [softland.wire.proof :refer [check finish]]))

(defn- ask-code!
  "Starts a program sign-in ask at `url` and returns its `code`."
  [url asks]
  (let [resp (drive/request :post (str url "/sign-in/program")
                            {:edn {:hash (credential/hash-of (credential/make-secret))
                                   :asking-hash (credential/hash-of (credential/make-secret)) :asks asks}})
        [_ form] (frame/read-one (:body resp))]
    (:code form)))

(defn- post-yes
  "Posts an answer to `POST /yes` and returns `{:status :body}`."
  [url cookie code {:keys [say agent]}]
  (select-keys
   (drive/request :post (str url "/yes")
                  {:cookie cookie
                   :origin url
                   :form (drive/yes-form code {:say say :agent agent})})
   [:status :body]))

(defn- strip-html
  "Strips HTML tags and collapses whitespace for readable failure output."
  [html]
  (-> (str html)
      (str/replace #"<style>[^<]*</style>" " ")
      (str/replace #"<[^>]+>" " ")
      (str/replace #"\s+" " ")
      str/trim))

(defn -main [& _]
  (h/with-cluster
    (fn [{:keys [cluster stand-in]}]
      (let [copy (h/start-copy! {:cluster cluster :stand-in stand-in})]
        (try
          (h/setup-first-person! copy)
          (let [url (:url copy)
                cookie (:cookie (drive/sign-in url (assoc h/first-person :call-her "Sid")))
                ;; 1a. First yes as herself, second yes as an agent.
                code-1 (ask-code! url "prog-me-then-agent")
                first-1 (post-yes url cookie code-1 {:say "yes"})
                second-1 (post-yes url cookie code-1 {:say "yes" :agent "helper"})
                ;; 1b. First yes as an agent, second yes as herself.
                code-2 (ask-code! url "prog-agent-then-me")
                first-2 (post-yes url cookie code-2 {:say "yes" :agent "helper"})
                second-2 (post-yes url cookie code-2 {:say "yes"})
                ;; 2. First yes as herself, then no after her own yes.
                code-3 (ask-code! url "prog-yes-then-no")
                first-3 (post-yes url cookie code-3 {:say "yes"})
                no-after-yes (post-yes url cookie code-3 {:say "no"})
                no-text (str/lower-case (strip-html (:body no-after-yes)))]
            (check "The yes page tells the truth: a second answer that names another actor than the one that stands is not told \"Done\"; a person who says no after her own yes is told that the program is signed in as her."
                   (and (= 200 (:status first-1))
                        (= 200 (:status first-2))
                        (= 200 (:status first-3))
                        (= 409 (:status second-1))
                        (not (str/includes? (:body second-1) "Done"))
                        (= 409 (:status second-2))
                        (not (str/includes? (:body second-2) "Done"))
                        (= 409 (:status no-after-yes))
                        (not (str/includes? (:body no-after-yes) "Done"))
                        (not (str/includes? no-text "not signed in"))
                        (str/includes? no-text "signed in")
                        (str/includes? no-text "you"))
                   {:me-then-agent {:status (:status second-1) :text (strip-html (:body second-1))}
                    :agent-then-me {:status (:status second-2) :text (strip-html (:body second-2))}
                    :no-after-yes  {:status (:status no-after-yes) :text (strip-html (:body no-after-yes))}}))
          (finally
            (h/stop-copy! copy))))))
  (finish))
