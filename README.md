# clr.tools.namespace #

A port of [clojure/tools.namespace](https://github.com/clojure/tools.namespace) library to ClojureCLR.

From the original README:

> Tools for managing namespaces in Clojure. Parse `ns` declarations from
> source files, extract their dependencies, build a graph of namespace
> dependencies within a project, update that graph as files change, and
> reload files in the correct order.

> This is only about namespace dependencies *within* a single project.
> It has nothing to do with Leiningen, Maven, JAR files, or
> repositories.

# Releases

Nuget reference:

    PM> Install-Package clojure.tools.namespace

Leiningen/Clojars reference:

   [org.clojure.clr/tools.namespace "0.2.2"]
   

## Notes on the ported code ##

The namespaces are the same as in the original to simplify porting code using the original.  Thus, `clojure.tools.namespace.find`, `clojure.tools.namespace.dir`, etc.

See the original's [API documentation](http://clojure.github.com/tools.namespace/) for details.

### clojure.tools.namespace ###
NOTE: The `clojure.tools.namespace` namespace is deprecated in the original.  Therefore, we did not port it.  Please use the `clojure.tools.namspace.*` namespaces instead.

### clojure.tools.namespace.move ###

NOTE: The `clojure.tools.namespace.move` namespace is marked and 'ALPHA and subject to change'.  We have not ported it yet.

### clojure.tools.namespace.find ###

We did not implement searching in JAR files.


Copyright and License
----------------------------------------

Original Clojure(JVM) code: 

> Copyright © 2012 Stuart Sierra All rights reserved. The use and
> distribution terms for this software are covered by the
> [Eclipse Public License 1.0] which can be found in the file
> epl-v10.html at the root of this distribution. By using this software
> in any fashion, you are agreeing to be bound by the terms of this
> license. You must not remove this notice, or any other, from this
> software.

Modified version for ClojureCLR:

> Copyright © 2013 David Miller All rights reserved. The use and
> distribution terms for this software are covered by the
> [Eclipse Public License 1.0] which can be found in the file
> epl-v10.html at the root of this distribution. By using this software
> in any fashion, you are agreeing to be bound by the terms of this
> license. You must not remove this notice, or any other, from this
> software.

[Eclipse Public License 1.0]: http://opensource.org/licenses/eclipse-1.0.php