# clj-rad

A clojure wrapper of the Netflix Surus Robust Anamoly Detection (RAD) https://github.com/Netflix/Surus

## Installing

Add the following to your [Leiningen](http://github.com/technomancy/leiningen) `project.clj`:

![latest clj-kafka version](https://clojars.org/clj-rad/latest-version.svg)

## Usage

### Code

```clojure
(require '[clj-rad.core :as rad])

(def data [{:date 20150101 :a 1 :b 1 :value 14}
{:date 20150102 :a 1 :b 2 :value 35}
{:date 20150103 :a 1 :b 1 :value 12}
{:date 20150104 :a 1 :b 1 :value 10}
{:date 20150105 :a 1 :b 1 :value 13}
{:date 20150106 :a 1 :b 1 :value 9}
{:date 20150107 :a 1 :b 1 :value 15}])

(rad/rpca-outliers-daily data 7 [:a :b])
; {:group                  {:a 1 :b 1}
;  :raw-data               [...]
;  :low-rank-approximation [...]
;  :sparse                 [...]
;  :error                  [...]}
```

### Analysis

The result returned contains a group and 4 timeseries collections. Each could be plotted on the same timeseries chart.

* `:raw-data` this is the original time series data for this group
* `:low-rank-approximation` this is an approximation of the raw time series data
* `:sparse` these are the outliers
* `:error` this is the acceptable error due to random variance

At each point in time the oringal data value (X) should be decomposable to it's low rank approximation (L) and it's sparse (S) components, allowing for the error i (E) value.

Such that `X=L+S+E`

## License

Copyright © 2015 Christian Blunden

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
