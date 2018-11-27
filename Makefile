.PHONY: repl

repl:
	clj -A:repl

outdated:
	clojure -Aoutdated -a outdated
