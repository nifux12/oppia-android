package org.oppia.android.scripts.docs

import java.io.File

/**
 *
 * Usage:
 *   bazel run //scripts:kdoc_closing_check -- <path_to_directory_root>
 *
 * Arguments:
 * - path_to_directory_root: directory path to the root of the repository
 */
fun main(vararg args: String) {
  if (args.isEmpty()) {
    println("Please provide the repository path as an argument")
    return
  }

  val repoPath = "${args[0]}/"
  val excludedFiles = setOf(
    "KdocValidityCheckTest.kt",
    "RegexPatternValidationCheckTest.kt"
  )
  val kotlinFiles = File(repoPath).walk()
    .filter { file ->
      file.isFile &&
        file.extension == "kt" &&
        file.name !in excludedFiles
    }
    .toList()

  val formattingIssues = mutableListOf<Pair<File, Int>>()

  kotlinFiles.forEach { file ->
    val issues = checkKDocClosingTags(file)
    formattingIssues.addAll(issues)
  }

  // Report results
  if (formattingIssues.isNotEmpty()) {
    println("Found KDoc closing tag issues:")
    formattingIssues.sortedWith(compareBy({ it.first.path }, { it.second }))
      .forEach { (file, line) ->
        println("${file.path}:$line - Extra asterisk found in KDoc closing tag")
      }
    throw Exception("KDOC CLOSING TAG CHECK FAILED")
  } else {
    println("KDOC CLOSING TAG CHECK PASSED")
  }
}

/**
 * Checks a single file for improper KDoc closing tags.
 *
 * @param file The Kotlin file to check
 * @return List of pairs containing the file and line numbers where improper closing tags were found
 */
private fun checkKDocClosingTags(file: File): List<Pair<File, Int>> {
  val issues = mutableListOf<Pair<File, Int>>()

  val lines = file.readLines()

  var insideKDoc = false

  lines.forEachIndexed { index, line ->
    val trimmedLine = line.trim()

    if (trimmedLine.startsWith("/**")) {
      insideKDoc = true
    }

    if (insideKDoc && trimmedLine.matches("""\*\s*\*/""".toRegex())) {
      issues.add(Pair(file, index + 1))
    }
    if (trimmedLine.endsWith("*/")) {
      insideKDoc = false
    }
  }

  return issues
}