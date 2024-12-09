(ns clojure.tools.namespace.repl-test
    (:require [clojure.test :refer [deftest is use-fixtures]]
              [clojure.tools.namespace.dir :as dir]
              [clojure.tools.namespace.find :as find]
              [clojure.tools.namespace.repl :as repl]
              [clojure.tools.namespace.test-helpers :as help]))
			
;; Tests contributed by Brandon Correa

(defn reset-repl! []
  (repl/clear)
  (repl/set-refresh-dirs))
  
(defn reset-repl-fixture [test-fn]
  (reset-repl!)
  (test-fn)
  (reset-repl!))
  
(use-fixtures :each reset-repl-fixture)
			
(defn current-time-millis []
  (long
    (/ (- (.-Ticks DateTime/UtcNow)
          (.-Ticks DateTime/UnixEpoch))
       TimeSpan/TicksPerMillisecond)))
	   
(deftest t-repl-scan-time-component
  (let [before (current-time-millis)
        scan   (repl/scan {:platform find/clj})
        after  (current-time-millis)
        time   (::dir/time scan)]
    (is (<= before time after))
    (is (integer? (::dir/time scan)))))
	
(deftest t-repl-scan-twice
  (let [dir       (help/create-temp-dir "t-repl-scan")
        other-dir (help/create-temp-dir "t-repl-scan-other")
        main-clj  (help/create-source dir 'example.main :clj '[example.one])
        one-cljc  (help/create-source dir 'example.one :clj)
        _         (repl/set-refresh-dirs dir other-dir)
        scan-1    (repl/scan {:platform find/clj})
        scan-2    (repl/scan {:platform find/clj})
        paths-1   (map str (::dir/files scan-1))
        paths-2   (map str (::dir/files scan-2))
        paths     (set paths-1)]
       (is (= 2 (count paths-1)))
       (is (= paths-1 paths-2))
       (is (contains? paths (str main-clj)))
       (is (contains? paths (str one-cljc)))))

(deftest t-repl-scan-after-file-modified
  (let [dir       (help/create-temp-dir "t-repl-scan-after-file-modified")
        main-clj  (help/create-source dir 'example.main :clj)
        _         (repl/set-refresh-dirs dir)
        scan-1    (repl/scan {:platform find/clj})
        _         (System.IO.File/SetLastWriteTimeUtc (.-FullName main-clj) DateTime/UtcNow)
        scan-2    (repl/scan {:platform find/clj})
        paths-1   (map str (::dir/files scan-1))
        paths-2   (map str (::dir/files scan-2))
        paths     (set paths-1)]
    (is (= 1 (count paths-1)))
    (is (= paths-1 paths-2))
    (is (contains? paths (str main-clj)))))