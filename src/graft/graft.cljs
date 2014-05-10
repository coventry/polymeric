(ns graft
  (:require [clojure.walk :as w]))

(def id-prop "__cljs_graft_id__")

(defn get-id! [obj]
  (if-let [id (aget obj id-prop)]
    id
    (aset obj id-prop (gensym id-prop))))

(defn add-ref! [duplicates seen ref]
  (let [id (get-id! ref)]
    (if (not (@seen id))
      (do (swap! seen conj id)
          ;; Continue the traversal
          ref)
      (do (swap! duplicates assoc id ref)
          ;; Don't follow this node further
          nil))))

(defn get-duplicates [obj]
  (let [duplicates (atom nil)
        seen (atom #{})
        add-ref! (partial add-ref! duplicates seen)]
    (w/prewalk #(when (coll? %) (add-ref! %)) obj)
    @duplicates))

(defn prune-duplicates [obj duplicates]
  (let [prune #(let [id (get-id! %)] (if (duplicates id) id %))]
    (w/prewalk prune obj)))

(defn graft-duplicates [obj duplicates]
  (w/prewalk-replace duplicates obj))
