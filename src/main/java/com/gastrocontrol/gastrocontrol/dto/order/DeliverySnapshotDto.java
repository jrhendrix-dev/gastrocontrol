// src/main/java/com/gastrocontrol/gastrocontrol/dto/order/DeliverySnapshotDto.java
package com.gastrocontrol.gastrocontrol.dto.order;

import jakarta.validation.constraints.Size;

public record DeliverySnapshotDto(
        @Size(max = 120) String name,
        @Size(max = 30) String phone,
        @Size(max = 190) String addressLine1,
        @Size(max = 190) String addressLine2,
        @Size(max = 120) String city,
        @Size(max = 20) String postalCode,
        @Size(max = 500) String notes
) {}
