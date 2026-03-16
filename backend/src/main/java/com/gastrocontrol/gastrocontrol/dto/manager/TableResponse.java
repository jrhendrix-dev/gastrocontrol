// src/main/java/com/gastrocontrol/gastrocontrol/dto/manager/TableResponse.java
package com.gastrocontrol.gastrocontrol.dto.manager;

/**
 * Manager-facing representation of a dining table.
 *
 * @param id    the table's primary key (database-generated)
 * @param label the display label shown to staff on the POS grid
 */
public record TableResponse(Long id, String label) {}