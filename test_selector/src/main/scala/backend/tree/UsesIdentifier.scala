package backend.tree

trait UsesIdentifier[T] {
  def usesIdentifier(x: T): Boolean
}