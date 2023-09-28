package backend.tree.merging.use_cases.automatically_generated

import backend.expression.Util.input

class RandomTreeConstructor(assignmentsGenerator: GeneratesAssignments[Int], startingInputId: Int = 0) {
  def constructRandomTree(maxInputId: Int, maxValue: Int): GeneratedTreeNode = {
    def loop(inputId: Int, value: Int, path: String): GeneratedTreeNode = {
      if (inputId > maxInputId) {
        LeafNode(path)
      } else if (value > maxValue) {
        loop(inputId + 1, 0, path + 'E')
      } else {
        val optThenAssignment = assignmentsGenerator.generateAssignment()
        val thenNode = loop(inputId + 1, 0, path + 'T')

        val optElseAssignment = assignmentsGenerator.generateAssignment()
        val elseNode = loop(inputId, value + 1, path + 'E')

        BranchNode(
          input(inputId),
          value,
          To(thenNode, optThenAssignment),
          To(elseNode, optElseAssignment))
      }
    }
    loop(startingInputId, 0, "")
  }
}
