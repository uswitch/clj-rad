(ns clj-rad.core
  (:require [clojure.math.numeric-tower :as math]))

(def DAYS-PER-WEEK 7)
(def LPENALTY_DEFAULT_NO_DIFF 1.0)
(def SPENALTY_DEFAULT_NO_DIFF 1.4)

(defn- lpenalty []
  LPENALTY_DEFAULT_NO_DIFF)

(defn- spenalty [n-days nrows]
  (/ SPENALTY_DEFAULT_NO_DIFF (math/sqrt (max n-days (/ nrows n-days)))))

(defn- statistics
  [data]
  (let [n        (count data)
        mean     (/ (reduce + data) n)
        variance (/ (reduce + (map #(math/expt (- % mean) 2) data)) (dec n))
        stdev    (math/sqrt variance)]
    {:mean mean :stdev stdev}))

(defn- normalise [mean stdev]
  (fn [x] (if (zero? stdev) 0 (/ (- x mean) stdev))))

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
  (let [rsvd   (org.surus.math.RPCA. (to-matrix ncols data) (lpenalty) (spenalty n-days nrows))
        unroll (fn [d] (for [row (.getData d) col row] col))]
    {:raw-data               data
     :low-rank-approximation (unroll (.. rsvd getL))
     :sparse                 (unroll (.. rsvd getS))
     :error                  (unroll (.. rsvd getE))}))

(defn rad [n-days data]
  (let [unrolled-data (mapv :value data)
        stats         (statistics unrolled-data)]
    (->> unrolled-data
         (transform stats)
         (rpca n-days (count data) DAYS-PER-WEEK)
         (untransform stats))))

(defn build-padded-dataset [data group-columns]
  (let [data-lookup (group-by #(select-keys % (cons :date group-columns)) data)]
    (for [date  (->> data (map :date) distinct sort)
          group (->> data (group-by #(select-keys % group-columns)) keys)
          :let [group (assoc group :date date)] ]
      (assoc group :value (-> group data-lookup first :value (or 0))))))

(defn rpca-outliers-daily
  [data freq group-columns]
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
  (let [padded-data (build-padded-dataset data group-columns)]
    (for [[group group-data] (group-by #(select-keys % group-columns) padded-data)]
      (merge {:group group} (rad freq group-data)))))
