package backend.solvers.Z3Solver

import backend.expression.SymbolicInput
import backend.solvers.ComputedValue

sealed trait Z3Result
case class Satisfiable(solution: Map[SymbolicInput, ComputedValue]) extends Z3Result
case object Unsatisfiable extends Z3Result
case class SomeZ3Error(ex: Exception) extends Z3Result
