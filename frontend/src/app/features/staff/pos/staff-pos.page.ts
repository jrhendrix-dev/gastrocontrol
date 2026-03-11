import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, OnInit } from '@angular/core';
import { CommonModule, CurrencyPipe, NgFor, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { StaffTablesApi } from '@app/app/core/api/staff/staff-tables.api';
import { StaffOrdersApi } from '@app/app/core/api/staff/staff-orders.api';
import { StaffProductsApi } from '@app/app/core/api/staff/staff-products.api';
import { CatalogApi } from '@app/app/core/api/catalog/catalog.api';
import { DiningTableResponse, OrderResponse, ProductResponse } from '@app/app/core/api/staff/staff.models';
import { CatalogCategoryDto, CatalogProductDto } from '@app/app/core/api/catalog/catalog.models';


/**
 * If you don't have this yet, tell me and I'll generate it:
 * - GET /api/catalog/categories
 * - GET /api/catalog/products?categoryId=...
 */
// import { CatalogApi, CatalogCategoryDto } from '@app/app/core/api/catalog/catalog.api';

type CategoryVm = { id: number | null; name: string };

@Component({
  standalone: true,
  selector: 'gc-staff-pos-page',
  imports: [CommonModule, FormsModule, NgIf, NgFor, CurrencyPipe],
  templateUrl: './staff-pos.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StaffPosPage implements OnInit {
  private readonly tablesApi = inject(StaffTablesApi);
  private readonly ordersApi = inject(StaffOrdersApi);
  private readonly productsApi = inject(StaffProductsApi);
  private readonly catalogApi = inject(CatalogApi);

  loadingTables = signal(false);
  tables = signal<DiningTableResponse[]>([]);
  tableQuery = signal('');

  loadingOrder = signal(false);
  currentTable = signal<DiningTableResponse | null>(null);
  order = signal<OrderResponse | null>(null);
  orderError = signal<string | null>(null);
  newNoteValue = '';              // bound to the textarea via [(ngModel)]
  addingNote = signal(false);
  /**
   * The id of the note currently being edited inline, or null if none.
   * Used to show/hide the inline edit textarea on the correct note card.
   */
  editingNoteId = signal<number | null>(null);

  /**
   * The live text value inside the inline edit textarea.
   * Bound via [(ngModel)].
   */
  editingNoteValue = '';

  /**
   * The id of the note awaiting delete confirmation, or null if none.
   * A note moves to this state when the user first clicks the trash icon.
   * A second click confirms and fires the delete request.
   */
  confirmDeleteNoteId = signal<number | null>(null);

  /** Whether a note update request is currently in-flight. */
  savingNote = signal(false);

  /** Whether a note delete request is currently in-flight. */
  deletingNote = signal(false);

  // Category browsing
  catalogProducts = signal<CatalogProductDto[]>([]);
  loadingCategories = signal(false);
  categories = signal<CategoryVm[]>([{ id: null, name: 'Todos' }]);
  selectedCategoryId = signal<number | null>(null);

  productQuery = signal('');
  loadingProducts = signal(false);
  products = signal<ProductResponse[]>([]);

  // Template-friendly bindable fields (ngModel works with plain fields)
  tableQueryValue = '';
  productQueryValue = '';

  canEdit = computed(() => {
    const o = this.order();
    if (!o) return false;
    return o.status === 'DRAFT' || o.status === 'PENDING';
  });

  isDraft = computed(() => this.order()?.status === 'DRAFT');
  isPending = computed(() => this.order()?.status === 'PENDING');

  constructor() {
    // Keep signals in sync with ngModel fields.
    effect(() => {
      // We want products to be available for browsing even if there is no order yet.
      // Search refines the current product list fetch.
      const q = this.productQuery().trim();
      this.refreshProducts({ query: q, categoryId: this.selectedCategoryId() });
    });
  }

  ngOnInit(): void {
    // Fix: initial load should happen here (reliably triggers when screen is entered).
    this.refreshTables();
    this.loadCategories();
    this.loadCatalogProducts(null);

    // Category browsing:
    // If you already have categories available somewhere else, call that.
    // Otherwise, wire CatalogApi and uncomment loadCategories().
    // this.loadCategories();

    // Minimal fallback: if you don't load categories yet, still load products for "Todos"
    this.refreshProducts({ query: '', categoryId: null });
  }

  trackTable = (_: number, t: DiningTableResponse) => t.id;
  trackProduct = (_: number, p: ProductResponse) => p.id;
  trackItem = (_: number, i: any) => i.id;
  trackCategory = (_: number, c: CategoryVm) => String(c.id);
  /**
   * TrackBy for the notes list — prevents DOM thrashing when a new note is added.
   *
   * @param _ index (unused)
   * @param note the note item
   */
  trackNote(_: number, note: { id: number }): number {
    return note.id;
  }

  onTableQueryChange(v: string) {
    this.tableQueryValue = v ?? '';
    this.tableQuery.set(this.tableQueryValue);
    this.refreshTables(); // do not require length >= 1
  }

  onProductQueryChange(v: string) {
    this.productQueryValue = v ?? '';
    this.productQuery.set(this.productQueryValue);
  }

  refreshTables() {
    this.loadingTables.set(true);
    this.tablesApi
      .list({
        q: this.tableQuery().trim() || undefined,
        size: 50,
        includeActiveOrder: true,
        sort: 'id,asc',
      })
      .subscribe({
        next: (res) => this.tables.set(res.content ?? []),
        error: () => this.tables.set([]),
        complete: () => this.loadingTables.set(false),
      });
  }

  /**
   * Option A: Selecting a table must NOT create a draft.
   * If the table has an active order -> load it. Otherwise -> order stays null.
   */
  selectTable(t: DiningTableResponse) {
    this.currentTable.set(t);
    this.orderError.set(null);
    this.order.set(null);

    const activeOrderId = t.activeOrder?.orderId;
    if (activeOrderId) {
      this.loadOrder(activeOrderId);
      return;
    }

    // No active order: table is free, and we don't open a draft until first product is added.
    // Keep order null; UI can show the hint "Añade un producto para abrir el ticket."
  }

  loadOrder(orderId: number) {
    this.loadingOrder.set(true);
    this.ordersApi.getById(orderId).subscribe({
      next: (o) => this.order.set(o),
      error: () => this.orderError.set('No se pudo cargar el pedido.'),
      complete: () => this.loadingOrder.set(false),
    });
  }

  /**
   * Lazy order creation:
   * - If there's no order yet for the selected table -> create draft, then add item
   * - Else -> add item directly
   */
  addProduct(p: ProductResponse) {
    const table = this.currentTable();
    if (!table) return;

    const o = this.order();
    if (!o) {
      this.openDraftThenAdd(table.id, p.id);
      return;
    }

    if (!this.canEdit()) return;

    this.ordersApi.addItem(o.id, { productId: p.id, quantity: 1 }).subscribe({
      next: (updated) => {
        this.order.set(updated);
        this.refreshTables();
      },
      error: (err) => this.orderError.set(this.extractErrorMessage(err) ?? 'No se pudo añadir el producto.'),
    });
  }

  private openDraftThenAdd(tableId: number, productId: number) {
    this.loadingOrder.set(true);
    this.orderError.set(null);

    this.ordersApi.createDraft({ type: 'DINE_IN', tableId }).subscribe({
      next: (draft) => {
        this.order.set(draft);
        // Now add item
        this.ordersApi.addItem(draft.id, { productId, quantity: 1 }).subscribe({
          next: (updated) => {
            this.order.set(updated);
            this.refreshTables(); // table becomes occupied only now
          },
          error: (err) => this.orderError.set(this.extractErrorMessage(err) ?? 'No se pudo añadir el producto.'),
          complete: () => this.loadingOrder.set(false),
        });
      },
      error: (err) => {
        this.orderError.set(this.extractErrorMessage(err) ?? 'No se pudo abrir el ticket.');
        this.loadingOrder.set(false);
      },
    });
  }

  /**
   * Staff cancel: DRAFT + PENDING only (backend enforces).
   */
  cancelOrder() {
    const o = this.order();
    if (!o) return;

    // You will add this endpoint:
    // POST /api/staff/orders/{orderId}/actions/cancel
    this.ordersApi.cancel(o.id).subscribe({
      next: () => {
        this.order.set(null);
        this.refreshTables();
      },
      error: (err) => this.orderError.set(this.extractErrorMessage(err) ?? 'No se pudo cancelar el pedido.'),
    });
  }

  inc(itemId: number) {
    const o = this.order();
    if (!o) return;
    const item = o.items.find((i) => i.id === itemId);
    if (!item) return;
    this.setQty(itemId, item.quantity + 1);
  }

  dec(itemId: number) {
    const o = this.order();
    if (!o) return;
    const item = o.items.find((i) => i.id === itemId);
    if (!item) return;
    const nextQty = item.quantity - 1;
    if (nextQty <= 0) {
      this.remove(itemId);
      return;
    }
    this.setQty(itemId, nextQty);
  }

  setQty(itemId: number, quantity: number) {
    const o = this.order();
    if (!o) return;
    if (!this.canEdit()) return;

    this.ordersApi.updateItemQuantity(o.id, itemId, { quantity }).subscribe({
      next: (updated) => this.order.set(updated),
      error: (err) => this.orderError.set(this.extractErrorMessage(err) ?? 'No se pudo actualizar la cantidad.'),
    });
  }

  remove(itemId: number) {
    const o = this.order();
    if (!o) return;
    if (!this.canEdit()) return;

    this.ordersApi.removeItem(o.id, itemId).subscribe({
      next: (updated) => this.order.set(updated),
      error: (err) => this.orderError.set(this.extractErrorMessage(err) ?? 'No se pudo eliminar la línea.'),
    });
  }

  submitToKitchen() {
    const o = this.order();
    if (!o) return;

    this.ordersApi.submit(o.id).subscribe({
      next: () => {
        this.loadOrder(o.id);
        this.refreshTables();
      },
      error: (err) => this.orderError.set(this.extractErrorMessage(err) ?? 'No se pudo enviar a cocina.'),
    });
  }

  /**
   * Submits a new note for the current order.
   *
   * On success the order signal is updated (which re-renders the notes list),
   * and the textarea is cleared. On error a message is shown inline.
   *
   * @param orderId the order to annotate
   */
  addNote(orderId: number): void {
    const text = this.newNoteValue.trim();
    if (!text) return;

    this.addingNote.set(true);
    this.ordersApi.addNote(orderId, { note: text }).subscribe({
      next: (updated) => {
        this.order.set(updated);
        this.newNoteValue = '';
        this.addingNote.set(false);
      },
      error: (err) => {
        this.orderError.set(this.extractErrorMessage(err) ?? 'No se pudo guardar la nota.');
        this.addingNote.set(false);
      },
    });
  }

  /**
   * Enters inline-edit mode for the given note.
   * Pre-fills the textarea with the note's current text.
   *
   * @param noteId  the note to edit
   * @param current the note's current text value
   */
  startEditNote(noteId: number, current: string): void {
    this.editingNoteId.set(noteId);
    this.editingNoteValue = current;
    this.confirmDeleteNoteId.set(null); // cancel any pending delete confirmation
  }

  /**
   * Cancels inline-edit mode without saving.
   */
  cancelEditNote(): void {
    this.editingNoteId.set(null);
    this.editingNoteValue = '';
  }

  /**
   * Saves the edited note text.
   *
   * Optimistic update: the order signal is updated immediately with the new text
   * so the UI feels instant. On error the original text is restored and an error
   * message is shown.
   *
   * @param orderId the order the note belongs to
   * @param noteId  the note to update
   */
  saveEditNote(orderId: number, noteId: number): void {
    const text = this.editingNoteValue.trim();
    if (!text) return;

    const previousOrder = this.order();
    if (!previousOrder) return;

    // Optimistic update: replace note text in the local signal immediately
    this.order.update((o) =>
      o
        ? {
          ...o,
          notes: o.notes.map((n) =>
            n.id === noteId ? { ...n, note: text } : n
          ),
        }
        : null
    );

    this.editingNoteId.set(null);
    this.editingNoteValue = '';
    this.savingNote.set(true);

    this.ordersApi.updateNote(orderId, noteId, { note: text }).subscribe({
      next: (updated) => {
        // Reconcile with server truth (picks up originalNote / editedAt)
        this.order.set(updated);
        this.savingNote.set(false);
      },
      error: (err) => {
        // Roll back to the previous order state
        this.order.set(previousOrder);
        this.savingNote.set(false);
        this.orderError.set(this.extractErrorMessage(err) ?? 'No se pudo guardar la nota.');
      },
    });
  }

  /**
   * Handles the delete flow for a note.
   *
   * First click: sets the note as pending confirmation (shows a red "confirm" button).
   * Second click (same noteId): fires the delete request.
   * Clicking a different note's trash icon cancels the previous confirmation.
   *
   * @param orderId the order the note belongs to
   * @param noteId  the note to delete
   */
  requestDeleteNote(orderId: number, noteId: number): void {
    // Cancel any open inline edit
    this.editingNoteId.set(null);

    if (this.confirmDeleteNoteId() === noteId) {
      // Second click: confirmed — fire the delete
      this.executeDeleteNote(orderId, noteId);
    } else {
      // First click: enter confirmation state
      this.confirmDeleteNoteId.set(noteId);
    }
  }

  /**
   * Cancels a pending delete confirmation without deleting.
   */
  cancelDeleteNote(): void {
    this.confirmDeleteNoteId.set(null);
  }

  /**
   * Fires the actual delete request after confirmation.
   * Uses optimistic removal for immediate feedback.
   *
   * @param orderId the order the note belongs to
   * @param noteId  the note to delete
   */
  private executeDeleteNote(orderId: number, noteId: number): void {
    const previousOrder = this.order();
    if (!previousOrder) return;

    // Optimistic: remove from local signal immediately
    this.order.update((o) =>
      o ? { ...o, notes: o.notes.filter((n) => n.id !== noteId) } : null
    );
    this.confirmDeleteNoteId.set(null);
    this.deletingNote.set(true);

    this.ordersApi.deleteNote(orderId, noteId).subscribe({
      next: (updated) => {
        this.order.set(updated);
        this.deletingNote.set(false);
      },
      error: (err) => {
        // Roll back
        this.order.set(previousOrder);
        this.deletingNote.set(false);
        this.orderError.set(this.extractErrorMessage(err) ?? 'No se pudo eliminar la nota.');
      },
    });
  }

  /**
   * Products browsing:
   * - Always show products for the current category (even with empty query)
   * - If query is non-empty, use it as a server-side search filter
   */
  private refreshProducts(opts: { query: string; categoryId: number | null }) {
    const q = (opts.query ?? '').trim();
    const categoryId = opts.categoryId ?? null;

    this.loadingProducts.set(true);
    this.productsApi
      .list({
        active: true,
        categoryId: categoryId ?? undefined,
        q: q.length ? q : undefined,
        size: 50,
        sort: 'name,asc',
      })
      .subscribe({
        next: (res) => this.products.set(res.content ?? []),
        error: () => this.products.set([]),
        complete: () => this.loadingProducts.set(false),
      });
  }

  private extractErrorMessage(err: any): string | null {
    const details = err?.error?.error?.details;
    if (details && typeof details === 'object') {
      const firstKey = Object.keys(details)[0];
      if (firstKey) return String(details[firstKey]);
    }
    const msg = err?.error?.message || err?.message;
    return msg ? String(msg) : null;
  }

  private loadCategories() {
    this.loadingCategories.set(true);
    this.catalogApi.listCategories().subscribe({
      next: (cats) => {
        this.categories.set([{ id: null, name: 'Todos' }, ...cats.map(c => ({ id: c.id, name: c.name }))]);
      },
      error: () => this.categories.set([{ id: null, name: 'Todos' }]),
      complete: () => this.loadingCategories.set(false),
    });
  }

  selectCategory(id: number | null) {
    this.selectedCategoryId.set(id);
    this.loadCatalogProducts(id);
  }

  private loadCatalogProducts(categoryId: number | null) {
    this.loadingProducts.set(true);
    this.catalogApi.listProducts({ categoryId }).subscribe({
      next: (ps) => {
        // Map catalog product shape into your existing ProductResponse shape expected by template
        // (or update template to use CatalogProductDto)
        this.products.set(
          ps.map((p) => ({
            id: p.id,
            name: p.name,
            priceCents: p.priceCents,
            categoryName: p.categoryName ?? '',
          })) as any
        );
      },
      error: () => this.products.set([]),
      complete: () => this.loadingProducts.set(false),
    });
  }




  canStartOrEdit = computed(() => {
    const o = this.order();
    if (!o) return true; // allow opening draft by adding first item
    return o.status === 'DRAFT' || o.status === 'PENDING';
  });

}
