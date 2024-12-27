package org.oppia.android.testing.math

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.oppia.android.app.model.MathBinaryOperation
import org.oppia.android.app.model.MathEquation
import org.oppia.android.app.model.MathExpression
import org.oppia.android.app.model.Real

/** Tests for [MathEquationSubject]. */
@RunWith(JUnit4::class)
class MathEquationSubjectTest {

  @Test
  fun testHasLeftHandSide_withValidExpression_matchesExpression() {
    val equation = createEquation(
      leftSide = createConstantExpression(5),
      rightSide = createConstantExpression(0)
    )

    MathEquationSubject.assertThat(equation).hasLeftHandSideThat().hasStructureThatMatches {
      constant {
        withValueThat().isIntegerThat().isEqualTo(5)
      }
    }
  }

  @Test
  fun testHasLeftHandSide_withDefaultExpression_hasNoExpressionType() {
    val equation = MathEquation.getDefaultInstance()

    // Default expression should have no type set
    MathEquationSubject.assertThat(equation).hasLeftHandSideThat().isEqualTo(
      MathExpression.getDefaultInstance()
    )
  }

  @Test
  fun testHasRightHandSide_withValidExpression_matchesExpression() {
    val equation = createEquation(
      leftSide = createConstantExpression(0),
      rightSide = createConstantExpression(10)
    )

    MathEquationSubject.assertThat(equation).hasRightHandSideThat().hasStructureThatMatches {
      constant {
        withValueThat().isIntegerThat().isEqualTo(10)
      }
    }
  }

  @Test
  fun testHasRightHandSide_withDefaultExpression_hasNoExpressionType() {
    val equation = MathEquation.getDefaultInstance()

    // Default expression should have no type set
    MathEquationSubject.assertThat(equation).hasRightHandSideThat().isEqualTo(
      MathExpression.getDefaultInstance()
    )
  }

  @Test
  fun testConvertsToLatex_simpleEquation_producesCorrectString() {
    val equation = createEquation(
      leftSide = createConstantExpression(5),
      rightSide = createConstantExpression(10)
    )

    MathEquationSubject.assertThat(equation)
      .convertsToLatexStringThat()
      .isEqualTo("5 = 10")
  }

  @Test
  fun testConvertsToLatex_withDivision_retainsDivisionOperator() {
    val equation = createEquation(
      leftSide = createBinaryOperation(
        MathBinaryOperation.Operator.DIVIDE,
        createConstantExpression(10),
        createConstantExpression(2)
      ),
      rightSide = createConstantExpression(5)
    )

    MathEquationSubject.assertThat(equation)
      .convertsToLatexStringThat()
      .isEqualTo("10 \\div 2 = 5")
  }

  @Test
  fun testConvertsToLatexWithFractions_withDivision_producesFractionNotation() {
    val equation = createEquation(
      leftSide = createBinaryOperation(
        MathBinaryOperation.Operator.DIVIDE,
        createConstantExpression(10),
        createConstantExpression(2)
      ),
      rightSide = createConstantExpression(5)
    )

    MathEquationSubject.assertThat(equation)
      .convertsWithFractionsToLatexStringThat()
      .isEqualTo("\\frac{10}{2} = 5")
  }

  @Test
  fun testConvertsToLatex_complexExpression_producesCorrectString() {
    val equation = createEquation(
      leftSide = createBinaryOperation(
        MathBinaryOperation.Operator.ADD,
        createConstantExpression(3),
        createBinaryOperation(
          MathBinaryOperation.Operator.MULTIPLY,
          createConstantExpression(4),
          createVariableExpression("x")
        )
      ),
      rightSide = createConstantExpression(0)
    )

    MathEquationSubject.assertThat(equation)
      .convertsToLatexStringThat()
      .isEqualTo("3 + 4 \\times x = 0")
  }

  @Test
  fun testLeftHandSide_wrongExpression_failsWithAppropriateMessage() {
    val equation = createEquation(
      leftSide = createConstantExpression(5),
      rightSide = createConstantExpression(0)
    )

    val exception = assertThrows(AssertionError::class.java) {
      MathEquationSubject.assertThat(equation).hasLeftHandSideThat().hasStructureThatMatches {
        constant {
          withValueThat().isIntegerThat().isEqualTo(6)
        }
      }
    }
    assertThat(exception).hasMessageThat().contains("expected: 6")
  }

  @Test
  fun testRightHandSide_wrongExpression_failsWithAppropriateMessage() {
    val equation = createEquation(
      leftSide = createConstantExpression(0),
      rightSide = createConstantExpression(10)
    )

    val exception = assertThrows(AssertionError::class.java) {
      MathEquationSubject.assertThat(equation).hasRightHandSideThat().hasStructureThatMatches {
        constant {
          withValueThat().isIntegerThat().isEqualTo(11)
        }
      }
    }
    assertThat(exception).hasMessageThat().contains("expected: 11")
  }

  private fun createEquation(
    leftSide: MathExpression,
    rightSide: MathExpression
  ): MathEquation {
    return MathEquation.newBuilder()
      .setLeftSide(leftSide)
      .setRightSide(rightSide)
      .build()
  }

  private fun createConstantExpression(value: Int): MathExpression {
    return MathExpression.newBuilder()
      .setConstant(Real.newBuilder().setInteger(value))
      .build()
  }

  private fun createVariableExpression(name: String): MathExpression {
    return MathExpression.newBuilder()
      .setVariable(name)
      .build()
  }

  private fun createBinaryOperation(
    operator: MathBinaryOperation.Operator,
    left: MathExpression,
    right: MathExpression
  ): MathExpression {
    return MathExpression.newBuilder()
      .setBinaryOperation(
        MathBinaryOperation.newBuilder()
          .setOperator(operator)
          .setLeftOperand(left)
          .setRightOperand(right)
      )
      .build()
  }
}
