(ns components.design-tokens
  "Shared design tokens — single source of truth for colors, spacing, radii, shadows.
   Used by both client (loop.cljs) and server (server_jetty.clj) pipelines.")

(def dt
  "Design tokens — Linear/shadcn-inspired dark theme."
  {:colors {:bg           [0.09 0.09 0.11 1.0]
            :bg-subtle    [0.11 0.11 0.13 1.0]
            :bg-muted     [0.14 0.14 0.17 1.0]
            :bg-elevated  [0.13 0.13 0.16 1.0]
            :bg-hover     [0.16 0.16 0.19 1.0]
            :bg-selected  [0.20 0.24 0.36 0.9]
            :bg-active    [0.15 0.15 0.18 1.0]
            :border       [0.22 0.22 0.28 1.0]
            :border-subtle [0.18 0.18 0.22 0.6]
            :fg           [0.90 0.90 0.92 1.0]
            :fg-muted     [0.55 0.55 0.60 1.0]
            :fg-subtle    [0.40 0.40 0.45 0.8]
            :fg-section   [0.42 0.42 0.48 1.0]
            :accent       [0.35 0.55 0.95 1.0]
            :accent-muted [0.25 0.38 0.65 0.3]
            :destructive  [0.90 0.30 0.30 1.0]
            :success      [0.30 0.80 0.50 1.0]
            :warning      [0.95 0.75 0.25 1.0]}
   :spacing {:xs 4 :sm 8 :md 12 :lg 16 :xl 24 :xxl 32}
   :radii   {:sm 4 :md 6 :lg 8 :xl 12 :full 9999}
   :shadows {:sm  {:blur 4  :offset-y 1 :color [0 0 0 0.15]}
             :md  {:blur 8  :offset-y 2 :color [0 0 0 0.25]}
             :lg  {:blur 16 :offset-y 4 :color [0 0 0 0.35]}}
   :font-sizes {:xs 10 :sm 12 :md 14 :lg 16 :xl 20}
   ;; Surface elevation — depth over borders
   :surfaces {:sunken       [0.05 0.06 0.08 1.0]
              :base         [0.07 0.08 0.11 1.0]
              :elevated     [0.12 0.15 0.21 1.0]
              :hover        [0.13 0.17 0.22 1.0]
              :active       [0.18 0.23 0.31 1.0]
              :text-primary   [0.90 0.93 0.97 1.0]
              :text-secondary [0.55 0.61 0.70 1.0]
              :text-muted     [0.36 0.42 0.50 1.0]
              :accent         [0.24 0.63 1.0 1.0]}})
