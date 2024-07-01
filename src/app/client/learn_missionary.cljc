(ns app.client.learn-missionary)



;; - Flow:
;;   ?< ?= ?> latest none relieve sample
;;
;; - Discrete:
;;   ap buffer eduction group-by observe recudcions seed subscribe zip
;;   reduce
;;
;; - Continious
;;   cp watch
;;
;; - Task:
;;   ? absolve attempt compel join race rdv sleep sp timeout via
;;
;; - function:
;;   ! ?! ?? blk cpu dfv mbx sem
;;
;; - form:
;;   amd amb= amb>
;;
;; - body
;;   holding
;;
;; - publishers:
;;   memo stream signal publisher


;; Create a task

;; Task is async by nature.
(comment
  (m/sleep 2)
  (m/sp (println "Is this a task?"))
  (m/sp 10))

;; https://claude.ai/chat/73c373ad-f3d6-4fec-ad57-d80635664d7e

;; Run a task

(comment
  (m/? (m/sleep 2))
  (m/? (m/sp (println "Is this a task?")))
  (m/? (m/sp 10)))


;; Create a flow

;; A process able to produce arbitrary number of values before terminating. Async under the hood.
;; Build a flow from
;; a collection

(comment
  (m/seed (range 10))
  (m/seed [1 2 3 4 5]))

;; This will emit the values its being passed, now we can convert the values to a task and then read it.
;; We can also use ap which is similar to sp.
;; sp
;; - Used to create a task
;; - Used to wrap code to make it sequential. i.e sequential composition. You can make tasks and then
;;   sequence them in bigger scope.

;; ap
;; - Used to create a flow
;; - Can be used to make sequential flow just like sp, but it has more it can be used to create.  sequential process
;; - Ambiguous evaluation. ambiguous process

;; Read values from a flow

(comment
  (->> [1 2 3]
    (m/seed)         ;; Convert the collection to a flow
    (m/?>)           ;; For each of the result in collection take it and continue with rest of calculation.
    (println)        ;; This is where the action will happen
    (m/ap)           ;; Wrapper for the ambiguous process
    (m/reduce conj)  ;; Converting the flow to a task
    (m/?)))          ;; Run the task, from my understanding this is the place which tells the process to run and turn
;; into a program (running instantiation of a process)
;; AM I understanding it correctly? This make it seem that ap is lazy and so is seed, they don't produce until called?
;; The doc says `cp` is lazy and only works when called
(comment
  (->> [1 2 3]
    (m/seed)         ;; Convert the collection to a flow
    (m/?<)           ;; For each of the result in collection take it and continue with rest of calculation.
    (println)        ;; This is where the action will happen
    (m/cp)))


;; Wonder if this is it do I have to convert a flow value in to a task and then be able to read its value or do stuff
;; with it in clojure??
;; Lets test it out without the fork, what can I remove and still be able to run the code

(comment
  (->> [1 2 3]
    (m/seed)         ;; Convert the collection to a flow
    (m/reduce conj)  ;; Converting the flow to a task
    (m/?)))


;; So this is how to obtain the result from a flow, use some function that will output values of the flow.
;; Once you have a value you convert it to a task and run the task to get the final value ???

;; Now I wonder how the reduce is doing it? Can I remove  the reduce and read only one value of the code?
(comment
  (->> [1 2 3]
    (m/seed)         ;; Convert the collection to a flow
    ;; So I can't continue with the following, if I have to get the value of the flow.
    ;; I mean this is to say we just have a bunch of values. what do you want to do with them how would the flow know?
    ;; so It just outputs the seed function which is correct. It is upto me what I want to do with the values.
    ;; Do I want to aggregate them and return the original seed? can do that using (reduce conj )
    ;; Do I want to take another action for each of the values? then we can fork each of the value mold each value and
    ;; perform the action for all the values in the flow.
    ;; DO I want to halt the current flow when new value is available? Drop the current evaluation of the flow value
    ;; and work on the latest one because the old one is not of value to us anymore. This is what we call relieving
    ;; backpressure (I think).
    ;; Or another strategy on what to discard is based on some time, so say "I want only the last value of all the values
    ;; that were put out in the last `x` duration". We can do that using debounce.
    ;; ~~Another thing we can do is to do some concurrent operations on a value, say for each value I want to do something
    ;; in the frontend and something in the backend.~~ WE can use ?> to fork the current evaluation context (I think value)
    ;; from the flow. I understood wrong what is meant by concurrent here, by concurrent it means how many computations
    ;; to do in parallel? i.e how many threads to spawn? each thread will take 1 value from the branch and then work on it.
    (m/sp)
    (m/?)))

(comment
  ;; One at a time.
  (m/? (m/reduce conj (m/ap
                        (let [v (m/?> (m/seed [1 2 3]))]
                          (println "v1" v)
                          (println "v2" v)))))
  ;; Concurrently
  (m/? (m/reduce conj (m/ap
                        (let [v (m/?> 3 (m/seed [1 2 3 4 5 6 7]))]
                          (println "v" v))))))



;; Without reduce, I just want to read one value off the flow, how?

;; So Flows are encoded as function, which are invoked by 2 callbacks.
(comment
  (def d)
  (def s (m/seed [1 2 3 4]))
  (def it (s
            (fn notify [] (println ::Notify))
            (fn notify [] (println ::Cancel))))
  @it
  (it)

  (def !a (atom []))
  (def a (m/watch !a))
  (def bit (a
             (fn notify [] (println ::Notify))
             (fn notify [] (println ::Cancel))))
  @bit
  (reset! !a [1])
  @bit
  (swap! !a conj 3)
  (swap! !a conj 4)
  (swap! !a conj 5)
  (swap! !a conj 6)
  @bit

  (defrecord node-events [action-type node-data event-data])
  (->node-events
    :new-node
    {:rect11 {:id :rect11
              :x 50.99
              :y 60.0
              :type-specific-data {:text "GM Hello"
                                   :width 400
                                   :height 800}
              :type "rect"
              :fill  "lightblue"}}
    {:graph-name :main}))
;; Here's a breakdown of how this works:

;; The Seed.run method returns a Process object. This Process class is an inner class of Seed and has some important characteristics:

;; It implements AFn (making it callable as a function) and IDeref (making it derefable).
;; It holds the iterator for the collection, along with notifier and terminator functions.)
;;
;; (m/seed my-collection) returns a function that, when called with notifier and terminator functions,
;; creates and returns a Process object.
;; The Process object:
;;
;; Can be derefed (@process) to get the next item from the collection.
;; Can be called as a function ((process)) to cancel the iteration.
;;
;;
;; Each time you deref the Process:
;;
;; It returns the next item from the collection.
;; It calls the notifier function if there are more items.
;; It calls the terminator function when the iteration is complete.
;;
;;
;; If you call the Process as a function, it cancels the iteration, preventing further items from being retrieved.

(comment
  (m/seed [1 2 3])
  (m/ap)
  (m/buffer)
  (m/eduction)
  (m/group-by)

  (m/latest (m/seed [1 2 3])) ;; Running inf flows concurrently.
  (m/mbx);; A hunch that this seems to fit somewhere not sure where
  (m/memo) ;; for task Returns a new publisher.
  (m/observe)
  (m/publisher)
  (m/rdv)
  (m/reduction)
  (m/relieve)
  (m/sample)
  (m/seed)
  (m/signal)
  (m/stream)
  (m/subscribe)
  (m/watch)
  (m/zip))

