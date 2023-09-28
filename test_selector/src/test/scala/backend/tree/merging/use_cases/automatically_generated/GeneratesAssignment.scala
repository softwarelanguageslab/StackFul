package backend.tree.merging.use_cases.automatically_generated

import backend.execution_state.store.AssignmentsStoreUpdate
import backend.expression._

import scala.util.Random

case class GenerateAssignmentsStoreUpdate[T](willBecomeAssignments: List[(String, GeneratedValue[T])]) {
  def generate(store: Store[T]): (AssignmentsStoreUpdate, Store[T]) = {
//    val assignments = willBecomeAssignments.map(tuple => {
//      val (exp, value) = tuple._2.generate(store)
//      (tuple._1, (exp, value))
//    })
//    val storeUpdate = AssignmentsStoreUpdate(assignments.toMap.view.mapValues(_._1).toMap)
//    val newStore = assignments.foldLeft(store)((store, tuple) => {
//      store + (tuple._1 -> tuple._2)
//    })
//    (storeUpdate, newStore)

    val assignments = willBecomeAssignments.foldLeft[(List[(String, (SymbolicExpression, T))], Store[T])]((Nil,store))(
      (accTuple, willBecomeAssignment) => {
        val (listAcc, mapAcc) = accTuple
        val identifier = willBecomeAssignment._1
//        val storeToUse = mapAcc.get(identifier) match {
//          case None => store
//          case Some(alreadyUpdatedValue) => store.updated(identifier, alreadyUpdatedValue)
//        }
        val (exp, value) = willBecomeAssignment._2.generate(mapAcc)
        val toAppend = (identifier, (exp, value))
        (listAcc :+ toAppend, mapAcc + (identifier -> (exp, value)))
      })._1
      val storeUpdate = AssignmentsStoreUpdate(assignments.map(tuple => (tuple._1, tuple._2._1)))
      val newStore = assignments.foldLeft(store)((store, tuple) => {
        store + (tuple._1 -> tuple._2)
      })
      (storeUpdate, newStore)
  }
}

trait GeneratesAssignments[T] {
  def generateAssignment(): Option[GenerateAssignmentsStoreUpdate[T]]
}

trait UsesRandomInt {
  def randomInt(): Int = {
    Random.nextInt(20)
  }
}

class FixedAssignmentGenerator[T](
  val identifier: String,
  val exp: SymbolicExpression,
  val value: T
) extends GeneratesAssignments[T] {
  def generateAssignment(): Option[GenerateAssignmentsStoreUpdate[T]] = {
    Some(GenerateAssignmentsStoreUpdate(List((identifier, ReturnLiteral(exp, value)))))
  }
}

trait AssignsRandomIdentifier[T] extends GeneratesAssignments[T] {
  val identifiers: Set[String]
  val identifiersAsList: List[String] = identifiers.toList
  val nrOfIdentifiers: Int = identifiers.size
  def generateValue(identifier: String): GeneratedValue[T]
  def generateAssignment(): Option[GenerateAssignmentsStoreUpdate[T]] = {
    val nrOfIdentifiersToChoose = Random.nextInt(nrOfIdentifiers) + 1
    val identifiersWithValues: List[(String, GeneratedValue[T])] = 1
      .to(nrOfIdentifiersToChoose)
      .map(_ => {
        val identifier: String = selectRandomIdentifier()
        (identifier, generateValue(identifier))
      }).toList
    if (identifiersWithValues.nonEmpty) {
      Some(GenerateAssignmentsStoreUpdate(identifiersWithValues))
    } else {
      None
    }
  }
  def selectRandomIdentifier(): String = {
    val randomIdentifierIndex = Random.nextInt(nrOfIdentifiers)
    identifiersAsList(randomIdentifierIndex)
  }
}

sealed trait GeneratedValue[T] {
  def generate(store: Store[T]): (SymbolicExpression, T)
}
case class ReturnIdentifier[T](readFrom: String, assignTo: String) extends GeneratedValue[T] {
  def generate(store: Store[T]): (SymbolicExpression, T) = {
    val read = store(readFrom)
    (read._1.replaceIdentifier(Some(readFrom)), read._2)
  }
}
case class ReturnLiteral[T](literalExp: SymbolicExpression, literalValue: T) extends GeneratedValue[T] {
  def generate(ignored: Store[T]): (SymbolicExpression, T) = {
    (literalExp, literalValue)
  }
}
case class ReturnComposite[T](
  left: GeneratedValue[T],
  right: GeneratedValue[T],
  createExp: (SymbolicExpression, SymbolicExpression) => SymbolicExpression,
  createValue: (T, T) => T
) extends GeneratedValue[T] {
  def generate(store: Store[T]): (SymbolicExpression, T) = {
    val (leftExp, leftValue) = left.generate(store)
    val (rightExp, rightValue) = right.generate(store)
    val exp = createExp(leftExp, rightExp)
    val value = createValue(leftValue, rightValue)
    (exp, value)
  }
}

class RandomLiteralAssignmentGenerator(val identifiers: Set[String])
  extends AssignsRandomIdentifier[Int]
    with UsesRandomInt {
  override def generateValue(identifier: String): GeneratedValue[Int] = {
    val r = randomInt()
    ReturnLiteral(SymbolicInt(r), r)
  }
}

class RandomExpAssignmentGenerator(val identifiers: Set[String])
  extends AssignsRandomIdentifier[Int]
    with UsesRandomInt {

  override def generateValue(identifier: String): GeneratedValue[Int] = {
    val choice = Random.nextInt(10)
    if (choice < 5) {
      val r = randomInt()
      ReturnLiteral(SymbolicInt(r), r)
    } else if (choice < 9) {
      val randomIdentifier = selectRandomIdentifier()
      ReturnIdentifier(randomIdentifier, identifier)
    } else {
      val operatorChoice = Random.nextInt(3)
      if (operatorChoice == 0) {
        generateArithmeticOperation(identifier, IntPlus, _ + _)
      } else if (operatorChoice == 1) {
        generateArithmeticOperation(identifier, IntMinus, _ - _)
      } else {
        generateArithmeticOperation(identifier, IntTimes, _ * _)
      }
    }
  }
  protected def generateArithmeticOperation(
    identifier: String,
    operator: IntegerArithmeticalOperator,
    operation: (Int, Int) => Int
  ): GeneratedValue[Int] = {
    val left = generateValue(identifier)
    val right = generateValue(identifier)
    def createExp(leftExp: SymbolicExpression, rightExp: SymbolicExpression): SymbolicExpression = {
      ArithmeticalVariadicOperationExpression(operator, List(leftExp, rightExp))
    }
    ReturnComposite(left, right, createExp, operation)
  }
}