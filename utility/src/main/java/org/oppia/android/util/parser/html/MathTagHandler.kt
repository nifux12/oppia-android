package org.oppia.android.util.parser.html

import android.app.Application
import android.content.res.AssetManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.Spannable
import android.text.style.ImageSpan
import androidx.core.content.res.ResourcesCompat
import io.github.karino2.kotlitex.view.MathExpressionSpan
import org.json.JSONObject
import org.oppia.android.util.R
import org.oppia.android.util.logging.ConsoleLogger
import org.oppia.android.util.parser.html.CustomHtmlContentHandler.ImageRetriever.Type.BLOCK_IMAGE
import org.oppia.android.util.parser.html.CustomHtmlContentHandler.ImageRetriever.Type.INLINE_TEXT_IMAGE
import org.xml.sax.Attributes

/** The custom tag corresponding to [MathTagHandler]. */
const val CUSTOM_MATH_TAG = "oppia-noninteractive-math"
private const val CUSTOM_MATH_MATH_CONTENT_ATTRIBUTE = "math_content-with-value"
private const val CUSTOM_MATH_RENDER_TYPE_ATTRIBUTE = "render-type"

/**
 * A custom tag handler for properly formatting math items in HTML parsed with
 * [CustomHtmlContentHandler].
 */
class MathTagHandler(
  private val consoleLogger: ConsoleLogger,
  private val assetManager: AssetManager,
  private val lineHeight: Float,
  private val cacheLatexRendering: Boolean,
  private val application: Application
) : CustomHtmlContentHandler.CustomTagHandler {
  override fun handleTag(
    attributes: Attributes,
    openIndex: Int,
    closeIndex: Int,
    output: Editable,
    imageRetriever: CustomHtmlContentHandler.ImageRetriever?
  ) {
    // Only insert the image tag if it's parsed correctly.
    val content = MathContent.parseMathContent(
      attributes.getJsonObjectValue(CUSTOM_MATH_MATH_CONTENT_ATTRIBUTE)
    )
    // TODO(#4170): Fix vertical alignment centering for inline cached LaTeX.
    val useInlineRendering = when (attributes.getValue(CUSTOM_MATH_RENDER_TYPE_ATTRIBUTE)) {
      "inline" -> true
      "block" -> false
      else -> true
    }
    checkNotNull(imageRetriever) { "Expected imageRetriever to be not null." }
    val newSpan = when (content) {
      is MathContent.MathAsSvg -> {
        ImageSpan(
          imageRetriever.loadDrawable(
            content.svgFilename,
            INLINE_TEXT_IMAGE
          ),
          content.svgFilename
        )
      }
      is MathContent.MathAsLatex -> {
        if (cacheLatexRendering) {
          CenteredLatexImageSpan(
            imageRetriever.loadMathDrawable(
              content.rawLatex,
              lineHeight,
              type = if (useInlineRendering) INLINE_TEXT_IMAGE else BLOCK_IMAGE
            )
          )
        } else {
          MathExpressionSpan(
            content.rawLatex,
            lineHeight,
            assetManager,
            isMathMode = !useInlineRendering,
            ResourcesCompat.getColor(
              application.resources,
              R.color.component_color_shared_equation_color,
              /* theme = */ null
            )
          )
        }
      }
      null -> {
        consoleLogger.e("MathTagHandler", "Failed to parse math tag")
        return
      }
    }

    // Insert an image span where the custom tag currently is to load the SVG/LaTeX span. Note that
    // this approach is based on Android's HTML parser.
    val (startIndex, endIndex) = output.run {
      // Use a control character to ensure that there's at least 1 character on which to
      // "attach" the image when rendering the HTML.
      val startIndex = length
      append('\uFFFC')
      return@run startIndex to length
    }
    output.setSpan(
      newSpan,
      startIndex,
      endIndex,
      Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
  }

  private sealed class MathContent {
    data class MathAsSvg(val svgFilename: String) : MathContent()

    data class MathAsLatex(val rawLatex: String) : MathContent()

    companion object {
      internal fun parseMathContent(obj: JSONObject?): MathContent? {
        // Kotlitex expects escaped backslashes.
        val rawLatex = obj?.getOptionalString("raw_latex")
        val svgFilename = obj?.getOptionalString("svg_filename")
        return when {
          svgFilename != null -> MathAsSvg(svgFilename)
          rawLatex != null -> MathAsLatex(rawLatex)
          else -> null
        }
      }

      /**
       * Returns a string corresponding to the specified name, or null if a mapping doesn't exist or
       * its value is null. There is no functionality in [JSONObject] that supports this exact
       * operation without relying on exceptions for control flow.
       */
      private fun JSONObject.getOptionalString(name: String): String? = opt(name)?.toJsonString()

      private fun Any?.toJsonString(): String? {
        // Based on JSON.toString() which is not available for public use.
        return when {
          this is String -> this
          this != null -> toString()
          else -> null
        }
      }
    }
  }
}

/** An [ImageSpan] that vertically centers a LaTeX drawable within the surrounding text. */
private class CenteredLatexImageSpan(drawable: Drawable?) :
  ImageSpan(drawable ?: createEmptyDrawable()) {
  override fun getSize(
    paint: Paint,
    text: CharSequence,
    start: Int,
    end: Int,
    fontMetricsInt: Paint.FontMetricsInt?
  ): Int {
    val d = drawable
    val bounds = d.bounds

    fontMetricsInt?.let {
      val paintFm = paint.fontMetricsInt
      val textHeight = paintFm.descent - paintFm.ascent
      val latexHeight = bounds.height()

      val centeringOffset = (textHeight - latexHeight) / 2

      it.ascent = paintFm.ascent + centeringOffset
      it.top = paintFm.top + centeringOffset
      it.descent = it.ascent + latexHeight
      it.bottom = it.top + latexHeight
    }

    return bounds.right
  }

  override fun draw(
    canvas: Canvas,
    text: CharSequence,
    start: Int,
    end: Int,
    x: Float,
    top: Int,
    y: Int,
    bottom: Int,
    paint: Paint
  ) {
    val d = drawable
    canvas.save()

    val fontMetrics = paint.fontMetricsInt
    val latexHeight = d.bounds.height()

    val centerY = y + (fontMetrics.descent + fontMetrics.ascent) / 2
    val drawableY = centerY - (latexHeight / 2)

    canvas.translate(x, drawableY.toFloat())
    d.draw(canvas)
    canvas.restore()
  }

  companion object {
    private fun createEmptyDrawable(): Drawable {
      return object : Drawable() {
        override fun draw(canvas: Canvas) {}
        override fun setAlpha(alpha: Int) {}
        override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
        override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSPARENT

        init {
          setBounds(0, 0, 1, 1)
        }
      }
    }
  }
}
