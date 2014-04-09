(ns async-atl.core
  (:require-macros [cljs.core.async.macros
                    :refer [go]])
  (:require [cljs.core.async
             :refer [chan >! <! put!
                     alts! timeout close!]
             :as async]
            [async-atl.util
             :refer [clear log get-element]]))


;; core.async does communication over channels.


;; writing to a channel blocks unless there is a reader
(let [c (chan)]
  (go
   (log "We got here")
   (>! c :val)
   (log "We won't get here")))

(clear)

;; Separate blocks communicate over shared channels
(let [c (chan)]
  (go
   (log "About to send")
   (>! c :val)
   (log "Just sent"))
  (go
   (log "About to receive")
   (log (str  "I received " (<! c)))))

(clear)

;; writing and reading can be done in any order
(let [c (chan)]
  (go
   (log "About to receive")
   (log (str "I received " (<! c))))
  (go
   (log "About to send")
   (>! c :val)
   (log "Just sent")))

(clear)

;; timeouts can be used to delay things
(let [c (chan)]
(go
 (>! c "wait for it..."))
(go
 (<! (timeout 2000))
 (log (<! c))))

;; use alts! to implement a timeout
(let [c (chan)
      t (timeout 5000)]
  (go
   (let [[val port] (alts! [c t])]
     (if (= port t)
       (log "operation timed out")
       (log val)))))

(clear)

;; timeouts and loops can be used to create timers
(let [c (chan)]
  (go
   (while true
     (log (<! c))))
  (go
   (dotimes [n 10]
     (<! (timeout 1000))
     (>! c n))))

;; events can be put on channels
;; This code copied from David Nolen's core.async demo
;; https://github.com/swannodette/hs-async
;; https://www.youtube.com/watch?v=AhxcGGeh5ho
(defn events [el type]
  (let [out (chan)]
    (.addEventListener el type
      (fn [e] (put! out e)))
    out))

(let [click (events (get-element "clickme")
                    "click")]
  (go (while true
        (<! click)
        (clear))))

(let [click (events (get-element "clickme") "click")]
  (go
       (<! click)
       (.alert js/window
                "this is another event listener")))


;; A stock ticker demo.
(def stocks [ ;; symbol min-interval starting-price
             ["AAPL" 1800 537 ]
             ["AMZN" 3800 345]
             ["GOOG" 5100 1127]
             ["MSFT" 8300 40]
             ["RHT"  3200 53]])

(defn adjust-price [old-price]
  (let  [adjustment (- (rand-int 6) 3)]
    (+ old-price adjustment)))


;; channel must be created outside the loop
;; timeout must be created in the loop
(defn make-ticker [symbol t start-price]
  (let [c (chan)]
    (go
     (loop [price start-price]
       (let [new-price (adjust-price price)]
         (<! (timeout t))
         (>! c {:symbol symbol :price new-price})
         (recur new-price))))
    c))

(defn run-sim []
  (let [ticker (async/merge
                (map #(apply make-ticker %)
                     stocks))]
    (go
     (loop [x 0]
       (when (< x 20)
         (log (str x "-" (<! ticker)))
         (recur (inc x)))))))

(do
  (clear)
  (run-sim))

(clear)

;; create a channel from a collection
(defn my-ints []
  (async/to-chan (range 10)))

(let [c (my-ints)]
  (go
   (dotimes [n 10]
     (log (<! c)))))

;; leaving in one of my mistakes:
;; when my-ints is exhausted, the channel is closed
;; reading from a closed channel returns nil immediately
#_(let [c (my-ints)]
  (go
   (while true
     (log (<! c)))))

;; we can map and filter and reduce
(let [c (my-ints)
      c2 (async/map< inc c)]
  (go
   (dotimes [n 10]
   (log (<! c2)))))

(clear)

(let [c (my-ints)]
  (go
   (log (<! (async/reduce + 0 c)))))

(let [c (my-ints)
      f (async/filter< odd? c)]
  (go
   (log (<! (async/reduce + 0 f)))))

;; There is no reason to write your own
;; filter, but if you want to...
(defn my-filter [pred c]
  (let [out (chan)]
    (go
     (loop []
       (let [val (<! c)]
         (cond
          (nil? val) (close! out)
          (pred val)
          (do
            (>! out val)
            (recur))
          :else (recur)))))
    out))

(let [c (my-ints)
      f (my-filter odd? c)]
  (go
   (log (<! (async/reduce + 0 f)))))

;; buffers allow us to write values without
;; waiting for a reader
(let [c (chan 1)]
  (go
   (>! c 1)
   (>! c 2)
   (>! c 3)
   (log "Put all my values on the channel"))
  (go
   (log (<! c))))

;; the dropping buffer drops
;; new values when it is full
(let [c (chan (async/dropping-buffer 1))]
  (go
   (>! c 1)
   (>! c 2)
   (>! c 3)
   (log "Put all my values on the channel"))
  (go
   (log (<! c))
   (log (<! c))))

;; the sliding buffer drops the oldest
;; values when new values are added
(let [c (chan (async/sliding-buffer 1))]
  (go
   (>! c 1)
   (>! c 2)
   (>! c 3)
   (log "Put all my values on the channel"))
  (go
   (log (<! c))))

;; the same channel can be written to from
;; different blocks of code.
(let [c (chan)]
  (go
   (>! c 1))
  (go
   (>! c 2))
  (go
   (log (<! c))
   (log (<! c))))
