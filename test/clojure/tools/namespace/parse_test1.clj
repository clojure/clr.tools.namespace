(ns clojure.tools.namespace.parse-test1
  (:use clojure.test
        clojure.tools.namespace.parse))
;;; DM: I don't know where this came from.  Does not appear to be part of the current or historical c.t.namespace code
(deftest t-comment?
  (testing "is a comment"
    (are [x] (comment? x)
       '(comment)
	   '(comment a b c)))
  (testing "not a comment"
    (are [x] (not (comment? x))
       7
	   ()
	   '(a b c)
	   'comment
	   '[comment a b])))
	 
(deftest t-ns-decl?
  (testing "is an ns"
    (are [x] (ns-decl? x)
       '(ns)
	   '(ns a b c)))
  (testing "not an ns"
    (are [x] (not (ns-decl? x))
       7 
       ()
	   '(a b c)
	   'ns
	   '[ns a b])))

(defn- read-ns-decl-from-string [s]
  (with-open [ptr (clojure.lang.PushbackTextReader. (System.IO.StringReader. s))]
    (read-ns-decl ptr)))
	 
(deftest t-read-ns-decl
   (are [v s] (= v (read-ns-decl-from-string s))
      nil ""
	  nil "(comment a b c)"
	  nil "(a) (b) (c)"
	  nil "(a) (ns a)"
	  '(ns a) "(ns a) (comment (ns b))"
	  '(ns a) "(comment (ns b)) (comment (ns c)) (ns a) (other things)"))
	  
(deftest t-deps-from-ns-decl
   (are [deps form] (= deps (deps-from-ns-decl form))
     #{'clojure.set
	   'clojure.zip} '(ns a (:require (clojure zip [set :as s])))
	 #{'clojure.set} '(ns a (:require clojure.set))
	 #{'clojure.set
	   'clojure.zip} '(ns a (:require clojure.set) (:use clojure.zip) (:refer clojure.whatever))))
	   
	   
	   
	 