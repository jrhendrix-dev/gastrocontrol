package com.gastrocontrol.gastrocontrol.dto.customer;

import jakarta.validation.constraints.NotBlank;

public class CustomerPickupRequest {
    @NotBlank private String name;
    private String phone;
    private String notes;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
