(ns app.server.rama.roam-ns)

;; Block attributes
;; ::block/open
;; ::block/props
;; ::block/heading
;; ::block/order
;; ::block/uid
;; ::block/text-align
;; ::block/refs
;; ::block/string
;; ::block/children
;; ::block/page
;; ::block/parents

;; User attributes
;; ::user/settings
;; ::user/photo-url
;; ::user/display-page
;; ::user/uid
;; ::user/display-name

;; Page attributes
;; ::page/permissions
;; ::page/sidebar

;; Entity attributes
;; ::ent/emojis
;; ::entity/attrs

;; Graph attributes
;; ::graph/name
;; ::graph/settings

;; Edit attributes
;; ::edit/time
;; ::edit/seen-by
;; ::edit/user

;; Version attributes
;; ::version/upgraded-nonce
;; ::version/nonce
;; ::version/id

;; Window attributes
;; ::window/id
;; ::window/filters

;; Create attributes
;; ::create/time
;; ::create/user

;; Miscellaneous attributes
;; ::node/title
;; ::children/view-type
;; ::log/id
;; ::attrs/lookup
;; ::last-used/time
;; ::vc/blocks


(def roam-readers
  {'graph/settings :graph/settings
   'block/text-align :block/text-align
   'block/parents :block/parents
   'user/photo-url :user/photo-url
   'graph/name :graph/name
   'block/order :block/order
   'version/upgraded-nonce :version/upgraded-nonce
   'block/page :block/page
   'version/id :version/id
   'last-used/time :last-used/time
   'user/settings :user/settings
   'page/sidebar :page/sidebar
   'create/user :create/user
   'block/string :block/string
   'vc/blocks :vc/blocks
   'children/view-type :children/view-type
   'version/nonce :version/nonce
   'edit/seen-by :edit/seen-by
   'window/id :window/id
   'attrs/lookup :attrs/lookup
   'block/props :block/props
   'create/time :create/time
   'node/title :node/title
   'block/heading :block/heading
   'block/refs :block/refs
   'edit/user :edit/user
   'user/display-name :user/display-name
   'ent/emojis :ent/emojis
   'block/children :block/children
   'log/id :log/id
   'block/uid :block/uid
   'block/open :block/open
   'user/uid :user/uid
   'edit/time :edit/time
   'page/permissions :page/permissions
   'db/id :db/id
   'entity/attrs :entity/attrs
   'window/filters :window/filters
   'user/display-page :user/display-page})