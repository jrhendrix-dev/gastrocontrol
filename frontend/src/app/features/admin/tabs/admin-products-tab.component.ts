// src/app/features/admin/tabs/admin-products-tab.component.ts
import {
  ChangeDetectionStrategy, Component, inject, OnInit, signal,
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
 * - Thumbnail preview in the product list
 * - Create product (name, description, price in €, category)
 * - Edit product (same fields) + image upload/remove
 * - Discontinue with optional reason
 * - Reactivate discontinued product
 */
@Component({
  selector: 'app-admin-products-tab',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="products-tab">

      <!-- ── Toolbar ──────────────────────────────────────────────────── -->
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

      <!-- ── Product table ─────────────────────────────────────────────── -->
      <div class="gc-card table-wrapper">
        <table class="products-table">
          <thead>
          <tr>
            <th class="col-img"></th>
            <th>Nombre</th>
            <th>Categoría</th>
            <th>Precio</th>
            <th>Estado</th>
            <th>Acciones</th>
          </tr>
          </thead>
          <tbody>
            @if (loading()) {
              <tr><td colspan="6" class="empty-cell">Cargando…</td></tr>
            } @else if (products().length === 0) {
              <tr><td colspan="6" class="empty-cell">No se encontraron productos.</td></tr>
            } @else {
              @for (p of products(); track p.id) {
                <tr [class.discontinued-row]="!p.active">

                  <!-- Thumbnail -->
                  <td class="col-img">
                    @if (p.imageUrl) {
                      <img [src]="p.imageUrl" [alt]="p.name" class="product-thumb" />
                    } @else {
                      <div class="product-thumb-placeholder">🍽️</div>
                    }
                  </td>

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
                  <td>
                    <div class="row-actions">
                      <button class="btn btn-sm btn-secondary" (click)="openEdit(p)">Editar</button>
                      @if (p.active) {
                        <button class="btn btn-sm btn-danger" (click)="openDiscontinue(p)">Descatalogar</button>
                      } @else {
                        <button class="btn btn-sm btn-secondary" (click)="openReactivate(p)">Reactivar</button>
                      }
                    </div>
                  </td>
                </tr>
              }
            }
          </tbody>
        </table>
      </div>

      <!-- ── Pagination ────────────────────────────────────────────────── -->
      @if (totalPages() > 1) {
        <div class="pagination">
          <button class="btn btn-sm btn-secondary"
                  [disabled]="currentPage() === 0"
                  (click)="changePage(currentPage() - 1)">‹ Anterior</button>
          <span class="page-info">Página {{ currentPage() + 1 }} de {{ totalPages() }}</span>
          <button class="btn btn-sm btn-secondary"
                  [disabled]="currentPage() >= totalPages() - 1"
                  (click)="changePage(currentPage() + 1)">Siguiente ›</button>
        </div>
      }

      <!-- ── Modal ─────────────────────────────────────────────────────── -->
      @if (modalType()) {
        <div class="modal-backdrop" (click)="closeModal()">
          <div class="modal" (click)="$event.stopPropagation()">

            <!-- Create / Edit -->
            @if (modalType() === 'create' || modalType() === 'edit') {
              <h2 class="modal-title">
                {{ modalType() === 'create' ? 'Nuevo producto' : 'Editar producto' }}
              </h2>

              @if (modalError()) {
                <div class="error-banner">{{ modalError() }}</div>
              }

              <div class="form-group">
                <label class="form-label">Nombre *</label>
                <input class="gc-input" type="text" [(ngModel)]="productForm.name"
                       placeholder="Ej. Tortilla española" />
              </div>

              <div class="form-group">
                <label class="form-label">Descripción</label>
                <textarea class="gc-input" rows="3" [(ngModel)]="productForm.description"
                          placeholder="Descripción corta del producto…"></textarea>
              </div>

              <div class="form-row">
                <div class="form-group" style="flex:1">
                  <label class="form-label">Precio (€) *</label>
                  <input class="gc-input" type="number" min="0" step="0.01"
                         [(ngModel)]="productPriceEuros"
                         placeholder="0.00" />
                </div>
                <div class="form-group" style="flex:1">
                  <label class="form-label">Categoría</label>
                  <select class="gc-input" [(ngModel)]="productForm.categoryId">
                    <option [ngValue]="null">Sin categoría</option>
                    @for (cat of categories(); track cat.id) {
                      <option [ngValue]="cat.id">{{ cat.name }}</option>
                    }
                  </select>
                </div>
              </div>

              <!-- ── Image section (edit only) ───────────────────────── -->
              @if (modalType() === 'edit' && selectedProduct()) {
                <div class="form-group image-section">
                  <label class="form-label">Imagen del producto</label>

                  <!-- Current image preview -->
                  @if (selectedProduct()!.imageUrl) {
                    <div class="image-preview-row">
                      <img [src]="selectedProduct()!.imageUrl!"
                           [alt]="selectedProduct()!.name"
                           class="image-preview" />
                      <button class="btn btn-sm btn-danger"
                              [disabled]="imageLoading()"
                              (click)="removeImage()">
                        {{ imageLoading() ? 'Eliminando…' : 'Eliminar imagen' }}
                      </button>
                    </div>
                  } @else {
                    <p class="image-hint">Este producto no tiene imagen.</p>
                  }

                  <!-- Upload new image -->
                  <div class="upload-row">
                    <input #fileInput type="file"
                           accept="image/jpeg,image/png,image/webp"
                           class="file-input"
                           (change)="onFileSelected($event)" />
                    <button class="btn btn-sm btn-secondary"
                            [disabled]="imageLoading() || !selectedFile()"
                            (click)="uploadImage()">
                      {{ imageLoading() ? 'Subiendo…' : (selectedProduct()!.imageUrl ? 'Cambiar imagen' : 'Subir imagen') }}
                    </button>
                  </div>

                  @if (selectedFile()) {
                    <p class="file-hint">Seleccionado: {{ selectedFile()!.name }}</p>
                  }
                  @if (imageError()) {
                    <p class="image-error">{{ imageError() }}</p>
                  }
                </div>
              }

              <div class="modal-actions">
                <button class="btn btn-secondary" (click)="closeModal()">Cancelar</button>
                <button class="btn btn-primary"
                        [disabled]="modalLoading()"
                        (click)="submitProductForm()">
                  {{ modalLoading() ? 'Guardando…' : 'Guardar' }}
                </button>
              </div>
            }

            <!-- Discontinue -->
            @if (modalType() === 'discontinue') {
              <h2 class="modal-title">Descatalogar producto</h2>
              <p class="modal-body">
                ¿Seguro que quieres descatalogar <strong>{{ selectedProduct()?.name }}</strong>?
                Dejará de aparecer en la carta.
              </p>
              <div class="form-group">
                <label class="form-label">Motivo (opcional)</label>
                <input class="gc-input" type="text" [(ngModel)]="discontinueReason"
                       placeholder="Ej. Fuera de temporada" />
              </div>
              @if (modalError()) {
                <div class="error-banner">{{ modalError() }}</div>
              }
              <div class="modal-actions">
                <button class="btn btn-secondary" (click)="closeModal()">Cancelar</button>
                <button class="btn btn-danger" [disabled]="modalLoading()" (click)="submitDiscontinue()">
                  {{ modalLoading() ? 'Descatalogando…' : 'Descatalogar' }}
                </button>
              </div>
            }

            <!-- Confirm reactivate -->
            @if (modalType() === 'confirmReactivate') {
              <h2 class="modal-title">Reactivar producto</h2>
              <p class="modal-body">
                ¿Reactivar <strong>{{ selectedProduct()?.name }}</strong>?
                Volverá a aparecer en la carta.
              </p>
              @if (modalError()) {
                <div class="error-banner">{{ modalError() }}</div>
              }
              <div class="modal-actions">
                <button class="btn btn-secondary" (click)="closeModal()">Cancelar</button>
                <button class="btn btn-primary" [disabled]="modalLoading()" (click)="submitReactivate()">
                  {{ modalLoading() ? 'Reactivando…' : 'Reactivar' }}
                </button>
              </div>
            }

          </div>
        </div>
      }

    </div>
  `,
  styles: [`
    .products-tab { display: flex; flex-direction: column; gap: 1rem; }

    /* ── Toolbar ──────────────────────────────────────────────────────── */
    .toolbar { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 0.75rem; }
    .filters { display: flex; gap: 0.5rem; flex-wrap: wrap; }
    .filter-search { width: 200px; }
    .filter-select { width: 160px; }

    /* ── Table ────────────────────────────────────────────────────────── */
    .table-wrapper { overflow-x: auto; }
    .products-table { width: 100%; border-collapse: collapse; font-size: 0.875rem; }
    .products-table th {
      text-align: left; padding: 0.625rem 0.875rem;
      border-bottom: 2px solid rgba(0,0,0,0.08);
      font-size: 0.75rem; font-weight: 700; color: #666;
      text-transform: uppercase; letter-spacing: 0.05em;
    }
    .products-table td { padding: 0.625rem 0.875rem; border-bottom: 1px solid rgba(0,0,0,0.06); vertical-align: middle; }
    .discontinued-row { opacity: 0.55; }
    .empty-cell { text-align: center; padding: 2rem; color: #999; }

    /* Thumbnail column */
    .col-img { width: 52px; padding: 0.375rem 0.5rem !important; }
    .product-thumb {
      width: 44px; height: 44px; border-radius: 6px;
      object-fit: cover; display: block;
      border: 1px solid rgba(0,0,0,0.08);
    }
    .product-thumb-placeholder {
      width: 44px; height: 44px; border-radius: 6px;
      background: #f0ede8; display: grid; place-items: center;
      font-size: 1.25rem; border: 1px solid rgba(0,0,0,0.06);
    }

    .product-name { font-weight: 600; color: #1a2e1a; }
    .product-desc { font-size: 0.75rem; color: #999; margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 240px; }
    .cell-muted { color: #888; }
    .cell-price { font-weight: 600; font-variant-numeric: tabular-nums; }

    .status-chip {
      display: inline-block; padding: 0.2rem 0.6rem;
      border-radius: 999px; font-size: 0.72rem; font-weight: 600;
      background: rgba(0,0,0,0.08); color: #666;
      &.active { background: rgba(26,46,26,0.1); color: #1a2e1a; }
    }

    .row-actions { display: flex; gap: 0.375rem; }

    /* ── Pagination ────────────────────────────────────────────────────── */
    .pagination { display: flex; align-items: center; justify-content: center; gap: 1rem; }
    .page-info { font-size: 0.875rem; color: #666; }

    /* ── Modal ─────────────────────────────────────────────────────────── */
    .modal-backdrop {
      position: fixed; inset: 0; background: rgba(0,0,0,0.45);
      display: grid; place-items: center; z-index: 1000;
      backdrop-filter: blur(2px);
    }
    .modal {
      background: white; border-radius: 12px; padding: 2rem;
      width: min(520px, 90vw); max-height: 90vh; overflow-y: auto;
      box-shadow: 0 20px 60px rgba(0,0,0,0.2);
    }
    .modal-title { font-size: 1.2rem; font-weight: 700; color: #1a2e1a; margin: 0 0 1.25rem; }
    .modal-body  { color: #555; margin: 0 0 1rem; line-height: 1.6; }

    /* ── Form ───────────────────────────────────────────────────────────── */
    .form-group { display: flex; flex-direction: column; gap: 0.35rem; margin-bottom: 1rem; }
    .form-label { font-size: 0.8rem; font-weight: 600; color: #444; }
    .gc-input { padding: 0.5rem 0.75rem; border: 1px solid rgba(0,0,0,0.15); border-radius: 6px; font-size: 0.875rem; outline: none; width: 100%; box-sizing: border-box; background: white; }
    .gc-input:focus { border-color: #1a2e1a; box-shadow: 0 0 0 2px rgba(26,46,26,0.1); }
    textarea.gc-input { resize: vertical; }
    .form-row { display: flex; gap: 0.75rem; }
    .modal-actions { display: flex; justify-content: flex-end; gap: 0.5rem; margin-top: 0.25rem; }

    /* ── Image upload ───────────────────────────────────────────────────── */
    .image-section { border-top: 1px solid rgba(0,0,0,0.08); padding-top: 1rem; margin-top: 0.5rem; }
    .image-preview-row { display: flex; align-items: center; gap: 1rem; margin-bottom: 0.75rem; }
    .image-preview { width: 80px; height: 80px; object-fit: cover; border-radius: 8px; border: 1px solid rgba(0,0,0,0.1); }
    .image-hint { font-size: 0.8rem; color: #999; margin: 0 0 0.75rem; }
    .upload-row { display: flex; align-items: center; gap: 0.75rem; flex-wrap: wrap; }
    .file-input { font-size: 0.8rem; flex: 1; min-width: 0; }
    .file-hint { font-size: 0.75rem; color: #666; margin: 0.25rem 0 0; }
    .image-error { font-size: 0.8rem; color: #c00; margin: 0.25rem 0 0; }

    /* ── Buttons ────────────────────────────────────────────────────────── */
    .btn { padding: 0.5rem 1rem; border: none; border-radius: 6px; font-size: 0.875rem; font-weight: 600; cursor: pointer; transition: opacity 0.15s; }
    .btn:disabled { opacity: 0.5; cursor: not-allowed; }
    .btn-primary   { background: #1a2e1a; color: white; }
    .btn-secondary { background: rgba(0,0,0,0.07); color: #333; }
    .btn-danger    { background: #c0392b; color: white; }
    .btn-sm { padding: 0.3rem 0.6rem; font-size: 0.8rem; }

    .error-banner { background: #fff0f0; color: #c00; padding: 0.75rem 1rem; border-radius: 6px; font-size: 0.875rem; margin-bottom: 0.75rem; }
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

  protected searchQuery      = '';
  protected filterCategoryId = '';
  protected filterActive     = '';

  private searchDebounce: ReturnType<typeof setTimeout> | null = null;

  protected readonly modalType       = signal<ModalType>(null);
  protected readonly modalLoading    = signal(false);
  protected readonly modalError      = signal<string | null>(null);
  protected readonly selectedProduct = signal<ProductResponse | null>(null);

  /** Currently selected File object (before upload). */
  protected readonly selectedFile  = signal<File | null>(null);
  /** True while an image upload or delete HTTP call is in flight. */
  protected readonly imageLoading  = signal(false);
  /** Error message from the last image operation. */
  protected readonly imageError    = signal<string | null>(null);

  protected productForm: { name: string; description: string; categoryId: number | null } =
    { name: '', description: '', categoryId: null };
  protected productPriceEuros: number | null = null;
  protected discontinueReason = '';

  private editingProductId: number | null = null;

  ngOnInit(): void {
    this.loadCategories();
    this.load();
  }

  // ── Data loading ────────────────────────────────────────────────────────

  private loadCategories(): void {
    this.api.listCategories().subscribe({ next: list => this.categories.set(list) });
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    const active     = this.filterActive ? this.filterActive === 'true' : undefined;
    const categoryId = this.filterCategoryId ? Number(this.filterCategoryId) : undefined;
    const q          = this.searchQuery.trim() || undefined;

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
    this.searchDebounce = setTimeout(() => this.load(), 350);
  }

  protected changePage(page: number): void {
    this.currentPage.set(page);
    this.load();
  }

  // ── Modal openers ────────────────────────────────────────────────────────

  protected openCreate(): void {
    this.editingProductId  = null;
    this.productForm       = { name: '', description: '', categoryId: null };
    this.productPriceEuros = null;
    this.modalError.set(null);
    this.modalType.set('create');
  }

  protected openEdit(p: ProductResponse): void {
    this.editingProductId  = p.id;
    this.productForm       = { name: p.name, description: p.description ?? '', categoryId: p.categoryId };
    this.productPriceEuros = p.priceCents / 100;
    this.selectedProduct.set(p);
    this.selectedFile.set(null);
    this.imageError.set(null);
    this.modalError.set(null);
    this.modalType.set('edit');
  }

  protected openDiscontinue(p: ProductResponse): void {
    this.selectedProduct.set(p);
    this.discontinueReason = '';
    this.modalError.set(null);
    this.modalType.set('discontinue');
  }

  protected openReactivate(p: ProductResponse): void {
    this.selectedProduct.set(p);
    this.modalError.set(null);
    this.modalType.set('confirmReactivate');
  }

  protected closeModal(): void {
    this.modalType.set(null);
    this.selectedProduct.set(null);
    this.selectedFile.set(null);
    this.imageError.set(null);
  }

  // ── Form submissions ─────────────────────────────────────────────────────

  protected submitProductForm(): void {
    const priceCents = Math.round((this.productPriceEuros ?? 0) * 100);

    if (!this.productForm.name.trim()) {
      this.modalError.set('El nombre es obligatorio.');
      return;
    }

    this.modalLoading.set(true);
    this.modalError.set(null);

    const obs = this.editingProductId
      ? this.api.updateProduct(this.editingProductId, {
        name:        this.productForm.name.trim(),
        description: this.productForm.description || null,
        priceCents,
        categoryId:  this.productForm.categoryId,
      } as UpdateProductRequest)
      : this.api.createProduct({
        name:        this.productForm.name.trim(),
        description: this.productForm.description || null,
        priceCents,
        categoryId:  this.productForm.categoryId,
      } as CreateProductRequest);

    obs.subscribe({
      next: () => { this.modalLoading.set(false); this.closeModal(); this.load(); },
      error: err => {
        this.modalLoading.set(false);
        this.modalError.set(err?.error?.error?.message ?? 'Error al guardar el producto.');
      },
    });
  }

  protected submitDiscontinue(): void {
    const id = this.selectedProduct()?.id;
    if (!id) return;
    this.modalLoading.set(true);
    this.api.discontinueProduct(id, this.discontinueReason || undefined).subscribe({
      next: () => { this.modalLoading.set(false); this.closeModal(); this.load(); },
      error: err => {
        this.modalLoading.set(false);
        this.modalError.set(err?.error?.error?.message ?? 'Error al descatalogar.');
      },
    });
  }

  protected submitReactivate(): void {
    const id = this.selectedProduct()?.id;
    if (!id) return;
    this.modalLoading.set(true);
    this.api.reactivateProduct(id).subscribe({
      next: () => { this.modalLoading.set(false); this.closeModal(); this.load(); },
      error: err => {
        this.modalLoading.set(false);
        this.modalError.set(err?.error?.error?.message ?? 'Error al reactivar.');
      },
    });
  }

  // ── Image operations ─────────────────────────────────────────────────────

  /**
   * Captures the File object when the user picks one in the file input.
   * Does not upload yet — upload is triggered by the button click.
   */
  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile.set(input.files?.[0] ?? null);
    this.imageError.set(null);
  }

  /**
   * Uploads the selected file, then optimistically updates the product list
   * and the selectedProduct signal so the preview refreshes immediately.
   */
  protected uploadImage(): void {
    const id   = this.selectedProduct()?.id;
    const file = this.selectedFile();
    if (!id || !file) return;

    this.imageLoading.set(true);
    this.imageError.set(null);

    this.api.uploadProductImage(id, file).subscribe({
      next: url => {
        this.imageLoading.set(false);
        this.selectedFile.set(null);

        // Update the product in the list and the modal signal
        this.patchProductImageUrl(id, url);
      },
      error: err => {
        this.imageLoading.set(false);
        this.imageError.set(
          err?.error?.error?.details?.file ??
          err?.error?.error?.message ??
          'Error al subir la imagen.'
        );
      },
    });
  }

  /**
   * Deletes the product image, then clears the URL from the local state.
   */
  protected removeImage(): void {
    const id = this.selectedProduct()?.id;
    if (!id) return;

    this.imageLoading.set(true);
    this.imageError.set(null);

    this.api.deleteProductImage(id).subscribe({
      next: () => {
        this.imageLoading.set(false);
        this.patchProductImageUrl(id, null);
      },
      error: err => {
        this.imageLoading.set(false);
        this.imageError.set(err?.error?.error?.message ?? 'Error al eliminar la imagen.');
      },
    });
  }

  /**
   * Applies an optimistic image URL update to both the products list signal
   * and the selectedProduct signal so the UI reflects the change immediately
   * without a full reload.
   *
   * @param id  the product ID to update
   * @param url the new image URL, or null to clear it
   */
  private patchProductImageUrl(id: number, url: string | null): void {
    this.products.update(list =>
      list.map(p => p.id === id ? { ...p, imageUrl: url } : p)
    );
    const current = this.selectedProduct();
    if (current?.id === id) {
      this.selectedProduct.set({ ...current, imageUrl: url });
    }
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  protected formatPrice(cents: number): string {
    return (cents / 100).toLocaleString('es-ES', { style: 'currency', currency: 'EUR' });
  }
}
