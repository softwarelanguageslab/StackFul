package backend.tree

import backend.Path
import backend.execution_state.ExecutionState
import backend.tree.constraints.constraint_with_execution_state.ConstraintES
import backend.tree.follow_path._

package object merging {

  type VirtualPath = List[VirtualDirection]

  def isGlobalMergePoint(node: SymbolicNodeWithConstraint[ConstraintES]): Boolean = {
    node.constraint.executionState.stackLength == 0
  }
  def isMergedGlobalMergePoint(node: SymbolicNodeWithConstraint[ConstraintES]): Boolean = {
    node.hasBeenMerged && isGlobalMergePoint(node)
  }

  def virtualPathToOldITEPath(virtualPath: VirtualPath): Path = {
    makeVirtualPathReal(virtualPath.filter({
      case RealThen | RealElse => true
      case _ => false
    }))
  }

  def makeVirtualPathReal(virtualPath: VirtualPath): Path = {
    virtualPath.map({
      case _: Thenny => ThenDirection
      case _: Elssy => ElseDirection
    })
  }

  def getOldRealPath(virtualPath: VirtualPath): Path = {
    virtualPath.flatMap({
      case RealThen => List(ThenDirection)
      case RealElse => List(ElseDirection)
      case _ => Nil
    })
  }

  def getRealPath(virtualPath: VirtualPath): Path = {
    virtualPath.flatMap({
      case NewThen | RealThen => List(ThenDirection)
      case NewElse | RealElse => List(ElseDirection)
      case _ => Nil
    })
  }

  def updateNewDirections(virtualPath: VirtualPath): VirtualPath = {
    virtualPath.map({
      case NewThen => RealThen
      case NewElse => RealElse
      case other => other
    })
  }

  def makePathVirtual(realPath: Path): VirtualPath = {
    realPath.map({
      case ThenDirection => VirtualThen
      case ElseDirection => VirtualElse
    })
  }

  sealed trait VirtualDirection {
    def isVirtual: Boolean
    def toDirection: Direction
  }
  sealed trait RealVirtualDirection {
    def isVirtual: Boolean = false
  }
  sealed trait VirtualVirtualDirection {
    def isVirtual: Boolean = true
  }
  sealed trait Thenny extends VirtualDirection {
    def toDirection: Direction = ThenDirection
  }
  sealed trait Elssy extends VirtualDirection {
    def toDirection: Direction = ElseDirection
  }
  case object RealThen extends Thenny with RealVirtualDirection
  case object RealElse extends Elssy with RealVirtualDirection
  case object VirtualThen extends Thenny with VirtualVirtualDirection
  case object VirtualElse extends Elssy with VirtualVirtualDirection
  case object NewThen extends Thenny with RealVirtualDirection
  case object NewElse extends Elssy with RealVirtualDirection

  import scala.languageFeature.implicitConversions
  case class OutOfScopeIdentifierDiscovered(identifier: Set[String]) extends Exception


}
