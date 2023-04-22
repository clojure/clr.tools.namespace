(ns clojure.tools.namespace.track-test
  (:refer-clojure :exclude (remove))
  (:use clojure.test
        clojure.tools.namespace.track))
		
(deftest t-tracker
  (let [x (tracker)]
    (is (empty? x))
    (is (map? x))))
  
(defn- equal-dep-map? [x load unload dependencies depenendents]
  (= x {:clojure.tools.namespace.track/load   load
        :clojure.tools.namespace.track/unload unload
		:clojure.tools.namespace.track/deps  (clojure.tools.namespace.dependency.MapDependencyGraph. dependencies depenendents)}))
		
  
(deftest t-add 
  (let [y (add (tracker) '{ a #{b} b #{c d}})
        w (add y '{a #{c} b #{e} d #{f g}})]
	(is (equal-dep-map? y '(b a) '(a b) '{b #{c d}, a #{b}} '{d #{b}, c #{b}, b #{a}}))                  ;;; ordering diff on unload: '(b a)
	(is (equal-dep-map? w '(a b d) '(a b d) '{d #{f g}, b #{e}, a #{c}} '{g #{d}, f #{d}, e #{b}, d #{b}, c #{b a}, b #{a}}))))  ;; ordering diff on unload: '(a d b))
	
(deftest t-remove
  (let [y (add (tracker) '{ a #{b} b #{c d}})
        z (remove y '(d))
		w (remove y '(q))]
	#_(is (equal-dep-map? z  '(b a)  '(a b d) '{b #{c d}, a #{b}} '{d #{b}, c #{b}, b #{a}}))  ;; with the TNS-6 change, shouldn't this test change?
	(is (equal-dep-map? z  '(b a)  '(a b d) '{b #{c}, a #{b}} '{c #{b}, b #{a}}))
	(is (= y w))))
	
	