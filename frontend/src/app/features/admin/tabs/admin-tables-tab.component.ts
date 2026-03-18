// src/app/features/admin/tabs/admin-tables-tab.component.ts
import {
  ChangeDetectionStrategy, Component, inject, OnInit, signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../admin-api.service';
import { TableResponse } from '../admin.types';

/**
 * Manager Tables tab.
 *
 * Displays all dining tables with inline label rename and delete.
 * Delete is blocked by the backend if active orders exist on the table.
 */
@Component({
  selector: 'app-admin-tables-tab',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="tables-tab">

      <!-- Add row -->
      <div class="add-row">
        <input class="gc-input add-input" type="text"
               placeholder="New table label (e.g. T6, Terraza 1)…"
               [(ngModel)]="newLabel"
               (keyup.enter)="create()"
               [disabled]="creating()" />
        <button class="btn btn-primary"
                [disabled]="creating() || !newLabel.trim()"
                (click)="create()">
          {{ creating() ? 'Adding…' : '+ Add Table' }}
        </button>
      </div>

      @if (createError()) {
        <div class="error-banner">{{ createError() }}</div>
      }

      <!-- List -->
      @if (loading()) {
        <div class="empty-state">Loading…</div>
      } @else if (tables().length === 0) {
        <div class="empty-state">No tables yet. Add one above.</div>
      } @else {
        <div class="gc-card list-card">
          @for (table of tables(); track table.id; let last = $last) {
            <div class="list-row" [class.last]="last">
              @if (editingId() === table.id) {
                <input class="gc-input edit-input" type="text"
                       [(ngModel)]="editLabel"
                       (keyup.enter)="saveEdit(table)"
                       (keyup.escape)="cancelEdit()" />
                <div class="row-actions">
                  <button class="btn btn-primary btn-sm"
                          [disabled]="savingId() === table.id"
                          (click)="saveEdit(table)">
                    {{ savingId() === table.id ? 'Saving…' : 'Save' }}
                  </button>
                  <button class="btn btn-outline btn-sm" (click)="cancelEdit()">Cancel</button>
                </div>
              } @else {
                <span class="table-icon">🪑</span>
                <span class="item-name">{{ table.label }}</span>
                <div class="row-actions">
                  <button class="btn btn-outline btn-sm" (click)="startEdit(table)">Rename</button>
                  <button class="btn-danger-soft btn-sm"
                          [disabled]="deletingId() === table.id"
                          (click)="confirmDelete(table)">
                    {{ deletingId() === table.id ? 'Deleting…' : 'Delete' }}
                  </button>
                </div>
              }
              @if (rowError(table.id)) {
                <span class="row-error">{{ rowError(table.id) }}</span>
              }
            </div>
          }
        </div>
      }
    </div>

    <!-- Delete confirm -->
    @if (pendingDelete()) {
      <div class="modal-backdrop" (click)="cancelDelete()">
        <div class="modal gc-card" (click)="$event.stopPropagation()">
          <h2 class="modal-title">Delete Table</h2>
          <p class="modal-sub">
            Delete table <strong>{{ pendingDelete()!.label }}</strong>?
            This will fail if there are active orders on this table.
          </p>
          @if (deleteError()) { <div class="error-banner">{{ deleteError() }}</div> }
          <div class="modal-actions">
            <button class="btn btn-outline" (click)="cancelDelete()">Cancel</button>
            <button class="btn btn-danger" [disabled]="deletingId() !== null" (click)="executeDelete()">
              {{ deletingId() !== null ? 'Deleting…' : 'Delete' }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .tables-tab { display: flex; flex-direction: column; gap: 1rem; max-width: 600px; }
    .add-row { display: flex; gap: 0.5rem; align-items: center; }
    .add-input { flex: 1; }
    .error-banner {
      background: rgba(220,38,38,0.06); border: 1px solid rgba(220,38,38,0.2);
      color: #b91c1c; border-radius: 0.5rem; padding: 0.75rem 1rem; font-size: 0.875rem;
    }
    .empty-state { color: var(--gc-ink-muted); padding: 2rem; text-align: center; font-size: 0.875rem; }
    .list-card { padding: 0; overflow: hidden; }
    .list-row {
      display: flex; align-items: center; gap: 0.75rem;
      padding: 0.875rem 1.25rem;
      border-bottom: 1px solid rgba(0,0,0,0.06);
      flex-wrap: wrap;
      &.last { border-bottom: none; }
    }
    .table-icon { font-size: 1rem; }
    .item-name { flex: 1; font-size: 0.9rem; font-weight: 500; color: var(--gc-ink); }
    .edit-input { flex: 1; min-width: 0; }
    .row-actions { display: flex; gap: 0.35rem; }
    .row-error { width: 100%; font-size: 0.8rem; color: #b91c1c; }
    .btn-danger-soft {
      background: rgba(220,38,38,0.07); color: #b91c1c;
      border: 1px solid rgba(220,38,38,0.2); border-radius: 0.375rem;
      padding: 0.3rem 0.65rem; font-size: 0.75rem; font-weight: 500; cursor: pointer;
      transition: background 0.15s;
      &:hover:not(:disabled) { background: rgba(220,38,38,0.14); }
      &:disabled { opacity: 0.45; cursor: not-allowed; }
    }
    .modal-backdrop {
      position: fixed; inset: 0; background: rgba(0,0,0,0.4);
      display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 1rem;
    }
    .modal { width: 100%; max-width: 380px; padding: 1.75rem; display: flex; flex-direction: column; gap: 1rem; }
    .modal-title { font-size: 1.1rem; font-weight: 700; color: var(--gc-ink); margin: 0; }
    .modal-sub { font-size: 0.875rem; color: var(--gc-ink-muted); margin: 0; }
    .modal-actions { display: flex; justify-content: flex-end; gap: 0.5rem; margin-top: 0.25rem; }
  `],
})
export class AdminTablesTabComponent implements OnInit {
  private readonly api = inject(AdminApiService);

  protected readonly tables      = signal<TableResponse[]>([]);
  protected readonly loading     = signal(false);
  protected readonly creating    = signal(false);
  protected readonly createError = signal<string | null>(null);
  protected readonly editingId   = signal<number | null>(null);
  protected readonly savingId    = signal<number | null>(null);
  protected readonly deletingId  = signal<number | null>(null);
  protected readonly pendingDelete = signal<TableResponse | null>(null);
  protected readonly deleteError   = signal<string | null>(null);

  private readonly _rowErrors = signal<Record<number, string>>({});

  protected newLabel  = '';
  protected editLabel = '';

  protected rowError(id: number): string | null {
    return this._rowErrors()[id] ?? null;
  }

  ngOnInit(): void { this.load(); }

  private load(): void {
    this.loading.set(true);
    this.api.listTables().subscribe({
      next: list => { this.tables.set(list); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  protected create(): void {
    const label = this.newLabel.trim();
    if (!label) return;
    this.creating.set(true);
    this.createError.set(null);
    this.api.createTable({ label }).subscribe({
      next: created => {
        this.tables.update(list => [...list, created]);
        this.newLabel = '';
        this.creating.set(false);
      },
      error: err => {
        this.createError.set(this.extractError(err, 'Failed to create table.'));
        this.creating.set(false);
      },
    });
  }

  protected startEdit(table: TableResponse): void {
    this.editingId.set(table.id);
    this.editLabel = table.label;
    this.clearRowError(table.id);
  }

  protected cancelEdit(): void { this.editingId.set(null); }

  protected saveEdit(table: TableResponse): void {
    const label = this.editLabel.trim();
    if (!label || label === table.label) { this.cancelEdit(); return; }
    this.savingId.set(table.id);
    this.api.updateTable(table.id, { label }).subscribe({
      next: updated => {
        this.tables.update(list => list.map(t => t.id === updated.id ? updated : t));
        this.editingId.set(null);
        this.savingId.set(null);
      },
      error: err => {
        this.setRowError(table.id, this.extractError(err, 'Failed to rename table.'));
        this.savingId.set(null);
      },
    });
  }

  protected confirmDelete(table: TableResponse): void {
    this.deleteError.set(null);
    this.pendingDelete.set(table);
  }

  protected cancelDelete(): void {
    this.pendingDelete.set(null);
    this.deleteError.set(null);
  }

  protected executeDelete(): void {
    const table = this.pendingDelete();
    if (!table) return;
    this.deletingId.set(table.id);
    this.deleteError.set(null);
    this.api.deleteTable(table.id).subscribe({
      next: () => {
        this.tables.update(list => list.filter(t => t.id !== table.id));
        this.pendingDelete.set(null);
        this.deletingId.set(null);
      },
      error: err => {
        this.deleteError.set(this.extractError(err, 'Cannot delete — active orders may exist on this table.'));
        this.deletingId.set(null);
      },
    });
  }

  private setRowError(id: number, msg: string): void {
    this._rowErrors.update(e => ({ ...e, [id]: msg }));
  }

  private clearRowError(id: number): void {
    this._rowErrors.update(e => { const n = { ...e }; delete n[id]; return n; });
  }

  private extractError(err: unknown, fallback: string): string {
    const details = (err as any)?.error?.error?.details;
    if (details) return Object.values(details).join(' ');
    return (err as any)?.error?.error?.message ?? fallback;
  }
}
