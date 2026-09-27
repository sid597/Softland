;; The adapter driven as a page drives it, with no browser: against the rig's
;; module on Inland's local cluster (bin/inland cluster; bin/inland deploy).
;; Run from the repository root:
;;   clojure -J-Xss16m -M:inland src/proposal/pointer-screen-2026-09-27/tools/screen_smoke.clj
;; Prints what it saw, step by step; exits 0 when every check held.
(require '[missionary.core :as m]
         '[rig.store.client :as c]
         '[softland.inland.facts :as f]
         '[softland.inland.store :as store])

(def failures (atom []))
(defn check [what ok & [detail]]
  (println (if ok "  ok  " "  FAIL") what (if detail (str "  " (pr-str detail)) ""))
  (when-not ok (swap! failures conj what)))

(defn wait-until [f ms]
  (let [deadline (+ (System/currentTimeMillis) ms)]
    (loop [] (let [v (try (f) (catch Throwable _ nil))]
               (if (or v (> (System/currentTimeMillis) deadline)) v (do (Thread/sleep 50) (recur)))))))

(defn watch
  "An Inland read address watched through the adapter, as execution/ReadStatus does:
   its values collect in an atom. Returns [values cancel]."
  [owner kind path]
  (let [seen (atom [])
        cancel ((m/reduce (fn [_ v] (swap! seen conj v)) nil (store/watch-path owner kind path))
                (fn [_]) (fn [e] (println "flow failed" e)))]
    [seen cancel]))

(defn hand-acts
  "The acts in a hand layer, as the operator's own view holds them (never shown to a person)."
  [layer]
  (let [st (:store @store/connection)]
    (group-by (comp first :id) (:facts (c/read-as-of st layer (c/clock st layer))))))

(println "connect: the first facts and the genesis records when not in")
(store/connect!)
(check "connected, the bells' task keys found" (= 2 (count (:task-keys @store/connection))) (:task-keys @store/connection))

(println "a page for alice")
(def info (store/page-open! "smoke-a" :alice))
(check "a session in the store, its stored cells' address" (and (:thing info) (= "alice-hand" (:hand info))) info)

(println "the world record, through a standing read")
(def world+ (watch "smoke-a" :rows ["workbench" "base/world"]))
(def world (first world+))
(def stop-world (second world+))
(check "the world arrives as a value" (wait-until #(some (fn [v] (= :value (:status v))) @world) 20000)
       (mapv :status @world))
(check "its session defaults are the seed's" (= "pointer" (get-in (some #(when (= :value (:status %)) (:value %)) @world) [:session "active"])))

(println "the stored context cell")
(def ctx+ (watch "smoke-a" :rows ["workbench" (str "alice-hand/" (:thing info)) :cell/context]))
(def ctx (first ctx+))
(def stop-ctx (second ctx+))
(check "the context arrives: alice's own layer, then the base"
       (wait-until #(some (fn [v] (= ["alice" "base"] (get-in v [:value :layers]))) @ctx) 20000) (last @ctx))

(println "a gesture: point at the instrument, which opens its rule")
(def g1 (store/gesture! "smoke-a" {:kind :point :subject "pointer" :id "e1"} {"active" "pointer"}))
(check "the gesture's reads were recorded in one act" (some? (:recorded g1)) (select-keys g1 [:recorded :failure]))
(check "its effects open the rule in the editor"
       (some #(= "targeting" (get-in % [:writes "inspecting"])) (:effects g1)) (:effects g1))

(println "the targeting rule, watched in alice's layer and the base")
(def rule-a+ (watch "smoke-a" :rows ["workbench" "alice/targeting" :body]))
(def rule-a (first rule-a+))
(def stop-ra (second rule-a+))
(def rule-b+ (watch "smoke-a" :rows ["workbench" "base/targeting" :body]))
(def rule-b (first rule-b+))
(def stop-rb (second rule-b+))
(check "alice's layer answers (her own rule, or complete absence)"
       (wait-until #(some (fn [v] (#{:value :absent} (:status v))) @rule-a) 20000) (last @rule-a))
(check "present in the base" (wait-until #(some (fn [v] (= :value (:status v))) @rule-b) 20000) (last @rule-b))

;; state-independent: this run's rule selects the line `k` after the one pointed at
(def run-id (str (System/currentTimeMillis)))
(def k (+ 2 (mod (quot (System/currentTimeMillis) 1000) 7)))

(println "a gesture: point at a line under the current rule (no material: the subject stands for itself)")
(def g2 (store/gesture! "smoke-a" {:kind :point :material true :subject {:file "file/none" :line 7} :id "e2"} {"active" "pointer"}))
(check "recorded, no failure" (and (:recorded g2) (not (:failure g2))) (select-keys g2 [:recorded :failure :decisions]))
(def sel+ (watch "smoke-a" :rows ["workbench" (str "alice-hand/" (:thing info)) :cell/selection]))
(def sel (first sel+))
(def stop-sel (second sel+))
(check "the selection is a stored fact, pushed back"
       (wait-until #(some (fn [v] (= :value (:status v))) @sel) 20000) (last @sel))

(println "a gesture: change the rule while using it (submit a draft into alice's layer)")
(def draft (pr-str {:label (str "Select the line " k " after the one I point at (run " run-id ")")
                    :body {:steps [] :return {:file [:get :subject :file] :from [:+ k [:get :subject :line]]
                                              :to [:+ k [:get :subject :line]] :kind "line" :label (str "line +" k)}}}))
;; what alice's context resolves now: her own layer's rule, else the base's
(def seen-revision (:revision (or (store/read-one :rows ["workbench" "alice/targeting"])
                                  (store/read-one :rows ["workbench" "base/targeting"]))))
(def g3 (store/gesture! "smoke-a" {:kind :submit :id "e3"}
                        {"active" "pointer" "inspecting" "targeting" "draft" {:value draft :base seen-revision}}))
(check "the edit was admitted" (= :accepted (:status (first (:decisions g3)))) g3)
(check "the new rule arrives in alice's layer by push"
       (wait-until #(some (fn [v] (and (= :value (:status v)) (= k (get-in v [:value :return :from 1])))) @rule-a) 20000)
       (last @rule-a))

(println "a gesture: point again; the new rule chooses")
(def g4 (store/gesture! "smoke-a" {:kind :point :material true :subject {:file "file/none" :line 7} :id "e4"} {"active" "pointer"}))
(check (str "the selection is now line " (+ 7 k))
       (wait-until #(some (fn [v] (= (+ 7 k) (get-in v [:value :from]))) @sel) 20000) (last @sel))

(println "what the store holds in alice's hand layer (the operator's view)")
(def acts (hand-acts :alice-hand))
(def entry-acts (filter (fn [[_ fs]] (some #(#{:read/point :read/pattern :read/standing :read/delivery :read/closed} (:k %)) fs)) acts))
(check "read records are in the hand layer" (pos? (count entry-acts)) (count entry-acts))
(println "   acts in alice-hand:" (count acts) "of which read records:" (count entry-acts)
         " lines:" (frequencies (mapcat (fn [[_ fs]] (map :k fs)) entry-acts)))

(run! #(%) [stop-world stop-ctx stop-ra stop-rb stop-sel])
(store/close-session! "smoke-a")
(Thread/sleep 1500)
(println (if (empty? @failures) "ALL CHECKS HELD" (str "FAILED: " @failures)))
(shutdown-agents)
(System/exit (if (empty? @failures) 0 1))
