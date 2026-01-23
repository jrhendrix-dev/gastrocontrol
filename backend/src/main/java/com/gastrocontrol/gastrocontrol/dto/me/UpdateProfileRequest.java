package com.gastrocontrol.gastrocontrol.dto.me;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for updating the authenticated user's profile fields.
 *
 * <p>All fields are optional:
 * <ul>
 *   <li>If a field is null, it is not modified.</li>
 *   <li>If a field is blank, it will be stored as null (clears the field).</li>
 * </ul>
 */
public record UpdateProfileRequest(

        @Size(max = 80, message = "firstName must be at most 80 characters")
        String firstName,

        @Size(max = 120, message = "lastName must be at most 120 characters")
        String lastName,

        @Size(max = 30, message = "phone must be at most 30 characters")
        @Pattern(
                regexp = "^[0-9+()\\-\\s]*$",
                message = "phone contains invalid characters"
        )
        String phone
) {}
