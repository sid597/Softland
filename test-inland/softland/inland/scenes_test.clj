(ns softland.inland.scenes-test
  "The pointer's scenes on the server side, against the rig's module on a fresh
   in-process cluster: two pages driven through the adapter as the browser drives
   them (gestures with the page's local cells, and watched reads), with no browser.
   Scene one: a person changes the pointer's rule while using it, and what stood
   on the old rule (a selection, a pin, a kept variation) is marked, found by the
   store's lookup from a fact to what stood on it. Scene two: two
   people on shared material, the base switching to shared at the first group, a
   member accepted after the group is made, a rule promoted into the group layer.
   Scene three: a note naming a person, that person forgotten, the note erased on
   its date for everyone who shows it. Every value checked here reached the page
   through the one read exit and a push. The material is the citation session's;
   without it the subject stands for a line of no file."
  (:require [clojure.test :refer [deftest is testing]]
            [com.rpl.rama.test :as rtest]
            [missionary.core :as m]
            [rig.store.module :as module]
            [softland.inland.store :as store]))

(defn- say [& xs] (apply println "OBSERVED" xs) (flush))

(defn- wait-until
  ([f] (wait-until f 30000))
  ([f ms]
   (let [deadline (+ (System/currentTimeMillis) ms)]
     (loop [] (let [v (try (f) (catch Throwable _ nil))]
                (if (or v (> (System/currentTimeMillis) deadline)) v (do (Thread/sleep 50) (recur))))))))

(defn- watch
  "An Inland read address watched through the adapter, as execution/ReadStatus
   does. Returns an atom of the values seen; the flow is stopped at the end."
  [stops owner kind path]
  (let [seen (atom [])
        cancel ((m/reduce (fn [_ v] (swap! seen conj v)) nil (store/watch-path owner kind path))
                (fn [_]) (fn [_]))]
    (swap! stops conj cancel)
    seen))

(defn- value [seen] (some->> @seen (filter #(= :value (:status %))) last :value))

(defn- gesture! [owner kind cells & {:as extra}]
  (store/gesture! owner (merge {:kind kind :id (str "e-" (random-uuid))} extra) (merge {"active" "pointer"} cells)))

(defn- point! [owner line]
  (gesture! owner :point {} :material true :subject {:file "file/none" :line line}))

(defn- accepted? [g] (and (nil? (:failure g)) (every? #(= :accepted (:status %)) (:decisions g))))

(declare scenes)

(defn -main
  "`bin/inland check`: the scenes, then exit nonzero on a failure."
  [& _]
  (let [r (clojure.test/run-tests 'softland.inland.scenes-test)]
    (shutdown-agents)
    (System/exit (if (pos? (+ (:fail r) (:error r))) 1 0))))

(deftest scenes
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc module/Store {:tasks 2 :threads 2 :workers 1})
    (store/connect-to! ipc)
    (let [stops (atom [])
          a (store/page-open! "page-a" :alice)
          b (store/page-open! "page-b" :bob)
          cell (fn [page info k] (watch stops page :rows ["workbench" (str (:hand info) "/" (:thing info)) (keyword "cell" k)]))
          a-sel (cell "page-a" a "selection")
          b-sel (cell "page-b" b "selection")
          marks (atom [])
          _ (swap! stops conj ((m/reduce (fn [_ v] (swap! marks conj v)) nil (store/marks-flow "page-a"))
                               (fn [_]) (fn [_])))
          ;; no view here reads a rule: the page holds live what its acts stood on
          marked (fn [] (store/marks-view "page-a" (or (last @marks) {})))]
      (try
        (testing "scene one, server side: alice changes her pointer's rule while using it; what stood on the old rule is marked"
          (do
            (is (accepted? (point! "page-a" 7)))
            (is (wait-until #(= 7 (:from (value a-sel)))) (pr-str (last @a-sel)))
            (is (accepted? (gesture! "page-a" :pin {"inspecting" "targeting"})) "alice pins the rule")
            (is (accepted? (gesture! "page-a" :keep {"variation" {:value "my-pointer"}})) "alice keeps a second instrument")
            (let [base (store/read-one :rows ["workbench" "base/targeting"])
                  draft (pr-str {:label "Select the line after the one I point at"
                                 :body {:steps [] :return {:file [:get :subject :file] :from [:+ 1 [:get :subject :line]]
                                                           :to [:+ 1 [:get :subject :line]] :kind "line" :label "the next line"}}})
                  g (gesture! "page-a" :submit {"inspecting" "targeting" "draft" {:value draft :base (:revision base)}})]
              (is (accepted? g) (pr-str g))
              (is (wait-until #(let [v (marked)]
                                 (and (get-in v [:cells "selection"]) (get-in v [:cells "context"])
                                      (get-in v [:things "my-pointer-targeting"]))))
                  (str "the selection, the pin and the kept variation are marked, found by the store's lookup: " (pr-str (marked))))
              (say "scene one: marks" (marked))
              (is (accepted? (gesture! "page-a" :live {})) "alice lets go of the pin")
              (is (wait-until #(nil? (get-in (marked) [:cells "context"]))) (str "the pin's mark went with the pin: " (pr-str (marked))))
              (is (accepted? (point! "page-a" 7)))
              (is (wait-until #(= 8 (:from (value a-sel)))) "the next point chose by the new rule")
              (is (wait-until #(nil? (get-in (marked) [:cells "selection"]))) (str "a selection made again is not marked: " (pr-str (marked))))
              (is (get-in (marked) [:things "my-pointer-targeting"]) "the kept variation stays marked: its copy was kept from the old rule")
              (say "scene one: alice's selection" (value a-sel)))))

        (testing "a new tool by writing data alone: a new rule as a record, the pointer pointed at it, no code"
          (let [rule (pr-str {:label "Select the line and the one after it"
                              :body {:steps [] :return {:file [:get :subject :file] :from [:get :subject :line]
                                                        :to [:+ 1 [:get :subject :line]] :kind "lines"
                                                        :label [:str "lines " [:get :subject :line] "–" [:+ 1 [:get :subject :line]]]}}})
                made (gesture! "page-a" :create-record {"variation" {:value "two-lines"} "draft" {:value rule}})
                pointer (store/read-one :rows ["workbench" "base/pointer"])
                draft (pr-str (-> pointer (dissoc :revision :resolved-layer :basis :erased) (assoc :targeting "two-lines")))
                pointed (gesture! "page-a" :submit {"inspecting" "pointer" "draft" {:value draft :base (:revision pointer)}})]
            (is (accepted? made) (str "the rule written through the editor as a record: " (pr-str made)))
            (is (accepted? pointed) (str "the pointer's :targeting now names it: " (pr-str pointed)))
            (is (accepted? (point! "page-a" 7)))
            (is (wait-until #(and (= 7 (:from (value a-sel))) (= 8 (:to (value a-sel)))))
                (str "the next point chose by the new rule: " (pr-str (value a-sel))))
            (say "a new tool, as data: alice's selection" (value a-sel))
            ;; the new rule edited in its own layer: the edit replaces its facts there, and the
            ;; selection that stood on them is marked
            (let [two (store/read-one :rows ["workbench" "alice/two-lines"])
                  three (pr-str {:label "Select the line and the two after it"
                                 :body {:steps [] :return {:file [:get :subject :file] :from [:get :subject :line]
                                                           :to [:+ 2 [:get :subject :line]] :kind "lines"
                                                           :label [:str "lines " [:get :subject :line] "–" [:+ 2 [:get :subject :line]]]}}})
                  g (gesture! "page-a" :submit {"inspecting" "two-lines" "draft" {:value three :base (:revision two)}})]
              (is (accepted? g) (pr-str g))
              (is (wait-until #(= "two-lines" (get-in (marked) [:cells "selection" :on-thing])))
                  (str "a same-layer edit marks the selection that stood on the facts it replaced: " (pr-str (marked))))
              (say "a same-layer edit: the selection's mark" (get-in (marked) [:cells "selection"])))
            ;; back to the rule the scenes go on with, the same way: data
            (let [back (pr-str (-> pointer (dissoc :revision :resolved-layer :basis :erased)))
                  now (store/read-one :rows ["workbench" "alice/pointer"])]
              (is (accepted? (gesture! "page-a" :submit {"inspecting" "pointer" "draft" {:value back :base (:revision now)}}))))))

        (testing "scene two: the pair; the base switches to shared; bob accepted later; alice shares her rule"
          (let [a-base (watch stops "page-a" :rows ["workbench" "base/@base" :class])
                a-world (watch stops "page-a" :rows ["workbench" "base/world"])]
            (is (wait-until #(= :by-layer (value a-base))) (pr-str @a-base))
            (is (accepted? (gesture! "page-a" :start-pair {})) "alice starts the pair")
            (is (wait-until #(= :by-entity (value a-base))) (str "the base switched to shared, pushed: " (pr-str (last @a-base))))
            (let [before (count @a-world)]
              (is (accepted? (gesture! "page-a" :use-pair {})) "alice reads the pair")
              (is (wait-until #(some? (value a-world))) "the world record still arrives across the switch")
              (say "scene two: the base's class for alice" (mapv :value (filter :value @a-base)) "world values" before "->" (count @a-world))))
          (is (accepted? (gesture! "page-a" :add-member {"member" {:value "bob"}})) "bob accepted after the pair was made")
          (is (accepted? (gesture! "page-b" :use-pair {})) "bob reads the pair")
          (let [share (gesture! "page-a" :share {"inspecting" "targeting"})
                b-rule (watch stops "page-b" :rows ["workbench" "pair/targeting" :body])]
            (is (accepted? share) (pr-str share))
            (say "scene two: the promotion" (-> share :decisions first :promotion))
            (is (wait-until #(= 1 (get-in (value b-rule) [:return :from 1])) 60000)
                (str "the shared rule lands in the pair and reaches bob by push: " (pr-str (last @b-rule))))
            (is (accepted? (point! "page-b" 7)))
            (is (wait-until #(= 8 (:from (value b-sel)))) (str "bob's pointer chooses by the shared rule: " (pr-str (last @b-sel))))))

        (testing "scene three: a note naming carol, in the pair; carol forgotten; the note erased on its date"
          (let [g (gesture! "page-a" :note {"note" {:value "Carol reviewed this function."} "names" {:value "carol"}})
                note-name (-> g :decisions first :name)
                a-note (watch stops "page-a" :rows ["workbench" (str "pair/" note-name) :note])
                b-note (watch stops "page-b" :rows ["workbench" (str "pair/" note-name) :note])]
            (is (accepted? g) (pr-str g))
            (is (wait-until #(= #{:carol} (:names (value b-note)))) (str "bob reads the note: " (pr-str (last @b-note))))
            (is (= :yes (:answer (store/forget-person! :carol))) "the operator forgets carol")
            (is (wait-until #(:erased-at (value b-note)) 60000) (str "bob's page shows the note erased: " (pr-str (last @b-note))))
            (is (wait-until #(:erased-at (value a-note)) 60000) (str "alice's too: " (pr-str (last @a-note))))
            (say "scene three: the note on bob's page" (value b-note))))
        (finally
          (run! #(%) @stops)
          (store/close-session! "page-a")
          (store/close-session! "page-b")
          (Thread/sleep 1500))))))
