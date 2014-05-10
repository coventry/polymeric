(ns graft
  (:require [clojure.walk :as w]))

(def id-prop "__cljs_graft_id__")

(defn get-id! [obj]
  (System/identityHashCode obj)
  #_(if-let [id (aget obj id-prop)]
    id
    (aset obj id-prop (gensym id-prop))))

(defn get-duplicates [obj]
  (let [duplicates (atom nil)
        seen (atom #{})
        add-ref!   #(let [id (get-id! %)]
                      (if (not (@seen id))
                        (do (swap! seen conj id)
                            ;; Continue the traversal
                            %)
                        (do (swap! duplicates assoc id %)
                            ;; Don't follow this node further
                            nil)))]
    (w/prewalk #(if (coll? %) (add-ref! %)) obj)
    @duplicates))

(defn prune-duplicates [obj duplicates]
  (let [prune #(let [id (get-id! %)] (if (duplicates id) id %))]
    (w/prewalk prune obj)))

(defn graft-duplicates [obj duplicates]
  (w/prewalk-replace duplicates obj))

(def eg (slurp "/tmp/eg.txt"))

(def d (get-duplicates eg))

(def p (prune-duplicates eg d))
