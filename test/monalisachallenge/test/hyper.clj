(ns monalisachallenge.test.hyper
  (:use [monalisachallenge.hyper]
        [midje.sweet]))

(fact "mutate-point-within-limits respects limits"
  (mutate-point-within-limits 1 0.1 [0 3]) => (fn [x] (and (<= x 3) (<= 0 x)))
  (mutate-point-within-limits 0 0.1 [0 3]) => (fn [x] (and (<= x 3) (<= 0 x))))

(fact "mutate sets :last"
  (mutate {:alpha 0.1 :last {:alpha 0.05} :limits {:alpha [0 1]}}) => (fn [x] (= 0.1 (get-in x [:last :alpha]))))

(fact "mutate preserves :limits"
  (mutate {:alpha 0.1 :last {:alpha 0.05} :limits {:alpha [0 1]}}) => (fn [x] (= [0 1] (get-in x [:limits :alpha]))))

(fact "mutate functiosn without :last"
  (mutate {:alpha 0.1 :limits {:alpha [0 1]}}) => (fn [x] (= 0.1 (get-in x [:last :alpha]))))
