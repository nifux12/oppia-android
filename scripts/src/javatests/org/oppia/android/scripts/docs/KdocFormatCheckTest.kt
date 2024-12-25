package org.oppia.android.scripts.docs

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.oppia.android.testing.assertThrows
import java.io.ByteArrayOutputStream
import java.io.PrintStream

private const val KDOC_CHECK_PASSED_OUTPUT_INDICATOR = "KDOC CLOSING TAG CHECK PASSED"
private const val KDOC_CHECK_FAILED_OUTPUT_INDICATOR = "KDOC CLOSING TAG CHECK FAILED"

/** Tests for [KdocFormatCheck]. */
class KdocFormatCheckTest {
  private val outContent: ByteArrayOutputStream = ByteArrayOutputStream()
  private val originalOut: PrintStream = System.out

  @field:[Rule JvmField] val tempFolder = TemporaryFolder()

  @Before
  fun setUp() {
    tempFolder.newFolder("testfiles")
    System.setOut(PrintStream(outContent))
  }

  @After
  fun restoreStreams() {
    System.setOut(originalOut)
  }

  @Test
  fun testKdoc_validClosingTag_checkShouldPass() {
    val testContent =
      """
      /**
       * Returns the string corresponding to this error's string resources.
       */
      fun getErrorMessage(): String {
        return "test"
      }
      """.trimIndent()
    val tempFile = tempFolder.newFile("testfiles/TempFile.kt")
    tempFile.writeText(testContent)

    runScript()

    assertThat(outContent.toString().trim()).isEqualTo(KDOC_CHECK_PASSED_OUTPUT_INDICATOR)
  }

  @Test
  fun testKdoc_extraAsteriskInClosingTag_checkShouldFail() {
    val testContent =
      """
      /**
       * Returns the string corresponding to this error's string resources.
       **/
      fun getErrorMessage(): String {
        return "test"
      }
      """.trimIndent()
    val tempFile = tempFolder.newFile("testfiles/TempFile.kt")
    tempFile.writeText(testContent)

    val exception = assertThrows<Exception> { runScript() }

    assertThat(exception).hasMessageThat().contains(KDOC_CHECK_FAILED_OUTPUT_INDICATOR)
    assertThat(outContent.toString()).contains(
      "${tempFile.path}:3 - Extra asterisk" +
        " found in KDoc closing tag"
    )
  }

  @Test
  fun testKdoc_multipleDocsWithExtraAsterisks_checksAllTags() {
    val testContent =
      """
      /**
       * First KDoc comment.
       **/
      val test1 = ""

      /**
       * Second KDoc comment.
       **/
      val test2 = ""
      """.trimIndent()
    val tempFile = tempFolder.newFile("testfiles/TempFile.kt")
    tempFile.writeText(testContent)

    val exception = assertThrows<Exception> { runScript() }

    assertThat(exception).hasMessageThat().contains(KDOC_CHECK_FAILED_OUTPUT_INDICATOR)
    val output = outContent.toString()
    assertThat(output).contains("${tempFile.path}:3 - Extra asterisk found in KDoc closing tag")
    assertThat(output).contains("${tempFile.path}:8 - Extra asterisk found in KDoc closing tag")
  }

  @Test
  fun testKdoc_blockCommentNotKdoc_checkShouldPass() {
    val testContent =
      """
      /*
       * This is a block comment, not a KDoc.
       */
      val test = ""
      """.trimIndent()
    val tempFile = tempFolder.newFile("testfiles/TempFile.kt")
    tempFile.writeText(testContent)

    runScript()

    assertThat(outContent.toString().trim()).isEqualTo(KDOC_CHECK_PASSED_OUTPUT_INDICATOR)
  }

  @Test
  fun testKdoc_multipleFiles_sortedOutput() {
    val testContent1 =
      """
      /**
       * First file KDoc.
       **/
      val test = ""
      """.trimIndent()
    val testContent2 =
      """
      /**
       * Second file KDoc.
       **/
      val test = ""
      """.trimIndent()

    val tempFile1 = tempFolder.newFile("testfiles/AFile.kt")
    val tempFile2 = tempFolder.newFile("testfiles/BFile.kt")
    tempFile1.writeText(testContent1)
    tempFile2.writeText(testContent2)

    val exception = assertThrows<Exception> { runScript() }

    assertThat(exception).hasMessageThat().contains(KDOC_CHECK_FAILED_OUTPUT_INDICATOR)
    val output = outContent.toString()
    // Files should be reported in alphabetical order
    assertThat(output).contains("Found KDoc closing tag issues:")
    assertThat(output).contains("${tempFile1.path}:3")
    assertThat(output).contains("${tempFile2.path}:3")
  }

  @Test
  fun testKdoc_noKotlinFiles_checkShouldPass() {
    val testContent = "Not a Kotlin file"
    val tempFile = tempFolder.newFile("testfiles/file.txt")
    tempFile.writeText(testContent)

    runScript()

    assertThat(outContent.toString().trim()).isEqualTo(KDOC_CHECK_PASSED_OUTPUT_INDICATOR)
  }

  @Test
  fun testKdoc_emptyDirectory_checkShouldPass() {
    runScript()

    assertThat(outContent.toString().trim()).isEqualTo(KDOC_CHECK_PASSED_OUTPUT_INDICATOR)
  }

  @Test
  fun testKdoc_noRepoPathProvided_printsError() {
    main()

    assertThat(outContent.toString().trim()).isEqualTo(
      "Please provide" +
        " the repository path as an argument"
    )
  }

  /** Retrieves the absolute path of testfiles directory. */
  private fun retrieveTestFilesDirectoryPath(): String {
    return "${tempFolder.root}/testfiles"
  }

  /** Runs the kdoc_closing_tag_check script. */
  private fun runScript() {
    main(retrieveTestFilesDirectoryPath())
  }
}
