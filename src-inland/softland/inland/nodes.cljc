(ns softland.inland.nodes
  "Electric ownership for existing Softland rendering. Identity controls
   allocation lifetime; value dependencies control narrow preparation effects."
  (:require [hyperfiddle.electric3 :as e]
            [softland.inland.reactive :as reactive]
            #?(:cljs [softland.inland.render :as render])))

(e/defn Await [flow]
  ;; for-by returns a vector in this Electric version. diff-by exposes the
  ;; actual zero-or-one table, preserving absence while the source is pending.
  (e/diff-by (constantly :value) (e/input (reactive/table flow))))

(e/defn Text [r id order face text box size rgba]
  (e/client
    (let [owned (e/input (render/node r id :text order face))]
      (render/text! r owned text box size rgba))))

(e/defn Path [r id order material]
  (e/client
    (let [owned (e/input (render/node r id :path order nil))]
      (render/path! r owned material))))

(e/defn Hit [r id order box action]
  (e/client
    (let [owned (e/input (render/node r id :hit order nil))]
      (render/hit! r owned box action order))))

(e/defn Scene [r id shapes rect options]
  (e/client
    (let [owned (e/input (render/node r id :scene 15 nil))]
      (when owned (render/scene! r shapes rect options)))))
