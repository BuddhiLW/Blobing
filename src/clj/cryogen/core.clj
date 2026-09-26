(ns cryogen.core
  (:require [cryogen-core.compiler :refer [compile-assets-timed]]
            [cryogen-core.plugins :refer [load-plugins]]
            [cryogen.content :as content]
            [blobing.render :as render]))

(defn -main []
  (load-plugins)
  (render/install!)
  (compile-assets-timed content/hooks)
  (System/exit 0))
