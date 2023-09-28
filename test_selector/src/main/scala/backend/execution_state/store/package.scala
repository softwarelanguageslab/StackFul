package backend.execution_state

import backend.expression.SymbolicExpression

package object store {

  type Assignment = (String, SymbolicExpression)
  type Assignments = List[Assignment]

}
