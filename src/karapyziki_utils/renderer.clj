(ns karapyziki-utils.renderer
  (:require [clojure.walk :as walk]))

; from https://gist.github.com/xavi/3729307

(defn replace-symbol
  [form old-sym-name new-sym-name]
  (walk/postwalk
    #(if (and (symbol? %)
              (= (name %) (name old-sym-name)))
        (symbol new-sym-name)
        %)
    form))

(defn compile-template
  [ss sym-name]
  (let [ss (->> ss
              (map #(if (and (symbol? %) (not= (name %) "%")) (eval %) %))
              (map #(if (string? %)
                      %
                      (replace-symbol % "%" sym-name)))
              (partition-by string?)
              (map #(if (string? (first %)) (list (apply str %)) %))
              (apply concat))]
    (if (= (count ss) 1) (first ss) (cons 'str ss))))

(defmacro renderer-fn
  [& ss]
  (let [m (gensym "m")]
    `(fn [& [~m]] ~(compile-template ss (name m)))))
