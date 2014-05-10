(ns test.graft
  (:require [graft :as g]
            
            [test.generators :as t-gen]
            [clojure.browser.repl]
            [cemerick.cljs.test :as t]
            [cemerick.double-check.generators :as gen]
            [cemerick.double-check.properties :as prop :include-macros true])
  (:use-macros [cemerick.cljs.test :only (is deftest testing)]))

(deftest get-duplicates 
  (let [foo [1 2]
        bar [1 2]
        composite [foo bar foo]
        duplicates (g/get-duplicates composite)
        seen? #(seq (filter (partial identical? %1) (vals duplicates)))
        pruned (g/prune-duplicates composite duplicates)]
    (is (seen? foo))
    (is (not (seen? bar)))
    (is (some (partial = (g/get-id! foo)) (flatten pruned)))
    (is (not (some (partial = (g/get-id! bar)) (flatten pruned) )))
    (is (= composite (g/graft-duplicates pruned duplicates)))))
