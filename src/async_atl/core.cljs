(ns async-atl.core
  (:require-macros [cljs.core.async.macros
                    :refer [go]])
  (:require [cljs.core.async
             :refer [chan <! >!]]))

(def display (.getElementById js/document "app"))

(defn log [msg]
    (set! (.-innerHTML display) (+ (.-innerHTML display) (+ msg "<br />"))))

(defn clear []
  (set! (.-innerHTML display) ""))

(let [c (chan)]
  (go
   (log "We got here")
   (>! c :val)
   (log "We won't get here")))

(clear)

(let [c (chan)]
  (go
   (log "About to send")
   (>! c :val)
   (log "Just sent"))
  (go
   (log "About to receive")
   (log (str  "I received" (<! c)))))

(clear)
