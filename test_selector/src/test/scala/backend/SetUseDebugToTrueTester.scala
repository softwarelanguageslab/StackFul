package backend

import backend.tree.merging.{CentralMergedPathRegistry, CentralVirtualPathStore}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funsuite.AnyFunSuite

trait SetUseDebugToTrueTester extends AnyFunSuite with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    super.beforeEach()
    Main.useDebug = true
    CentralMergedPathRegistry.reset()
    CentralVirtualPathStore.reset()
  }

}
