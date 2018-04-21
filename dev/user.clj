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
  ;;
  (d/q '[:find ?a ?v
         :in $ ?e
         :where
         [?e ?a ?v]]
       db-1
       august-id)

  )

(comment
  (d/q '[:find ?a
         :in $ ?e
         :where
         [?e ?a]]
       db-1
       august-id)

  )

(comment
  (->> (d/q '[:find [?a ...]
              :in $ ?e
              :where
              [?e ?a ?v]]
            db-1
            august-id))

  )

(comment
  (->> (d/q '[:find [?a ...]
              :in $ ?e
              :where
              [?e ?a ?v]]
            db-1
            august-id)
       (map #(d/ident db-1 %)))

  )

(comment
  (-> (d/entity db-1 august-id)
      (d/touch))

  )

(comment
  (def tx-res-2 @(d/transact conn [[:db/add august-id :person/age 32]]))

  )

(comment
  (-> (d/entity db-1 august-id)
      (d/touch))

  )

(comment
  (def db-2 (d/db conn))

  )

(comment
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
  (d/q '[:find ?a ?v ?t
         :in $ ?e
         :where
         [?e ?a ?v ?t true]]
       (d/history db-2)
       august-id)

  )

(comment
  (->> (d/q '[:find ?a ?v ?t
              :in $ ?e
              :where
              [?e ?a ?v ?t true]]
            (d/history db-2)
            august-id)
       (map (fn [[a v t]] {:attr (d/ident db-2 a) :val v :tx t}))
       (group-by :tx))

  )

(comment
  (d/q '[:find ?e
         :where
         [?e :color "red"]]
       [[1 :color "blue"]
        [2 :color "red"]
        [3 :color "yellow"]
        [4 :color "red"]])

  )

(comment
  (d/q '[:find (count ?e) .
         :where
         [?e :color "red"]]
       [[1 :color "blue"]
        [2 :color "red"]
        [3 :color "yellow"]
        [4 :color "red"]])

  )


(comment
  (d/q '[:find ?company-name
         :in $db $x ?name
         :where
         [$db ?p-e :person/name ?name]
         [$x ?c-e :company/name ?company-name]]
       db-2
       [[august-id :person/company 123]
        [123 :company/name "Kodemaker"]]
       "August Lilleaas")
  )