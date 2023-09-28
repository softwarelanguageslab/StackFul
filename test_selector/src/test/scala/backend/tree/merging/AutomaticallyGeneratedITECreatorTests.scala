package backend.tree.merging

import backend.SetUseDebugToTrueTester
import backend.expression.Util._
import backend.expression._

import scala.annotation.tailrec
import scala.util.Random

class AutomaticallyGeneratedITECreatorTests
  extends SetUseDebugToTrueTester
    with TestsSymbolicITECreator
    with RandomlyGeneratedTests {

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset()
  }

  protected def choosePathToMergeWith(pathToAdd: String, pathsToChooseFrom: List[String]): Option[(Int, String)] = {
    var bestMatch: Option[(Int, String)] = None
    @tailrec
    def findMatchBetween(path1: String, path2: String, matchSize: Int): Int = {
      if (path1.isEmpty) {
        matchSize
      } else if (path1.head == path2.head) {
        findMatchBetween(path1.tail, path2.tail, matchSize + 1)
      } else {
        matchSize
      }
    }
    if (pathsToChooseFrom.isEmpty) {
      None
    } else {
      pathsToChooseFrom.foreach(path => {
        val matchSize = findMatchBetween(path, pathToAdd, 0)
        if (bestMatch.isEmpty) {
          bestMatch = Some((matchSize, path))
        } else if (matchSize > bestMatch.get._1) {
          bestMatch = Some((matchSize, path))
        }
      })
      Some(bestMatch.get)
    }
  }

  test(s"Simulate adding and merging 20 randomly generated trees") {
    val treeHeight = 5
    /**
      * var x = "";
      * if (i0 == 0) { x += "T"; } else { x += "E""; }
      * if (i1 == 1) { x += "T"; } else { x += "E""; }
      * if (i2 == 2) { x += "T"; } else { x += "E""; }
      * if (i3 == 3) { x += "T"; } else { x += "E""; }
      */

    def checkAllPaths(seed: Int): Unit = {
      scala.util.Random.setSeed(seed)
      def generateListOfPaths: List[String] = {
        var allPaths: List[String] = Nil
        def loop(i: Int, prefix: String): Unit = {
          if (i >= treeHeight) {
            allPaths :+= prefix
          } else {
            loop(i + 1, prefix :+ 'T')
            loop(i + 1, prefix :+ 'E')
          }
        }
        loop(0, "")
        Random.shuffle(allPaths)
      }
      val listOfPaths = generateListOfPaths
      def pathToInputs(path: String): List[(SymbolicInputInt, Int)] = {
        path.toList.zipWithIndex.map(tuple => {
          val (direction, idx) = tuple
          if (direction == 'T') {
            (input(idx), idx)
          } else {
            (input(idx), 42)
          }
        })
      }
      def idxToPred(idx: Int): RelationalExpression = {
        RelationalExpression(input(idx), IntEqual, idx)
      }
      var pathsAdded: List[String] = Nil
      def doAllChecks(iteToCheck: SymbolicExpression): Unit = {
        pathsAdded.foreach(path => {
          val inputsWithValues = pathToInputs(path)
          CheckITEExpressionAsserter.checkITEExpression(inputsWithValues, iteToCheck, path)
        })
      }
      listOfPaths.zipWithIndex.foldLeft[Option[SymbolicExpression]](None)((optIte, pathIdxTuple) => {
        val (path, testIteration) = pathIdxTuple
        println(s"Test iteration $testIteration, adding path $path")
        val optPathToMerge = choosePathToMergeWith(path, pathsAdded)
        pathsAdded :+= path
        if (optPathToMerge.isEmpty) {
          optIte
        } else {
          val (idx, pathToMergeWith) = optPathToMerge.get
          val newITE = newIteCreator.createITE(
            idxToPred(idx), pathToMergeWith, path, optIte.getOrElse(s(pathToMergeWith, "x")), s(path, "x"), "x")
          doAllChecks(newITE)
          Some(newITE)
        }
      })
    }

    1.to(nrOfTreesToCreate).foreach(seed => {
      beforeEach()
      printSeed(seed)
      checkAllPaths(seed)
    })
  }

  test("Randomly generate paths for 20 non-uniform binary trees and try to merge then") {
    val maxInput = 2
    val maxExpectedValue = 4

    def generateListOfPaths: List[String] = {
      var allPaths: Set[String] = Set()
      def nrOfElseThensToString(nrOfElseThens: Int): String = 1.to(nrOfElseThens).map(_ => 'E').mkString
      def loop(inputId: Int, toCompareWith: Int, prefix: String): Unit = {
        if (inputId > maxInput) {
          allPaths += prefix
        } else {
          loop(inputId + 1, 0, prefix :+ 'T')
          val nrOfElseThensToGenerate = Random.nextInt(maxExpectedValue)
          1.to(nrOfElseThensToGenerate).foreach(idx => {
            val elseThensAddedToPrefix = nrOfElseThensToString(idx) :+ 'T'
            loop(inputId + 1, 0, prefix + elseThensAddedToPrefix)
          })
          val elses = nrOfElseThensToString(nrOfElseThensToGenerate) :+ 'E'
          loop(inputId + 1, 0, prefix + elses)
        }
      }
      loop(0, 0, "")
      println(s"Generated ${allPaths.size} paths")
      Random.shuffle(allPaths.toList)
    }

    def pathToExpectedInputValues(path: String): List[Int] = {
      @tailrec
      def loop(path: String, values: List[Int], expectedValue: Int): List[Int] = path.headOption match {
        case Some('T') => loop(path.tail, values :+ expectedValue, 0)
        case Some('E') => loop(path.tail, values, expectedValue + 1)
        case None => values
      }
      loop(path, Nil, 0)
    }

    def checkAllPaths(seed: Int): Unit = {
      scala.util.Random.setSeed(seed)
      val allPaths = generateListOfPaths

      def pathsToPred(path1: String, path2: String): RelationalExpression = {
        @tailrec
        def loop(path1: String, path2: String, inputId: Int, expectedValue: Int): RelationalExpression = {
          (path1.headOption, path2.headOption) match {
            case (Some('T'), Some('T')) => loop(path1.tail, path2.tail, inputId + 1, 0)
            case (Some('E'), Some('E')) => loop(path1.tail, path2.tail, inputId, expectedValue + 1)
            case (Some(_), Some(_)) => RelationalExpression(input(inputId), IntEqual, expectedValue)
            case (None, None) => throw new Exception("Should not happen: paths should not be identical")
            case (Some(_), None) | (None, Some(_)) =>
              throw new Exception(
                "Should not happen: should have found a difference between paths before reaching end of one path")
          }
        }
        loop(path1, path2, 0, 0)
      }

      var pathsAdded: List[String] = Nil
      def doAllChecks(iteToCheck: SymbolicExpression): Unit = {
        pathsAdded.foreach(path => {
          val inputsWithValues = pathToExpectedInputValues(path).zipWithIndex.map(tuple => {
            val (value, idx) = tuple
            (input(idx), value)
          })
          CheckITEExpressionAsserter.checkITEExpression(inputsWithValues, iteToCheck, path)
        })
      }
      allPaths.zipWithIndex.foldLeft[Option[SymbolicExpression]](None)((optIte, pathIdxTuple) => {
        val (path, testIteration) = pathIdxTuple
        println(s"Test iteration $testIteration, adding path $path")
        val optPathToMerge = choosePathToMergeWith(path, pathsAdded)
        pathsAdded :+= path
        if (optPathToMerge.isEmpty) {
          optIte
        } else {
          val (_, pathToMergeWith) = optPathToMerge.get
          val newITE = newIteCreator.createITE(
            pathsToPred(pathToMergeWith, path), pathToMergeWith, path, optIte.getOrElse(s(pathToMergeWith, "x")),
            s(path, "x"), "x")
          doAllChecks(newITE)
          Some(newITE)
        }
      })
    }

    1.to(nrOfTreesToCreate).foreach(seed => {
      beforeEach()
      printSeed(seed)
      checkAllPaths(seed)
    })

  }

}
