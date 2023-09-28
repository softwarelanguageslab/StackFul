package backend.path_filtering

import dk.brics.automaton.{Automaton, State}

import scala.annotation.tailrec

case class PartialRegexMatcher(automaton: Automaton, initialState: State, lastState: State) {

  def this(automaton: Automaton) = {
    this(automaton, automaton.getInitialState, automaton.getInitialState)
  }

  def partialMatch(string: String): Boolean = {
    tryPartialMatch(initialState, string).representsMatch
  }
  private def tryPartialMatch(startState: State, string: String): MatchResult = {
    @tailrec
    def loop(currentState: State, currentString: String): MatchResult =
      currentString.headOption match {
        case None =>
          if (currentState.isAccept) {
            CompleteMatch(currentState)
          } else {
            PartialMatch(currentState)
          }
        case Some(head) =>
          val transitions = scala.collection.JavaConverters.asScalaSet(currentState.getTransitions)
          transitions.find(_.getMin == head) match {
            case None =>
              /* Transition required that does not exist in the automaton */
              NoMatch
            case Some(transition) =>
              loop(transition.getDest, currentString.tail)
          }
      }
    loop(startState, string)
  }
  /**
    * Similar to [[partialMatch]], but starts checking the string from the final, cached state that was reached in the
    * last incremental match.
    * If no final state was cached yet, e.g., because this is the first incremental match, starts from the automaton's
    * initial state.
    *
    * If the string does not match, the final state is not cached.
    */
  def incrementalMatch(string: String): (Boolean, PartialRegexMatcher) = {
    val matched = incrementalMatchAndMaybeUpdateState(string)
    (matched.representsMatch, this.copy(lastState = matched.terminatingState))
  }
  /**
    * Similar to [[partialMatch]], but starts checking the string from the final, cached state that was reached in the
    * last incremental match.
    */
  private def incrementalMatchAndMaybeUpdateState(string: String): MatchResult = {
    tryPartialMatch(lastState, string) match {
      case PartialMatch(endState) =>
        PartialMatch(endState)
      case CompleteMatch(endState) =>
        CompleteMatch(endState)
      case NoMatch =>
        /* Don't reassign lastState */
        NoMatch
    }
  }
  def tentativeIncrementalMatch(string: String): Boolean = {
    incrementalMatchAndMaybeUpdateState(string).representsMatch
  }

  sealed trait MatchResult {
    def representsMatch: Boolean
    def terminatingState: State
  }

  case class PartialMatch(terminatingState: State) extends MatchResult {
    override def representsMatch: Boolean = true
  }

  case class CompleteMatch(terminatingState: State) extends MatchResult {
    override def representsMatch: Boolean = true
  }

  case object NoMatch extends MatchResult {
    override def terminatingState: State = lastState
    override def representsMatch: Boolean = false
  }

}
