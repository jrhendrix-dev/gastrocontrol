// src/main/java/com/gastrocontrol/gastrocontrol/dto/staff/AddNoteRequest.java
package com.gastrocontrol.gastrocontrol.dto.staff;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/staff/orders/{orderId}/notes}.
 */
public class AddNoteRequest {

    @NotBlank(message = "note must not be blank")
    @Size(max = 500, message = "note must not exceed 500 characters")
    private String note;

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}