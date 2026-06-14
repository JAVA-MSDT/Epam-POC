/**
 * Copyright (c) 2025: Ahmed Samy, All rights reserved.
 * LinkedIn: https://www.linkedin.com/in/java-msdt/
 * GitHub: https://github.com/JAVA-MSDT
 * Email: serenitydiver@hotmail.com
 */
package com.javamsdt.masking.dto;


import com.javamsdt.masking.maskme.condition.PhoneMaskingCondition;
import io.github.javamsdt.maskme.api.annotation.MaskMe;
import io.github.javamsdt.maskme.implementation.condition.AlwaysMaskMeCondition;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record UserDto(
        Long id,
        String name,
        @MaskMe(conditions = {AlwaysMaskMeCondition.class}, maskValue = "")
        String email,
        @MaskMe(conditions = {AlwaysMaskMeCondition.class})
        String password,
        @MaskMe(conditions = {PhoneMaskingCondition.class})
        String phone,
        AddressDto address,
        LocalDate birthDate,
        String genderId,
        String genderName,
        BigDecimal balance,
        Instant createdAt
) {
}
