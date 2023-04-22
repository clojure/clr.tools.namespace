(ns clojure.tools.namespace.reload-test
  (:use clojure.test
        clojure.tools.namespace.reload))
		
(deftest t-remove-lib
   (with-bindings {#'clojure.core/*loaded-libs* (ref '#{a.b c.d})}
     (create-ns 'a.b)
	 (is (find-ns 'a.b))
	 (is (some #{'a.b} @@#'clojure.core/*loaded-libs*))
	 (remove-lib 'a.b)
	 (is (not (find-ns 'a.b)))
	 (is (= @@#'clojure.core/*loaded-libs* '#{c.d}))))
	 
(defn- create-tracker [load unload]
  {:clojure.tools.namespace.track/load load
   :clojure.tools.namespace.track/unload unload
   :clojure.tools.namespace.track/deps  (clojure.tools.namespace.dependency.MapDependencyGraph. {} {})})

(defmacro with-req-rem [reqfn remfn & body]
  `(with-redefs [clojure.core/require ~reqfn
                 clojure.core/remove-ns ~remfn]
		~@body))
		

(defmacro with-recording [& body]
  `(let [rems# (atom [])
         reqs# (atom [])]
	(with-req-rem 
	   (fn [& args#] (swap! reqs# conj args#))
	   (fn [lib#] (swap! rems# conj lib#))
       (let [result# (do ~@body) ]
	     [result# @rems# @reqs#]))))
 
  		
 
(defn- do-reload-one [load unload]
  (with-recording 
    (track-reload-one (create-tracker load unload))))
	
		  
(defn- do-reload-one-with-load-error [load]
  (with-req-rem 
    (fn [& args] (throw (Exception.)))
	identity
	(track-reload-one (create-tracker load nil))))
  
(defn- do-reload [load unload]
  (with-recording
    (track-reload (create-tracker load unload))))
	
(defn- do-reload-with-load-error [load error-sym]
  (with-req-rem 
    (fn [lib & args] (when (= lib error-sym) (throw (Exception.))))
	identity
	(track-reload (create-tracker load nil))))

	
(defn- equal-dep-map? [x load unload]
  (and (= (:clojure.tools.namespace.track/load x) load)
       (= (:clojure.tools.namespace.track/unload x) unload)))

	   
(deftest t-track-reload-one	 
  (testing "unload" 
     (let [[new-tracker rems reqs] (do-reload-one '(a b) '(c d))]
	   (is (equal-dep-map? new-tracker '(a b) '(d)))
	   (is (= rems '[c]))
	   (is (empty? reqs))))
  (testing "load"
     (let [[new-tracker rems reqs] (do-reload-one '(a b) '())]
	   (is (equal-dep-map? new-tracker '(b) '()))
	   (is (empty? rems))
	   (is (= reqs '[(a :reload)]))))
  (testing "load with error"
    (let [new-tracker (do-reload-one-with-load-error '(a b))]
	  (is (equal-dep-map? new-tracker '(a b) '(a b)))
	  (is (instance? Exception (:clojure.tools.namespace.reload/error new-tracker)))
	  (is (= (:clojure.tools.namespace.reload/error-ns new-tracker) 'a)))))
	  
(deftest t-track-reload 
  (testing "normal"	  
     (let [[new-tracker rems reqs] (do-reload '(a b) '(c d))]
	    (is (equal-dep-map? new-tracker '() '()))
		(is (= rems '[c d]))
		(is (= reqs '[(a :reload) (b :reload)]))))
  (testing "load with error"
      (let [new-tracker (do-reload-with-load-error '(a b c) 'b)]
	    (is (equal-dep-map? new-tracker '(b c) '(b c)))
        (is (instance? Exception (:clojure.tools.namespace.reload/error new-tracker)))
	    (is (= (:clojure.tools.namespace.reload/error-ns new-tracker) 'b)))))
  
		
		
	  
	  
	  
		