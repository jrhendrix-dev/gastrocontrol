package com.gastrocontrol.gastrocontrol.domain.enums;

/**
 * Status lifecycle for an order in the restaurant.
 */
public enum OrderStatus {
    /**
     * Staff POS "open ticket" state.
     * Items can be added/edited before the ticket is submitted to the kitchen.
     */
    DRAFT,
    PENDING,          // Created, not yet started in kitchen
    IN_PREPARATION,   // Being prepared
    READY,            // Ready to serve / pickup
    SERVED,           // Served to table (food delivered) / picked up
    FINISHED,         // Table fully finished (customers have left)
    CANCELLED         // Cancelled before completion
}