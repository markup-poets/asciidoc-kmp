# cli-app

Command-line tool that converts an AsciiDoc file into Graphviz DOT, for visualizing the parsed Abstract Semantic Graph.

```bash
./asciidoc2dot.sh document.adoc
dot -Tpng document.dot -o document.png
```

**Full documentation:** [Export the ASG to Graphviz DOT](https://markup-poets.github.io/asciidoc-kmp/how-to/export-asg-to-dot.html) — usage, the node colour scheme, rendering, troubleshooting, and using `asg-graphviz-export` as a library.
