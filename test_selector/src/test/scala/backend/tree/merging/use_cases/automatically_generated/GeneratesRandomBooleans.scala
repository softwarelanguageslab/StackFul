package backend.tree.merging.use_cases.automatically_generated

import scala.util.Random

trait GeneratesRandomBooleans {
  def nextBoolean(): Boolean
}

object PrefixedRandomBoolGenerator extends GeneratesRandomBooleans {
  private var bools = List(true, true, false, true)
  def nextBoolean(): Boolean = {
    if (bools.nonEmpty) {
      val head = bools.head
      bools = bools.tail
      head
    } else {
      Random.nextBoolean()
    }
  }
}

object PseudoRandomBoolGenerator extends GeneratesRandomBooleans {
  def nextBoolean(): Boolean = Random.nextBoolean()
}
