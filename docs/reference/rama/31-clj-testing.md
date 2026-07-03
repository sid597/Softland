# Clojure: Testing
> Source: https://redplanetlabs.com/docs/~/clj-testing.html
> Local Rama reference — raw extraction from Red Planet Labs documentation

# Testing

Rama modules can be easily tested using a built-in facility called [InProcessCluster](https://redplanetlabs.com/javadoc/com/rpl/rama/test/InProcessCluster.html). The [com.rpl.rama.test](https://redplanetlabs.com/clojuredoc/com.rpl.rama.test.html) package provides a nice Clojure API to using `InProcessCluster` and other testing facilities. See the [main page on testing](testing.html) for complete details on testing facilities available, which all have corresponding analogues in `com.rpl.rama.test`. The [intro blog post](https://blog.redplanetlabs.com/2023/10/11/introducing-ramas-clojure-api/) and [rama-demo-gallery](https://github.com/redplanetlabs/rama-demo-gallery) also have many examples of using `InProcessCluster` through the Clojure API.

You can also unit test `deframafn` and `deframaop` that operate on PStates using the [create-test-pstate](https://redplanetlabs.com/clojuredoc/com.rpl.rama.test.html#var-create-test-pstate) facility in `com.rpl.rama.test`. Here’s an example of using this:

```java
(use 'com.rpl.rama)
(use 'com.rpl.rama.path)
(require '[com.rpl.rama.test :as rtest])

(deframafn foo-op [$$p]
  (local-transform> [:a "b" (term inc)] $$p)
  (:>))

(with-open [tp (rtest/create-test-pstate
                 {clojure.lang.Keyword (map-schema String
                                                   Long
                                                   {:subindex? true})})]
  (rtest/test-pstate-transform [:a "b" (termval 10)] tp)
  (println "Initial:" (rtest/test-pstate-select-one [:a "b"] tp))
  (foo-op tp)
  (println "After one call:" (rtest/test-pstate-select-one [:a "b"] tp))
  (foo-op tp)
  (println "After two calls:" (rtest/test-pstate-select-one [:a "b"] tp)))
```

Running this prints:

```java
Initial: 10
After one call: 11
After two calls: 12
```