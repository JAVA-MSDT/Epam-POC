package com.javamsdt.masking.maskme.condition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@DisplayName("BalanceMaskingCondition")
class BalanceMaskingConditionTest {

    private BalanceMaskingCondition condition;

    @BeforeEach
    void setUp() {
        condition = new BalanceMaskingCondition();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // setInput — safe parsing
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("setInput — safe parsing")
    class SetInput {

        @Test
        @DisplayName("Should parse valid integer string as threshold")
        void should_ParseThreshold_When_ValidIntegerStringProvided() {
            // Given / When
            condition.setInput("50");

            // Then — threshold is set; balance above it must mask
            assertThat(condition.shouldMask(new BigDecimal("100"), null)).isTrue();
        }

        @Test
        @DisplayName("Should parse valid decimal string as threshold")
        void should_ParseThreshold_When_ValidDecimalStringProvided() {
            // Given / When
            condition.setInput("100.4599");

            // Then — 100.46 > 100.4599 → mask
            assertThat(condition.shouldMask(new BigDecimal("100.46"), null)).isTrue();
        }

        @Test
        @DisplayName("Should not throw and should not mask when input is null")
        void should_NotThrowAndNotMask_When_InputIsNull() {
            // Given / When / Then
            assertThatNoException().isThrownBy(() -> condition.setInput(null));
            assertThat(condition.shouldMask(new BigDecimal("100"), null)).isFalse();
        }

        @Test
        @DisplayName("Should not mask when input is empty string")
        void should_NotMask_When_InputIsEmptyString() {
            // Given / When
            condition.setInput("");

            // Then
            assertThat(condition.shouldMask(new BigDecimal("100"), null))
                    .as("Empty string input must not trigger masking")
                    .isFalse();
        }

        @Test
        @DisplayName("Should not mask when input is blank string")
        void should_NotMask_When_InputIsBlankString() {
            // Given / When
            condition.setInput("   ");

            // Then
            assertThat(condition.shouldMask(new BigDecimal("100"), null))
                    .as("Blank string input must not trigger masking")
                    .isFalse();
        }

        @Test
        @DisplayName("Should not throw and not mask when input is non-numeric string")
        void should_NotThrowAndNotMask_When_InputIsNonNumeric() {
            // Given / When / Then
            assertThatNoException().isThrownBy(() -> condition.setInput("abc"));
            assertThat(condition.shouldMask(new BigDecimal("100"), null))
                    .as("Non-numeric input must not trigger masking")
                    .isFalse();
        }

        @Test
        @DisplayName("Should not mask when input is non-String type")
        void should_NotMask_When_InputIsNotAString() {
            // Given / When
            condition.setInput(50); // Integer, not String

            // Then
            assertThat(condition.shouldMask(new BigDecimal("100"), null))
                    .as("Non-String input type must not trigger masking")
                    .isFalse();
        }

        @Test
        @DisplayName("Should reset threshold to null when called with empty after valid value")
        void should_ResetThreshold_When_CalledWithEmptyAfterValidValue() {
            // Given — first call sets a valid threshold
            condition.setInput("50");
            assertThat(condition.shouldMask(new BigDecimal("100"), null)).isTrue();

            // When — second call with empty resets it
            condition.setInput("");

            // Then
            assertThat(condition.shouldMask(new BigDecimal("100"), null))
                    .as("Threshold must be cleared after empty input")
                    .isFalse();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // shouldMask — happy path
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("shouldMask — happy path")
    class ShouldMaskHappyPath {

        @Test
        @DisplayName("Should mask when balance strictly exceeds threshold")
        void should_Mask_When_BalanceExceedsThreshold() {
            // Given
            condition.setInput("50");

            // When / Then
            assertThat(condition.shouldMask(new BigDecimal("100.46"), null))
                    .as("100.46 > 50 must be masked")
                    .isTrue();
        }

        @Test
        @DisplayName("Should not mask when balance is below threshold")
        void should_NotMask_When_BalanceIsBelowThreshold() {
            // Given
            condition.setInput("200");

            // When / Then
            assertThat(condition.shouldMask(new BigDecimal("100.46"), null))
                    .as("100.46 < 200 must not be masked")
                    .isFalse();
        }

        @Test
        @DisplayName("Should not mask when balance equals threshold (strictly greater-than)")
        void should_NotMask_When_BalanceEqualsThreshold() {
            // Given
            condition.setInput("100.46");

            // When / Then
            assertThat(condition.shouldMask(new BigDecimal("100.46"), null))
                    .as("100.46 == 100.46 must NOT be masked (strictly greater-than)")
                    .isFalse();
        }

        @Test
        @DisplayName("Should mask when threshold is zero and balance is positive")
        void should_Mask_When_ThresholdIsZeroAndBalanceIsPositive() {
            // Given
            condition.setInput("0");

            // When / Then
            assertThat(condition.shouldMask(new BigDecimal("20.0"), null))
                    .as("20.0 > 0 must be masked")
                    .isTrue();
        }

        @Test
        @DisplayName("Should not mask when threshold is zero and balance is zero")
        void should_NotMask_When_ThresholdIsZeroAndBalanceIsZero() {
            // Given
            condition.setInput("0");

            // When / Then
            assertThat(condition.shouldMask(BigDecimal.ZERO, null))
                    .as("0 == 0 must NOT be masked")
                    .isFalse();
        }

        @Test
        @DisplayName("Should mask when threshold is negative and balance is positive")
        void should_Mask_When_ThresholdIsNegativeAndBalanceIsPositive() {
            // Given
            condition.setInput("-10");

            // When / Then
            assertThat(condition.shouldMask(new BigDecimal("20.0"), null))
                    .as("20.0 > -10 must be masked")
                    .isTrue();
        }

        @Test
        @DisplayName("Should mask with high-precision threshold where balance just exceeds it")
        void should_Mask_When_BalanceJustExceedsHighPrecisionThreshold() {
            // Given — 100.46 > 100.4599
            condition.setInput("100.4599");

            // When / Then
            assertThat(condition.shouldMask(new BigDecimal("100.46"), null))
                    .as("100.46 > 100.4599 must be masked")
                    .isTrue();
        }

        @Test
        @DisplayName("Should not mask when threshold is very large")
        void should_NotMask_When_ThresholdIsVeryLarge() {
            // Given
            condition.setInput("999999999");

            // When / Then
            assertThat(condition.shouldMask(new BigDecimal("100.46"), null))
                    .as("100.46 < 999999999 must not be masked")
                    .isFalse();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // shouldMask — edge cases & null safety
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("shouldMask — edge cases and null safety")
    class ShouldMaskEdgeCases {

        @Test
        @DisplayName("Should not mask when no setInput was called (threshold is null)")
        void should_NotMask_When_SetInputNeverCalled() {
            // Given — fresh condition, setInput never called
            // When / Then
            assertThat(condition.shouldMask(new BigDecimal("100"), null))
                    .as("Null threshold must not trigger masking")
                    .isFalse();
        }

        @Test
        @DisplayName("Should not throw and not mask when balance field value is null")
        void should_NotThrowAndNotMask_When_BalanceFieldValueIsNull() {
            // Given
            condition.setInput("50");

            // When / Then
            assertThatNoException().isThrownBy(() -> condition.shouldMask(null, null));
            assertThat(condition.shouldMask(null, null))
                    .as("Null balance must not trigger masking")
                    .isFalse();
        }

        @Test
        @DisplayName("Should not mask when field value is not a BigDecimal")
        void should_NotMask_When_FieldValueIsNotBigDecimal() {
            // Given — condition placed on a non-BigDecimal field by mistake
            condition.setInput("50");

            // When / Then
            assertThat(condition.shouldMask("not-a-number", null))
                    .as("Non-BigDecimal field value must not trigger masking")
                    .isFalse();
        }

        @Test
        @DisplayName("Should not mask when field value is an Integer (wrong type)")
        void should_NotMask_When_FieldValueIsInteger() {
            // Given
            condition.setInput("50");

            // When / Then
            assertThat(condition.shouldMask(100, null))
                    .as("Integer field value must not trigger masking — only BigDecimal is supported")
                    .isFalse();
        }

        @Test
        @DisplayName("Should use compareTo not equals — BigDecimal scale difference must not affect result")
        void should_CompareByValue_When_BigDecimalScaleDiffers() {
            // Given — 100.460 and 100.46 are equal by value but differ in scale
            // equals() would return false, compareTo() returns 0 → must NOT mask
            condition.setInput("100.460");

            // When / Then
            assertThat(condition.shouldMask(new BigDecimal("100.46"), null))
                    .as("100.46 compareTo 100.460 == 0, must NOT be masked (not strictly greater)")
                    .isFalse();
        }

        @Test
        @DisplayName("Should ignore objectContainingMaskedField — it is not used in logic")
        void should_IgnoreContainingObject_WhenEvaluating() {
            // Given
            condition.setInput("50");
            Object arbitraryContainer = new Object();

            // When / Then
            assertThat(condition.shouldMask(new BigDecimal("100"), arbitraryContainer))
                    .as("Containing object is irrelevant to balance threshold logic")
                    .isTrue();
        }
    }
}
