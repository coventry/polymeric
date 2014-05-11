(ns test.graft
  (:require [graft :as g]
            [test.generators :as t-gen]
            [clojure.browser.repl]
            [cemerick.cljs.test :as t]
            [cemerick.double-check.generators :as gen]
            [cemerick.double-check.properties :as prop :include-macros true])
  (:use-macros [cemerick.cljs.test :only (is deftest testing)]
               [cemerick.double-check.clojure-test :only [defspec]]))

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
    (is (= composite (g/reflate pruned duplicates))
        "reconstruction should result in equivalent structure")))

(get-duplicates)

(deftest reflate
  (let [b [1 2]
        c [1 1]
        a [b c]
        r [a [b b] a]
        duplicates-result (g/get-duplicates r)
        pruned (g/prune-duplicates duplicates-result)
        reflated (g/reflate-duplicates pruned)
        pruned-r (g/prune-object r (into {} pruned))]
    (is (= (:duplicates duplicates-result) reflated)
        "Reflating the duplicates should produce the same map")
    (is (= (g/reflate pruned-r reflated) r)
        "Reflating the object should produce the same map")))

(reflate)

(defn roundtrip [s]
  (let [duplicates-result (g/get-duplicates s)
        pruned (g/prune-duplicates duplicates-result)
        reflated (g/reflate-duplicates pruned)
        pruned-s (g/prune-object s (into {} pruned))]
    (g/reflate pruned-s reflated)))

;; XXX Redo as a defspec??
(deftest roundtrip-gives-same-result2
  (is (every? #(= % (roundtrip %)) (gen/sample (t-gen/structured) 20))))

(roundtrip-gives-same-result2)

(comment
  ;; XXX If I do this as a defspec, not sure how to test in the repl
  (defspec roundtrip-gives-same-result
    20
    (prop/for-all [s (t-gen/structured)]
                  (= s (roundtrip s))))

  (roundtrip-gives-same-result)

  ;; This defspec fails too.
  (defspec first-element-is-min-after-sorting ;; the name of the test
    100 ;; the number of iterations for test.check to test
    (prop/for-all [v (gen/such-that not-empty (gen/vector gen/int))]
                  (= (apply min v)
                     (first (sort v)))))

  (first-element-is-min-after-sorting))
