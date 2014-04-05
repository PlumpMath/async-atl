(ns async-atl.util)

(defn get-element [id]
  (.getElementById js/document id))

(def display (get-element "app"))

(defn log [msg]
    (set! (.-innerHTML display)
          (+ (.-innerHTML display) (+ msg "<br />"))))

(defn clear []
  (set! (.-innerHTML display) ""))
