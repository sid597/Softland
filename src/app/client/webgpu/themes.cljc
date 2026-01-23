(ns app.client.webgpu.themes
  "Syntax highlighting themes for the editor.
   Extracted to avoid circular dependencies between electric-flow and loop.")

;; ============================================================================
;; SYNTAX HIGHLIGHTING THEMES
;; ============================================================================
;; Each theme maps token types to RGBA colors (0.0-1.0 range for WebGPU)

(def themes
  {:gruvbox-dark
   {:name "Gruvbox Dark"
    :background {:r 0.11 :g 0.13 :b 0.13 :a 1.0}  ;; #1d2021 (hard)
    :keyword    {:r 0.996 :g 0.502 :b 0.098 :a 1.0}  ;; #fe8019 orange
    :macro      {:r 0.556 :g 0.752 :b 0.486 :a 1.0}  ;; #8ec07c aqua
    :string     {:r 0.722 :g 0.733 :b 0.149 :a 1.0}  ;; #b8bb26 green
    :comment    {:r 0.573 :g 0.514 :b 0.455 :a 1.0}  ;; #928374 gray
    :delimiter  {:r 0.659 :g 0.600 :b 0.518 :a 0.7}  ;; #a89984 fg dim
    :number     {:r 0.827 :g 0.525 :b 0.608 :a 1.0}  ;; #d3869b purple
    :character  {:r 0.827 :g 0.525 :b 0.608 :a 1.0}  ;; #d3869b purple
    :boolean    {:r 0.827 :g 0.525 :b 0.608 :a 1.0}  ;; #d3869b purple
    :nil        {:r 0.984 :g 0.286 :b 0.204 :a 1.0}  ;; #fb4934 red
    :text       {:r 0.922 :g 0.859 :b 0.698 :a 1.0}} ;; #ebdbb2 fg

   :gruvbox-light
   {:name "Gruvbox Light"
    :background {:r 0.984 :g 0.945 :b 0.847 :a 1.0}  ;; #fbf1c7
    :keyword    {:r 0.839 :g 0.365 :b 0.055 :a 1.0}  ;; #d65d0e orange
    :macro      {:r 0.408 :g 0.616 :b 0.416 :a 1.0}  ;; #689d6a aqua
    :string     {:r 0.596 :g 0.592 :b 0.102 :a 1.0}  ;; #98971a green
    :comment    {:r 0.573 :g 0.514 :b 0.455 :a 1.0}  ;; #928374 gray
    :delimiter  {:r 0.404 :g 0.361 :b 0.325 :a 0.7}  ;; #665c54 fg dim
    :number     {:r 0.694 :g 0.384 :b 0.525 :a 1.0}  ;; #b16286 purple
    :character  {:r 0.694 :g 0.384 :b 0.525 :a 1.0}  ;; #b16286 purple
    :boolean    {:r 0.694 :g 0.384 :b 0.525 :a 1.0}  ;; #b16286 purple
    :nil        {:r 0.800 :g 0.141 :b 0.114 :a 1.0}  ;; #cc241d red
    :text       {:r 0.235 :g 0.220 :b 0.212 :a 1.0}} ;; #3c3836 fg

   :rose-pine
   {:name "Rosé Pine"
    :background {:r 0.114 :g 0.106 :b 0.141 :a 1.0}  ;; #191724
    :keyword    {:r 0.922 :g 0.576 :b 0.545 :a 1.0}  ;; #eb6f92 love
    :macro      {:r 0.769 :g 0.659 :b 0.890 :a 1.0}  ;; #c4a7e7 iris
    :string     {:r 0.945 :g 0.820 :b 0.545 :a 1.0}  ;; #f1d18b gold (adjusted)
    :comment    {:r 0.416 :g 0.400 :b 0.525 :a 1.0}  ;; #6e6a86 muted
    :delimiter  {:r 0.576 :g 0.549 :b 0.659 :a 0.7}  ;; #908caa subtle
    :number     {:r 0.922 :g 0.576 :b 0.545 :a 1.0}  ;; #eb6f92 love
    :character  {:r 0.922 :g 0.820 :b 0.659 :a 1.0}  ;; #ebbcba rose
    :boolean    {:r 0.769 :g 0.659 :b 0.890 :a 1.0}  ;; #c4a7e7 iris
    :nil        {:r 0.922 :g 0.576 :b 0.545 :a 1.0}  ;; #eb6f92 love
    :text       {:r 0.878 :g 0.851 :b 0.914 :a 1.0}} ;; #e0def4 text

   :kanagawa
   {:name "Kanagawa"
    :background {:r 0.102 :g 0.102 :b 0.137 :a 1.0}  ;; #1a1a23
    :keyword    {:r 0.886 :g 0.639 :b 0.545 :a 1.0}  ;; #e2a38b surimiOrange
    :macro      {:r 0.498 :g 0.686 :b 0.702 :a 1.0}  ;; #7fb4b3 waveAqua2
    :string     {:r 0.596 :g 0.737 :b 0.545 :a 1.0}  ;; #98bb6c springGreen
    :comment    {:r 0.455 :g 0.478 :b 0.529 :a 1.0}  ;; #727d87 fujiGray
    :delimiter  {:r 0.565 :g 0.565 :b 0.659 :a 0.7}  ;; #9090a8 dim
    :number     {:r 0.882 :g 0.557 :b 0.698 :a 1.0}  ;; #e18eb2 sakuraPink
    :character  {:r 0.882 :g 0.557 :b 0.698 :a 1.0}  ;; #e18eb2 sakuraPink
    :boolean    {:r 0.710 :g 0.580 :b 0.780 :a 1.0}  ;; #b594c7 oniViolet
    :nil        {:r 0.886 :g 0.451 :b 0.451 :a 1.0}  ;; #e27373 autumnRed
    :text       {:r 0.863 :g 0.855 :b 0.820 :a 1.0}} ;; #dcdad1 fujiWhite

   :cyberdream
   {:name "Cyberdream"
    :background {:r 0.063 :g 0.063 :b 0.094 :a 1.0}  ;; #101018
    :keyword    {:r 1.0   :g 0.380 :b 0.573 :a 1.0}  ;; #ff6192 pink
    :macro      {:r 0.373 :g 0.843 :b 0.961 :a 1.0}  ;; #5fd7f5 cyan
    :string     {:r 0.565 :g 0.933 :b 0.565 :a 1.0}  ;; #90ee90 green
    :comment    {:r 0.420 :g 0.420 :b 0.490 :a 1.0}  ;; #6b6b7d gray
    :delimiter  {:r 0.600 :g 0.600 :b 0.700 :a 0.7}  ;; #9999b3 dim
    :number     {:r 0.988 :g 0.722 :b 0.424 :a 1.0}  ;; #fcb86c orange
    :character  {:r 0.988 :g 0.722 :b 0.424 :a 1.0}  ;; #fcb86c orange
    :boolean    {:r 0.816 :g 0.529 :b 0.937 :a 1.0}  ;; #d087ef purple
    :nil        {:r 1.0   :g 0.380 :b 0.573 :a 1.0}  ;; #ff6192 pink
    :text       {:r 0.949 :g 0.949 :b 0.969 :a 1.0}} ;; #f2f2f7 white

   :classic-dark
   {:name "Classic Dark"
    :background {:r 0.12 :g 0.12 :b 0.14 :a 1.0}
    :keyword    {:r 0.8 :g 0.4 :b 0.8 :a 1.0}
    :macro      {:r 0.3 :g 0.6 :b 1.0 :a 1.0}
    :string     {:r 0.6 :g 0.8 :b 0.4 :a 1.0}
    :comment    {:r 0.5 :g 0.5 :b 0.5 :a 1.0}
    :delimiter  {:r 1.0 :g 1.0 :b 1.0 :a 0.6}
    :number     {:r 1.0 :g 0.6 :b 0.3 :a 1.0}
    :character  {:r 0.9 :g 0.7 :b 0.4 :a 1.0}
    :boolean    {:r 0.7 :g 0.3 :b 0.9 :a 1.0}
    :nil        {:r 0.8 :g 0.3 :b 0.3 :a 1.0}
    :text       {:r 0.9 :g 0.9 :b 0.9 :a 1.0}}})

;; Default theme (can be overridden by settings)
(def default-theme-id :gruvbox-dark)

;; List of theme IDs for UI
(def theme-ids (keys themes))

;; Ordered list for cycling in UI
(def theme-list [:gruvbox-dark :gruvbox-light :rose-pine :kanagawa :cyberdream :classic-dark])

(defn get-theme [theme-id]
  "Get a theme by ID, falling back to default"
  (get themes theme-id (get themes default-theme-id)))

(defn get-color
  "Get color for a token type from the specified theme"
  ([type] (get-color type default-theme-id))
  ([type theme-id]
   (let [theme (get-theme theme-id)]
     (get theme type (:text theme)))))

(defn theme-index [theme-id]
  "Get index of theme in theme-list for UI cycling"
  (or (first (keep-indexed (fn [i t] (when (= t theme-id) i)) theme-list)) 0))
