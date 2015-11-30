(ns clj-rad.core
  (:require [clojure.set                :refer [difference]]
            [clojure.math.numeric-tower :as math]))

(def days-per-week 7)
(def LPENALTY_DEFAULT_NO_DIFF 1.0)
(def SPENALTY_DEFAULT_NO_DIFF 1.4)

(defn- lpenalty []
  LPENALTY_DEFAULT_NO_DIFF)

(defn- spenalty [n-days nrows]
  (/ SPENALTY_DEFAULT_NO_DIFF (math/sqrt (max n-days (/ nrows n-days)))))

(defn- unique-dates
  [data]
  (into #{} (map :date data)))

(defn- pad-missing-dates
  [complete-dateset data]
  (->> data
       unique-dates
       (difference complete-dateset)
       (map #(hash-map :date % :value 0))
       (into data)))

(defn- prepare
  [date-set data]
  (->> data
      (pad-missing-dates date-set)
      (sort-by :date)
      (mapv :value)))

(defn- statistics
  [data]
  (let [n (count data)
        mean (/ (reduce + data) n)
        variance (/ (reduce + (map #(math/expt (- % mean) 2) data)) (dec n))
        stdev (math/sqrt variance)]
    {:mean mean :stdev stdev}))

(defn- normalise [mean stdev]
  (fn [x] (/ (- x mean) stdev)))

(defn- un-normalise [mean stdev]
  (fn [x] (+ (* x stdev) mean)))

(defn- transform
  [{:keys [mean stdev]} data]
    (mapv (normalise mean stdev) data))

(defn- to-matrix
  [ncols data]
  (into-array (map double-array (partition-all ncols data))))

(defn- untransform
  [{:keys [mean stdev]} {:keys [raw-data low-rank-approximation sparse error] :as data}]
  (merge data {:raw-data               (mapv (un-normalise mean stdev) raw-data)
               :low-rank-approximation (mapv (un-normalise mean stdev) low-rank-approximation)
               :sparse                 (mapv #(* stdev %) sparse)
               :error                  (mapv #(* stdev %) error)}))

(defn rpca
  [n-days nrows ncols data]
  (let [rsvd (org.surus.math.RPCA. (to-matrix ncols data) (lpenalty) (spenalty n-days nrows))
        unroll (fn [d] (for [row (.getData d) col row] col))]
    {:raw-data               data
     :low-rank-approximation (unroll (.. rsvd getL))
     :sparse                 (unroll (.. rsvd getS))
     :error                  (unroll (.. rsvd getE))}))

(defn rad [n-days complete-dateset data]
  (let [clean-data (prepare complete-dateset data)
        stats (statistics clean-data)]
    (->> clean-data
         (transform stats)
         (rpca n-days (/ n-days days-per-week) days-per-week)
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
