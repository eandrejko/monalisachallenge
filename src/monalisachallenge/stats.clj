(ns monalisachallenge.stats)

(defn urand
  "uniform random number betwen a and b"
  [a b]
  (+ a (* (rand) (- b a))))

(defn random-normal
  "generates a random normal deviate of meanu 0 and variance var"
  [var]
  (let [u (rand)
        v (rand)]
    (* var
       (Math/sqrt
        (* -2 (Math/log u)))
       (Math/cos (* 2 Math/PI v)))))

(defn add-int-noise
  [var x]
  (Math/round (+ x (random-normal var))))

(defn add-noise
  [var x]
  (+ x (random-normal var)))

(defn clamp
  "clamps x between a and b"
  [x a b]
  (min (max x a) b))
