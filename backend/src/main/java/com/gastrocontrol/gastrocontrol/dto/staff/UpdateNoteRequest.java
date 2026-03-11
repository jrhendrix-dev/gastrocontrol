// src/main/java/com/gastrocontrol/gastrocontrol/dto/staff/UpdateNoteRequest.java
package com.gastrocontrol.gastrocontrol.dto.staff;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code PATCH /api/staff/orders/{orderId}/notes/{noteId}}.
 *
 * <p>Only the note text can be changed. The author role is immutable after
 * creation — it identifies who originally wrote the note.</p>
 */
public class UpdateNoteRequest {

    @NotBlank(message = "note must not be blank")
    @Size(max = 500, message = "note must not exceed 500 characters")
    private String note;

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}