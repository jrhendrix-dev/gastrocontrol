// src/app/features/admin/tabs/admin-tables-tab.component.ts
import {
  ChangeDetectionStrategy, Component, inject, OnInit, signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../admin-api.service';
import { TableResponse } from '../admin.types';

type ModalType = 'create' | 'delete' | null;

/**
 * Pestaña de Mesas del panel de administración.
 *
 * Creación mediante modal (consistente con Productos y Usuarios).
 * Renombrado inline. Eliminación con confirmación modal.
 */
@Component({
  selector: 'app-admin-tables-tab',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="tables-tab">

      <!-- Toolbar -->
      <div class="toolbar">
        <span class="item-count">
          {{ tables().length }} mesa{{ tables().length !== 1 ? 's' : '' }}
        </span>
        <button class="btn btn-primary" (click)="openCreate()">+ Nueva mesa</button>
      </div>

      @if (error()) {
        <div class="error-banner">{{ error() }}</div>
      }

      <!-- List -->
      @if (loading()) {
        <div class="empty-state">Cargando…</div>
      } @else if (tables().length === 0) {
        <div class="empty-state">Aún no hay mesas.</div>
      } @else {
        <div class="gc-card list-card">
          @for (table of tables(); track table.id; let last = $last) {
            <div class="list-row" [class.last]="last">
              @if (editingId() === table.id) {
                <!-- Inline rename mode -->
                <input class="gc-input edit-input" type="text"
                       [(ngModel)]="editLabel"
                       maxlength="50"
                       (keyup.enter)="saveEdit(table)"
                       (keyup.escape)="cancelEdit()"
                       [disabled]="savingId() === table.id" />
                <div class="row-actions">
                  <button class="btn btn-primary btn-sm"
                          [disabled]="savingId() === table.id || !editLabel.trim()"
                          (click)="saveEdit(table)">
                    {{ savingId() === table.id ? 'Guardando…' : 'Guardar' }}
                  </button>
                  <button class="btn btn-outline btn-sm"
                          [disabled]="savingId() === table.id"
                          (click)="cancelEdit()">Cancelar</button>
                </div>
              } @else {
                <span class="table-icon">🪑</span>
                <span class="item-name">{{ table.label }}</span>
                <div class="row-actions">
                  <button class="btn btn-outline btn-sm" (click)="startEdit(table)">Renombrar</button>
                  <button class="btn-danger-soft btn-sm" (click)="openDelete(table)">Eliminar</button>
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

    <!-- ── Modal: Nueva mesa ──────────────────────────────────────────────── -->
    @if (modalType() === 'create') {
      <div class="modal-backdrop" (click)="closeModal()">
        <div class="modal gc-card" (click)="$event.stopPropagation()">
          <h2 class="modal-title">Nueva mesa</h2>
          <p class="modal-sub">La etiqueta identifica la mesa en el TPV y en las órdenes.</p>
          <div class="form-field">
            <label>Etiqueta *</label>
            <input class="gc-input" type="text" [(ngModel)]="createLabel"
                   maxlength="50"
                   placeholder="Ej. T6, Terraza 1, Barra…"
                   (keyup.enter)="submitCreate()" />
          </div>
          @if (modalError()) { <div class="error-banner">{{ modalError() }}</div> }
          <div class="modal-actions">
            <button class="btn btn-outline" (click)="closeModal()"
                    [disabled]="modalLoading()">Cancelar</button>
            <button class="btn btn-primary"
                    [disabled]="modalLoading() || !createLabel.trim()"
                    (click)="submitCreate()">
              {{ modalLoading() ? 'Creando…' : 'Crear mesa' }}
            </button>
          </div>
        </div>
      </div>
    }

    <!-- ── Modal: Eliminar mesa ───────────────────────────────────────────── -->
    @if (modalType() === 'delete') {
      <div class="modal-backdrop" (click)="closeModal()">
        <div class="modal gc-card" (click)="$event.stopPropagation()">
          <h2 class="modal-title">Eliminar mesa</h2>
          <p class="modal-sub">
            ¿Eliminar la mesa <strong>{{ selectedTable()?.label }}</strong>?
            Esta acción fallará si hay pedidos activos en esta mesa.
          </p>
          @if (modalError()) { <div class="error-banner">{{ modalError() }}</div> }
          <div class="modal-actions">
            <button class="btn btn-outline" (click)="closeModal()"
                    [disabled]="modalLoading()">Cancelar</button>
            <button class="btn btn-danger" [disabled]="modalLoading()" (click)="submitDelete()">
              {{ modalLoading() ? 'Eliminando…' : 'Eliminar' }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .tables-tab { display: flex; flex-direction: column; gap: 1rem; max-width: 600px; }

    .toolbar {
      display: flex; align-items: center; justify-content: space-between;
    }
    .item-count { font-size: 0.875rem; color: var(--gc-ink-muted); }

    .error-banner {
      background: rgba(220,38,38,0.06); border: 1px solid rgba(220,38,38,0.2);
      color: #b91c1c; border-radius: 0.5rem; padding: 0.75rem 1rem; font-size: 0.875rem;
    }
    .empty-state {
      color: var(--gc-ink-muted); padding: 2rem; text-align: center; font-size: 0.875rem;
      background: white; border: 1px solid rgba(0,0,0,0.06); border-radius: 10px;
    }

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

    /* Modal */
    .modal-backdrop {
      position: fixed; inset: 0; background: rgba(0,0,0,0.4);
      display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 1rem;
    }
    .modal {
      width: 100%; max-width: 400px; padding: 1.75rem;
      display: flex; flex-direction: column; gap: 1rem;
    }
    .modal-title { font-size: 1.1rem; font-weight: 700; color: var(--gc-ink); margin: 0; }
    .modal-sub   { font-size: 0.875rem; color: var(--gc-ink-muted); margin: 0; line-height: 1.5; }
    .form-field {
      display: flex; flex-direction: column; gap: 0.375rem;
      label { font-size: 0.8rem; color: var(--gc-ink-muted); font-weight: 500; }
    }
    .modal-actions { display: flex; justify-content: flex-end; gap: 0.5rem; margin-top: 0.25rem; }
  `],
})
export class AdminTablesTabComponent implements OnInit {
  private readonly api = inject(AdminApiService);

  protected readonly tables        = signal<TableResponse[]>([]);
  protected readonly loading       = signal(false);
  protected readonly error         = signal<string | null>(null);
  protected readonly editingId     = signal<number | null>(null);
  protected readonly savingId      = signal<number | null>(null);
  protected readonly modalType     = signal<ModalType>(null);
  protected readonly modalLoading  = signal(false);
  protected readonly modalError    = signal<string | null>(null);
  protected readonly selectedTable = signal<TableResponse | null>(null);

  private readonly _rowErrors = signal<Record<number, string>>({});

  protected createLabel = '';
  protected editLabel   = '';

  protected rowError(id: number): string | null {
    return this._rowErrors()[id] ?? null;
  }

  ngOnInit(): void { this.load(); }

  private load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listTables().subscribe({
      next: list => { this.tables.set(list); this.loading.set(false); },
      error: () => { this.error.set('Error al cargar las mesas.'); this.loading.set(false); },
    });
  }

  // ── Create modal ──────────────────────────────────────────────────────────

  protected openCreate(): void {
    this.createLabel = '';
    this.modalError.set(null);
    this.modalLoading.set(false);
    this.modalType.set('create');
  }

  protected submitCreate(): void {
    const label = this.createLabel.trim();
    if (!label) return;
    this.modalLoading.set(true);
    this.modalError.set(null);
    this.api.createTable({ label }).subscribe({
      next: created => {
        this.tables.update(list => [...list, created]);
        this.closeModal();
      },
      error: err => {
        this.modalLoading.set(false);
        this.modalError.set(this.extractError(err, 'Error al crear la mesa.'));
      },
    });
  }

  // ── Inline rename ─────────────────────────────────────────────────────────

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
        this.setRowError(table.id, this.extractError(err, 'Error al renombrar la mesa.'));
        this.savingId.set(null);
      },
    });
  }

  // ── Delete modal ──────────────────────────────────────────────────────────

  protected openDelete(table: TableResponse): void {
    this.selectedTable.set(table);
    this.modalError.set(null);
    this.modalLoading.set(false);
    this.modalType.set('delete');
  }

  protected submitDelete(): void {
    const table = this.selectedTable();
    if (!table) return;
    this.modalLoading.set(true);
    this.modalError.set(null);
    this.api.deleteTable(table.id).subscribe({
      next: () => {
        this.tables.update(list => list.filter(t => t.id !== table.id));
        this.closeModal();
      },
      error: err => {
        this.modalLoading.set(false);
        this.modalError.set(this.extractError(err, 'No se puede eliminar — puede haber pedidos activos en esta mesa.'));
      },
    });
  }

  protected closeModal(): void {
    this.modalType.set(null);
    this.modalLoading.set(false);
    this.modalError.set(null);
    this.selectedTable.set(null);
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
