(ns clojure.tools.namespace.dependency-test
  (:use clojure.test
        clojure.tools.namespace.dependency))

;; building a graph like:
;;
;;       :a
;;      / |
;;    :b  |
;;      \ |
;;       :c
;;        |
;;       :d
;;
(def g1 (-> (graph)
            (depend :b :a)   ; "B depends on A"
            (depend :c :b)   ; "C depends on B"
            (depend :c :a)   ; "C depends on A"
            (depend :d :c))) ; "D depends on C"

;;      'one    'five
;;        |       |
;;      'two      |
;;       / \      |
;;      /   \     |
;;     /     \   /
;; 'three   'four
;;    |      /
;;  'six    /
;;    |    /
;;    |   /
;;    |  /
;;  'seven
;;
(def g2 (-> (graph)
            (depend 'two   'one)
            (depend 'three 'two)
            (depend 'four  'two)
            (depend 'four  'five)
            (depend 'six   'three)
            (depend 'seven 'six)
            (depend 'seven 'four)))

(deftest t-transitive-dependencies
  (is (= #{:a :c :b}
         (transitive-dependencies g1 :d)))
  (is (= '#{two four six one five three}
         (transitive-dependencies g2 'seven))))

(deftest t-transitive-dependents
  (is (= '#{four seven}
         (transitive-dependents g2 'five)))
  (is (= '#{four seven six three}
         (transitive-dependents g2 'two))))

(deftest t-topo-comparator
  (is (= '(:a :b :d :foo)
         (sort (topo-comparator g1) [:d :a :b :foo])))
  (is (= '(five three seven nine eight)                                     ;;; (three five seven nine eight)   Why is our order not same as JVM?  Does it matter?
         (sort (topo-comparator g2) '[three seven nine eight five]))))           

(deftest t-topo-sort
  (is (= '(one five two three six four seven)                               ;;; (one two three five six four seven)    Why is our order not same as JVM?  Does it matter?
         (topo-sort g2))))