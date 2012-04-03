(ns monalisachallenge.test.rendering
  (:use [monalisachallenge.rendering]
        [monalisachallenge.representation]
        [midje.sweet]))

(defn valid-rendering?
  [img]
  (and (= (:width default-params) (.getWidth img))
       (= (:height default-params) (.getHeight img))))

(fact "renders representation"
  (render-representation (random-representation default-params)) => valid-rendering?)