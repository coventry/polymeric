(ns test.generators
  (:require [graft :as g]
            [clojure.browser.repl]
            [cemerick.cljs.test :as t]
            [cemerick.double-check.generators :as gen]
            [cemerick.double-check.properties :as prop :include-macros true])
  (:use-macros [cemerick.cljs.test :only (is deftest testing)]))

(defn structured* [size]
  (if (not (pos? size))
    gen/any
    (let [new-size (* (quot size 4) 3)
          smaller-structure (gen/resize new-size (gen/sized structured))]
      (gen/bind (gen/frequency
                 [[1 (gen/such-that not-empty (gen/vector gen/simple-type))]
                  [10 (gen/such-that not-empty (gen/vector smaller-structure))]])
                (fn [%]
                  (if (and (coll? %) (> (count %) 0))
                    (gen/sized (gen/sized-container (gen/elements %)))
                    (gen/return %)))))))

(defn structured []
  (gen/sized structured*))
