# Concolic Testing backend

## Installation
Run  ``sbt package`` to compile the backend to a JAR that can be included in other projects.

## How to use
Extend the current symbolic tree by calling ``Reporter.addExploredPath`` with the path that was explored in the previous run.
Call ``ConcolicSolver.solve`` to compute a set of inputs that enables exploring a different path of the tree, if the tree has not yet been fully explored.
Call ``Reporter.clear`` between two runs of the concolic tester to reset some bookkeeping information.

## Limitations
Currently, only integer-arithmetic is supported. The path constraints are not optimized. SMT solving only via Z3.
