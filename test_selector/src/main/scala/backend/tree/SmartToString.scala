package backend.tree

trait GeneratesString {
  def genString: String
}

trait SmartToString extends Product with Serializable {
  this: GeneratesString =>
  private lazy val string: String = genString
  override def toString: String = string
}
