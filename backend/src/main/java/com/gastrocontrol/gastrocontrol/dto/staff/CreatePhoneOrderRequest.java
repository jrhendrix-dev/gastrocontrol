// src/main/java/com/gastrocontrol/gastrocontrol/dto/staff/CreatePhoneOrderRequest.java
package com.gastrocontrol.gastrocontrol.dto.staff;

import com.gastrocontrol.gastrocontrol.domain.enums.OrderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for creating a TAKE_AWAY or DELIVERY order placed by phone.
 *
 * <p>Items are not included at creation time — staff add them via
 * {@code POST /api/staff/orders/{id}/items} after the order is created.</p>
 */
public class CreatePhoneOrderRequest {

    /** TAKE_AWAY or DELIVERY. DINE_IN is rejected by the service layer. */
    @NotNull
    private OrderType type;

    /** Customer name for pickup/delivery identification. */
    @NotBlank
    private String name;

    /** Customer phone number. Optional but recommended for coordination. */
    private String phone;

    /** Kitchen notes from the call (allergies, preferences, etc.). Optional. */
    private String notes;

    /** Delivery address line. Required when type is DELIVERY. */
    private String address;

    /** Delivery city. Required when type is DELIVERY. */
    private String city;

    public OrderType getType()        { return type; }
    public void setType(OrderType t)  { this.type = t; }

    public String getName()           { return name; }
    public void setName(String n)     { this.name = n; }

    public String getPhone()          { return phone; }
    public void setPhone(String p)    { this.phone = p; }

    public String getNotes()          { return notes; }
    public void setNotes(String n)    { this.notes = n; }

    public String getAddress()        { return address; }
    public void setAddress(String a)  { this.address = a; }

    public String getCity()           { return city; }
    public void setCity(String c)     { this.city = c; }
}