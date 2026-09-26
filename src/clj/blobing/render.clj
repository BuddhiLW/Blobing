(ns blobing.render
  "Bridges cryogen's Selmer-based rendering to the hiccup views.

  cryogen always renders `themes/<theme>/html/<layout>.html` through Selmer.
  A ported layout's file is a single tag, e.g. `{% page post %}`; the tag
  hands Selmer's context (cryogen's full params map) to
  `blobing.views.pages/view` and emits the resulting document. Layouts that
  were not ported keep working as plain Selmer templates."
  (:require [blobing.views.pages :as pages]
            [selmer.parser :as selmer]))

(defn render-tag
  "Selmer tag handler: `{% page <kind> %}` renders view `<kind>`."
  [[kind] context]
  (pages/view (keyword kind) context))

(defn install!
  "Registers the `page` Selmer tag. Idempotent; call before compiling."
  []
  (selmer/add-tag! :page render-tag))
