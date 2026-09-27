(ns softland.inland.scenes-test
  "The pointer's scenes on the server side, against the rig's module on a fresh
   in-process cluster: two pages driven through the adapter as the browser drives
   them (gestures with the page's local cells, and watched reads), with no browser.
   Scene one: a person changes the pointer's rule while using it. Scene two: two
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
          b-sel (cell "page-b" b "selection")]
      (try
        (testing "scene one, server side: alice changes her pointer's rule while using it"
          (is (accepted? (point! "page-a" 7)))
          (is (wait-until #(= 7 (:from (value a-sel)))) (pr-str (last @a-sel)))
          (let [base (store/read-one :rows ["workbench" "base/targeting"])
                draft (pr-str {:label "Select the line after the one I point at"
                               :body {:steps [] :return {:file [:get :subject :file] :from [:+ 1 [:get :subject :line]]
                                                         :to [:+ 1 [:get :subject :line]] :kind "line" :label "the next line"}}})
                g (gesture! "page-a" :submit {"inspecting" "targeting" "draft" {:value draft :base (:revision base)}})]
            (is (accepted? g) (pr-str g))
            (is (accepted? (point! "page-a" 7)))
            (is (wait-until #(= 8 (:from (value a-sel)))) "the next point chose by the new rule")
            (say "scene one: alice's selection" (value a-sel))))

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
