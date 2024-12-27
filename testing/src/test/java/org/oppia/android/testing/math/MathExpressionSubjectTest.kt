package org.oppia.android.testing.math

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.oppia.android.app.model.MathBinaryOperation
import org.oppia.android.app.model.MathExpression
import org.oppia.android.app.model.MathFunctionCall
import org.oppia.android.app.model.MathUnaryOperation
import org.oppia.android.app.model.Real

/** Tests for [MathExpressionSubject]. */
@RunWith(JUnit4::class)
class MathExpressionSubjectTest {

  @Test
  fun testConstantExpression_withInteger_matchesStructure() {
    val expression = createConstantExpression(5)

    MathExpressionSubject.assertThat(expression).hasStructureThatMatches {
      constant {
        withValueThat().isIntegerThat().isEqualTo(5)
      }
    }
  }

  @Test
  fun testConstantExpression_withWrongValue_fails() {
    val expression = createConstantExpression(5)

    val exception = assertThrows(AssertionError::class.java) {
      MathExpressionSubject.assertThat(expression).hasStructureThatMatches {
        constant {
          withValueThat().isIntegerThat().isEqualTo(6)
        }
      }
    }
    assertThat(exception).hasMessageThat().contains("expected: 6")
  }

  @Test
  fun testVariableExpression_matchesStructure() {
    val expression = createVariableExpression("x")

    MathExpressionSubject.assertThat(expression).hasStructureThatMatches {
      variable {
        withNameThat().isEqualTo("x")
      }
    }
  }

  @Test
  fun testVariableExpression_withWrongName_fails() {
    val expression = createVariableExpression("x")

    val exception = assertThrows(AssertionError::class.java) {
      MathExpressionSubject.assertThat(expression).hasStructureThatMatches {
        variable {
          withNameThat().isEqualTo("y")
        }
      }
    }
    assertThat(exception).hasMessageThat().contains("expected: y")
  }

  @Test
  fun testBinaryOperation_addition_matchesStructure() {
    val expression = createBinaryOperation(
      MathBinaryOperation.Operator.ADD,
      createConstantExpression(3),
      createConstantExpression(4)
    )

    MathExpressionSubject.assertThat(expression).hasStructureThatMatches {
      addition {
        leftOperand {
          constant {
            withValueThat().isIntegerThat().isEqualTo(3)
          }
        }
        rightOperand {
          constant {
            withValueThat().isIntegerThat().isEqualTo(4)
          }
        }
      }
    }
  }

  @Test
  fun testBinaryOperation_multiplication_withImplicit_matchesStructure() {
    val expression = createImplicitMultiplication(
      createConstantExpression(2),
      createConstantExpression(3)
    )

    MathExpressionSubject.assertThat(expression).hasStructureThatMatches {
      multiplication(isImplicit = true) {
        leftOperand {
          constant {
            withValueThat().isIntegerThat().isEqualTo(2)
          }
        }
        rightOperand {
          constant {
            withValueThat().isIntegerThat().isEqualTo(3)
          }
        }
      }
    }
  }

  @Test
  fun testUnaryOperation_negation_matchesStructure() {
    val expression = createUnaryOperation(
      MathUnaryOperation.Operator.NEGATE,
      createConstantExpression(5)
    )

    MathExpressionSubject.assertThat(expression).hasStructureThatMatches {
      negation {
        operand {
          constant {
            withValueThat().isIntegerThat().isEqualTo(5)
          }
        }
      }
    }
  }

  @Test
  fun testFunctionCall_squareRoot_matchesStructure() {
    val expression = createFunctionCall(
      MathFunctionCall.FunctionType.SQUARE_ROOT,
      createConstantExpression(16)
    )

    MathExpressionSubject.assertThat(expression).hasStructureThatMatches {
      functionCallTo(MathFunctionCall.FunctionType.SQUARE_ROOT) {
        argument {
          constant {
            withValueThat().isIntegerThat().isEqualTo(16)
          }
        }
      }
    }
  }

  @Test
  fun testComplexExpression_matchesStructure() {
    // Creates expression: 3 + 4 * (-5)
    val expression = createBinaryOperation(
      MathBinaryOperation.Operator.ADD,
      createConstantExpression(3),
      createBinaryOperation(
        MathBinaryOperation.Operator.MULTIPLY,
        createConstantExpression(4),
        createUnaryOperation(
          MathUnaryOperation.Operator.NEGATE,
          createConstantExpression(5)
        )
      )
    )

    MathExpressionSubject.assertThat(expression).hasStructureThatMatches {
      addition {
        leftOperand {
          constant {
            withValueThat().isIntegerThat().isEqualTo(3)
          }
        }
        rightOperand {
          multiplication {
            leftOperand {
              constant {
                withValueThat().isIntegerThat().isEqualTo(4)
              }
            }
            rightOperand {
              negation {
                operand {
                  constant {
                    withValueThat().isIntegerThat().isEqualTo(5)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  @Test
  fun testGroupExpression_matchesStructure() {
    val expression = createGroupExpression(createConstantExpression(42))

    MathExpressionSubject.assertThat(expression).hasStructureThatMatches {
      group {
        constant {
          withValueThat().isIntegerThat().isEqualTo(42)
        }
      }
    }
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

  private fun createImplicitMultiplication(
    left: MathExpression,
    right: MathExpression
  ): MathExpression {
    return MathExpression.newBuilder()
      .setBinaryOperation(
        MathBinaryOperation.newBuilder()
          .setOperator(MathBinaryOperation.Operator.MULTIPLY)
          .setIsImplicit(true)
          .setLeftOperand(left)
          .setRightOperand(right)
      )
      .build()
  }

  private fun createUnaryOperation(
    operator: MathUnaryOperation.Operator,
    operand: MathExpression
  ): MathExpression {
    return MathExpression.newBuilder()
      .setUnaryOperation(
        MathUnaryOperation.newBuilder()
          .setOperator(operator)
          .setOperand(operand)
      )
      .build()
  }

  private fun createFunctionCall(
    functionType: MathFunctionCall.FunctionType,
    argument: MathExpression
  ): MathExpression {
    return MathExpression.newBuilder()
      .setFunctionCall(
        MathFunctionCall.newBuilder()
          .setFunctionType(functionType)
          .setArgument(argument)
      )
      .build()
  }

  private fun createGroupExpression(inner: MathExpression): MathExpression {
    return MathExpression.newBuilder()
      .setGroup(inner)
      .build()
  }
}
