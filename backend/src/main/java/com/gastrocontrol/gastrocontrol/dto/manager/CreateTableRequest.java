// src/main/java/com/gastrocontrol/gastrocontrol/dto/manager/CreateTableRequest.java
package com.gastrocontrol.gastrocontrol.dto.manager;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for adding a new dining table.
 */
public class CreateTableRequest {

    @NotBlank(message = "label is required")
    @Size(max = 50, message = "label must not exceed 50 characters")
    private String label;

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}