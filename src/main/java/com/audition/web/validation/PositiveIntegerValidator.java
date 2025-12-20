package com.audition.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

public class PositiveIntegerValidator implements ConstraintValidator<PositiveInteger, String> {

    @Override
    public void initialize(final PositiveInteger constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        if (StringUtils.isBlank(value)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Post ID cannot be null or empty")
                .addConstraintViolation();
            return false;
        }

        try {
            final int intValue = Integer.parseInt(value.trim());
            if (intValue <= 0) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Post ID must be a positive integer")
                    .addConstraintViolation();
                return false;
            }
            return true;
        } catch (final NumberFormatException e) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Post ID must be a valid integer")
                .addConstraintViolation();
            return false;
        }
    }
}

