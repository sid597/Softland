(ns app.face-integration-test
  "W1-INT gates (framework CONTRACT §11).

   G14(a) — the worn-UI structural-equality falsifier at assembly grain, the
   JVM instance (the live check (b) ran at INT in the then-current dev app and
   is recorded in the INT artifact — its cljs surface was cljs-only, §10/§11
   split).
   The named trail-face sub-tree: the expansion card's HOLES COLUMN
   (scene.cljc:366-372 + :399-402) — map-indexed hole-endpoint-cards stacked
   in a column at y = i * (4 + line-height). Criterion held: >=1 :each, >=2
   nesting levels, text + rects. Re-expressed as an assembly (:stack + :each
   + :hole-card) over the same committed fixture; children must be
   STRUCTURALLY EQUAL — ids, bounds, styles, text, data (`=`, whole nodes).

   Carve-outs (named, quantified — never silent):
   - the CONTAINER node is the mount: the shipped one is a bare positioned
     rt-node (:holes, explicit child y), the assembly one is :stack (engine
     arrangement, Δ1-stamped root :data). Containers are compared on width;
     the container :h differs by EXACTLY one trailing gap — the shipped math
     allocates gap-after-each (n * (gap + row-h)), :stack gaps only BETWEEN
     children (n * row-h + (n-1) * gap). That delta is a real G14 finding
     (recorded in the INT artifact) and is asserted EXACTLY below so any
     drift beyond the named difference still fails.
   - hole rows carry :relation-id, not :id, so the :each id fallback counts
     them in the apply-report (trap T6 honesty) — asserted, not hidden.

   Plus the registry-wiring smoke: the WORN outline assembly
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
            [app.client.workspace.face-primitives :as prims]
            [app.client.workspace.trail-face.cards :as cards]))

;; ===========================================================================
;; Shared geometry — one value for both sides (bounds equality demands it)
;; ===========================================================================

(def line-height 18)
(def card-w 320)

(def geom
  {:viewport-w 760 :viewport-h 600 :content-w card-w
   :font-size 14 :char-advance (* 14 0.56) :line-height line-height
   :now-ms 0})

;; ===========================================================================
;; G14(a) fixture — committed hole rows (every row carries :relation-id so the
;; shipped (hash row) fallback is never reached; :kind keywords are what
;; hole-endpoint-card names in its text op)
;; ===========================================================================

(def hole-rows
  [{:relation-id "rel-a" :kind :hole}
   {:relation-id "rel-b" :kind :unresolved}
   {:relation-id "rel-c" :kind :hole}])

(defn shipped-holes-column
  "The SHIPPED arrangement, scene.cljc:366-372 + :399-402 verbatim math:
   hole-endpoint-cards positioned at y = i * (4 + line-height) inside a bare
   container sized n * (4 + line-height) (gap-after-each)."
  [rows]
  (let [hole-nodes (vec (map-indexed
                          (fn [i r]
                            (-> (cards/hole-endpoint-card
                                  r {:card-w card-w :line-height line-height})
                                (assoc-in [:bounds :y] (* i (+ 4 line-height)))))
                          rows))
        holes-h (* (count hole-nodes) (+ 4 line-height))]
    (rt/rt-node [:trail-face/holes "g14-fixture"] :holes
                {:x 0 :y 0 :w card-w :h holes-h}
                :children hole-nodes)))

(def holes-slice-assembly
  ;; the same sub-scene as ARRANGEMENT data (grammar v0, guard-clean):
  ;; column of hole cards, gap 4. :hole-card receives the row fields as
  ;; bound props (the wrapper's row fallback, face_primitives.cljc).
  {:assembly/name    "g14-holes-slice"
   :assembly/grammar 0
   :root
   {:prim :stack
    :props {:w 320 :gap 4}
    :children
    [{:each [:holes]
      :template
      {:prim :hole-card
       :props {:w 320
               :kind        {:bind [:kind]}
               :relation-id {:bind [:relation-id]}}}}]}})

(defn- strip-src-path
  "Structural equality is compared MODULO the interpreter's :assembly/src-path
   provenance stamp: the interpreter records the template node each rt-node came
   from (designer edit-mode), but the hand-shipped arrangement — built directly,
   not through the interpreter — legitimately has none. Provenance is metadata,
   not arrangement, so it is removed before the shape comparison. Removing the
   only key restores :data to its pre-stamp form (nil), so an unstamped shipped
   node still matches exactly."
  [node]
  (let [d (dissoc (:data node) :assembly/src-path)]
    (-> node
        (assoc :data (if (seq d) d nil))
        (update :children (fn [cs] (mapv strip-src-path cs))))))

(deftest g14a-holes-column-structural-equality
  (let [shipped  (rt/resolve-layout (shipped-holes-column hole-rows))
        compiled (fa/compile-assembly prims/registry holes-slice-assembly)
        tree     (fa/apply-assembly compiled {:holes hole-rows}
                                    {:view-instance :g14
                                     :address "g14-fixture"
                                     :geom geom})]
    (testing "the assembly compiles against the REAL registry"
      (is (not (fa/error? compiled)) (pr-str (fa/compile-errors compiled))))
    (testing "children are structurally EQUAL — ids, bounds, styles, text, data (modulo provenance)"
      (is (= 3 (count (:children shipped)) (count (:children tree))))
      ;; whole-node equality, child by child (failure prints the diff pair);
      ;; the assembly side carries the :assembly/src-path stamp the hand-shipped
      ;; side cannot — stripped before compare (provenance is not arrangement)
      (doseq [[s a] (map vector (:children shipped) (:children tree))]
        (is (= s (strip-src-path a)))))
    (testing "container width equal; height differs by EXACTLY one trailing gap"
      (is (= (get-in shipped [:bounds :w]) (get-in tree [:bounds :w])))
      (is (= (get-in shipped [:bounds :h])
             (+ (get-in tree [:bounds :h]) 4))))
    (testing "Δ1 stamp + honest id fallback count (rows carry no :id)"
      (is (= :g14 (get-in tree [:data :view-instance])))
      (is (= "g14-fixture" (get-in tree [:data :address])))
      (is (= 3 (get-in tree [:data :assembly/apply-report :items-without-id])))
      (is (zero? (get-in tree [:data :assembly/apply-report :binds-missing]))))
    (testing "both flatten through the real render primitives"
      (is (seq (rt/tree->rects shipped)))
      (is (seq (rt/tree->rects tree)))
      (is (= (count (rt/tree->text-ops shipped))
             (count (rt/tree->text-ops tree)))))))

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
