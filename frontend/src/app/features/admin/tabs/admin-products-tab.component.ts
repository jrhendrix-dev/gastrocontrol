// src/app/features/admin/tabs/admin-products-tab.component.ts
import {
  ChangeDetectionStrategy, Component, inject, OnInit, signal, computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../admin-api.service';
import {
  CategoryResponse, CreateProductRequest, ProductResponse, UpdateProductRequest,
} from '../admin.types';

type ModalType = 'create' | 'edit' | 'discontinue' | 'confirmReactivate' | null;

/**
 * Manager Products tab.
 *
 * Features:
 * - Paginated list with active/category/search filters
 * - Create product (name, description, price in €, category)
 * - Edit product (same fields)
 * - Discontinue with optional reason
 * - Reactivate discontinued product
 * - Price displayed in euros (priceCents / 100)
 */
@Component({
  selector: 'app-admin-products-tab',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="products-tab">

      <!-- Toolbar -->
      <div class="toolbar">
        <div class="filters">
          <input class="gc-input filter-search" type="text"
                 placeholder="Buscar productos…"
                 [(ngModel)]="searchQuery"
                 (ngModelChange)="onSearchChange()" />
          <select class="gc-input filter-select" [(ngModel)]="filterCategoryId" (ngModelChange)="load()">
            <option value="">Todas las categorías</option>
            @for (cat of categories(); track cat.id) {
              <option [value]="cat.id">{{ cat.name }}</option>
            }
          </select>
          <select class="gc-input filter-select" [(ngModel)]="filterActive" (ngModelChange)="load()">
            <option value="">Todos los estados</option>
            <option value="true">Activo</option>
            <option value="false">Descatalogado</option>
          </select>
        </div>
        <button class="btn btn-primary" (click)="openCreate()">+ Nuevo producto</button>
      </div>

      @if (error()) {
        <div class="error-banner">{{ error() }}</div>
      }

      <!-- Table -->
      <div class="gc-card table-wrapper">
        <table class="products-table">
          <thead>
          <tr>
            <th>Nombre</th>
            <th>Categoría</th>
            <th>Precio</th>
            <th>Estado</th>
            <th>Acciones</th>
          </tr>
          </thead>
          <tbody>
            @if (loading()) {
              <tr><td colspan="5" class="empty-cell">Loading…</td></tr>
            } @else if (products().length === 0) {
              <tr><td colspan="5" class="empty-cell">No se encontraron productos.</td></tr>
            } @else {
              @for (p of products(); track p.id) {
                <tr [class.discontinued-row]="!p.active">
                  <td>
                    <div class="product-name">{{ p.name }}</div>
                    @if (p.description) {
                      <div class="product-desc">{{ p.description }}</div>
                    }
                  </td>
                  <td class="cell-muted">{{ p.categoryName ?? '—' }}</td>
                  <td class="cell-price">{{ formatPrice(p.priceCents) }}</td>
                  <td>
                    <span class="status-chip" [class.active]="p.active">
                      {{ p.active ? 'Activo' : 'Descatalogado' }}
                    </span>
                  </td>
                  <td class="actions-cell">
                    <div class="action-menu">
                      <button class="btn btn-outline btn-sm" (click)="openEdit(p)">Editar</button>
                      @if (p.active) {
                        <button class="btn-danger-soft btn-sm" (click)="openDiscontinue(p)">Descatalogar</button>
                      } @else {
                        <button class="btn btn-outline btn-sm" (click)="openReactivate(p)">Reactivar</button>
                      }
                    </div>
                  </td>
                </tr>
              }
            }
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      @if (totalPages() > 1) {
        <div class="pagination">
          <button class="btn btn-outline btn-sm"
                  [disabled]="currentPage() === 0"
                  (click)="goToPage(currentPage() - 1)">← Prev</button>
          <span class="page-info">Page {{ currentPage() + 1 }} of {{ totalPages() }}</span>
          <button class="btn btn-outline btn-sm"
                  [disabled]="currentPage() >= totalPages() - 1"
                  (click)="goToPage(currentPage() + 1)">Next →</button>
        </div>
      }
    </div>

    <!-- ── Create / Edit Modal ─────────────────────────────────────── -->
    @if (modalType() === 'create' || modalType() === 'edit') {
      <div class="modal-backdrop" (click)="closeModal()">
        <div class="modal gc-card" (click)="$event.stopPropagation()">
          <h2 class="modal-title">{{ modalType() === 'create' ? 'Nuevo producto' : 'Editar producto' }}</h2>

          <div class="form-field">
            <label>Nombre *</label>
            <input class="gc-input" type="text" [(ngModel)]="productForm.name" placeholder="Nombre del producto" maxlength="60" />
          </div>
          <div class="form-field">
            <label>Descripción</label>
            <textarea class="gc-input" rows="2" [(ngModel)]="productForm.description"
                      placeholder="Descripción opcional" maxlength="300"></textarea>
          </div>
          <div class="form-row">
            <div class="form-field">
              <label>Precio (€) *</label>
              <input class="gc-input" type="number" min="0" step="0.01"
                     [(ngModel)]="productPriceEuros"
                     placeholder="0.00" />
            </div>
            <div class="form-field">
              <label>Categoría</label>
              <select class="gc-input" [(ngModel)]="productForm.categoryId">
                <option [ngValue]="null">Sin categoría</option>
                @for (cat of categories(); track cat.id) {
                  <option [ngValue]="cat.id">{{ cat.name }}</option>
                }
              </select>
            </div>
          </div>

          @if (modalError()) { <div class="error-banner">{{ modalError() }}</div> }

          <div class="modal-actions">
            <button class="btn btn-outline" (click)="closeModal()">Cancelar</button>
            <button class="btn btn-primary" [disabled]="modalLoading()" (click)="submitProductForm()">
              {{ modalLoading() ? 'Guardando…' : (modalType() === 'create' ? 'Crear' : 'Guardar cambios') }}
            </button>
          </div>
        </div>
      </div>
    }

    <!-- ── Discontinue Modal ───────────────────────────────────────── -->
    @if (modalType() === 'discontinue') {
      <div class="modal-backdrop" (click)="closeModal()">
        <div class="modal gc-card" (click)="$event.stopPropagation()">
          <h2 class="modal-title">Descatalogar producto</h2>
          <p class="modal-sub">
            <strong>{{ selectedProduct()?.name }}</strong> ya no aparecerá en el TPV.
          </p>
          <div class="form-field">
            <label>Motivo (opcional)</label>
            <input class="gc-input" type="text" [(ngModel)]="discontinueReason"
                   placeholder="Ej. Sin existencias, seasonal item…" />
          </div>
          @if (modalError()) { <div class="error-banner">{{ modalError() }}</div> }
          <div class="modal-actions">
            <button class="btn btn-outline" (click)="closeModal()">Cancelar</button>
            <button class="btn btn-danger" [disabled]="modalLoading()" (click)="submitDiscontinue()">
              {{ modalLoading() ? 'Procesando…' : 'Descatalogar' }}
            </button>
          </div>
        </div>
      </div>
    }

    <!-- ── Reactivate Confirm ──────────────────────────────────────── -->
    @if (modalType() === 'confirmReactivate') {
      <div class="modal-backdrop" (click)="closeModal()">
        <div class="modal modal-sm gc-card" (click)="$event.stopPropagation()">
          <h2 class="modal-title">Reactivar producto</h2>
          <p class="modal-sub">
            <strong>{{ selectedProduct()?.name }}</strong> volverá a aparecer en el TPV.
          </p>
          @if (modalError()) { <div class="error-banner">{{ modalError() }}</div> }
          <div class="modal-actions">
            <button class="btn btn-outline" (click)="closeModal()">Cancelar</button>
            <button class="btn btn-primary" [disabled]="modalLoading()" (click)="submitReactivate()">
              {{ modalLoading() ? 'Procesando…' : 'Reactivar' }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .products-tab { display: flex; flex-direction: column; gap: 1.25rem; }

    .toolbar {
      display: flex; align-items: center;
      justify-content: space-between; gap: 1rem;
    }
    .filters { display: flex; gap: 0.5rem; align-items: center; flex-wrap: nowrap; }
    .filter-search { width: 200px; }
    .filter-select { width: 185px; }

    .error-banner {
      background: rgba(220,38,38,0.06); border: 1px solid rgba(220,38,38,0.2);
      color: #b91c1c; border-radius: 0.5rem; padding: 0.75rem 1rem; font-size: 0.875rem;
    }

    .table-wrapper { overflow-x: auto; padding: 0; }
    .products-table {
      width: 100%; border-collapse: collapse; font-size: 0.875rem;
      th {
        background: var(--gc-brand-analogous, #1a4a37);
        color: rgba(255,255,255,0.85);
        font-weight: 600; font-size: 0.75rem;
        text-transform: uppercase; letter-spacing: 0.05em;
        padding: 0.75rem 1rem; text-align: left; white-space: nowrap;
      }
      td {
        padding: 0.875rem 1rem;
        border-top: 1px solid rgba(0,0,0,0.05);
        color: var(--gc-ink); vertical-align: middle;
      }
      tr:hover td { background: rgba(0,0,0,0.02); }
    }
    .discontinued-row td { opacity: 0.5; }
    .product-name { font-weight: 500; }
    .product-desc { font-size: 0.8rem; color: var(--gc-ink-muted); margin-top: 0.15rem; }
    .cell-muted { color: var(--gc-ink-muted); font-size: 0.85rem; }
    .cell-price { font-weight: 600; white-space: nowrap; }
    .empty-cell { text-align: center; color: var(--gc-ink-muted); padding: 2rem !important; }

    .status-chip {
      font-size: 0.72rem; font-weight: 600; letter-spacing: 0.05em;
      text-transform: uppercase; padding: 0.2rem 0.55rem; border-radius: 4px;
      background: rgba(220,38,38,0.08); color: #b91c1c;
      border: 1px solid rgba(220,38,38,0.15);
      &.active { background: rgba(15,47,36,0.08); color: var(--gc-brand); border-color: rgba(15,47,36,0.15); }
    }

    .actions-cell { white-space: nowrap; }
    .action-menu { display: flex; gap: 0.35rem; }
    .btn-danger-soft {
      background: rgba(220,38,38,0.07); color: #b91c1c;
      border: 1px solid rgba(220,38,38,0.2); border-radius: 0.375rem;
      padding: 0.3rem 0.65rem; font-size: 0.75rem; font-weight: 500; cursor: pointer;
      transition: background 0.15s;
      &:hover:not(:disabled) { background: rgba(220,38,38,0.14); }
      &:disabled { opacity: 0.45; cursor: not-allowed; }
    }

    .pagination {
      display: flex; align-items: center; justify-content: center; gap: 1rem;
    }
    .page-info { font-size: 0.875rem; color: var(--gc-ink-muted); }

    /* Modal */
    .modal-backdrop {
      position: fixed; inset: 0; background: rgba(0,0,0,0.4);
      display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 1rem;
    }
    .modal {
      width: 100%; max-width: 480px; padding: 1.75rem;
      display: flex; flex-direction: column; gap: 1rem;
      &.modal-sm { max-width: 360px; }
    }
    .modal-title { font-size: 1.1rem; font-weight: 700; color: var(--gc-ink); margin: 0; }
    .modal-sub { font-size: 0.875rem; color: var(--gc-ink-muted); margin: 0; }
    .form-field {
      display: flex; flex-direction: column; gap: 0.375rem; flex: 1;
      label { font-size: 0.8rem; color: var(--gc-ink-muted); font-weight: 500; }
      textarea { resize: vertical; }
    }
    .form-row { display: flex; gap: 0.75rem; }
    .modal-actions { display: flex; justify-content: flex-end; gap: 0.5rem; margin-top: 0.25rem; }
  `],
})
export class AdminProductsTabComponent implements OnInit {
  private readonly api = inject(AdminApiService);

  protected readonly products    = signal<ProductResponse[]>([]);
  protected readonly categories  = signal<CategoryResponse[]>([]);
  protected readonly loading     = signal(false);
  protected readonly error       = signal<string | null>(null);
  protected readonly currentPage = signal(0);
  protected readonly totalPages  = signal(0);

  protected searchQuery     = '';
  protected filterCategoryId = '';
  protected filterActive    = '';

  private searchDebounce: ReturnType<typeof setTimeout> | null = null;

  protected readonly modalType      = signal<ModalType>(null);
  protected readonly modalLoading   = signal(false);
  protected readonly modalError     = signal<string | null>(null);
  protected readonly selectedProduct = signal<ProductResponse | null>(null);

  protected productForm: { name: string; description: string; categoryId: number | null } =
    { name: '', description: '', categoryId: null };
  protected productPriceEuros: number | null = null;
  protected discontinueReason = '';

  // id of the product being edited (null for create)
  private editingProductId: number | null = null;

  ngOnInit(): void {
    this.loadCategories();
    this.load();
  }

  private loadCategories(): void {
    this.api.listCategories().subscribe({
      next: list => this.categories.set(list),
    });
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    const active = this.filterActive ? this.filterActive === 'true' : undefined;
    const categoryId = this.filterCategoryId ? Number(this.filterCategoryId) : undefined;
    const q = this.searchQuery.trim() || undefined;

    this.api.listProducts(active, this.currentPage(), 20, categoryId, q).subscribe({
      next: page => {
        this.products.set(page.content);
        this.totalPages.set(page.totalPages);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al cargar los productos. Inténtalo de nuevo.');
        this.loading.set(false);
      },
    });
  }

  protected onSearchChange(): void {
    if (this.searchDebounce) clearTimeout(this.searchDebounce);
    this.searchDebounce = setTimeout(() => { this.currentPage.set(0); this.load(); }, 350);
  }

  protected goToPage(page: number): void { this.currentPage.set(page); this.load(); }

  protected formatPrice(cents: number): string {
    return (cents / 100).toLocaleString('es-ES', { style: 'currency', currency: 'EUR' });
  }

  // ── Modals ────────────────────────────────────────────────────────────────

  protected openCreate(): void {
    this.editingProductId = null;
    this.productForm = { name: '', description: '', categoryId: null };
    this.productPriceEuros = null;
    this.openModal('create');
  }

  protected openEdit(p: ProductResponse): void {
    this.editingProductId = p.id;
    this.productForm = {
      name: p.name,
      description: p.description ?? '',
      categoryId: p.categoryId ?? null,
    };
    this.productPriceEuros = p.priceCents / 100;
    this.selectedProduct.set(p);
    this.openModal('edit');
  }

  protected openDiscontinue(p: ProductResponse): void {
    this.selectedProduct.set(p);
    this.discontinueReason = '';
    this.openModal('discontinue');
  }

  protected openReactivate(p: ProductResponse): void {
    this.selectedProduct.set(p);
    this.openModal('confirmReactivate');
  }

  protected submitProductForm(): void {
    if (!this.productForm.name.trim()) {
      this.modalError.set('El nombre es obligatorio.');
      return;
    }
    if (this.productPriceEuros == null || this.productPriceEuros < 0) {
      this.modalError.set('Se requiere un precio válido.');
      return;
    }

    const priceCents = Math.round(this.productPriceEuros * 100);
    this.modalLoading.set(true);
    this.modalError.set(null);

    if (this.editingProductId == null) {
      // Create
      const req: CreateProductRequest = {
        name: this.productForm.name.trim(),
        description: this.productForm.description.trim() || undefined,
        priceCents,
        categoryId: this.productForm.categoryId ?? undefined,
      };
      this.api.createProduct(req).subscribe({
        next: () => { this.closeModal(); this.currentPage.set(0); this.load(); },
        error: err => this.handleModalError(err),
      });
    } else {
      // Update
      const req: UpdateProductRequest = {
        name: this.productForm.name.trim(),
        description: this.productForm.description.trim() || undefined,
        priceCents,
        categoryId: this.productForm.categoryId ?? undefined,
      };
      this.api.updateProduct(this.editingProductId, req).subscribe({
        next: () => { this.closeModal(); this.load(); },
        error: err => this.handleModalError(err),
      });
    }
  }

  protected submitDiscontinue(): void {
    const p = this.selectedProduct();
    if (!p) return;
    this.modalLoading.set(true);
    this.modalError.set(null);
    this.api.discontinueProduct(p.id, this.discontinueReason.trim() || undefined).subscribe({
      next: () => { this.closeModal(); this.load(); },
      error: err => this.handleModalError(err),
    });
  }

  protected submitReactivate(): void {
    const p = this.selectedProduct();
    if (!p) return;
    this.modalLoading.set(true);
    this.modalError.set(null);
    this.api.reactivateProduct(p.id).subscribe({
      next: () => { this.closeModal(); this.load(); },
      error: err => this.handleModalError(err),
    });
  }

  protected closeModal(): void {
    this.modalType.set(null);
    this.modalLoading.set(false);
    this.modalError.set(null);
    this.selectedProduct.set(null);
  }

  private openModal(type: ModalType): void {
    this.modalError.set(null);
    this.modalLoading.set(false);
    this.modalType.set(type);
  }

  private handleModalError(err: unknown): void {
    this.modalLoading.set(false);
    const details = (err as any)?.error?.error?.details;
    if (details) { this.modalError.set(Object.values(details).join(' ')); return; }
    this.modalError.set((err as any)?.error?.error?.message ?? 'Ha ocurrido un error inesperado.');
  }
}
