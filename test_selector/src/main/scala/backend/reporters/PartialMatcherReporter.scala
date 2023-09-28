//package backend.reporters
//
//import backend.modes.Mode
//import backend.path_filtering.PartialRegexMatcher
//import backend.tree._
//import backend.tree.constraints.{Constraint, ConstraintNegater, Optimizer}
//import backend.tree.constraints.basic_constraints.BranchConstraint
//import backend.tree.follow_path.{ElseDirection, ThenDirection}
//import backend.{PartialMatcherPCElement, Path, PathConstraintWithMatchers}
//
//import scala.annotation.tailrec
//
//class PartialMatcherReporter[C <: Constraint : ConstraintNegater : Optimizer](implicit mode: Mode, startingProcess: Int)
//    extends BaseReporter[PartialRegexMatcher, PathConstraintWithMatchers[C], C] {
//
//  type PMSymbolicNode = SymbolicNode[PartialRegexMatcher, C]
//  type PMBranchSymbolicNode = BranchSymbolicNode[PartialRegexMatcher, C]
//  type PMSetChild = SetChild[PartialRegexMatcher]
//
//  protected var optRoot: Option[PMSymbolicNode] = None
//
//  def getRoot: Option[PMSymbolicNode] = optRoot
//  def setRoot(newRoot: PMSymbolicNode): Unit = {
//    optRoot = Some(newRoot)
//  }
//
//  /**
//    * Completely removes the symbolic tree that was already explored.
//    */
//  def deleteSymbolicTree(): Unit = {
//    optRoot = None
//  }
//
//  def addExploredPath(currentConstraint: PathConstraintWithMatchers[C],
//                      finishedTestRun: Boolean): Unit = {
//    internalAddExploredPath(currentConstraint,
//                            if (finishedTestRun) RegularLeafNode[PartialRegexMatcher, C]()
//                            else UnexploredNode[PartialRegexMatcher, C]())
//  }
//
//  import scala.language.implicitConversions
//  implicit protected def pathConstraintToPath(pathConstraint: PathConstraintWithMatchers[C]): Path = {
//    pathConstraint.map({
//      case PartialMatcherPCElement(_, false, _) => ElseDirection
//      case PartialMatcherPCElement(_, true, _)  => ThenDirection
//    })
//  }
//
//  private def internalAddExploredPath(constraints: PathConstraintWithMatchers[C],
//                                      terminationNode: SymbolicNode[PartialRegexMatcher, C]): Unit = {
//
//    if (constraints.isEmpty) {
//      return
//    } else if (optRoot.exists({ case SafeNode(_) => true; case _ => false })) {
//      return
//    } else if (constraints.head.optMatcher.isEmpty) {}
//
//    /* We have already checked whether the path constraint contains an initial partial matcher. */
//    val initialPartialMatcher: PartialRegexMatcher = constraints.head.optMatcher.get
//
//    /*
//     * Constraints contains at least one element, now find the first BranchConstraint in the list of constraints.
//     * Will throw an error if there are no BranchConstraints in the list of constraints.
//     */
//    val (prefix, (headConstraint, headIsTrue, maybeHeadPartialMatcher), remainder) =
//      findFirstBranchConstraintWithMatchers(constraints).get
//    /* Is there still a potential error reachable after dealing with the first sequence of non-BranchConstraints? */
//    val (potentialErrorPresent, prefixMatchedPartialMatcher) =
//      matchPrefix(prefix, initialPartialMatcher)
//    if (!potentialErrorPresent) {
//      optRoot = Some(SafeNode(optRoot.get))
//      return
//    }
//
//    /**
//      *
//      * @param setChild
//      * @param currentConstraintIsTrue
//      * @param partialMatcher The [[PartialRegexMatcher]] with which to check whether a path may lead to an error.
//      * @return True iff a potential error is reachable along the branch that was true according to currentConstraintIsTrue-parameter.
//      *         Also returns the incrementally updated partial matcher.
//      */
//    def makeNonErrorBranchInaccessibleAndCheckIfShouldContinue(
//        setChild: PMSetChild,
//        currentConstraintIsTrue: Boolean,
//        partialMatcher: PartialRegexMatcher): (Boolean, PartialRegexMatcher) = {
//      val path = if (currentConstraintIsTrue) List(ThenDirection) else List(ElseDirection)
//      val (shouldContinue, updatedPartialMatcher) = partialMatcher.incrementalMatch(path)
//      if (!shouldContinue) {
//        setChild.setSafeChild()
//        (false, updatedPartialMatcher)
//      } else {
//        (true, updatedPartialMatcher)
//      }
//    }
//
//    @tailrec
//    def loopWithPartialMatcher(currentConstraints: PathConstraintWithMatchers[C],
//                               setChild: PMSetChild,
//                               partialMatcher: PartialRegexMatcher): Unit = {
//      /*
//       * partialMatcher can soundly be matched to both branches. If the current head constraint is a BranchConstraint
//       * with a partial matcher, that matcher might be more precise BUT that matcher can only match against the branch
//       * that was actually taken for that constraint.
//       */
//      val errorPathsAlongBranchesResult = errorPathsAlongBranches(Nil, partialMatcher)
//      currentConstraints.headOption match {
//        case Some(
//            PartialMatcherPCElement(constraint: BranchConstraint,
//                                    currentConstraintIsTrue,
//                                    maybeNewPartialMatcher)) =>
//          /* Set the child of the parent node: either take the existing child if one exists, or make a new one. */
//          lazy val newNode =
//            branchConstraintToPMNode(constraint, maybeNewPartialMatcher.getOrElse(partialMatcher))
//          val childNode: PMSymbolicNode = setChild.existingChild
//          val node = if (childNode == UnexploredNode()) newNode else childNode
//          /*
//           * If child node already existed and was a safe node, the child node will still be the same
//           * safe node after applying the setChild.
//           */
//          setChild.setChild(node)
//
//          node match {
//            case MergedNode() | RegularLeafNode() | UnexploredNode() | UnsatisfiableNode() =>
//              throw new Exception("Should not happen: node should not be a EmptyNode")
//            case node: SafeNode[PartialRegexMatcher, C] =>
//              /* Stop expansion of the symbolic tree */
//              throw new Exception(s"Should not happen: $node should not be a PMSafeNode")
//            case node: PMBranchSymbolicNode =>
//              /*
//               * errorPathsAlongBranchesResult was computed via partialMatcher, which can soundly be used to find
//               * errors along either branch. maybeNewPartialMatcher (if it is defined) is more precise, but only
//               * for the branch that was taken with this constraint. Hence, we only (potentially) invalidate now the
//               * branch that was _not_ taken.
//               */
//              if (currentConstraintIsTrue && !errorPathsAlongBranchesResult.alongElseBranch) {
//                node.setElseBranch(SafeNode(node.elseBranch))
//              } else if (!currentConstraintIsTrue && !errorPathsAlongBranchesResult.alongThenBranch) {
//                node.setThenBranch(SafeNode(node.thenBranch))
//              }
//
//              val newSetChild = nodeToPMSetChild(node, currentConstraintIsTrue)
//              val partialMatcherToUse = maybeNewPartialMatcher.getOrElse(partialMatcher)
//              val (shouldContinue, newPartialMatcher) =
//                makeNonErrorBranchInaccessibleAndCheckIfShouldContinue(newSetChild,
//                                                                       currentConstraintIsTrue,
//                                                                       partialMatcherToUse)
//              if (shouldContinue) {
//                if (currentConstraintIsTrue) {
//                  assert(errorPathsAlongBranchesResult.alongThenBranch)
//                } else {
//                  assert(errorPathsAlongBranchesResult.alongElseBranch)
//                }
//                loopWithPartialMatcher(currentConstraints.tail, newSetChild, newPartialMatcher)
//              } else {
//                /*
//               * If the path that is currently followed does not potentially lead to an error, there is no point in
//               * continuing to expand the symbolic tree (the corresponding path has also been made inaccessible),
//               * so stop looping.
//               */
//              }
//          }
//
//        case None => setChild.setChild(terminationNode)
//      }
//    }
//
//    val headBranchConstraintPartialMatcher =
//      maybeHeadPartialMatcher.getOrElse(prefixMatchedPartialMatcher)
//    // Make sure root exists.
//    optRoot = Some(
//      optRoot.getOrElse(
//        branchConstraintToPMNode(headConstraint, headBranchConstraintPartialMatcher)))
//    val root = optRoot.get.asInstanceOf[PMBranchSymbolicNode]
//    val setChildOfRoot = nodeToPMSetChild(root, headIsTrue)
//    val (shouldContinue, headMatchedPartialMatcher) =
//      makeNonErrorBranchInaccessibleAndCheckIfShouldContinue(setChildOfRoot,
//                                                             headIsTrue,
//                                                             headBranchConstraintPartialMatcher)
//    if (shouldContinue) {
//      loopWithPartialMatcher(remainder, setChildOfRoot, headMatchedPartialMatcher)
//    }
//  }
//
//  def nodeToPMSetChild(node: PMBranchSymbolicNode, constraintIsTrue: Boolean): PMSetChild = {
//    if (constraintIsTrue) {
//      SetChildThenBranch(node)
//    } else {
//      SetChildElseBranch(node)
//    }
//  }
//
//  protected def errorPathsAlongBranches(
//      currentPath: Path,
//      partialMatcher: PartialRegexMatcher): ErrorPathsAlongBranches = {
//    val currentPathFollowingElse = currentPath :+ ElseDirection
//    val currentPathFollowingThen = currentPath :+ ThenDirection
//    val isErrorViaElse = partialMatcher.tentativeIncrementalMatch(currentPathFollowingElse)
//    val isErrorViaThen = partialMatcher.tentativeIncrementalMatch(currentPathFollowingThen)
//    ErrorPathsAlongBranches(isErrorViaElse, isErrorViaThen)
//  }
//
//  /**
//    * Finds the first BranchConstraint in the list of constraints, if there is one, and returns a triple of
//    * all non-BranchConstraints before the BranchConstraints, the BranchConstraint itself, and all constraints
//    * that follows this BranchConstraint.
//    * @param constraints
//    * @return
//    */
//  protected def findFirstBranchConstraintWithMatchers(constraints: PathConstraintWithMatchers[C])
//    : Option[(PathConstraintWithMatchers[C],
//              (BranchConstraint, Boolean, Option[PartialRegexMatcher]),
//              PathConstraintWithMatchers[C])] = {
//    val (prefix, remainder) = constraints.span({
//      case PartialMatcherPCElement(BranchConstraint(_), _, _) => false
//      case _                                                  => true
//    })
//    remainder match {
//      case Nil => None
//      /* If remainder is not empty, its first element should be a tuple where the first field is a BranchConstraint */
//      case PartialMatcherPCElement(headConstraint: BranchConstraint,
//                                   headConstraintTrue,
//                                   maybePartialMatcher) :: rest =>
//        Some((prefix, (headConstraint, headConstraintTrue, maybePartialMatcher), rest))
//    }
//  }
//
//  /**
//    * Initial partial matcher should be a partial matcher that was generated *BEFORE* the first constraint of the
//    * prefix constraints was evaluated.
//    * @param prefixConstraints
//    * @param initialPartialMatcher
//    * @return
//    */
//  protected def matchPrefix(
//      prefixConstraints: PathConstraintWithMatchers[C],
//      initialPartialMatcher: PartialRegexMatcher): (Boolean, PartialRegexMatcher) = {
//    def loop(constraints: PathConstraintWithMatchers[C],
//             lastMatcher: PartialRegexMatcher): (Boolean, PartialRegexMatcher) =
//      constraints.headOption match {
//        case None => (true, lastMatcher)
//        case Some(PartialMatcherPCElement(b: BranchConstraint, _, _)) =>
//          throw new Exception(
//            "Should not happen: constraints should solely consist of UnusableConstraints")
//      }
//    loop(prefixConstraints, initialPartialMatcher)
//  }
//
//  protected def branchConstraintToPMNode(branch: C,
//                                         pm: PartialRegexMatcher): PMBranchSymbolicNode = {
//    BranchSymbolicNode(branch, UnexploredNode(), UnexploredNode(), pm)
//  }
//
//  def mergePath(currentConstraints: PathConstraintWithMatchers[C]): Unit = {
//    internalAddExploredPath(currentConstraints, MergedNode[PartialRegexMatcher, C]())
//  }
//
//  protected def matchConstraint(
//      constraintIsTrue: Boolean,
//      partialMatcher: PartialRegexMatcher): (Boolean, PartialRegexMatcher) = {
//    val path = if (constraintIsTrue) List(ThenDirection) else List(ElseDirection)
//    val (matched, newPartialMatcher) = partialMatcher.incrementalMatch(path)
//    if (matched) {
//      (true, newPartialMatcher)
//    } else {
//      (false, partialMatcher)
//    }
//  }
//
//  /* Making a separate case class here, instead of just using a tuple, to minimize the risk of confusing one branch for the other. */
//  protected case class ErrorPathsAlongBranches(alongElseBranch: Boolean, alongThenBranch: Boolean)
//
//}
