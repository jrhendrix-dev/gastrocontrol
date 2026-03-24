import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, CurrencyPipe, NgFor, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { interval, Subscription } from 'rxjs';
import { startWith, switchMap, catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { StaffTablesApi } from '@app/app/core/api/staff/staff-tables.api';
import { StaffOrdersApi } from '@app/app/core/api/staff/staff-orders.api';
import { StaffProductsApi } from '@app/app/core/api/staff/staff-products.api';
import { CatalogApi } from '@app/app/core/api/catalog/catalog.api';
import { StaffPaymentApi } from '@app/app/core/api/staff/staff-payment.api';
import { PaymentModalComponent, PaymentConfirmedEvent } from './payment-modal/payment-modal.component';
import { DiningTableResponse, OrderResponse, ProductResponse } from '@app/app/core/api/staff/staff.models';
import { CatalogProductDto } from '@app/app/core/api/catalog/catalog.models';

type CategoryVm = { id: number | null; name: string };

/** How often the Externos section auto-refreshes (ms). */
const EXTERNOS_POLL_MS = 15_000;

/** Active statuses shown in the Externos queue. */
const EXTERNOS_STATUSES = 'PENDING,IN_PREPARATION,READY,SERVED';

/**
 * POS page.
 *
 * Left sidebar has two sections:
 *  1. Mesas — DINE_IN table grid (existing)
 *  2. Externos — active TAKE_AWAY / DELIVERY orders queue (new)
 *
 * Staff can advance external orders through their lifecycle directly from
 * the POS without needing the Operations tab.
 */
@Component({
  standalone: true,
  selector: 'gc-staff-pos-page',
  imports: [CommonModule, FormsModule, NgIf, NgFor, CurrencyPipe, PaymentModalComponent],
  templateUrl: './staff-pos.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StaffPosPage implements OnInit, OnDestroy {
  private readonly tablesApi  = inject(StaffTablesApi);
  private readonly ordersApi  = inject(StaffOrdersApi);
  private readonly productsApi = inject(StaffProductsApi);
  private readonly catalogApi = inject(CatalogApi);
  private readonly paymentApi = inject(StaffPaymentApi);
  private readonly http       = inject(HttpClient);

  // ── Tables / dine-in (existing) ──────────────────────────────────────────
  loadingTables = signal(false);
  tables        = signal<DiningTableResponse[]>([]);
  tableQuery    = signal('');

  loadingOrder  = signal(false);
  currentTable  = signal<DiningTableResponse | null>(null);
  order         = signal<OrderResponse | null>(null);
  orderError    = signal<string | null>(null);
  showPaymentModal = signal(false);
  paymentLoading   = signal(false);
  paymentError     = signal<string | null>(null);
  newNoteValue     = '';
  addingNote       = signal(false);
  editingNoteId    = signal<number | null>(null);
  editingNoteValue = '';
  confirmDeleteNoteId = signal<number | null>(null);
  savingNote   = signal(false);
  deletingNote = signal(false);

  // ── Externos (new) ───────────────────────────────────────────────────────
  /** Whether the Externos section is expanded. */
  externosOpen      = signal(true);
  /** Active external orders currently being displayed. */
  externosOrders    = signal<OrderResponse[]>([]);
  externosLoading   = signal(false);
  /** orderId → true while a status change is in-flight. */
  externosChanging  = signal<Record<number, boolean>>({});
  /** orderId → error string, or absent if no error. */
  externosErrors    = signal<Record<number, string>>({});

  private externosSub?: Subscription;

  // ── Products / categories (existing) ─────────────────────────────────────
  catalogProducts    = signal<CatalogProductDto[]>([]);
  loadingCategories  = signal(false);
  categories         = signal<CategoryVm[]>([{ id: null, name: 'Todos' }]);
  selectedCategoryId = signal<number | null>(null);
  productQuery       = signal('');
  loadingProducts    = signal(false);
  products           = signal<ProductResponse[]>([]);
  tableQueryValue    = '';
  productQueryValue  = '';

  // ── Computed (existing) ──────────────────────────────────────────────────
  canEdit = computed(() => {
    const o = this.order();
    if (!o) return false;
    return o.status === 'DRAFT' || o.status === 'PENDING';
  });

  isDraft   = computed(() => this.order()?.status === 'DRAFT');
  isPending = computed(() => this.order()?.status === 'PENDING');
  isServed  = computed(() => this.order()?.status === 'SERVED');

  canStartOrEdit = computed(() => {
    const o = this.order();
    if (!o) return true;
    return o.status === 'DRAFT' || o.status === 'PENDING';
  });

  canSubmitToKitchen = computed(() => {
    const o = this.order();
    if (!o) return false;
    return o.status === 'DRAFT' || (o.status === 'PENDING' && !!o.reopened);
  });

  /** Count of active external orders — shown on the section header badge. */
  externosCount = computed(() => this.externosOrders().length);

  constructor() {
    effect(() => {
      const q = this.productQuery().trim();
      this.refreshProducts({ query: q, categoryId: this.selectedCategoryId() });
    });
  }

  ngOnInit(): void {
    this.refreshTables();
    this.loadCategories();
    this.loadCatalogProducts(null);
    this.refreshProducts({ query: '', categoryId: null });
    this.startExternosPolling();
  }

  ngOnDestroy(): void {
    this.externosSub?.unsubscribe();
  }

  // ── Externos methods ─────────────────────────────────────────────────────

  /**
   * Starts polling the backend for active TAKE_AWAY / DELIVERY orders.
   * Fires immediately then every EXTERNOS_POLL_MS ms.
   * Errors are silently swallowed — the section shows stale data rather than crashing.
   */
  private startExternosPolling(): void {
    this.externosSub = interval(EXTERNOS_POLL_MS).pipe(
      startWith(0),
      switchMap(() => {
        this.externosLoading.set(true);
        return this.http.get<{ content: OrderResponse[] }>(
          `/api/staff/orders?status=${EXTERNOS_STATUSES}&page=0&size=50&sort=createdAt,asc`
        ).pipe(catchError(() => of(null)));
      }),
    ).subscribe(res => {
      this.externosLoading.set(false);
      if (!res) return;
      // Filter to only TAKE_AWAY and DELIVERY — the endpoint may return DINE_IN too
      const external = (res.content ?? []).filter(
        o => o.type === 'TAKE_AWAY' || o.type === 'DELIVERY'
      );
      this.externosOrders.set(external);
    });
  }

  /** Forces an immediate refresh of the Externos list. */
  refreshExternos(): void {
    this.externosLoading.set(true);
    this.http.get<{ content: OrderResponse[] }>(
      `/api/staff/orders?status=${EXTERNOS_STATUSES}&page=0&size=50&sort=createdAt,asc`
    ).subscribe({
      next: res => {
        const external = (res.content ?? []).filter(
          o => o.type === 'TAKE_AWAY' || o.type === 'DELIVERY'
        );
        this.externosOrders.set(external);
        this.externosLoading.set(false);
      },
      error: () => this.externosLoading.set(false),
    });
  }

  /**
   * Returns the next logical status for an external order, or null if terminal.
   */
  externosNextStatus(status: string): string | null {
    const map: Record<string, string> = {
      PENDING:        'IN_PREPARATION',
      IN_PREPARATION: 'READY',
      READY:          'SERVED',
      SERVED:         'FINISHED',
    };
    return map[status] ?? null;
  }

  /**
   * Returns the Spanish label for the next action button.
   */
  externosNextLabel(status: string): string {
    const map: Record<string, string> = {
      PENDING:        'En preparación',
      IN_PREPARATION: 'Listo',
      READY:          'Servido',
      SERVED:         'Finalizar',
    };
    return map[status] ?? '';
  }

  /**
   * Advances an external order to its next status.
   * Uses optimistic update — the card reflects the new status immediately.
   *
   * @param order the order to advance
   */
  externosAdvance(order: OrderResponse): void {
    const next = this.externosNextStatus(order.status);
    if (!next) return;

    // Mark as in-flight
    this.externosChanging.update(m => ({ ...m, [order.id]: true }));
    this.externosErrors.update(m => { const c = { ...m }; delete c[order.id]; return c; });

    // Optimistic update
    this.externosOrders.update(orders =>
      orders.map(o => o.id === order.id ? { ...o, status: next as any } : o)
    );

    this.http.patch(`/api/staff/orders/${order.id}/status`, { newStatus: next, message: null })
      .subscribe({
        next: () => {
          this.externosChanging.update(m => { const c = { ...m }; delete c[order.id]; return c; });
          // Remove FINISHED orders from the list
          if (next === 'FINISHED') {
            this.externosOrders.update(orders => orders.filter(o => o.id !== order.id));
          }
        },
        error: err => {
          // Roll back optimistic update
          this.externosOrders.update(orders =>
            orders.map(o => o.id === order.id ? { ...o, status: order.status } : o)
          );
          this.externosChanging.update(m => { const c = { ...m }; delete c[order.id]; return c; });
          const msg = this.extractErrorMessage(err) ?? 'Error al cambiar el estado.';
          this.externosErrors.update(m => ({ ...m, [order.id]: msg }));
        },
      });
  }

  /**
   * Collects cash for a SERVED external order, then finalizes it.
   * Calls confirm-manual first, then advances to FINISHED in sequence.
   *
   * @param order the SERVED order to collect payment for
   */
  externosCashCollect(order: OrderResponse): void {
    this.externosChanging.update(m => ({ ...m, [order.id]: true }));
    this.externosErrors.update(m => { const c = { ...m }; delete c[order.id]; return c; });

    this.http.post(`/api/staff/payments/orders/${order.id}/confirm-manual`,
      { manualReference: 'Efectivo' }
    ).subscribe({
      next: () => {
        // Payment confirmed — now advance to FINISHED
        this.http.patch(`/api/staff/orders/${order.id}/status`,
          { newStatus: 'FINISHED', message: null }
        ).subscribe({
          next: () => {
            this.externosOrders.update(orders => orders.filter(o => o.id !== order.id));
            this.externosChanging.update(m => { const c = { ...m }; delete c[order.id]; return c; });
          },
          error: err => {
            this.externosChanging.update(m => { const c = { ...m }; delete c[order.id]; return c; });
            this.externosErrors.update(m => ({ ...m, [order.id]: this.extractErrorMessage(err) ?? 'Error al finalizar.' }));
          },
        });
      },
      error: err => {
        this.externosChanging.update(m => { const c = { ...m }; delete c[order.id]; return c; });
        this.externosErrors.update(m => ({ ...m, [order.id]: this.extractErrorMessage(err) ?? 'Error al cobrar.' }));
      },
    });
  }

  /**
   * Returns true if an order is SERVED and has a cash (MANUAL) payment pending.
   * Used to show the "Cobrar en efectivo" button instead of the normal Finalizar.
   */
  externosNeedsCashCollection(order: OrderResponse): boolean {
    return order.status === 'SERVED';
  }

  externosStatusLabel(status: string): string {
    const map: Record<string, string> = {
      PENDING:        'Pendiente',
      IN_PREPARATION: 'En preparación',
      READY:          'Listo',
      SERVED:         'Servido',
      FINISHED:       'Finalizado',
      CANCELLED:      'Cancelado',
    };
    return map[status] ?? status;
  }

  externosStatusClass(status: string): string {
    const map: Record<string, string> = {
      PENDING:        'gc-chip-pending',
      IN_PREPARATION: 'gc-chip-preparation',
      READY:          'gc-chip-ready',
      SERVED:         'gc-chip-served',
    };
    return map[status] ?? '';
  }

  externosDisplayName(order: OrderResponse): string {
    if (order.type === 'TAKE_AWAY') return order.pickup?.name ?? '—';
    if (order.type === 'DELIVERY')  return order.delivery?.name ?? '—';
    return '—';
  }

  externosIsChanging(orderId: number): boolean {
    return !!this.externosChanging()[orderId];
  }

  externosError(orderId: number): string | null {
    return this.externosErrors()[orderId] ?? null;
  }

  trackExternos = (_: number, o: OrderResponse) => o.id;

  // ── Existing table / order methods (unchanged) ───────────────────────────

  trackTable   = (_: number, t: DiningTableResponse) => t.id;
  trackProduct = (_: number, p: ProductResponse) => p.id;
  trackItem    = (_: number, i: any) => i.id;
  trackCategory = (_: number, c: CategoryVm) => String(c.id);
  trackNote(_: number, note: { id: number }): number { return note.id; }

  onTableQueryChange(v: string) {
    this.tableQueryValue = v ?? '';
    this.tableQuery.set(this.tableQueryValue);
    this.refreshTables();
  }

  onProductQueryChange(v: string) {
    this.productQueryValue = v ?? '';
    this.productQuery.set(this.productQueryValue);
  }

  refreshTables() {
    this.loadingTables.set(true);
    this.tablesApi.list({ q: this.tableQuery().trim() || undefined, size: 50, includeActiveOrder: true, sort: 'id,asc' })
      .subscribe({
        next: (res) => this.tables.set(res.content ?? []),
        error: () => this.tables.set([]),
        complete: () => this.loadingTables.set(false),
      });
  }

  selectTable(t: DiningTableResponse) {
    this.currentTable.set(t);
    this.orderError.set(null);
    this.order.set(null);
    const activeOrderId = t.activeOrder?.orderId;
    if (activeOrderId) { this.loadOrder(activeOrderId); return; }
  }

  loadOrder(orderId: number) {
    this.loadingOrder.set(true);
    this.ordersApi.getById(orderId).subscribe({
      next: (o) => this.order.set(o),
      error: () => this.orderError.set('No se pudo cargar el pedido.'),
      complete: () => this.loadingOrder.set(false),
    });
  }

  addProduct(p: ProductResponse) {
    const table = this.currentTable();
    if (!table) return;
    const o = this.order();
    if (!o) { this.openDraftThenAdd(table.id, p.id); return; }
    if (!this.canEdit()) return;
    this.ordersApi.addItem(o.id, { productId: p.id, quantity: 1 }).subscribe({
      next: (updated) => { this.order.set(updated); this.refreshTables(); },
      error: (err) => this.orderError.set(this.extractErrorMessage(err) ?? 'No se pudo añadir el producto.'),
    });
  }

  private openDraftThenAdd(tableId: number, productId: number) {
    this.loadingOrder.set(true);
    this.orderError.set(null);
    this.ordersApi.createDraft({ type: 'DINE_IN', tableId }).subscribe({
      next: (draft) => {
        this.order.set(draft);
        this.ordersApi.addItem(draft.id, { productId, quantity: 1 }).subscribe({
          next: (updated) => { this.order.set(updated); this.refreshTables(); },
          error: (err) => this.orderError.set(this.extractErrorMessage(err) ?? 'No se pudo añadir el producto.'),
          complete: () => this.loadingOrder.set(false),
        });
      },
      error: (err) => { this.orderError.set(this.extractErrorMessage(err) ?? 'No se pudo abrir el ticket.'); this.loadingOrder.set(false); },
    });
  }

  cancelOrder() {
    const o = this.order();
    if (!o) return;
    this.ordersApi.cancel(o.id).subscribe({
      next: () => { this.order.set(null); this.refreshTables(); },
      error: (err) => this.orderError.set(this.extractErrorMessage(err) ?? 'No se pudo cancelar el pedido.'),
    });
  }

  inc(itemId: number) {
    const o = this.order(); if (!o) return;
    const item = o.items.find((i) => i.id === itemId); if (!item) return;
    this.setQty(itemId, item.quantity + 1);
  }

  dec(itemId: number) {
    const o = this.order(); if (!o) return;
    const item = o.items.find((i) => i.id === itemId); if (!item) return;
    const nextQty = item.quantity - 1;
    if (nextQty <= 0) { this.remove(itemId); return; }
    this.setQty(itemId, nextQty);
  }

  setQty(itemId: number, quantity: number) {
    const o = this.order(); if (!o || !this.canEdit()) return;
    this.ordersApi.updateItemQuantity(o.id, itemId, { quantity }).subscribe({
      next: (updated) => this.order.set(updated),
      error: (err) => this.orderError.set(this.extractErrorMessage(err) ?? 'No se pudo actualizar la cantidad.'),
    });
  }

  remove(itemId: number) {
    const o = this.order(); if (!o || !this.canEdit()) return;
    this.ordersApi.removeItem(o.id, itemId).subscribe({
      next: (updated) => this.order.set(updated),
      error: (err) => this.orderError.set(this.extractErrorMessage(err) ?? 'No se pudo eliminar la línea.'),
    });
  }

  submitToKitchen() {
    const o = this.order(); if (!o) return;
    this.ordersApi.submit(o.id).subscribe({
      next: () => { this.loadOrder(o.id); this.refreshTables(); },
      error: (err) => this.orderError.set(this.extractErrorMessage(err) ?? 'No se pudo enviar a cocina.'),
    });
  }

  addNote(orderId: number): void {
    const text = this.newNoteValue.trim(); if (!text) return;
    this.addingNote.set(true);
    this.ordersApi.addNote(orderId, { note: text }).subscribe({
      next: (updated) => { this.order.set(updated); this.newNoteValue = ''; this.addingNote.set(false); },
      error: (err) => { this.orderError.set(this.extractErrorMessage(err) ?? 'No se pudo guardar la nota.'); this.addingNote.set(false); },
    });
  }

  startEditNote(noteId: number, current: string): void {
    this.editingNoteId.set(noteId); this.editingNoteValue = current;
    this.confirmDeleteNoteId.set(null);
  }

  cancelEditNote(): void { this.editingNoteId.set(null); this.editingNoteValue = ''; }

  saveEditNote(orderId: number, noteId: number): void {
    const text = this.editingNoteValue.trim(); if (!text) return;
    const previousOrder = this.order(); if (!previousOrder) return;
    this.order.update(o => o ? { ...o, notes: o.notes.map(n => n.id === noteId ? { ...n, note: text } : n) } : null);
    this.editingNoteId.set(null); this.editingNoteValue = ''; this.savingNote.set(true);
    this.ordersApi.updateNote(orderId, noteId, { note: text }).subscribe({
      next: (updated) => { this.order.set(updated); this.savingNote.set(false); },
      error: (err) => { this.order.set(previousOrder); this.savingNote.set(false); this.orderError.set(this.extractErrorMessage(err) ?? 'No se pudo guardar la nota.'); },
    });
  }

  requestDeleteNote(orderId: number, noteId: number): void {
    this.editingNoteId.set(null);
    if (this.confirmDeleteNoteId() === noteId) { this.executeDeleteNote(orderId, noteId); }
    else { this.confirmDeleteNoteId.set(noteId); }
  }

  cancelDeleteNote(): void { this.confirmDeleteNoteId.set(null); }

  private executeDeleteNote(orderId: number, noteId: number): void {
    const previousOrder = this.order(); if (!previousOrder) return;
    this.order.update(o => o ? { ...o, notes: o.notes.filter(n => n.id !== noteId) } : null);
    this.confirmDeleteNoteId.set(null); this.deletingNote.set(true);
    this.ordersApi.deleteNote(orderId, noteId).subscribe({
      next: (updated) => { this.order.set(updated); this.deletingNote.set(false); },
      error: (err) => { this.order.set(previousOrder); this.deletingNote.set(false); this.orderError.set(this.extractErrorMessage(err) ?? 'No se pudo eliminar la nota.'); },
    });
  }

  private refreshProducts(opts: { query: string; categoryId: number | null }) {
    const q = (opts.query ?? '').trim();
    this.loadingProducts.set(true);
    this.productsApi.list({ active: true, categoryId: opts.categoryId ?? undefined, q: q.length ? q : undefined, size: 50, sort: 'name,asc' })
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
      next: (cats) => this.categories.set([{ id: null, name: 'Todos' }, ...cats.map(c => ({ id: c.id, name: c.name }))]),
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
      next: (ps) => this.products.set(ps.map(p => ({ id: p.id, name: p.name, priceCents: p.priceCents, categoryName: p.categoryName ?? '' })) as any),
      error: () => this.products.set([]),
      complete: () => this.loadingProducts.set(false),
    });
  }

  openPaymentModal(): void {
    if (!this.isServed()) return;
    this.paymentError.set(null);
    this.showPaymentModal.set(true);
  }

  onPaymentConfirmed(event: PaymentConfirmedEvent): void {
    const o = this.order(); if (!o) return;
    const reference = event.reference ? `${event.method} · ${event.reference}` : event.method;
    this.paymentLoading.set(true); this.paymentError.set(null);
    this.paymentApi.confirmAndFinalize(o.id, reference).subscribe({
      next: (finished) => { this.order.set(finished); this.paymentLoading.set(false); this.showPaymentModal.set(false); this.refreshTables(); },
      error: (err) => { this.paymentError.set(this.extractErrorMessage(err) ?? 'No se pudo procesar el pago.'); this.paymentLoading.set(false); },
    });
  }
}
