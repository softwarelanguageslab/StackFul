package backend.tree

import backend.tree.constraints.Optimizer

trait ConstantChecker[T] {
  def isConstant(t: T): Boolean
}

trait Sanitizer {
  def sanitize[T: Optimizer : ConstantChecker](list: List[T]): List[T]
}

object IdentitySanitizer extends Sanitizer {
  def sanitize[T: Optimizer : ConstantChecker](list: List[T]): List[T] = list
}

object OnlyDistinctSanitizer extends Sanitizer {
  def sanitize[T: Optimizer : ConstantChecker](list: List[T]): List[T] = {
    val distinctList = list.distinct
    distinctList
  }
}

object RemoveConstantsSanitizer extends Sanitizer {
  def sanitize[T: Optimizer : ConstantChecker](list: List[T]): List[T] = {
    val optimizer = implicitly[Optimizer[T]]
    val constantChecker = implicitly[ConstantChecker[T]]
    val optimizedList = list.map(optimizer.optimize)
    val filteredList = optimizedList.filter(!constantChecker.isConstant(_))
    filteredList
  }
}
