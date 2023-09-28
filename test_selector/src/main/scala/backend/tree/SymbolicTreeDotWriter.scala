package backend.tree

import java.io._

import backend.Main
import backend.execution_state.store._
import backend.tree.constraints._

import scala.annotation.tailrec
import scala.collection.mutable.{Map => MMap, Set => MSet}

class SymbolicTreeDotWriter[C <: Constraint]()
  (implicit val constraintHasStoreUpdates: HasStoreUpdates[C]) {
  type NodeId = Int
  type NodeTuple = (SymbolicNode[C], NodeId)

  private val nodeIdMap: MMap[SymbolicNodeWithConstraint[C], NodeId] = MMap()
  private val nodesProcessed: MSet[SymbolicNodeWithConstraint[C]] = MSet()
  private var id: NodeId = 0

  def writeTree(treeRoot: SymbolicNode[C], path: String): Unit = {
    reset()
    makeDirectories(path)
    val file = new File(path)
    val writer = new BufferedWriter(new FileWriter(file))
    writer.append("digraph G {\n")
    val rootId = writeNode(treeRoot, writer)
    loopTree(List((treeRoot, rootId)), writer)
    writer.append("}")
    writer.close()
  }
  private def reset(): Unit = {
    id = 0
    nodeIdMap.clear()
    nodesProcessed.clear()
  }

  private implicit class SymbolicNodeDotDescriptor(node: SymbolicNode[C]) {
    def color: Color = node match {
      case BranchSymbolicNode(_, _, _) => Colors.Yellow
      case MergedNode() => Colors.Red
      case RegularLeafNode() | UnexploredNode() => Colors.Green
      case SafeNode(_) => Colors.Pink
      case UnsatisfiableNode() => Colors.Black
    }
    def borderColor: Color = Colors.Black
    def label: String = node match {
      case bsn: BranchSymbolicNode[C] => constraintToString(bsn.constraint)
      case MergedNode() => "Merged"
      case RegularLeafNode() => "Leaf"
      case SafeNode(_) => "Safe"
      case UnexploredNode() => "Unexplored"
      case UnsatisfiableNode() => "Unsatisfiable"
    }
    private def constraintToString(constraint: C): String = {
      def withStoreUpdates(storeUpdates: Iterable[StoreUpdate]): String = {
        if (storeUpdates.isEmpty) {
          sanitizeString(constraint.toString)
        } else {
          s"[ ${sanitizeString(storeConstraintsToDot(storeUpdates))} ]<br/>${sanitizeString(constraint.toString)}"
        }
      }

      val storeUpdates = constraintHasStoreUpdates.storeUpdates(constraint)
      withStoreUpdates(storeUpdates)
    }
  }
  private def incId(): Int = {
    val temp = id
    id += 1
    temp
  }
  private def getNodeId(node: SymbolicNode[C]): Int = node match {
    case nwc: SymbolicNodeWithConstraint[C] => nodeIdMap.getOrElseUpdate(nwc, incId())
    case _ => incId()
  }

  private def sanitizeString(string: String): String = {
    List(
      ("&", "&amp;"),
      ("\"", "&quot;"),
      ("'", "&apos;"),
      ("<", "&lt;"),
      (">", "&gt;")
    ).foldLeft(string)((partiallySanitizedString, tuple) => {
      partiallySanitizedString.replace(tuple._1, tuple._2)
    })
  }

  private def writeNode(
    node: SymbolicNode[C],
    writer: Writer
  ): Int = {
    val nodeId = getNodeId(node)
    writer.append(s"\tnode_$nodeId[shape=box, xlabel=$nodeId, label=<${node.label}>, color=<${node.borderColor
    }>, fillcolor=<${node.color}>, style=<filled>];\n")
    nodeId
  }

  private def writeEdge(
    nodeId: Int,
    childNode: SymbolicNode[C],
    edgeLabel: String,
    writer: Writer
  ): (SymbolicNode[C], Int) = {
    val childNodeId = writeNode(childNode, writer)
    writer.append(s"node_$nodeId -> node_$childNodeId [label=<$edgeLabel>]\n")
    (childNode, childNodeId)
  }

  private def assignmentsConstraintToString(constraint: AssignmentsStoreUpdate): String = {
    constraint.assignments.map(tuple => s"${tuple._1} -> ${tuple._2}").toString
  }

  private def treeEdgeToString(edge: Edge[C]): String = {
    if (Main.useDebug) {
      "" + sanitizeString(storeConstraintsToDot(edge.storeUpdates))
    } else {
      ""
    }
  }

  /**
    *
    * @param workList   The work list of nodes and their ids that have to be written
    * @param writer The writer that writes the actual information to the output
    */
  @tailrec
  private def loopTree(
    workList: List[NodeTuple],
    writer: Writer
  ): Unit = {
    workList.headOption match {
      case None =>
      case Some((node, nodeId)) => node match {
        case MergedNode() | RegularLeafNode() | SafeNode(_) | UnexploredNode() | UnsatisfiableNode() =>
          loopTree(workList.tail, writer)
        case bsn@BranchSymbolicNode(_, thenEdge, elseEdge) =>
          if (!nodesProcessed.contains(bsn)) {
            val trueEdgeLabel = s"T ${treeEdgeToString(thenEdge)}"
            val falseEdgeLabel = s"F ${treeEdgeToString(elseEdge)}"
            val thenTuple = writeEdge(nodeId, thenEdge.to, trueEdgeLabel, writer)
            val elseTuple = writeEdge(nodeId, elseEdge.to, falseEdgeLabel, writer)
            nodesProcessed += bsn
            val newWorkList = thenTuple :: elseTuple :: workList.tail
            loopTree(newWorkList, writer)
          } else {
            loopTree(workList.tail, writer)
          }
      }
    }
  }

  private def storeConstraintsToDot(storeConstraints: Iterable[StoreUpdate]): String = {
    storeConstraints.foldLeft("")((string, constraint) => constraint match {
      case ac: AssignmentsStoreUpdate => string + assignmentsConstraintToString(ac)
      case ec: ExitScopeUpdate => string + ec.toString
      case _ => string
    })
  }

  private def makeDirectories(path: String): Unit = {
    val strings = path.split("/").map(_ + "/").scanLeft("") { (acc, token) =>
      acc + token
    }
    val firstAndLastRemoved = strings.tail.init
    firstAndLastRemoved.foreach(partialPath => {
      val file = new File(partialPath)
      if (!file.exists()) {
        file.mkdir()
      }
    })
  }

  /*
   * From: https://github.com/acieroid/scala-am/blob/master/src/main/scala/core/Graph.scala
   * Courtesy of Quentin Stiévenart
   */
  private case class Color(hex: String) {
    override def toString: String = hex
  }

  private object Colors {
    object Yellow extends Color("#FFFFDD")
    object Green extends Color("#DDFFDD")
    object Pink extends Color("#FFDDDD")
    object Red extends Color("#FF0000")
    object White extends Color("#FFFFFF")
    object Black extends Color("#000000")
  }

}
