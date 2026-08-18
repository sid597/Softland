(ns app.face-integration-test
  "W1-INT gates (framework CONTRACT §11).

   The registry-wiring smoke wears the outline assembly
   (resources/public/faces/outline.edn — the file the /face command fetches)
   compiles CLEAN against the real face_primitives registry and applies over
   a projection-shaped data-context (the §7 data contract) into a renderable,
   fully-bound tree.

   Run: clj -M:test -e \"(require 'app.face-integration-test)
                         (clojure.test/run-tests 'app.face-integration-test)\""
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [app.client.workspace.rect-tree :as rt]
            [app.client.workspace.face-assembly :as fa]
            [app.client.workspace.face-primitives :as prims]))

;; ===========================================================================
;; Shared geometry for the integration checks
;; ===========================================================================

(def geom
  {:viewport-w 760 :viewport-h 600 :content-w 320
   :font-size 14 :char-advance (* 14 0.56) :line-height 18
   :now-ms 0})

(deftest root-each-rejects-at-compile
  ;; G16 gate fix regression: an :each at the ROOT must reject at wear-time
  ;; compile (§5 two-stage law, trap T3) — it previously error-carded only
  ;; at apply, surfacing structural rejection at the wrong layer.
  (let [compiled (fa/compile-assembly
                   prims/registry
                   {:assembly/name "root-each" :assembly/grammar 0
                    :root {:each [:xs] :template {:prim :stack :props {}}}})]
    (is (fa/error? compiled))
    (is (some #(re-find #"root" (str (:msg %))) (fa/compile-errors compiled)))))

;; ===========================================================================
;; Registry wiring smoke — the WORN outline assembly over the REAL registry
;; ===========================================================================

(def worn-outline
  (delay (edn/read-string (slurp (io/resource "public/faces/outline.edn")))))

(def projection-shaped-context
  ;; the §7 data contract as face_projection.clj serves it (G10 shape):
  ;; turns -> blocks, ids everywhere (trap T6)
  {:turns
   [{:id "t-1" :speaker "sid" :order 0
     :blocks [{:id "b-1" :kind "text" :order 0
               :text "Can a face be an assembly, walked by one interpreter over a primitive registry?"}
              {:id "b-2" :kind "code" :order 1
               :text "{:prim :stack :children []}"}]}
    {:id "t-2" :speaker "claude" :order 1
     :blocks [{:id "b-3" :kind "text" :order 0
               :text "Electric owns truth transport; Missionary owns the frame path; the rt-node tree is the boundary object."}]}]
   :conversation/address "conv-smoke"
   :face/rendered-at-ms 1000})

(deftest worn-outline-assembly-over-real-registry
  (let [compiled (fa/compile-assembly prims/registry @worn-outline)
        tree     (fa/apply-assembly compiled projection-shaped-context
                                    {:view-instance :face-main
                                     :address "conv-smoke"
                                     :geom (assoc geom :content-w 728)})]
    (testing "the worn assembly compiles CLEAN against the real registry"
      (is (not (fa/error? compiled)) (pr-str (fa/compile-errors compiled))))
    (testing "fully bound, fully identified — the report must not lie"
      (is (zero? (get-in tree [:data :assembly/apply-report :binds-missing])))
      (is (zero? (get-in tree [:data :assembly/apply-report :items-without-id]))))
    (testing "renders: positive declared content-h, rects + every block's prose"
      (is (pos? (get-in tree [:data :assembly/content-h])))
      (is (seq (rt/tree->rects tree)))
      ;; tree->text-ops emits a nested vector of per-node op vectors — flatten
      (let [ops (mapcat identity (rt/tree->text-ops tree))]
        (doseq [b (mapcat :blocks (:turns projection-shaped-context))]
          (let [prefix (subs (:text b) 0 (min 12 (count (:text b))))]
            (is (some #(str/starts-with? (:text %) prefix) ops)
                (str "block " (:id b) " prose missing from text ops"))))
        (doseq [speaker ["sid" "claude"]]
          (is (some #(= speaker (:text %)) ops)
              (str "speaker badge " speaker " missing")))))))
