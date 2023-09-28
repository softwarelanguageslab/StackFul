package backend.expression

import backend.Path
import backend.logging.Logger
import backend.tree.{SymbolicNode, SymbolicNodeWithConstraint}
import backend.tree.constraints.Constraint
import backend.tree.follow_path.ThenDirection
import backend.tree.merging._
import backend.tree.search_strategy.TreePath

import scala.annotation.tailrec
import scala.collection.immutable.Queue

case class RestartMergingWith(
  predExp: BooleanExpression,
  globalPathToThenExp: Path,
  globalPathToElseExp: Path,
  toCreateITEFor: CreateITEFor
)
  extends Throwable

case class CreateITEFor(thenExp: SymbolicExpression, elseExp: SymbolicExpression, identifier: String)

class ExpsSaver {
  private var savedExps: Map[Path, SymbolicExpression] = Map()
  def addSavedExp(path: Path, exp: SymbolicExpression): Unit = {
    savedExps += path -> exp
  }
  def get(path: Path): Option[SymbolicExpression] = savedExps.get(path)
  private def updateSavedExp(path: Path, exp: SymbolicExpression): Boolean = {
    savedExps.get(path) match {
      case None =>
        savedExps += path -> exp
        true
      case _ => false
    }
  }

  private def equateTwoSavedPaths(path1: Path, path2: Path): Boolean = {
    def equateSavedPaths(path1: Path, path2: Path): Boolean = savedExps.get(path1) match {
      case Some(exp) => updateSavedExp(path2, exp)
      case None => false
    }
    val b1 = equateSavedPaths(path1, path2)
    val b2 = equateSavedPaths(path2, path1)
    b1 || b2
  }

  @tailrec
  final def updateToRedos(toRedos: List[RedoMergingFor]): Unit = {
    var hasChanged: Boolean = false
    toRedos.foreach(toRedo => {
      val pathsUpdated = equateTwoSavedPaths(toRedo.path1, toRedo.path2)
      if (!hasChanged) {
        hasChanged = pathsUpdated
      }
    })
    if (hasChanged) {
      updateToRedos(toRedos)
    }
  }
}

class SymbolicITECreator(
  protected val virtualPathStore: StoresVirtualPaths,
  protected val savesExps: ExpsSaver = new ExpsSaver,
) {

  case object IncompatibleVirtualPath extends Exception

  def getFullVirtualPath(
    storeToUse: StoresVirtualPaths,
    prefixVirtualPath: VirtualPath,
    globalPath: Path
  ): VirtualPath = {
    val oldVirtualPath = storeToUse.getVirtualPath(globalPath).get // Should exist

    def loop(
      prefixVirtualPath: VirtualPath,
      oldVirtualPath: VirtualPath
    ): VirtualPath = oldVirtualPath.headOption match {
      case None => Nil
      case Some(oldDir) => oldDir match {
        case VirtualThen => prefixVirtualPath.headOption match {
          case Some(NewThen) => RealThen :: loop(prefixVirtualPath.tail, oldVirtualPath.tail)
          case Some(RealThen) | Some(RealElse) => VirtualThen :: loop(prefixVirtualPath, oldVirtualPath.tail)
          case Some(VirtualThen) => VirtualThen :: loop(prefixVirtualPath.tail, oldVirtualPath.tail)
          case None => oldVirtualPath
          case Some(VirtualElse) =>
            throw IncompatibleVirtualPath
          case Some(NewElse) =>
            throw IncompatibleVirtualPath
        }
        case VirtualElse => prefixVirtualPath.headOption match {
          case Some(NewElse) => RealElse :: loop(prefixVirtualPath.tail, oldVirtualPath.tail)
          case Some(RealElse) | Some(RealThen) => VirtualElse :: loop(prefixVirtualPath, oldVirtualPath.tail)
          case Some(VirtualElse) => VirtualElse :: loop(prefixVirtualPath.tail, oldVirtualPath.tail)
          case Some(VirtualThen) | Some(NewThen) =>
            throw IncompatibleVirtualPath
          case None => oldVirtualPath
        }
        case RealThen => prefixVirtualPath.head match {
          case RealThen => RealThen :: loop(prefixVirtualPath.tail, oldVirtualPath.tail)
          case NewThen => RealThen :: loop(prefixVirtualPath.tail, oldVirtualPath.tail)
          case _ =>
            throw IncompatibleVirtualPath
        }
        case RealElse => prefixVirtualPath.head match {
          case RealElse => RealElse :: loop(prefixVirtualPath.tail, oldVirtualPath.tail)
          case NewElse => RealElse :: loop(prefixVirtualPath.tail, oldVirtualPath.tail)
          case _ =>
            throw IncompatibleVirtualPath
//            throw new Exception
        }
      }
    }
    loop(prefixVirtualPath, oldVirtualPath)
  }

  def getOptFullVirtualPath(
    storeToUse: StoresVirtualPaths,
    prefixVirtualPath: VirtualPath,
    globalPath: Path
  ): Option[VirtualPath] = {
    try {
      val virtualPath = getFullVirtualPath(storeToUse, prefixVirtualPath, globalPath)
      Some(virtualPath)
    } catch {
      case IncompatibleVirtualPath => None
    }
  }

  def createITE(
    predExp: BooleanExpression,
    globalPathToThenExp: Path,
    globalPathToElseExp: Path,
    thenExp: SymbolicExpression,
    elseExp: SymbolicExpression,
    identifier: String
  ): SymbolicExpression = {
    val set = createITE(
      predExp, globalPathToThenExp, globalPathToElseExp, List(CreateITEFor(thenExp, elseExp, identifier)))
    set.head._2
  }
  // SymbolicStoreMerger calls this method
  def createITE[C <: Constraint](
    predExp: BooleanExpression,
    globalPathToThenExp: TreePath[C],
    globalPathToElseExp: TreePath[C],
    toCreateITEFor: List[CreateITEFor]
  ): List[(String, SymbolicExpression)] = {
    def findSavedExpsInStore(virtualPathStore: StoresVirtualPaths): Unit = {
      virtualPathStore.getITEPaths.foreach({
        case (_, ITEMappedToMultiplePaths(toRedos)) => savesExps.updateToRedos(toRedos)
        case _ =>
      })
    }
    savesExps.addSavedExp(globalPathToThenExp.getPath, toCreateITEFor.head.thenExp)
    savesExps.addSavedExp(globalPathToElseExp.getPath, toCreateITEFor.head.elseExp)
    findSavedExpsInStore(virtualPathStore)

    createITE(predExp, globalPathToThenExp.getPath, globalPathToElseExp.getPath, toCreateITEFor)
  }
  /**
    * Note: assumes both paths lead to a non-ITE expression (i.e., a "leaf" expression)
    */
  def addEmptyEntryInVirtualPathStoreFor[C <: Constraint](
    mergingPredicateExp: BooleanExpression,
    savedExp: SymbolicExpression,
    globalPathToThenExp: TreePath[C],
    globalPathToElseExp: TreePath[C]
  ): Unit = {
    val (thenGlobalPath, elseGlobalPath) = (globalPathToThenExp.getPath, globalPathToElseExp.getPath)
    virtualPathStore.addTwoGlobalPaths(mergingPredicateExp, savedExp, Nil, thenGlobalPath, elseGlobalPath)
    virtualPathStore.finishStore()

  }

  def createITE(
    predExp: BooleanExpression,
    globalPathToThenExpParam: Path,
    globalPathToElseExpParam: Path,
    toCreateITEFor: List[CreateITEFor]
  ): List[(String, SymbolicExpression)] = {
    var globalPathToThenExp = globalPathToThenExpParam
    var globalPathToElseExp = globalPathToElseExpParam
    if (toCreateITEFor.isEmpty) {
      return Nil
    }
    assert(globalPathToThenExp.nonEmpty)
    assert(globalPathToElseExp.nonEmpty)

    case class WhatToDo(leafExp: SymbolicExpression, prefixVirtualPath: VirtualPath)

    /**
      * Returns the leaf expressions of exp, sorted from deepest to most shallow.
      */
    def getLeafExps(exp: SymbolicExpression, prefixVirtualPath: VirtualPath): List[WhatToDo] = {
      @tailrec
      def loop(workList: Queue[WhatToDo], leafExps: Queue[WhatToDo]): Queue[WhatToDo] = workList.headOption match {
        case Some(item@WhatToDo(exp, prefixVirtualPath)) => exp match {
          case SymbolicITEExpression(_, thenExp, elseExp, _) =>
            val thenItem = WhatToDo(thenExp, prefixVirtualPath :+ RealThen)
            val elseItem = WhatToDo(elseExp, prefixVirtualPath :+ RealElse)
            loop(workList.tail :+ thenItem :+ elseItem, leafExps)
          case _ =>
            loop(workList.tail, leafExps :+ item)
        }
        case None => leafExps
      }
      val start = WhatToDo(exp, prefixVirtualPath)
      val leafExps = loop(Queue(start), Queue())
      leafExps.toList.reverse
    }

    def handleLeafExps(
      exp: SymbolicExpression,
      identifier: String,
      prefixVirtualPath: VirtualPath,
      storeToUse: StoresVirtualPaths,
      virtualPathToUse: VirtualPath,
      itePathOfNewITE: Path,
      optOtherPathToMaybeRemove: Option[Path],
      isANewPath: Boolean
    ): List[UndoOperation] = {

      // Returns all the RedoMergingFor values that have been removed
      def reduceITEMappedToMultiplePaths(
        store: StoresVirtualPaths,
        oldITEPath: Path,
        toRedoToRemove: RedoMergingFor
      ): Unit = {
        store.getGlobalPaths(oldITEPath) match {
          case Some(ITEMappedToMultiplePaths(toRedos)) =>
            val remaining = toRedos.filter(! _.usesPaths(toRedoToRemove.path1, toRedoToRemove.path2))
            val newMappedTo = if (remaining.isEmpty) {
              None
            } else {
              Some(ITEMappedToMultiplePaths(remaining))
            }
            store.replaceGlobalPath(oldITEPath, newMappedTo)
          case _ =>
        }
      }

      def extractNextToRedo(
        oldITEPath: Path,
        toRedos: List[RedoMergingFor]
      ): RedoMergingFor = {
        val toRedo = toRedos.head
        reduceITEMappedToMultiplePaths(storeToUse, oldITEPath, toRedo)
        toRedo
      }

      // itePath is the full ITE path: it starts from the "root" of the ITE expression and leads to a leaf or an "old" (belonging to an ancestor) ITE exp
      // exp on the other hand was already (partially) traversed while looping through the virtualPath1 and virtualPath2 in the main loop of createITE.
      // Hence, first take the part of the full ITE path which has noy yet been traversed, and then use this remainder of the ITE path to reach the correct exp.
      @tailrec
      def getRemainderAfter[T](
        possibleSubsequence: List[T],
        sequence: List[T]
      ): Option[List[T]] = (possibleSubsequence.headOption, sequence.headOption) match {
        case (None, None) => Some(Nil)
        case (None, _) => Some(sequence)
        case (_, None) => None
        case (Some(a), Some(b)) if a == b => getRemainderAfter(possibleSubsequence.tail, sequence.tail)
        case _ => None
      }

      @tailrec
      def computeWhatToDo(itePath: Path, exp: SymbolicExpression, prefixVirtualPath: VirtualPath): WhatToDo = {
        itePath.headOption match {
          case None => WhatToDo(exp, prefixVirtualPath)
          case Some(direction) => exp match {
            case SymbolicITEExpression(_, thenExp, elseExp, _) =>
              if (direction == ThenDirection) {
                computeWhatToDo(itePath.tail, thenExp, prefixVirtualPath :+ RealThen)
              } else {
                computeWhatToDo(itePath.tail, elseExp, prefixVirtualPath :+ RealElse)
              }
            case _ =>
              throw new Exception
          }
        }
      }

      def findITEPaths(entries: Iterable[(Path, ITEMappedTo)]): List[(Path, ITEMappedTo)] = {
        val onlySubexpressions = entries.toList.flatMap(entry => {
          // itePath is the full ITE path: it starts from the "root" of the ITE expression and leads to a leaf or an "old" (belonging to an ancestor) ITE exp
          // exp on the other hand was already (partially) traversed while looping through the virtualPath1 and virtualPath2 in the main loop of createITE.
          // Hence, first take the part of the full ITE path which has noy yet been traversed, and then use this remainder of the ITE path to reach the correct exp.
          val optRemainder = getRemainderAfter(itePathOfNewITE, entry._1)
          optRemainder match {
            case None => Nil
            case Some(tuple) => List((tuple, entry._2))
          }
        })
        onlySubexpressions.sortBy(_._1.size).reverse
      }

      val start = WhatToDo(exp, prefixVirtualPath)
      val whatToDos = if (isANewPath || storeToUse.getITEPaths.isEmpty) {
        List(start)
      } else {
        val iteKeysToProcess = findITEPaths(storeToUse.getITEPaths)
        iteKeysToProcess.map(tuple => computeWhatToDo(tuple._1, exp, prefixVirtualPath))
      }

      def updateVirtualPath(
        exp: SymbolicExpression,
        fullVirtualPath: VirtualPath,
        oldITEPath: Path,
        newDirectionsMadeReal: VirtualPath,
        globalPath: Path
      ): List[UndoOperation] = {
        val undoRemoveGlobalPath = storeToUse.removeGlobalPath(oldITEPath)
        val undoAddGlobalPath = storeToUse.addGlobalPath(predExp, exp, getRealPath(newDirectionsMadeReal), globalPath)
        val undoUpdateVirtualPath = storeToUse.updateVirtualPath(globalPath, fullVirtualPath)
        List(undoRemoveGlobalPath, undoAddGlobalPath, undoUpdateVirtualPath)
      }
      def updateSavedVirtualPath(
        fullVirtualPath: VirtualPath,
        oldITEPath: Path,
        newDirectionsMadeReal: VirtualPath,
        globalPath: Path
      ): UndoOperation = {
        storeToUse.updateVirtualPath(globalPath, fullVirtualPath)
      }

      val undoOperations = whatToDos.flatMap[UndoOperation]({
        case wtd@WhatToDo(exp, prefixVirtualPath) =>
          val oldITEPath = virtualPathToOldITEPath(prefixVirtualPath)
          val newDirectionsMadeReal = updateNewDirections(prefixVirtualPath)
          storeToUse.getGlobalPaths(oldITEPath) match {
            case Some(mappedTo) => mappedTo match {
              case ITEMappedToOnePath(globalPath) =>
                if (optOtherPathToMaybeRemove.contains(globalPath)) {
                  // Can ignore globalPath entirely and treat it as an empty entry, but cannot actually remove the entry here yet, because it may be processed by the other invocation of handleLeafExps
                  val suffixVirtualPath = virtualPathToUse.tail
                  val madeReal = makeVirtualPathReal(newDirectionsMadeReal ++ suffixVirtualPath)
                  val undoAddGlobalPath = storeToUse.addGlobalPath(
                    predExp, exp, getRealPath(newDirectionsMadeReal), madeReal)
                  val fullVirtualPath = newDirectionsMadeReal ++ suffixVirtualPath
                  val undoUpdateVirtualPath = storeToUse.updateVirtualPath(makeVirtualPathReal(fullVirtualPath), fullVirtualPath)
                  List(undoAddGlobalPath, undoUpdateVirtualPath)
                } else {
                  val optFullVirtualPath = getOptFullVirtualPath(storeToUse, prefixVirtualPath, globalPath)
                  if (optFullVirtualPath.isDefined) {
                    updateVirtualPath(exp, optFullVirtualPath.get, oldITEPath, newDirectionsMadeReal, globalPath)
                  } else {
                    Nil
                  }
                }
              case ITEMappedToMultiplePaths(toRedos) =>
                assert(toRedos.nonEmpty)
                val undoOpertions: List[UndoOperation] = storeToUse.getSavedPaths.toList.flatMap(savedPath => {
                  val optVirtualPath = getOptFullVirtualPath(storeToUse, prefixVirtualPath, savedPath)
                  if (optVirtualPath.isDefined) {
                    val undoOperation = updateSavedVirtualPath(optVirtualPath.get, oldITEPath, newDirectionsMadeReal, savedPath)
                    List(undoOperation)
                  } else {
                    Nil
                  }
                })
//                updateToRedos(toRedos)
                if (toRedos.nonEmpty) {
                  val toRedo = extractNextToRedo(oldITEPath, toRedos)
                  var optSavedExp = savesExps.get(toRedo.path1)
                  if (optSavedExp.isEmpty) {
                    optSavedExp = savesExps.get(toRedo.path2)
                  }
                  if (optSavedExp.isEmpty) {
                    throw new Error("Should not happen")
                  }
                  undoOpertions.foreach(operation => storeToUse.undoOperation(operation))
                  throw RestartMergingWith(
                    toRedo.predExp, toRedo.path1, toRedo.path2, CreateITEFor(optSavedExp.get, optSavedExp.get, identifier))
                } else {
                  undoOpertions
                }
            }
            case None =>
              val suffixVirtualPath = virtualPathToUse.tail
              val madeReal = makeVirtualPathReal(newDirectionsMadeReal ++ suffixVirtualPath)
              val undoAddGlobalPath = storeToUse.addGlobalPath(
                predExp, exp, getRealPath(newDirectionsMadeReal), madeReal)
              val fullVirtualPath = newDirectionsMadeReal ++ suffixVirtualPath
              val undoUpdateVirtualPath = storeToUse.updateVirtualPath(
                makeVirtualPathReal(fullVirtualPath), fullVirtualPath)
              List(undoAddGlobalPath, undoUpdateVirtualPath)
          }
      })
      undoOperations
    }

    def loop(
      virtualPath1: VirtualPath,
      virtualPath2: VirtualPath,
      prefixVirtualPath: VirtualPath,
      exps: List[CreateITEFor]
    ): List[(String, SymbolicExpression)] = {
      (virtualPath1.head, virtualPath2.head) match {
        case (_: Thenny, _: Elssy) =>
          val resultExps = exps.map(toCreateITEFor => {
            val ite = SymbolicITEExpression(
              predExp, toCreateITEFor.thenExp, toCreateITEFor.elseExp, Some(toCreateITEFor.identifier))
            (toCreateITEFor.identifier, ite: SymbolicExpression)
          })
          val CreateITEFor(exp, elseExp, identifier) = exps.head
          val thenLeafsUndoOperations = handleLeafExps(exp, identifier, prefixVirtualPath :+ NewThen, virtualPathStore, virtualPath1, getRealPath(prefixVirtualPath), Some(globalPathToElseExp), false)
          try {
            handleLeafExps(elseExp, identifier, prefixVirtualPath :+ NewElse, virtualPathStore, virtualPath2, getRealPath(prefixVirtualPath), None, true)
          } catch {
            case ex: RestartMergingWith =>
              thenLeafsUndoOperations.foreach(operation => virtualPathStore.undoOperation(operation))
              throw ex
          }
          resultExps
        case (_: Elssy, _: Thenny) =>
          val resultExps = exps.map(toCreateITEFor => {
            val ite = SymbolicITEExpression(
              predExp, toCreateITEFor.elseExp, toCreateITEFor.thenExp, Some(toCreateITEFor.identifier))
            (toCreateITEFor.identifier, ite: SymbolicExpression)
          })
          val CreateITEFor(exp, elseExp, identifier) = exps.head
          val elseLeafsUndoOperations = handleLeafExps(elseExp, identifier, prefixVirtualPath :+ NewThen, virtualPathStore, virtualPath2, getRealPath(prefixVirtualPath), Some(globalPathToThenExp), true)
          try {
            handleLeafExps(exp, identifier, prefixVirtualPath :+ NewElse, virtualPathStore, virtualPath1, getRealPath(prefixVirtualPath), None, false)
          } catch {
            case ex: RestartMergingWith =>
              elseLeafsUndoOperations.foreach(operation => virtualPathStore.undoOperation(operation))
              throw ex
          }
          resultExps
        case (VirtualThen, VirtualThen) =>
          loop(virtualPath1.tail, virtualPath2.tail, prefixVirtualPath :+ VirtualThen, exps)
        case (VirtualThen, RealThen) =>
          throw new Exception
        case (RealThen, VirtualThen) | (RealThen, RealThen) =>
          val newExps = exps.map(createIteFor => createIteFor.thenExp match {
            case SymbolicITEExpression(_, thenExp2, _, _) =>
              CreateITEFor(thenExp2, createIteFor.elseExp, createIteFor.identifier)
            case other =>
              throw new Exception(s"Expected a SymbolicITEExpession but got $other")
          })
          val results = loop(virtualPath1.tail, virtualPath2.tail, prefixVirtualPath :+ RealThen, newExps)
          results.zip(exps).map(resultExp => {
            val ((_, result), CreateITEFor(thenExp, _, identifier)) = resultExp
            val SymbolicITEExpression(predExp2, _, elseExp2, id2) = thenExp
            (identifier, SymbolicITEExpression(predExp2, result, elseExp2, id2))
          })
        case (VirtualElse, VirtualElse) =>
          loop(virtualPath1.tail, virtualPath2.tail, prefixVirtualPath :+ VirtualElse, exps)
        case (VirtualElse, RealElse) =>
          throw new Exception
        case (RealElse, VirtualElse) | (RealElse, RealElse) =>
          val newExps = exps.map(createIteFor => createIteFor.thenExp match {
            case SymbolicITEExpression(_, _, elseExp2, _) =>
              CreateITEFor(elseExp2, createIteFor.elseExp, createIteFor.identifier)
            case other => throw new Exception(
              s"Expected a SymbolicITEExpession but got $other for identifier ${createIteFor.identifier}")
          })
          val results = loop(virtualPath1.tail, virtualPath2.tail, prefixVirtualPath :+ RealElse, newExps)
          results.zip(exps).map(resultExp => {
            val ((_, result), CreateITEFor(thenExp, _, identifier)) = resultExp
            val SymbolicITEExpression(predExp2, thenExp2, _, id2) = thenExp
            (identifier, SymbolicITEExpression(predExp2, thenExp2, result, id2))
          })
      }
    }

    def mainLoop(toCreateITEFor: List[CreateITEFor]): List[(String, SymbolicExpression)] = {
      try {
        val recomputedVirtualPath1 = virtualPathStore.getVirtualPath(globalPathToThenExp).getOrElse(
          makePathVirtual(globalPathToThenExp))
        val recomputedVirtualPath2 = virtualPathStore.getVirtualPath(globalPathToElseExp).getOrElse(
          makePathVirtual(globalPathToElseExp))
        val result = loop(recomputedVirtualPath1, recomputedVirtualPath2, Nil, toCreateITEFor)
        result
      } catch {
        case RestartMergingWith(predExp2, globalPathToThenExp2, globalPathToElseExp2, redoToCreateITEFor) =>
          Logger.n(s"Had to restart merging for identifier ${redoToCreateITEFor.identifier}")
          // If globalPathToElse exp was used in the previous merge operation, flip around the then and else paths
          val wasSwitched = (globalPathToElseExp == globalPathToThenExp2)
          val (thenPathToUse, elsePathToUse) = if (wasSwitched) {
            (globalPathToElseExp2, globalPathToThenExp2)
          } else {
            (globalPathToThenExp2, globalPathToElseExp2)
          }
          val temp = new SymbolicITECreator(virtualPathStore, savesExps)
          val resultList = temp.createITE(
            predExp2, thenPathToUse, elsePathToUse, List(redoToCreateITEFor))
          assert(toCreateITEFor.length == 1)
          val newToCreateITEFor: List[CreateITEFor] = resultList.map(tuple => {
            CreateITEFor(tuple._2, toCreateITEFor.head.elseExp, toCreateITEFor.head.identifier)
          })
          mainLoop(newToCreateITEFor)
      }
    }

    val result = mainLoop(toCreateITEFor)
    virtualPathStore.finishStore()
    result
  }
}

object SymbolicITECreator {
  def findLastSharedNodeBetween[C <: Constraint](
    pathToThenExp: TreePath[C],
    pathToElseExp: TreePath[C]
  ): Option[(Path, Path)] = {
    val reverseOrder: List[SymbolicNodeWithConstraint[C]] = pathToThenExp.original.reverse
    // Find last node along the pathToThenExp that is shared between both paths
    val optSharedLastNode = reverseOrder.find(node => pathToElseExp.original.contains(node))

    /**
      * Get the Path that remains after finding `toFind` in the path.
      * Assumes `toFind` exists somewhere in the path.
      */
    @tailrec
    def findRemainder(treePath: TreePath[C], toFind: SymbolicNode[C]): Path = treePath.original.headOption match {
      case Some(head) if head == toFind => treePath.getPath
      case Some(_) => findRemainder(treePath.tail, toFind)
      case None => throw new Exception(s"Should not happen: shared node $toFind should have been found in path")
    }
    if (optSharedLastNode.isEmpty) {
      None
    } else {
      val sharedLastNode = optSharedLastNode.get
      val thenPath = findRemainder(pathToThenExp, sharedLastNode)
      val elsePath = findRemainder(pathToElseExp, sharedLastNode)
      Some((thenPath, elsePath))
    }
  }
}
