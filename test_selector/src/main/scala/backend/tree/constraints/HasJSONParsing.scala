package backend.tree.constraints

import backend.communication.JSONParsing

trait HasJSONParsing[C <: Constraint] {
  def getJSONParsing: JSONParsing[C]
}
