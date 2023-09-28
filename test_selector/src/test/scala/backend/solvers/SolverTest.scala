package backend.solvers

trait SolverTest {

  def assertComputedValueMatches(value: ComputedValue, expected: Int): Unit = {
    val int = expectInt(value)
    assert(int == expected, s"Expected computed int to be $expected, but was $int instead")
  }
  def assertComputedValueMatches(value: ComputedValue, expected: String): Unit = {
    val string = expectString(value)
    assert(string == expected, s"Expected computed string to be $expected, but was $string instead")
  }
  def expectString(value: ComputedValue): String = value match {
    case ComputedString(string) => string
    case _ =>
      val message = s"Expected a ComputedString, but got a $value"
      assert(false, message)
      throw new Exception(message)
  }
  def assertComputedValueMatches(value: ComputedValue, pred: (Int) => Boolean): Unit = {
    val int = expectInt(value)
    assert(pred(int), s"Predicate is false for the computed value $value")
  }
  def expectInt(value: ComputedValue): Int = value match {
    case ComputedInt(int) => int
    case _ =>
      val message = s"Expected a ComputedInt, but got a $value"
      assert(false, message)
      throw new Exception(message)
  }

}
