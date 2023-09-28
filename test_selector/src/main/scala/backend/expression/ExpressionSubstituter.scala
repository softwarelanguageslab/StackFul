package backend.expression

import backend.Path
import backend.execution_state.{ExecutionState, SymbolicStore}
import backend.tree.OffersVirtualPath

object ExpressionSubstituter {

  def replaceAtTopLevel(
    store: SymbolicStore,
    exp: SymbolicExpression
  ): SymbolicExpression = exp match {
    case SymbolicBool(_, id) if id.exists(store.contains) => store(id.get).replaceIdentifier(id)
    case SymbolicFloat(_, id) if id.exists(store.contains) => store(id.get).replaceIdentifier(id)
    case SymbolicInt(_, id) if id.exists(store.contains) => store(id.get).replaceIdentifier(id)
    case SymbolicString(_, id) if id.exists(store.contains) => store(id.get).replaceIdentifier(id)
    case input: SymbolicInput if input.identifier.exists(store.contains) => store(input.identifier.get).replaceIdentifier(input.identifier)
//    case SymbolicIntVariable(input) =>
//      SymbolicIntVariable(substitute(identifier, input, makeSubstitute).asInstanceOf[SymbolicInputInt])
    case StringOperationProducesStringExpression(op, args, id) =>
      if (id.exists(store.contains)) {
        store(id.get).replaceIdentifier(id)
      } else {
        val processedArgs = args.map(replaceAtTopLevel(store, _))
        val processedExp = StringOperationProducesStringExpression(op, processedArgs, id)
        processedExp
      }
    case StringOperationProducesIntExpression(op, args, id) =>
      if (id.exists(store.contains)) {
        store(id.get).replaceIdentifier(id)
      } else {
        val processedArgs = args.map(replaceAtTopLevel(store, _))
        val processedExp = StringOperationProducesIntExpression(op, processedArgs, id)
        processedExp
      }
    case RelationalExpression(left, op, right, id) =>
      if (id.exists(store.contains)) {
        store(id.get).replaceIdentifier(id)
      } else {
        val processedLeft = replaceAtTopLevel(store, left)
        val processedRight = replaceAtTopLevel(store, right)
        val processedExp = RelationalExpression(processedLeft, op, processedRight, id)
        processedExp
      }
    case LogicalBinaryExpression(left, op, right, id) =>
      if (id.exists(store.contains)) {
        store(id.get).replaceIdentifier(id)
      } else {
        val processedLeft = replaceAtTopLevel(store, left).asInstanceOf[BooleanExpression]
        val processedRight = replaceAtTopLevel(store, right).asInstanceOf[BooleanExpression]
        val processedExp = LogicalBinaryExpression(processedLeft, op, processedRight, id)
        processedExp
      }
    case ArithmeticalVariadicOperationExpression(op, args, id) =>
      if (id.exists(store.contains)) {
        store(id.get).replaceIdentifier(id)
      } else {
        val processedArgs = args.map(replaceAtTopLevel(store, _))
        val processedExp = ArithmeticalVariadicOperationExpression(op, processedArgs, id)
        processedExp
      }
    case ArithmeticalUnaryOperationExpression(op, arg, id) =>
      if (id.exists(store.contains)) {
        store(id.get).replaceIdentifier(id)
      } else {
        val processedArg = replaceAtTopLevel(store, arg)
        val processedExp = ArithmeticalUnaryOperationExpression(op, processedArg, id)
        processedExp
      }
    case LogicalUnaryExpression(op, arg, id) =>
      if (id.exists(store.contains)) {
        store(id.get).replaceIdentifier(id)
      } else {
        val processedArg = replaceAtTopLevel(store, arg).asInstanceOf[BooleanExpression]
        val processedExp = LogicalUnaryExpression(op, processedArg, id)
        processedExp
      }
    case SymbolicITEExpression(predExp, thenExp, elseExp, id) =>
      if (id.exists(store.contains)) {
        store(id.get).replaceIdentifier(id)
      } else {
        val processedPredExp = replaceAtTopLevel(store, predExp).asInstanceOf[BooleanExpression]
        val processedThenExp = replaceAtTopLevel(store, thenExp)
        val processedElseExp = replaceAtTopLevel(store, elseExp)
        val processedExp = SymbolicITEExpression(processedPredExp, processedThenExp, processedElseExp, id)
        processedExp
      }
    case other => other
  }

  def replaceAtTopLevels(
    exp: SymbolicExpression,
    store: SymbolicStore,
    edgeExitIdentifiers: Set[String]
  ): SymbolicExpression = {
    replaceAtTopLevel(store, exp)
//    store.foldLeft(exp)((accExp, keyValue) => {
//      if (edgeExitIdentifiers.contains(keyValue._1)) {
//        throw OutOfScopeIdentifierDiscovered(Set(keyValue._1))
//      } else {
//        replaceAtTopLevel(keyValue._1, accExp, keyValue._2)
//      }
//    })
  }

  def substitute(
    identifier: String,
    exp: SymbolicExpression,
    makeSubstitute: SymbolicExpression => SymbolicExpression
  ): SymbolicExpression =
    exp match {
      case SymbolicITEExpression(predExp, thenExp, elseExp, id) =>
        val processedPredExp = substitute(identifier, predExp, makeSubstitute).asInstanceOf[BooleanExpression]
        val processedThenExp = substitute(identifier, thenExp, makeSubstitute)
        val processedElseExp = substitute(identifier, elseExp, makeSubstitute)
        val processedExp = SymbolicITEExpression(processedPredExp, processedThenExp, processedElseExp, id)
        if (id.contains(identifier)) makeSubstitute(processedExp) else processedExp
      case SymbolicBool(_, id) if id.contains(identifier) => makeSubstitute(exp)
      case SymbolicFloat(_, id) if id.contains(identifier) => makeSubstitute(exp)
      case SymbolicInt(_, id) if id.contains(identifier) => makeSubstitute(exp)
      case SymbolicString(_, id) if id.contains(identifier) => makeSubstitute(exp)
      case input: SymbolicInput if input.identifier.contains(identifier) => makeSubstitute(exp)
      case SymbolicIntVariable(input) =>
        SymbolicIntVariable(
          substitute(identifier, input, makeSubstitute).asInstanceOf[SymbolicInputInt])
      case StringOperationProducesStringExpression(op, args, id) =>
        val processedArgs = args.map(substitute(identifier, _, makeSubstitute))
        val processedExp = StringOperationProducesStringExpression(op, processedArgs, id)
        if (id.contains(identifier)) makeSubstitute(processedExp) else processedExp
      case StringOperationProducesIntExpression(op, args, id) =>
        val processedArgs = args.map(substitute(identifier, _, makeSubstitute))
        val processedExp = StringOperationProducesIntExpression(op, processedArgs, id)
        if (id.contains(identifier)) makeSubstitute(processedExp) else processedExp
      case RelationalExpression(left, op, right, id) =>
        val processedLeft = substitute(identifier, left, makeSubstitute)
        val processedRight = substitute(identifier, right, makeSubstitute)
        val processedExp = RelationalExpression(processedLeft, op, processedRight, id)
        if (id.contains(identifier)) makeSubstitute(processedExp) else processedExp
      case LogicalBinaryExpression(left, op, right, id) =>
        val processedLeft =
          substitute(identifier, left, makeSubstitute).asInstanceOf[BooleanExpression]
        val processedRight =
          substitute(identifier, right, makeSubstitute).asInstanceOf[BooleanExpression]
        val processedExp = LogicalBinaryExpression(processedLeft, op, processedRight, id)
        if (id.contains(identifier)) makeSubstitute(processedExp) else processedExp
      case ArithmeticalVariadicOperationExpression(op, args, id) =>
        val processedArgs = args.map(substitute(identifier, _, makeSubstitute))
        val processedExp = ArithmeticalVariadicOperationExpression(op, processedArgs, id)
        if (id.contains(identifier)) makeSubstitute(processedExp) else processedExp
      case ArithmeticalUnaryOperationExpression(op, arg, id) =>
        val processedArg = substitute(identifier, arg, makeSubstitute)
        val processedExp = ArithmeticalUnaryOperationExpression(op, processedArg, id)
        if (id.contains(identifier)) makeSubstitute(processedExp) else processedExp
      case LogicalUnaryExpression(op, arg, id) =>
        val processedArg =
          substitute(identifier, arg, makeSubstitute).asInstanceOf[BooleanExpression]
        val processedExp = LogicalUnaryExpression(op, processedArg, id)
        if (id.contains(identifier)) makeSubstitute(processedExp) else processedExp
      case other => other
    }

  def merge(
    executionState: ExecutionState,
    exp: SymbolicExpression,
    mergingPredicateExp: BooleanExpression,
    store: SymbolicStore,
    pathToExpNode: Path,
    pathToElse: Path,
    virtualPathStore: OffersVirtualPath,
  ): SymbolicExpression = {
    def try1Identifier(
      exp: SymbolicExpression,
      identifier: String,
      storedExp: SymbolicExpression,
    ): SymbolicExpression = {
      substitute(
        identifier,
        exp,
        processedExp => if (processedExp == storedExp) {
          processedExp
        } else {
          val virtualPathStoreForIdentifier = virtualPathStore.getVirtualPathStoreForIdentifier(identifier)
          val iteCreator = new SymbolicITECreator(virtualPathStoreForIdentifier)
          iteCreator.createITE(mergingPredicateExp, pathToExpNode, pathToElse, processedExp, storedExp, identifier)
        })
    }
    store.toList
    val result = store.foldLeft(exp)((accExp, keyValue) => try1Identifier(accExp, keyValue._1, keyValue._2))
    result
  }

  def replace(exp: SymbolicExpression, store: SymbolicStore): SymbolicExpression = {
    store.foldLeft(exp)((accExp, keyValue) => substitute(keyValue._1, accExp, _ => keyValue._2))
  }
}
