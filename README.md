# cljclr.tools.namespace #

A port of [clojure/tools.namespace](https://github.com/clojure/tools.namespace) library to ClojureCLR.

From the original README:

> Tools for managing namespaces in Clojure. Parse `ns` declarations from
> source files, extract their dependencies, build a graph of namespace
> dependencies within a project, update that graph as files change, and
> reload files in the correct order.

> This is only about namespace dependencies *within* a single project.
> It has nothing to do with Leiningen, Maven, JAR files, or
> repositories.


## Notes on the ported code ##

See the original's [API documentation](http://clojure.github.com/tools.namespace/) for details.

### `clojure.tools.namespace` namespace ###
NOTE: The `clojure.tools.namespace` namespace is deprecated in the original.  Therefore, we did not port it.  Please use the `clojure.tools.namspace.*` namespaces instead.

### `clojure.tools.namespace.move` namespace ###

NOTE: The `clojure.tools.namespace.move` namespace is marked and 'ALPHA and subject to change'.  We have not ported it yet.

### `clojure.tools.namespace.find` namespace

We did not implement searching in JAR files.