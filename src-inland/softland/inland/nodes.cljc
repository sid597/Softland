(ns softland.inland.nodes
  "Electric occurrence wrappers for the existing Softland render adapter.
   Takes render owner, occurrence identities and target values; gives prepared text,
   paths, hit regions and scenes. Holds subscriptions that own native node allocation;
   borrows the surface and its engine assets. Stable node identity retains resources,
   while changed values run preparation for that occurrence. No application-specific
   tool or editor interpretation belongs to these wrappers."
  (:require [hyperfiddle.electric3 :as e]
            [softland.inland.reactive :as reactive]
            #?(:cljs [softland.inland.render :as render])))

(e/defn Await
  "Acquisition flow → its delivered value in an Electric branch.
   An initialized zero-or-one table has no value while acquisition is pending;
   diff-by extracts the singleton rather than returning a for-by result vector."
  [flow]
  ;; for-by returns a vector in this Electric version. diff-by exposes the
  ;; actual zero-or-one table, preserving absence while the source is pending.
  (e/diff-by (constantly :value) (e/input (reactive/table flow))))

(e/defn Text
  "Text occurrence inputs → positioned layout and prepared glyph resources.
   The node subscription owns allocation/disposal; text changes rerun preparation
   without replacing identity. The surface supplies the selected font face."
  [r id order face text box size rgba]
  (e/client
    (let [owned (e/input (render/node r id :text order face))]
      (render/text! r owned text box size rgba))))

(e/defn Path
  "Path occurrence identity and material → preparation on its retained node.
   Owns node removal through input cancellation; the shared path system is borrowed."
  [r id order material]
  (e/client
    (let [owned (e/input (render/node r id :path order nil))]
      (render/path! r owned material))))

(e/defn Hit
  "Occurrence box/action/order → owned hit-registry entry.
   Changing its box or action updates the entry; cancelling the node removes it."
  [r id order box action]
  (e/client
    (let [owned (e/input (render/node r id :hit order nil))]
      (render/hit! r owned box action order))))

(e/defn Scene
  "Scene identity, shape pairs, rectangle and options → region preparation.
   The node owns the scene occurrence; renderer/scene! validates and realizes values
   using the surface's shared Region3D system."
  [r id shapes rect options]
  (e/client
    (let [owned (e/input (render/node r id :scene 15 nil))]
      (when owned (render/scene! r shapes rect options)))))
