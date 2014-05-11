(ns graft
  (:require [clojure.walk :as w]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Tracking object identity

(def id-prop "__cljs_graft_id__")

(defn get-id! [obj]
  "Assign an ID to OBJ as a javascript property if it does not already
  have one.  Used to track identity of substructures during
  compression."
  (if-let [id (aget obj id-prop)]
    id
    (aset obj id-prop (gensym id-prop))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Analyzing identity relationships in object

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
  "Modify the DUPLICATES atom"
  (let [current-count (:seen-count @duplicates)
        new-duplicates (swap! duplicates add-ref!* ref)]
    (if (< current-count (:seen-count new-duplicates))
      ;; Was a previously seen object, keep descending tree
      ref)))

(defn get-duplicates [obj]
  (let [duplicates (atom duplicate-start)
        add-ref! (partial add-ref! duplicates)]
    (w/prewalk #(when (coll? %) (add-ref! %)) obj)
    @duplicates))

(defn maybe-prune-node [duplicates obj]
  (let [id (get-id! obj)]
    (if (contains? duplicates id)
      id
      obj)))

(defn prune-object [obj duplicates]
  "Returns OBJ with all instances of substructures whose ids (via
  GET-ID!) are keys in DUPLICATES replaced by corresponding values."
  (w/prewalk (partial maybe-prune-node (dissoc duplicates (get-id! obj)))
             obj))

(defn prune-duplicates [duplicates]
  "Return a TOPSORTed list of [id pruned-object] pairs from
  DUPLICATES, based on the values in its :seen key"
  (let [{:keys [seen duplicates]} duplicates]
    (for [[id obj] (sort-by (comp seen first) duplicates)]
      [id (prune-object obj duplicates)])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Reflation of objects

(defn reflate [obj duplicates]
  "Walk the reference tree of OBJ, replacing any keys in DUPLICATES
  with corresponding values."
  (w/prewalk-replace duplicates obj))

(defn reflate-duplicates [duplicates]
  "Take a TOPSORTed list of [id obj] pairs, such as produced by
  PRUNE-DUPLICATES, and replace any abbreviations found in them."
  (reduce (fn [m [id obj]]
            (assoc m id (reflate obj m)))
          {}
          (reverse duplicates)))
