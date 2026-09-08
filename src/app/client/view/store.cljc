(ns app.client.view.store
  "The uncommitted store: records by id, saved as they are, and an
   append-only log of every put with the record it replaced. Takes records
   and edits; gives store values. Holds nothing (the browser keeps one atom).
   Evidence: run_test.clj.

   No revision is minted here yet: a put keeps the id and the log keeps the
   previous value. The server's revision rows are not reached from here.")

(defn empty-store [] {:records {} :log []})

(defn put
  "Store and a record with :id → the store holding it, the previous value
   logged."
  [store record]
  (-> store
      (assoc-in [:records (:id record)] record)
      (update :log conj {:op :put :id (:id record) :previous (get-in store [:records (:id record)])})))

(defn record [store id] (get-in store [:records id]))

(defn records
  "Store → every record, in id order, the vector a query loops over."
  [store]
  (vec (sort-by :id (vals (:records store)))))

(defn edit
  "Store, id, function and arguments → the store with that record replaced by
   (f record args…)."
  [store id f & args]
  (put store (apply f (record store id) args)))

(defn append-key
  "Store, a run's id and one keystroke record → the store with the key at the
   end of that run's stream."
  [store run-id key]
  (edit store run-id update :keys (fnil conj []) key))
