(ns clj-rad.core
  (:require [clojure.math.numeric-tower :as math]))

(def days-per-week 7)
(def LPENALTY_DEFAULT_NO_DIFF 1.0)
(def SPENALTY_DEFAULT_NO_DIFF 1.4)

(defn lpenalty []
  LPENALTY_DEFAULT_NO_DIFF)

(defn spenalty [ncols nrows]
  (/ SPENALTY_DEFAULT_NO_DIFF (math/sqrt (max ncols nrows))))

(defn unique-dates 
  [data]
  (into #{} (map :date data)))

(defn pad-missing-dates
  [complete-dateset data]
  (->> data
       unique-dates
       (clojure.set/difference complete-dateset)
       (map #(hash-map :date % :value 0))
       (into data)))

(defn prepare 
  [date-set data]
  (->> data
      (pad-missing-dates date-set)
      (sort-by :date)
      (mapv :value)))

(defn statistics 
  [data]
  (let [n (count data)
        mean (/ (reduce + data) n)
        variance (/ (reduce + (map #(math/expt (- % mean) 2) data)) (dec n))
        stdev (math/sqrt variance)]
    {:mean mean :stdev stdev}))

(defn transform 
  [{:keys [mean stdev]} data]
    (mapv #(/ (- % mean) stdev) data))

(defn to-matrix 
  [ncols data]
  (into-array (map double-array (partition-all ncols data))))

(defn rpca 
  [nrows ncols data]
  (let [rsvd (org.surus.math.RPCA. (to-matrix ncols data) (lpenalty) (spenalty nrows ncols))
        unroll (fn [d] (for [row (.getData d) col row] col))]
    {:x-transform data
     :rsvd-l (unroll (.. rsvd getL))
     :rsvd-s (unroll (.. rsvd getS))
     :rsvd-e (unroll (.. rsvd getE))}))

(defn untransform
  [{:keys [mean stdev]} {:keys [rsvd-l rsvd-s rsvd-e] :as data}]
  (merge data {:rsvd-l (map #(+ mean (* stdev %)) rsvd-l)
               :rsvd-s (map #(* stdev %) rsvd-s)
               :rsvd-e (map #(* stdev %) rsvd-e)}))

(defn rad [n-days complete-dateset data]
  (let [clean-data (prepare complete-dateset data)
        stats (statistics clean-data)]
    (->> clean-data
         (transform stats)
         (rpca (/ n-days days-per-week) days-per-week)
         (untransform stats))))

(defn rpca-outliers-daily
  [data n-days group-columns]
  """
  expects that you have already aggregated the data at a daily grain, performed a 'group by' on the keys of interest.
  The date column must always be labeled :date and daterange must be a multiple of 7 days ie: 7,14,21,28..
  Example: you could imagine data with the following structure
  where it has been grouped by :a :b and the aggregate data is :value
    [{:date 20150101 :a 1 :b 1 :value 10}
     {:date 20150101 :a 1 :b 2 :value 20}
     {:date 20150102 :a 1 :b 1 :value 11}
     {:date 20150102 :a 1 :b 2 :value 12}]
  """
  (let [complete-dateset (unique-dates data)]
    (for [[group group-data] (group-by #(select-keys % group-columns) data)]
      (merge (rad n-days complete-dateset group-data) {:group group}))))
