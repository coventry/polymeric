(ns graft
  (:require [clojure.walk :as w]))

(def id-prop "__cljs_graft_id__")

(defn get-id! [obj]
  "Assign an ID to OBJ as a javascript property if it does not already
  have one.  Used to track identity of substructures during
  compression."
  (if-let [id (aget obj id-prop)]
    id
    (aset obj id-prop (gensym id-prop))))

;; XXX Since I'm using an atom anyway, probably better to make this a
;; set of JS objects which I bash in-place.
(def duplicate-start
  {;; Used to keep track of when we come across something we've seen
   ;; before, so that we can identify duplicates, and TOPSORT the
   ;; dependencies between them.
   :seen {}
   :seen-count 0
   ;; Used to track the actual duplicates
   :duplicates {}})

(defn add-unseen [duplicates id]
  (-> duplicates
      (update-in [:seen] assoc id (:seen-count duplicates))
      (update-in [:seen-count] inc)))

(defn add-ref!* [duplicates ref]
  (let [id (get-id! ref)]
    (if (not (contains? (:seen duplicates) id))
      (add-unseen duplicates id)
      (update-in duplicates [:duplicates] assoc id ref))))

(defn add-ref! [duplicates ref]
  (let [current-count (:seen-count duplicates)
        new-duplicates (swap! duplicates add-ref!* ref)]
    (if (< current-count (:seen-count new-duplicates))
      ;; Was a previously seen object, keep descending tree
      ref)))

(defn get-duplicates [obj]
  (let [duplicates (atom duplicate-start)
        add-ref! (partial add-ref! duplicates)]
    (w/prewalk #(when (coll? %) (add-ref! %)) obj)
    @duplicates))

;; apply to the (key/val swapped) duplicates map itself to deflate it?
(defn prune-duplicates [obj duplicates]
  "Returns OBJ with all instances of substructures whose ids (via
  GET-ID!) are keys in DUPLICATES replaced by corresponding values."
  (let [prune #(let [id (get-id! %)] (if (duplicates id) id %))]
    (w/prewalk prune obj)))

(defn find-grafts [obj duplicates]
  "Walk OBJ, looking for values which are keys in DUPLICATES, and replacing them with ")

(defn reflate* [duplicates reflated id obj seen]
  )

(defn reflate [duplicates reflated id obj]
  (if (contains? reflated id)
    reflated
    (trampoline reflate* duplicates reflated id obj #{})))

(defn reflate-duplicates [duplicates]
  "Recursively replace all instances of ids referred to by vals of
  DUPLICATES with their given values.  SEEN-IDS is used to keep track
  of cyclic dependencies between DUPLICATES... XXX"
  (reduce-kv (partial reflate duplicates) {} duplicates)
  ;; Imperative version, gets me nothing.
  #_(let [reflated (atom nil)]
    (doseq [[id obj] duplicates
            :when (not (contains? @reflated id))
            :let [reflated-obj (reflate duplicates reflated obj)]]
      (swap! reflated assoc id reflated-obj))))

(defn graft-duplicates [obj duplicates]
  (w/prewalk-replace duplicates obj))
