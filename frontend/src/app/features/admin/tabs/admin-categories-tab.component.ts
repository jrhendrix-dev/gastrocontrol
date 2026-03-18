// src/app/features/admin/tabs/admin-categories-tab.component.ts
import {
  ChangeDetectionStrategy, Component, inject, OnInit, signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../admin-api.service';
import { CategoryResponse } from '../admin.types';

/**
 * Manager Categories tab.
 *
 * Displays all categories with inline rename and delete.
 * Delete is blocked by the backend if products are still assigned.
 */
@Component({
  selector: 'app-admin-categories-tab',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="cat-tab">

      <!-- Add row -->
      <div class="add-row">
        <input class="gc-input add-input" type="text"
               placeholder="New category name…"
               [(ngModel)]="newName"
               (keyup.enter)="create()"
               [disabled]="creating()" />
        <button class="btn btn-primary"
                [disabled]="creating() || !newName.trim()"
                (click)="create()">
          {{ creating() ? 'Adding…' : '+ Add Category' }}
        </button>
      </div>

      @if (createError()) {
        <div class="error-banner">{{ createError() }}</div>
      }

      <!-- List -->
      @if (loading()) {
        <div class="empty-state">Loading…</div>
      } @else if (categories().length === 0) {
        <div class="empty-state">No categories yet. Add one above.</div>
      } @else {
        <div class="gc-card list-card">
          @for (cat of categories(); track cat.id; let last = $last) {
            <div class="list-row" [class.last]="last">
              @if (editingId() === cat.id) {
                <input class="gc-input edit-input" type="text"
                       [(ngModel)]="editName"
                       (keyup.enter)="saveEdit(cat)"
                       (keyup.escape)="cancelEdit()" />
                <div class="row-actions">
                  <button class="btn btn-primary btn-sm"
                          [disabled]="savingId() === cat.id"
                          (click)="saveEdit(cat)">
                    {{ savingId() === cat.id ? 'Saving…' : 'Save' }}
                  </button>
                  <button class="btn btn-outline btn-sm" (click)="cancelEdit()">Cancel</button>
                </div>
              } @else {
                <span class="item-name">{{ cat.name }}</span>
                <div class="row-actions">
                  <button class="btn btn-outline btn-sm" (click)="startEdit(cat)">Rename</button>
                  <button class="btn-danger-soft btn-sm"
                          [disabled]="deletingId() === cat.id"
                          (click)="confirmDelete(cat)">
                    {{ deletingId() === cat.id ? 'Deleting…' : 'Delete' }}
                  </button>
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

    <!-- Delete confirm -->
    @if (pendingDelete()) {
      <div class="modal-backdrop" (click)="cancelDelete()">
        <div class="modal gc-card" (click)="$event.stopPropagation()">
          <h2 class="modal-title">Delete Category</h2>
          <p class="modal-sub">
            Delete <strong>{{ pendingDelete()!.name }}</strong>?
            This will fail if products are still assigned to it.
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
    .cat-tab { display: flex; flex-direction: column; gap: 1rem; max-width: 600px; }
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
export class AdminCategoriesTabComponent implements OnInit {
  private readonly api = inject(AdminApiService);

  protected readonly categories  = signal<CategoryResponse[]>([]);
  protected readonly loading     = signal(false);
  protected readonly creating    = signal(false);
  protected readonly createError = signal<string | null>(null);
  protected readonly editingId   = signal<number | null>(null);
  protected readonly savingId    = signal<number | null>(null);
  protected readonly deletingId  = signal<number | null>(null);
  protected readonly pendingDelete = signal<CategoryResponse | null>(null);
  protected readonly deleteError   = signal<string | null>(null);

  private readonly _rowErrors = signal<Record<number, string>>({});

  protected newName  = '';
  protected editName = '';

  protected rowError(id: number): string | null {
    return this._rowErrors()[id] ?? null;
  }

  ngOnInit(): void { this.load(); }

  private load(): void {
    this.loading.set(true);
    this.api.listCategories().subscribe({
      next: list => { this.categories.set(list); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  protected create(): void {
    const name = this.newName.trim();
    if (!name) return;
    this.creating.set(true);
    this.createError.set(null);
    this.api.createCategory({ name }).subscribe({
      next: created => {
        this.categories.update(list => [...list, created]);
        this.newName = '';
        this.creating.set(false);
      },
      error: err => {
        this.createError.set(this.extractError(err, 'Failed to create category.'));
        this.creating.set(false);
      },
    });
  }

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
        this.setRowError(cat.id, this.extractError(err, 'Failed to rename category.'));
        this.savingId.set(null);
      },
    });
  }

  protected confirmDelete(cat: CategoryResponse): void {
    this.deleteError.set(null);
    this.pendingDelete.set(cat);
  }

  protected cancelDelete(): void {
    this.pendingDelete.set(null);
    this.deleteError.set(null);
  }

  protected executeDelete(): void {
    const cat = this.pendingDelete();
    if (!cat) return;
    this.deletingId.set(cat.id);
    this.deleteError.set(null);
    this.api.deleteCategory(cat.id).subscribe({
      next: () => {
        this.categories.update(list => list.filter(c => c.id !== cat.id));
        this.pendingDelete.set(null);
        this.deletingId.set(null);
      },
      error: err => {
        this.deleteError.set(this.extractError(err, 'Cannot delete — products may still be assigned.'));
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
