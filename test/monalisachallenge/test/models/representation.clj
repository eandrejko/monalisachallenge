(ns monalisachallenge.test.models.representation
  (:use [monalisachallenge.rendering]
        [monalisachallenge.representation]
        [monalisachallenge.models.representation]
        [midje.sweet]))

(fact "representation is zero distance from self" 
  (let [r (random-representation monalisachallenge.representation/default-params)
        r (render-representation r)]
    (score-representation r r) => 0.0))

(fact "representation is non-zero distance from others" 
  (let [r (-> monalisachallenge.representation/default-params random-representation render-representation)
        s (-> monalisachallenge.representation/default-params random-representation render-representation)]
    (score-representation r s) => (fn [z] (< 0.0 z))))