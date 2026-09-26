(ns blobing.bundle-test
  "Size budget for the compiled client bundle (src/cljs -> themes/lotus/js/site.js).

  One generic `=`, `str` on an untyped value, `doseq` over a NodeList or a
  `clj->js` pulls ~95 KB of cljs.core into a bundle that is otherwise ~5 KB
  (see the blobing.site docstring). This catches that regression at the unit
  layer; rebuild with `clojure -M:cljs` before running it."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]))

(def ^:private bundle (io/file "themes/lotus/js/site.js"))
(def ^:private budget-bytes 12000)

(deftest bundle-stays-within-budget
  (is (.exists bundle) "compile it with `clojure -M:cljs`")
  (is (< (.length bundle) budget-bytes)
      (str "site.js is " (.length bundle) " bytes; budget " budget-bytes
           ". Look for =, untyped str, doseq over JS arrays or clj->js in src/cljs.")))
