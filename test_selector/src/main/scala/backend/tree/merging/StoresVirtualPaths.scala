package backend.tree.merging

import backend.Path
import backend.expression.{BooleanExpression, SymbolicExpression}
import backend.tree.OffersVirtualPath

import scala.annotation.tailrec

trait HasVirtualPathsStores {

  protected var virtualPathStoreForIdentifiers: Map[String, StoresVirtualPaths] = Map()

  def getVirtualPathStoreForIdentifier(identifier: String): StoresVirtualPaths = {
    virtualPathStoreForIdentifiers.get(identifier) match {
      case Some(store) => store
      case None =>
        val newStore = makeNewVirtualPathStore
        virtualPathStoreForIdentifiers += (identifier -> newStore)
        newStore
    }
  }

  protected def makeNewVirtualPathStore: StoresVirtualPaths
}

case class RedoMergingFor(
  predExp: BooleanExpression,
  savedExp: SymbolicExpression,
  path1: Path,
  path2: Path
) {
  def usesPaths(path1: Path, path2: Path): Boolean = {
    (this.path1 == path1 && this.path2 == path2) ||
      (this.path2 == path1 && this.path1 == path2)
  }
}

sealed trait ITEMappedTo
case class ITEMappedToOnePath(path: Path) extends ITEMappedTo
case class ITEMappedToMultiplePaths(path: List[RedoMergingFor]) extends ITEMappedTo {
  def add(redoMerging: RedoMergingFor): ITEMappedToMultiplePaths = {
    if (path.contains(redoMerging)) {
      this
    } else {
      this.copy(redoMerging :: path)
    }
  }
}

sealed trait UndoOperation
case class UndoAddGlobalPath(
  itePath: Path,
  oldContent: Option[ITEMappedTo],
  globalPath: Path
) extends UndoOperation
case class UndoRemoveGlobalPath(
  itePath: Path,
  oldContent: Option[ITEMappedTo]
) extends UndoOperation
case class UndoUpdateVirtualPath(
  globalPath: Path,
  overriddenVirtualPath: Option[VirtualPath]
) extends UndoOperation

trait StoresVirtualPaths {

  case class ITEPathToAdd(itePath: Path, globalPath: Path, exp: SymbolicExpression, predExp: BooleanExpression)

  protected var globalToVirtualPaths: Map[Path, VirtualPath] = Map()
  protected var itePathToGlobalPaths: Map[Path, ITEMappedTo] = Map()
  protected var savedPaths: Set[Path] = Set()
  protected var itePathsToAdd: Set[ITEPathToAdd] = Set()

  def getITEPaths: Map[Path, ITEMappedTo] = itePathToGlobalPaths

  private def addToITEPathsToAdd(entry: ITEPathToAdd): Unit = {
    if (! itePathsToAdd.exists(otherEntry => entry.itePath == otherEntry.itePath && entry.globalPath == otherEntry.globalPath)) {
      itePathsToAdd += entry
    }
  }

  def finishStore(): Unit = {
    def processEntry(entry: ITEPathToAdd): Unit = {
      val existingMappedTo = itePathToGlobalPaths.get(entry.itePath)
      existingMappedTo match {
        case None =>
          itePathToGlobalPaths += (entry.itePath -> ITEMappedToOnePath(entry.globalPath))
        case Some(oldValue) => oldValue match {
          case ITEMappedToOnePath(otherPath) =>
            val res = RedoMergingFor(entry.predExp, entry.exp, entry.globalPath, otherPath)
            val newMappedTo = ITEMappedToMultiplePaths(List(res))
            itePathToGlobalPaths += (entry.itePath -> newMappedTo)
          case mappedTo@ITEMappedToMultiplePaths(toRedos) =>
            assert(toRedos.nonEmpty)
            val path1 = toRedos.head.path1
            val path2 = toRedos.head.path2
            val newToRedo = RedoMergingFor(
              entry.predExp, entry.exp, entry.globalPath, if (path1 == entry.globalPath) path2 else path1)
            val newMappedTo = mappedTo.add(newToRedo)
            itePathToGlobalPaths += (entry.itePath -> newMappedTo)
        }
      }
    }
    itePathsToAdd.foreach(entry => {
      processEntry(entry)
    })
    itePathsToAdd = Set()
  }

  def getVirtualPath(globalPath: Path): Option[VirtualPath] = {
    globalToVirtualPaths.get(globalPath)
  }

  def getSavedPaths: Set[Path] = savedPaths
  def addSavedPath(path: Path): Unit = {
    savedPaths += path
  }
  def removeSavedPath(path: Path): Unit = {
    savedPaths -= path
  }

  private def actualUpdateVirtualPath(
    globalPath: Path,
    virtualPath: VirtualPath
  ): Unit = {
    globalToVirtualPaths += (globalPath -> virtualPath)
  }

  def updateVirtualPath(
    globalPath: Path,
    virtualPath: VirtualPath
  ): UndoUpdateVirtualPath = {
    val overriddenValue = globalToVirtualPaths.get(globalPath)
    actualUpdateVirtualPath(globalPath, virtualPath)
    UndoUpdateVirtualPath(globalPath, overriddenValue)
  }

  def undoOperation(operation: UndoOperation): Unit = operation match {
    case op: UndoAddGlobalPath => undoOperation(op)
    case op: UndoRemoveGlobalPath => undoOperation(op)
    case op: UndoUpdateVirtualPath => undoOperation(op)
  }

  def undoOperation(undoOperation: UndoUpdateVirtualPath): Unit = {
    undoOperation.overriddenVirtualPath match {
      case Some(virtualPath) => globalToVirtualPaths += undoOperation.globalPath -> virtualPath
      case None => globalToVirtualPaths = globalToVirtualPaths.removed(undoOperation.globalPath)
    }
  }

  def removeVirtualPath(globalPath: Path): Unit = {
    globalToVirtualPaths = globalToVirtualPaths.removed(globalPath)
  }

  def getGlobalPaths(itePath: Path): Option[ITEMappedTo] = {
    itePathToGlobalPaths.get(itePath)
  }

  def replaceGlobalPath(itePath: Path, value: Option[ITEMappedTo]): Unit = {
    itePathToGlobalPaths = itePathToGlobalPaths.updatedWith(itePath)(_.flatMap(_ => value))
  }

  def addTwoGlobalPaths(
    predExp: BooleanExpression,
    savedExp: SymbolicExpression,
    itePath: Path,
    thenGlobalPath: Path,
    elseGlobalPath: Path
  ): Unit = {
    val existingMappedTo = itePathToGlobalPaths.get(itePath)
    lazy val newToRedo = RedoMergingFor(predExp, savedExp, thenGlobalPath, elseGlobalPath)
    existingMappedTo match {
      case None =>
        itePathToGlobalPaths += itePath -> ITEMappedToMultiplePaths(List(newToRedo))
      case Some(oldValue) => oldValue match {
        case _: ITEMappedToOnePath =>
          throw new Exception
        case mappedTo@ITEMappedToMultiplePaths(toRedos) =>
          assert(toRedos.nonEmpty)
          val alreadyExists = toRedos.find(_.usesPaths(thenGlobalPath, elseGlobalPath))
          alreadyExists match {
            case Some(redo) =>
              assert(redo.predExp == predExp)
            case None =>
              itePathToGlobalPaths += itePath -> mappedTo.add(newToRedo)
          }
      }
    }
  }

  def addGlobalPath(
    predExp: BooleanExpression,
    exp: SymbolicExpression,
    itePath: Path,
    globalPath: Path
  ): UndoAddGlobalPath = {
    removeSavedPath(globalPath)
    val existingMappedTo = itePathToGlobalPaths.get(itePath)
    val toAdd = ITEPathToAdd(itePath, globalPath, exp, predExp)
    addToITEPathsToAdd(toAdd)
    UndoAddGlobalPath(itePath, existingMappedTo, globalPath)
  }
  def undoOperation(undoOperation: UndoAddGlobalPath): Unit = {
    itePathsToAdd = itePathsToAdd.filterNot(
      entry => entry.itePath == undoOperation.itePath && entry.globalPath == undoOperation.globalPath)
  }

  def getITEPath(globalPath: Path): Option[Path] = {
    itePathToGlobalPaths.find(tuple => tuple._2 match {
      case ITEMappedToOnePath(someGlobalPath) => globalPath == someGlobalPath
      case ITEMappedToMultiplePaths(paths) =>
        paths.exists(redoMerging => redoMerging.path1 == globalPath || redoMerging.path2 == globalPath)
      case _ => false
    }).map(_._1)
  }

  def updateITEPredicateFor(itePath1: Path, itePath2: Path, newPredicateExp: BooleanExpression): Unit = {
    def updateITEMappedTo(path: Path): Unit = {
      itePathToGlobalPaths(path) match {
        case ite@ITEMappedToMultiplePaths(toRedo) =>
          val toRedoWithPredicateUpdated = toRedo.map(redoMergingFor => redoMergingFor.copy(predExp = newPredicateExp))
          itePathToGlobalPaths = itePathToGlobalPaths.updated(path, ite.copy(path = toRedoWithPredicateUpdated))
        case _: ITEMappedToOnePath =>
      }
    }
    updateITEMappedTo(itePath1)
    updateITEMappedTo(itePath2)
    finishStore()
  }

  def updateSavedExp(itePath1: Path, itePath2: Path, newSavedExp: SymbolicExpression): Unit = {
    def updateITEMappedTo(path: Path): Unit = {
      itePathToGlobalPaths(path) match {
        case ite@ITEMappedToMultiplePaths(toRedo) =>
          val toRedoWithPredicateUpdated = toRedo.map(redoMergingFor => redoMergingFor.copy(savedExp = newSavedExp))
          itePathToGlobalPaths = itePathToGlobalPaths.updated(path, ite.copy(path = toRedoWithPredicateUpdated))
        case _: ITEMappedToOnePath =>
      }
    }
    updateITEMappedTo(itePath1)
    updateITEMappedTo(itePath2)
    finishStore()
  }

  def removeGlobalPath(itePath: Path): UndoRemoveGlobalPath = {
    val oldContent = itePathToGlobalPaths.get(itePath)
    itePathToGlobalPaths = itePathToGlobalPaths.removed(itePath)
    UndoRemoveGlobalPath(itePath, oldContent)
  }
  def undoOperation(undoOperation: UndoRemoveGlobalPath): Unit = {
    undoOperation.oldContent match {
      case Some(overriddenValue) => itePathToGlobalPaths += undoOperation.itePath -> overriddenValue
      case None => itePathToGlobalPaths = itePathToGlobalPaths.removed(undoOperation.itePath)
    }
  }

  //      def isRealDirection(dir: VirtualDirection) = dir match {
  //        case VirtualElse | VirtualThen => false
  //        case _ => true
  //      }
  //      val vPath1Count = vPath1.count(isRealDirection)
  //      val vPath2Count = vPath2.count(isRealDirection)
  //      vPath1Count > vPath2Count
  //    }

  def merge(otherStore: StoresVirtualPaths): Unit = {
    def mergeVirtualPaths(vPath1: VirtualPath, vPath2: VirtualPath): VirtualPath = {
      @tailrec
      def loop(vPath1: VirtualPath, vPath2: VirtualPath, acc: VirtualPath): VirtualPath = (vPath1.headOption, vPath2.headOption) match {
        case (None, None) => acc.reverse
        case (Some(dir1), Some(dir2)) =>
          assert(dir1.toDirection == dir2.toDirection)
          if (!dir1.isVirtual) {
            loop(vPath1.tail, vPath2.tail, dir1 :: acc)
          } else if (!dir2.isVirtual) {
            loop(vPath1.tail, vPath2.tail, dir2 :: acc)
          } else {
            loop(vPath1.tail, vPath2.tail, dir1 :: acc)
          }
      }
      loop(vPath1, vPath2, Nil)
    }
    otherStore.globalToVirtualPaths.foreach(tuple => {
      val (key, value) = tuple
      if (this.globalToVirtualPaths.contains(key)) {
        val mergedVirtualPath = mergeVirtualPaths(this.globalToVirtualPaths(key), value)
        this.globalToVirtualPaths += (key -> mergedVirtualPath)
      } else {
        this.globalToVirtualPaths += (key -> value)
      }
    })
    otherStore.itePathToGlobalPaths.foreach(tuple => {
      val (key, value) = tuple
      if (this.itePathToGlobalPaths.contains(key)) {
      } else {
        this.itePathToGlobalPaths += (key -> value)
      }
    })
  }
}

class VirtualPathStore extends StoresVirtualPaths

class VirtualPathStorePerIdentifier extends HasVirtualPathsStores with OffersVirtualPath {
  override protected def makeNewVirtualPathStore: StoresVirtualPaths = new VirtualPathStore
  def getAllVirtualPathStores: Iterable[(String, StoresVirtualPaths)] = virtualPathStoreForIdentifiers.toList
}