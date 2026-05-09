package com.javamsdt.masking.maskme.condition;

import io.github.javamsdt.maskme.api.condition.MaskMeCondition;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Masking condition that masks a {@code BigDecimal} balance field when its value
 * strictly exceeds a threshold supplied at runtime via the {@code Mask-Balance-Threshold} header.
 *
 * <p><b>Masking Logic:</b> {@code balance > threshold}</p>
 * <ul>
 *   <li>No header / empty / invalid value → threshold is {@code null} → no masking</li>
 *   <li>Balance {@code null} or not a {@code BigDecimal} → no masking</li>
 *   <li>Balance exactly equal to threshold → no masking (strictly greater-than)</li>
 * </ul>
 */
@Component
public class BalanceMaskingCondition implements MaskMeCondition {

    private BigDecimal threshold;

    @Override
    public boolean shouldMask(Object maskedFieldValue, Object objectContainingMaskedField) {
        if (threshold == null || !(maskedFieldValue instanceof BigDecimal balance)) {
            return false;
        }
        return balance.compareTo(threshold) > 0;
    }

    @Override
    public void setInput(Object input) {
        if (input instanceof String s && !s.isBlank()) {
            try {
                this.threshold = new BigDecimal(s);
            } catch (NumberFormatException e) {
                this.threshold = null;
            }
        } else {
            this.threshold = null;
        }
    }
}
