// src/main/java/com/gastrocontrol/gastrocontrol/controller/manager/ManagerTableController.java
package com.gastrocontrol.gastrocontrol.controller.manager;

import com.gastrocontrol.gastrocontrol.application.service.manager.ManagerTableService;
import com.gastrocontrol.gastrocontrol.dto.common.ApiResponse;
import com.gastrocontrol.gastrocontrol.dto.manager.CreateTableRequest;
import com.gastrocontrol.gastrocontrol.dto.manager.TableResponse;
import com.gastrocontrol.gastrocontrol.dto.manager.UpdateTableRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Manager endpoints for dining table management.
 *
 * <p>Accessible to {@code MANAGER} and {@code ADMIN} roles (enforced by
 * {@code SecurityConfig} at the {@code /api/manager/**} path level).</p>
 */
@RestController
@RequestMapping("/api/manager/tables")
public class ManagerTableController {

    private final ManagerTableService managerTableService;

    public ManagerTableController(ManagerTableService managerTableService) {
        this.managerTableService = managerTableService;
    }

    /**
     * Lists all dining tables sorted by label.
     *
     * @return list of all tables
     */
    @GetMapping
    public ResponseEntity<List<TableResponse>> listAll() {
        return ResponseEntity.ok(managerTableService.listAll());
    }

    /**
     * Creates a new dining table.
     *
     * @param req the display label for the new table
     * @return 201 Created with the new table including its generated id
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TableResponse>> create(
            @Valid @RequestBody CreateTableRequest req
    ) {
        TableResponse created = managerTableService.create(req);
        return ResponseEntity.status(201).body(ApiResponse.ok("Table created", created));
    }

    /**
     * Renames an existing dining table.
     *
     * @param tableId the table to rename
     * @param req     the new label
     * @return 200 OK with the updated table
     */
    @PatchMapping("/{tableId}")
    public ResponseEntity<ApiResponse<TableResponse>> update(
            @PathVariable Long tableId,
            @Valid @RequestBody UpdateTableRequest req
    ) {
        TableResponse updated = managerTableService.update(tableId, req);
        return ResponseEntity.ok(ApiResponse.ok("Table updated", updated));
    }

    /**
     * Permanently deletes a dining table. Blocked if active orders exist on it.
     *
     * @param tableId the table to delete
     * @return 200 OK with success message
     */
    @DeleteMapping("/{tableId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long tableId) {
        managerTableService.delete(tableId);
        return ResponseEntity.ok(ApiResponse.ok("Table deleted"));
    }
}