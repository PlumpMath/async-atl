(ns async-atl.util)

(def display (.getElementById js/document "app"))

(defn log [msg]
    (set! (.-innerHTML display) (+ (.-innerHTML display) (+ msg "<br />"))))

(defn clear []
  (set! (.-innerHTML display) ""))
