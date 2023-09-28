package backend.solvers.Z3Solver

import scala.util.matching.Regex

object Z3StringSanitizer {

  implicit private class ExtendedString(val string: String) {
    def replaceDoubleQuotes: String = {
      string.replaceAll("\"\"", "\"")
    }
    // From: https://stackoverflow.com/a/2608682
    def trimBeginAndEndQuotes: String = {
      string.replaceAll("^\"|\"$", "")
    }
    def replaceHexValues: String = {
      val regex = new Regex("""\\x[\\x00-\\x7F][\\x00-\\x7F]""")
      regex
        .findAllIn(string)
        .foldLeft(string)((acc, pattern) => {
          val doubleDigits = pattern.substring(2)
          val number = Integer.parseInt(doubleDigits, 16)
          val replacement = number.toChar.toString
          acc.replace(pattern, replacement)
        })
    }
  }

  def sanitizeString(string: String): String = {
    string.trimBeginAndEndQuotes.replaceDoubleQuotes.replaceHexValues
  }

}
