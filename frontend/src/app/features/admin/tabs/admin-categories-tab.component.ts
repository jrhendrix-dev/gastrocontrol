// src/app/features/admin/tabs/admin-categories-tab.component.ts
import {
  ChangeDetectionStrategy, Component, inject, OnInit, signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../admin-api.service';
import { CategoryResponse } from '../admin.types';

type ModalType = 'create' | 'delete' | null;

/**
 * Pestaña de Categorías del panel de administración.
 *
 * Creación mediante modal (consistente con Productos y Usuarios).
 * Renombrado inline. Eliminación con confirmación modal.
 */
@Component({
  selector: 'app-admin-categories-tab',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="cat-tab">

      <!-- Toolbar -->
      <div class="toolbar">
        <span class="item-count">
          {{ categories().length }} categoría{{ categories().length !== 1 ? 's' : '' }}
        </span>
        <button class="btn btn-primary" (click)="openCreate()">+ Nueva categoría</button>
      </div>

      @if (error()) {
        <div class="error-banner">{{ error() }}</div>
      }

      <!-- List -->
      @if (loading()) {
        <div class="empty-state">Cargando…</div>
      } @else if (categories().length === 0) {
        <div class="empty-state">Aún no hay categorías.</div>
      } @else {
        <div class="gc-card list-card">
          @for (cat of categories(); track cat.id; let last = $last) {
            <div class="list-row" [class.last]="last">
              @if (editingId() === cat.id) {
                <!-- Inline rename mode -->
                <input class="gc-input edit-input" type="text"
                       [(ngModel)]="editName"
                       (keyup.enter)="saveEdit(cat)"
                       (keyup.escape)="cancelEdit()"
                       [disabled]="savingId() === cat.id" />
                <div class="row-actions">
                  <button class="btn btn-primary btn-sm"
                          [disabled]="savingId() === cat.id || !editName.trim()"
                          (click)="saveEdit(cat)">
                    {{ savingId() === cat.id ? 'Guardando…' : 'Guardar' }}
                  </button>
                  <button class="btn btn-outline btn-sm"
                          [disabled]="savingId() === cat.id"
                          (click)="cancelEdit()">Cancelar</button>
                </div>
              } @else {
                <span class="item-name">{{ cat.name }}</span>
                <div class="row-actions">
                  <button class="btn btn-outline btn-sm" (click)="startEdit(cat)">Renombrar</button>
                  <button class="btn-danger-soft btn-sm" (click)="openDelete(cat)">Eliminar</button>
                </div>
              }
              @if (rowError(cat.id)) {
                <span class="row-error">{{ rowError(cat.id) }}</span>
              }
            </div>
          }
        </div>
      }
    </div>

    <!-- ── Modal: Nueva categoría ─────────────────────────────────────────── -->
    @if (modalType() === 'create') {
      <div class="modal-backdrop" (click)="closeModal()">
        <div class="modal gc-card" (click)="$event.stopPropagation()">
          <h2 class="modal-title">Nueva categoría</h2>
          <p class="modal-sub">Las categorías agrupan los productos en el TPV y el menú.</p>
          <div class="form-field">
            <label>Nombre *</label>
            <input class="gc-input" type="text" [(ngModel)]="createName"
                   placeholder="Ej. Entrantes, Bebidas, Postres…"
                   (keyup.enter)="submitCreate()" />
          </div>
          @if (modalError()) { <div class="error-banner">{{ modalError() }}</div> }
          <div class="modal-actions">
            <button class="btn btn-outline" (click)="closeModal()"
                    [disabled]="modalLoading()">Cancelar</button>
            <button class="btn btn-primary"
                    [disabled]="modalLoading() || !createName.trim()"
                    (click)="submitCreate()">
              {{ modalLoading() ? 'Creando…' : 'Crear categoría' }}
            </button>
          </div>
        </div>
      </div>
    }

    <!-- ── Modal: Eliminar categoría ─────────────────────────────────────── -->
    @if (modalType() === 'delete') {
      <div class="modal-backdrop" (click)="closeModal()">
        <div class="modal gc-card" (click)="$event.stopPropagation()">
          <h2 class="modal-title">Eliminar categoría</h2>
          <p class="modal-sub">
            ¿Eliminar <strong>{{ selectedCategory()?.name }}</strong>?
            Esta acción fallará si hay productos asignados a esta categoría.
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
    .cat-tab { display: flex; flex-direction: column; gap: 1rem; max-width: 600px; }

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
export class AdminCategoriesTabComponent implements OnInit {
  private readonly api = inject(AdminApiService);

  protected readonly categories    = signal<CategoryResponse[]>([]);
  protected readonly loading       = signal(false);
  protected readonly error         = signal<string | null>(null);
  protected readonly editingId     = signal<number | null>(null);
  protected readonly savingId      = signal<number | null>(null);
  protected readonly modalType     = signal<ModalType>(null);
  protected readonly modalLoading  = signal(false);
  protected readonly modalError    = signal<string | null>(null);
  protected readonly selectedCategory = signal<CategoryResponse | null>(null);

  private readonly _rowErrors = signal<Record<number, string>>({});

  protected createName = '';
  protected editName   = '';

  protected rowError(id: number): string | null {
    return this._rowErrors()[id] ?? null;
  }

  ngOnInit(): void { this.load(); }

  private load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listCategories().subscribe({
      next: list => { this.categories.set(list); this.loading.set(false); },
      error: () => { this.error.set('Error al cargar las categorías.'); this.loading.set(false); },
    });
  }

  // ── Create modal ──────────────────────────────────────────────────────────

  protected openCreate(): void {
    this.createName = '';
    this.modalError.set(null);
    this.modalLoading.set(false);
    this.modalType.set('create');
  }

  protected submitCreate(): void {
    const name = this.createName.trim();
    if (!name) return;
    this.modalLoading.set(true);
    this.modalError.set(null);
    this.api.createCategory({ name }).subscribe({
      next: created => {
        this.categories.update(list => [...list, created]);
        this.closeModal();
      },
      error: err => {
        this.modalLoading.set(false);
        this.modalError.set(this.extractError(err, 'Error al crear la categoría.'));
      },
    });
  }

  // ── Inline rename ─────────────────────────────────────────────────────────

  protected startEdit(cat: CategoryResponse): void {
    this.editingId.set(cat.id);
    this.editName = cat.name;
    this.clearRowError(cat.id);
  }

  protected cancelEdit(): void { this.editingId.set(null); }

  protected saveEdit(cat: CategoryResponse): void {
    const name = this.editName.trim();
    if (!name || name === cat.name) { this.cancelEdit(); return; }
    this.savingId.set(cat.id);
    this.api.updateCategory(cat.id, { name }).subscribe({
      next: updated => {
        this.categories.update(list => list.map(c => c.id === updated.id ? updated : c));
        this.editingId.set(null);
        this.savingId.set(null);
      },
      error: err => {
        this.setRowError(cat.id, this.extractError(err, 'Error al renombrar la categoría.'));
        this.savingId.set(null);
      },
    });
  }

  // ── Delete modal ──────────────────────────────────────────────────────────

  protected openDelete(cat: CategoryResponse): void {
    this.selectedCategory.set(cat);
    this.modalError.set(null);
    this.modalLoading.set(false);
    this.modalType.set('delete');
  }

  protected submitDelete(): void {
    const cat = this.selectedCategory();
    if (!cat) return;
    this.modalLoading.set(true);
    this.modalError.set(null);
    this.api.deleteCategory(cat.id).subscribe({
      next: () => {
        this.categories.update(list => list.filter(c => c.id !== cat.id));
        this.closeModal();
      },
      error: err => {
        this.modalLoading.set(false);
        this.modalError.set(this.extractError(err, 'No se puede eliminar — puede haber productos asignados.'));
      },
    });
  }

  protected closeModal(): void {
    this.modalType.set(null);
    this.modalLoading.set(false);
    this.modalError.set(null);
    this.selectedCategory.set(null);
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
