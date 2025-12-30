package com.gastrocontrol.gastrocontrol.dto.order;

public class OrderItemDto {
    private final Long productId;
    private final String name;
    private final int quantity;
    private final int unitPriceCents;

    public OrderItemDto(Long productId, String name, int quantity, int unitPriceCents) {
        this.productId = productId;
        this.name = name;
        this.quantity = quantity;
        this.unitPriceCents = unitPriceCents;
    }

    public Long getProductId() { return productId; }
    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    public int getUnitPriceCents() { return unitPriceCents; }
}
