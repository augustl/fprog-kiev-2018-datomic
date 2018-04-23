(ns user)


(comment
  ;; We need to be able to call the Datomic APIs

  (require '[datomic.api :as d])

  )

(comment
  ;; First step is to initialize the database
  (d/create-database "datomic:mem://localhost:4334/kiev-fprog")

  )

(comment
  ;; Then we connect
  (def conn (d/connect "datomic:mem://localhost:4334/kiev-fprog"))

  )

(comment
  ;; This will fail, no schema
  @(d/transact conn [[:db/add "1" :person/name "August Lilleaas"]
                     [:db/add "1" :person/age 31]])

  )

(comment
  ;; This is equivalent to the above statement - convenient API
  @(d/transact conn [{:db/id "1"
                      :person/name "August Lilleaas"
                      :person/age 31}])

  )

(comment
  ;; Define our schema
  @(d/transact conn [{:db/ident :person/name
                      :db/valueType :db.type/string
                      :db/cardinality :db.cardinality/one}

                     {:db/ident :person/age
                      :db/valueType :db.type/long
                      :db/cardinality :db.cardinality/one}])

  )

(comment
  ;; This should work, we have a schema now
  (def tx-res-1 @(d/transact conn [[:db/add "1" :person/name "August Lilleaas"]
                                   [:db/add "1" :person/age 31]]))

  )

(comment
  ;; Get a reference to the database for queries
  (def db-1 (d/db conn))

  )

(comment
  ;; Let's get the entity!
  (-> (d/entity db-1 (get-in tx-res-1 [:tempids "1"]))
      (d/touch))

  )

(comment
  ;; Let's run a query!
  (d/q '[:find ?e
         :where
         [?e :person/name "August Lilleaas"]]
       db-1)

  )

(comment
  ;; Let's run a parameterized query!
  (d/q '[:find ?e
         :in $ ?n
         :where
         [?e :person/name ?n]]
       db-1
       "August Lilleaas")

  )

(comment
  ;; Store entity id
  (def august-id (d/q '[:find ?e .
                        :in $ ?n
                        :where
                        [?e :person/name ?n]]
                      db-1
                      "August Lilleaas"))

  )

(comment
  ;; Query using the entity id, and see what a and v is
  (d/q '[:find ?a ?v
         :in $ ?e
         :where
         [?e ?a ?v]]
       db-1
       august-id)

  )

(comment
  ;; Get a only
  (d/q '[:find ?a
         :in $ ?e
         :where
         [?e ?a]]
       db-1
       august-id)

  )

(comment
  ;; Get a more convenient structure back from the query
  (->> (d/q '[:find [?a ...]
              :in $ ?e
              :where
              [?e ?a ?v]]
            db-1
            august-id))

  )

(comment
  ;; Get the real name. Anything can have a name, not just attributes!
  (->> (d/q '[:find [?a ...]
              :in $ ?e
              :where
              [?e ?a ?v]]
            db-1
            august-id)
       (map #(d/ident db-1 %)))

  )

(comment
  ;; Convenience api. Touch is not needed, but entity is lazy, touch realizes it
  (-> (d/entity db-1 august-id)
      (d/touch))

  )

(comment
  ;; Update my age
  (def tx-res-2 @(d/transact conn [[:db/add august-id :person/age 32]]))

  )

(comment
  ;; wat
  (-> (d/entity db-1 august-id)
      (d/touch))

  )

(comment
  ;; Get new db value
  (def db-2 (d/db conn))

  )

(comment
  ;; phew
  (-> (d/entity db-2 august-id)
      (d/touch))

  )

(comment
  (-> (d/entity (:db-after tx-res-1) august-id)
      (d/touch))

  )

(comment
  (-> (d/entity (:db-before tx-res-1) august-id)
      (d/touch))

  )

(comment
  (-> (d/entity (:db-before tx-res-2) august-id)
      (d/touch))

  )

(comment
  (-> (d/entity (:db-after tx-res-2) august-id)
      (d/touch))

  )

(comment
  ;; Add cardinality/many
  @(d/transact conn [{:db/ident :person/favorite-foods
                      :db/valueType :db.type/string
                      :db/cardinality :db.cardinality/many}])

  )

(comment
  ;; <3 pizza (not really though)
  @(d/transact conn [[:db/add august-id :person/favorite-foods "pizza"]])

  )

(comment
  ;; We get the most recent db directly.
  ;; It's a set!
  (-> (d/entity (d/db conn) august-id)
      (d/touch))

  )

(comment
  ;; More food
  @(d/transact conn [[:db/add august-id :person/favorite-foods "eggs"]
                     [:db/add august-id :person/favorite-foods "pasta"]])

  )

(comment
  ;; They're all there!
  (-> (d/entity (d/db conn) august-id)
      (d/touch))

  )

(comment
  ;; Less food
  @(d/transact conn [[:db/retract august-id :person/favorite-foods "eggs"]])

  )

(comment
  ;; Eggs are gone
  (-> (d/entity (d/db conn) august-id)
      (d/touch))

  )

(comment
  ;; Store a reference to most recent db
  (def db-3 (d/db conn))

  )

(comment
  ;; Query the history of the database
  (d/q '[:find ?a ?v ?t
         :in $ ?e
         :where
         [?e ?a ?v ?t]]
       (d/history db-3)
       august-id)

  )

(comment
  ;; Prettier output
  (->> (d/q '[:find ?a ?v ?t
              :in $ ?e
              :where
              [?e ?a ?v ?t true]]
            (d/history db-3)
            august-id)
       (map (fn [[a v t]] {:attr (d/ident db-2 a) :val v :tx t}))
       (group-by :tx))

  )

(comment
  ;; Transactions are entities!
  (->> (d/q '[:find ?a ?v ?t
              :in $ ?e
              :where
              [?e ?a ?v ?t true]]
            (d/history db-3)
            august-id)
       (map (fn [[a v t]] {:attr (d/ident db-2 a) :val v :tx (d/touch (d/entity db-3 t))}))
       (group-by #(get-in % [:tx :db/id])))

  )

(comment
  ;; Not just dbs
  (d/q '[:find ?e
         :where
         [?e :color "red"]]
       [[1 :color "blue"]
        [2 :color "red"]
        [3 :color "yellow"]
        [4 :color "red"]])

  )

(comment
  ;; Various aggregates are supported
  ;; Still useful even though query engine is local
  (d/q '[:find (count ?e) .
         :where
         [?e :color "red"]]
       [[1 :color "blue"]
        [2 :color "red"]
        [3 :color "yellow"]
        [4 :color "red"]])

  )

(comment
  ;; Join between database and non-database
  (d/q '[:find ?company-name
         :in $db $x ?name
         :where
         [$db ?p-e :person/name ?name]
         [$x ?c-e :company/name ?company-name]]
       db-3
       [[august-id :person/company 123]
        [123 :company/name "Kodemaker"]]
       "August Lilleaas")
  )|