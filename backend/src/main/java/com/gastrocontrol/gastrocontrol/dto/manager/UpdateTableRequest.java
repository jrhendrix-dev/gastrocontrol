// src/main/java/com/gastrocontrol/gastrocontrol/dto/manager/UpdateTableRequest.java
package com.gastrocontrol.gastrocontrol.dto.manager;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for renaming a dining table.
 */
public class UpdateTableRequest {

    @NotBlank(message = "label is required")
    @Size(max = 50, message = "label must not exceed 50 characters")
    private String label;

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}