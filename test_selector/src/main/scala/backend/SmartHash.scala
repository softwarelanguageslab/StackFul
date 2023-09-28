package backend

import scala.runtime.ScalaRunTime

trait SmartHash extends Product with Serializable {
  private lazy val cached: Int = ScalaRunTime._hashCode(this)
  override def hashCode(): Int = cached
}
