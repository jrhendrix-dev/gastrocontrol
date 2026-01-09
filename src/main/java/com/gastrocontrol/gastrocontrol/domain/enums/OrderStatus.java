package com.gastrocontrol.gastrocontrol.domain.enums;

/**
 * Status lifecycle for an order in the restaurant.
 */
public enum OrderStatus {
    PENDING,          // Created, not yet started in kitchen
    IN_PREPARATION,   // Being prepared
    READY,            // Ready to serve / pickup
    SERVED,           // Served to table or picked up
    CANCELLED         // Cancelled before completion
}
