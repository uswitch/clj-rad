# clj-rad

A clojure wrapper of the Netflix Surus Robust Anamoly Detection (RAD) https://github.com/Netflix/Surus

## Installing

Not yet released to clojars

## Usage

```clojure
(:require [clj-rad.core :as rad])

(def data [{:date 20150101 :a 1 :b 1 :value 14}
{:date 20150102 :a 1 :b 1 :value 35}
{:date 20150103 :a 1 :b 1 :value 12}
{:date 20150104 :a 1 :b 1 :value 10}
{:date 20150105 :a 1 :b 1 :value 13}
{:date 20150106 :a 1 :b 1 :value 9}
{:date 20150107 :a 1 :b 1 :value 15}])

(rad/rpca-outliers-daily data 7 [:a :b])
```

## License

Copyright © 2015 Christian Blunden

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
