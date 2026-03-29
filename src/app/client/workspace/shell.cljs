(ns app.client.workspace.shell
  "3-pane file layout: Editor | Chat | Preview."
  (:require [clojure.string :as str]
            [app.client.workspace.rect-tree :refer [rt-node wrap-line]]
            [app.client.workspace.runtime.workspace-actions :as ws]
            [app.client.workspace.ui-primitives :refer [dt typo-subtitle typo-body build-empty-state]]
            [app.client.workspace.trail :refer [trail->chat-nodes]]))

(defn build-file-layout
  "Build the 3-pane layout for any open file.
   ┌──────────────┬──────────────┬──────────────┐
   │  CODE FILE   │ CHAT SESSION │   PREVIEW    │
   │  (live file) │ (Claude CLI) │ (placeholder)│
   └──────────────┴──────────────┴──────────────┘
   Returns a single rt-node tree spanning the full content area.
   shimmer-alpha: 0.0-1.0 pulse for pending tool cards.
   collapsed: #{keyword} set of collapsed block ids."
  [w h current-file agent-output font-size shimmer-alpha collapsed
   & {:keys [local-world active-pane char-advance chat-scroll-y chat-input focus]
      :or {local-world nil active-pane :editor char-advance nil chat-scroll-y 0
           chat-input {:text "" :cursor 0} focus :editor}}]
  (let [colors (:colors dt)
        surfaces (:surfaces dt)
        fg (:fg colors)
        fg-dim (:fg-muted colors)
        border (:border colors)
        ;; Pane widths come from semantic pane descriptors when present.
        code-w (int (* w (ws/pane-width-pct local-world :main 0.4)))
        chat-w (int (* w (ws/pane-width-pct local-world :right 0.55)))
        render-w (- w code-w chat-w)
        ;; Header height — 36px (4px grid rhythm)
        header-h 36
        ;; Text helpers — use typography hierarchy
        fs (max font-size (:md (:font-sizes dt)))
        fs-hdr (:size typo-subtitle)
        hdr-alpha (:a typo-subtitle)
        line-h (+ fs 4)
        text-y (+ fs 6)
        pad (:lg (:spacing dt))
        char-advance (or char-advance (* fs 0.56))
        ;; Surface colors for depth hierarchy — focused pane gets elevated, others sunken
        elevated-bg (or (:elevated surfaces) (:bg-elevated colors))
        sunken-bg (or (:sunken surfaces) (:bg colors))
        hdr-bg elevated-bg
        ;; Chat pane uses a warm dark bg (matching terminal #090200 feel)
        chat-warm-bg [0.06 0.04 0.03 1.0]
        ;; Per-pane background — focus tracked but same bg (no highlight shift)
        code-bg (if (= active-pane :editor) sunken-bg sunken-bg)
        chat-bg (if (= active-pane :chat) chat-warm-bg chat-warm-bg)
        preview-bg (if (= active-pane :preview) sunken-bg sunken-bg)
        ;; Header text — brighter for focused pane
        text-primary (or (:text-primary surfaces) fg)
        text-secondary (or (:text-secondary surfaces) fg-dim)

        ;; === CODE PANE (left) — real editor renders beneath, we just add header + border ===
        code-header-label (or (:name current-file)
                              (get-in local-world [:selected-artifact :name])
                              "No file open")

        code-focused? (= active-pane :editor)
        code-hdr-fg (if code-focused? text-primary text-secondary)
        code-pane
        (rt-node :file-code-pane :panel
          {:x 0 :y 0 :w code-w :h h}
          :style {:bg code-bg}
          :children
          (cond-> [(rt-node :file-code-hdr :header
                     {:x 0 :y 0 :w code-w :h header-h}
                     :style {:bg hdr-bg
                             :border-widths [0 0 1 0]
                             :border-color (:border-subtle colors)}
                     :text [{:text code-header-label :type :keyword
                              :from 0 :to (count code-header-label)
                              :x pad :y 24 :size fs-hdr
                              :r (nth code-hdr-fg 0) :g (nth code-hdr-fg 1)
                              :b (nth code-hdr-fg 2) :a hdr-alpha}])]
            ;; Focus indicator: 2px bottom accent underline on focused pane header
            code-focused?
            (conj (rt-node :file-code-focus :accent-bar
                    {:x pad :y (- header-h 2) :w 40 :h 2}
                    :style {:bg (or (:accent surfaces) (:accent colors))
                            :radius 1}))))

        ;; === CHAT PANE (center) — typed block rendering ===
        a-status (:status agent-output)
        failed? (= :failed a-status)
        complete? (= :complete a-status)
        running? (or (= :running a-status) (= :submitting a-status))
        chat-status (cond
                      running? "Streaming..."
                      failed? "Failed"
                      complete? "Complete"
                      :else "Idle")
        chat-header-str (str "Chat - " chat-status)
        chat-input-h 36   ;; height of the chat input bar
        chat-body-h (- h header-h chat-input-h)
        trail (:trail agent-output)
        ;; Build chat body children: either typed blocks from trail, or placeholder
        chat-children
        (if (seq trail)
          ;; Status header + typed block nodes
          (let [provider-name (some-> (:provider agent-output) name str/upper-case)
                prompt-text (:prompt agent-output)
                status-label (str "[" (or provider-name "AI") "] " (when a-status (name a-status)) ": " prompt-text)
                status-c (case a-status
                           :complete {:r 0.55 :g 0.9 :b 0.55 :a 1.0}
                           :failed {:r 0.95 :g 0.45 :b 0.45 :a 1.0}
                           :running {:r 0.6 :g 0.8 :b 1.0 :a 1.0}
                           :submitting {:r 0.6 :g 0.8 :b 1.0 :a 1.0}
                           {:r 0.75 :g 0.75 :b 0.75 :a 1.0})
                status-node (rt-node :chat-status-hdr :reasoning-block
                              {:x 0 :y 0 :w chat-w :h (+ line-h 4)}
                              :text [{:text status-label :type :keyword
                                      :from 0 :to (count status-label)
                                      :x pad :y (+ fs 2) :size fs
                                      :r (:r status-c) :g (:g status-c)
                                      :b (:b status-c) :a (:a status-c)}])
                block-nodes (trail->chat-nodes trail chat-w fs char-advance
                                               (or shimmer-alpha 0.4) (or collapsed #{}))]
            (into [status-node] block-nodes))
          ;; No trail — centered empty state
          (if (seq (or (:output agent-output) ""))
            ;; Has flat output text — render it
            (let [result-text (:output agent-output)
                  c {:r 0.55 :g 0.55 :b 0.60 :a 0.7}
                  chat-max-chars (max 20 (int (/ (- chat-w (* 2 pad)) char-advance)))
                  all-lines (vec (mapcat #(wrap-line % chat-max-chars) (str/split-lines result-text)))
                  text-ops (vec (map-indexed
                                  (fn [i line]
                                    {:text line :type :comment
                                     :from 0 :to (count line)
                                     :x pad :y (+ fs (* i line-h))
                                     :size fs :r (:r c) :g (:g c) :b (:b c) :a (:a c)})
                                  all-lines))]
              [(rt-node :chat-placeholder :text-block
                 {:x 0 :y 0 :w chat-w :h (+ fs (* (count all-lines) line-h))}
                 :text text-ops)])
            ;; Empty — centered icon + headline + description
            [(build-empty-state :chat-empty chat-w chat-body-h
               {:icon "--"
                :headline "No session"
                :description "Type a prompt below to start a conversation."})]))

        ;; Chat scroll: use interactive scroll-y, clamped to content bounds
        chat-content-h (reduce + 0 (map #(get-in % [:bounds :h] 0) chat-children))
        max-chat-scroll (max 0 (- chat-content-h chat-body-h))
        chat-scroll-offset (min chat-scroll-y max-chat-scroll)

        ;; Chat header status color
        chat-hdr-accent (cond
                          running?  (:accent colors)
                          complete? (:success colors)
                          failed?   (:destructive colors)
                          :else     nil)

        chat-focused? (= active-pane :chat)
        chat-hdr-fg (if chat-focused? text-primary text-secondary)

        chat-pane
        (rt-node :file-chat-pane :panel
          {:x code-w :y 0 :w chat-w :h h}
          :style {:bg chat-bg}
          :children
          (cond-> [(rt-node :file-chat-hdr :header
                     {:x 0 :y 0 :w chat-w :h header-h}
                     :style {:bg hdr-bg
                             :border-widths [0 0 1 0]
                             :border-color (:border-subtle colors)}
                     :text [{:text chat-header-str :type :keyword
                              :from 0 :to (count chat-header-str)
                              :x pad :y 24 :size fs-hdr
                              :r (nth chat-hdr-fg 0) :g (nth chat-hdr-fg 1)
                              :b (nth chat-hdr-fg 2) :a hdr-alpha}])
                   (rt-node :file-chat-body :panel-content
                     {:x 0 :y header-h :w chat-w :h chat-body-h}
                     :clip? true
                     :children
                     [(rt-node :file-chat-scroll :scroll-container
                        {:x 0 :y (- 4 chat-scroll-offset) :w chat-w :h (+ chat-content-h 8)}
                        :layout {:direction :column :gap 4 :padding [0 0 0 0]}
                        :children chat-children)])
                   ;; === CHAT INPUT BAR (bottom of chat pane) ===
                   (let [input-y (- h chat-input-h)
                         ci-text (:text chat-input)
                         ci-cursor (:cursor chat-input)
                         chat-focused? (= focus :chat)
                         prompt-str "> "
                         prompt-len (count prompt-str)
                         display-text (str prompt-str ci-text)
                         placeholder? (and (empty? ci-text) (not chat-focused?))
                         input-fg (if chat-focused?
                                    {:r 0.85 :g 0.84 :b 0.83 :a 1.0}
                                    {:r 0.50 :g 0.49 :b 0.48 :a 0.7})
                         prompt-fg {:r 0.45 :g 0.70 :b 0.45 :a 0.9}
                         placeholder-fg {:r 0.45 :g 0.43 :b 0.41 :a 0.5}
                         ;; Input text ops
                         input-text-ops
                         (if placeholder?
                           [{:text "Type a message..." :type :comment
                             :from 0 :to 18
                             :x (+ pad (* prompt-len char-advance)) :y (+ fs 10) :size fs
                             :r (:r placeholder-fg) :g (:g placeholder-fg)
                             :b (:b placeholder-fg) :a (:a placeholder-fg)}
                            {:text prompt-str :type :keyword
                             :from 0 :to prompt-len
                             :x pad :y (+ fs 10) :size fs
                             :r (:r prompt-fg) :g (:g prompt-fg)
                             :b (:b prompt-fg) :a (:a prompt-fg)}]
                           [{:text prompt-str :type :keyword
                             :from 0 :to prompt-len
                             :x pad :y (+ fs 10) :size fs
                             :r (:r prompt-fg) :g (:g prompt-fg)
                             :b (:b prompt-fg) :a (:a prompt-fg)}
                            {:text ci-text :type :keyword
                             :from 0 :to (count ci-text)
                             :x (+ pad (* prompt-len char-advance)) :y (+ fs 10) :size fs
                             :r (:r input-fg) :g (:g input-fg)
                             :b (:b input-fg) :a (:a input-fg)}])
                         ;; Caret rect (only when focused)
                         caret-x (+ pad (* (+ prompt-len ci-cursor) char-advance))
                         input-children
                         (if chat-focused?
                           [(rt-node :chat-input-caret :rect
                              {:x caret-x :y 8 :w 2 :h (+ fs 4)}
                              :style {:bg [0.85 0.84 0.83 1.0]})]
                           [])]
                     (rt-node :file-chat-input :panel
                       {:x 0 :y input-y :w chat-w :h chat-input-h}
                       :style {:bg [0.08 0.06 0.05 1.0]
                               :border-widths [1 0 0 0]
                               :border-color (:border-subtle colors)}
                       :text input-text-ops
                       :children input-children))]
            ;; Status accent: bottom underline for focus, left bar for streaming status
            chat-hdr-accent
            (conj (rt-node :file-chat-status :accent-bar
                    {:x 0 :y 0 :w 2 :h header-h}
                    :style {:bg chat-hdr-accent}))
            chat-focused?
            (conj (rt-node :file-chat-focus :accent-bar
                    {:x pad :y (- header-h 2) :w 40 :h 2}
                    :style {:bg (or (:accent surfaces) (:accent colors))
                            :radius 1}))))

        ;; === PREVIEW PANE (right) ===
        render-label "Preview"
        render-msg "No preview"

        preview-focused? (= active-pane :preview)
        preview-hdr-fg (if preview-focused? text-primary text-secondary)

        render-pane
        (rt-node :file-render-pane :panel
          {:x (+ code-w chat-w) :y 0 :w render-w :h h}
          :style {:bg preview-bg}
          :children
          (cond-> [(rt-node :file-render-hdr :header
                     {:x 0 :y 0 :w render-w :h header-h}
                     :style {:bg hdr-bg
                             :border-widths [0 0 1 0]
                             :border-color (:border-subtle colors)}
                     :text [{:text render-label :type :keyword
                              :from 0 :to (count render-label)
                              :x pad :y 24 :size fs-hdr
                              :r (nth preview-hdr-fg 0) :g (nth preview-hdr-fg 1)
                              :b (nth preview-hdr-fg 2) :a hdr-alpha}])
                   (rt-node :file-render-body :panel-content
                     {:x 0 :y header-h :w render-w :h (- h header-h)}
                     :children [(build-empty-state :preview-empty render-w (- h header-h)
                                  {:icon "[]"
                                   :headline "No preview"
                                   :description "Preview will appear when a component is compiled."})])]
            preview-focused?
            (conj (rt-node :file-render-focus :accent-bar
                    {:x pad :y (- header-h 2) :w 40 :h 2}
                    :style {:bg (or (:accent surfaces) (:accent colors))
                            :radius 1}))))]

    ;; Root: spans full content area — transparent so child pane bgs define depth
    (rt-node :file-layout-root :panel
      {:x 0 :y 0 :w w :h h}
      :children [code-pane chat-pane render-pane])))

