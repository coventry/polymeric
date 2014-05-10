(ns test.graft
  (:require [graft :as g]
            [test.generators :as t-gen]
            [clojure.browser.repl]
            [cemerick.cljs.test :as t]
            [cemerick.double-check.generators :as gen]
            [cemerick.double-check.properties :as prop :include-macros true])
  (:use-macros [cemerick.cljs.test :only (is deftest testing)]))

(deftest assoc-smart-about-identity
  (let [m {} n {m m} nn (assoc n m m)]
    (is (identical? n nn)
        "assoc does nothing when requested key/val are (identical?)")))

(deftest get-duplicates 
  (let [foo [1 2]
        bar [1 2]
        composite [foo bar foo]
        duplicates-result (g/get-duplicates composite)
        duplicates (:duplicates duplicates-result)
        dup? #(seq (filter (partial identical? %1) (vals duplicates)))
        pruned (g/prune-object composite duplicates)]
    (is (dup? foo)
        "get-duplicates should see substructure with multiple references")
    (is (not (dup? bar))
        "get-duplicates should ignore unique substructure")
    (is (< (get-in duplicates-result [:seen (g/get-id! foo)])
           (get-in duplicates-result [:seen (g/get-id! bar)]))
        "foo should be seen before bar")
    (is (some (partial = (g/get-id! foo)) (flatten pruned))
        (str "the reference to the duplicate structure should appear in "
             "the pruned version"))
    (is (not (some (partial = (g/get-id! bar)) (flatten pruned)))
        "there should be no reference to the unique structure")
    (is (= composite (g/graft-duplicates pruned duplicates))
        "reconstruction should result in equivalent structure")))

(get-duplicates)

(defn round-trip [s]
  )

(assoc-smart-about-identity)
