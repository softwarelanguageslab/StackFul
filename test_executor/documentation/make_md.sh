#!/bin/bash
sh make_dependency_graph.sh
./node_modules/md-to-pdf/cli.js notes.md
open notes.pdf
