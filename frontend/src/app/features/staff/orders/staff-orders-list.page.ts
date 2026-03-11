// src/app/features/staff/orders/staff-orders-list.page.ts
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CurrencyPipe, DatePipe, NgClass, NgFor, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { StaffOrdersListApi } from '../../../core/api/staff/staff-orders-list.api';
import { OrderResponse, OrderStatus, OrderType } from '../../../core/api/staff/staff.models';
import { PagedResponse } from '../../../core/api/types/paged-response';

/** All statuses the filter UI can offer. */
const ALL_STATUSES: OrderStatus[] = [
  'DRAFT',
  'PENDING',
  'IN_PREPARATION',
  'READY',
  'SERVED',
  'FINISHED',
  'CANCELLED',
];

/** All order types the filter UI can offer. */
const ALL_TYPES: OrderType[] = ['DINE_IN', 'TAKE_AWAY', 'DELIVERY'];

/**
 * Staff Orders List page.
 *
 * <p>Provides a filterable, paginated view of all orders in the system.
 * Staff can filter by status, type and date range, then click any row to
 * expand its full detail (items + notes) inline.</p>
 *
 * <p>Uses Angular Signals and {@code OnPush} change detection throughout.</p>
 */
@Component({
  selector: 'gc-staff-orders-list-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgIf, NgFor, NgClass, CurrencyPipe, DatePipe, FormsModule],
  templateUrl: './staff-orders-list.page.html',
})
export class StaffOrdersListPage implements OnInit {
  private readonly api = inject(StaffOrdersListApi);

  // ── Exposed constants for template ──────────────────────────────────────
  readonly allStatuses = ALL_STATUSES;
  readonly allTypes    = ALL_TYPES;

  // ── Filter state ─────────────────────────────────────────────────────────
  /** Which statuses are currently selected (empty = all). */
  readonly selectedStatuses = signal<Set<OrderStatus>>(new Set());
  /** Selected order type, or null for all. */
  readonly selectedType = signal<OrderType | null>(null);
  /** ISO date string lower bound (from the date input, YYYY-MM-DD). */
  readonly dateFrom = signal<string>('');
  /** ISO date string upper bound. */
  readonly dateTo   = signal<string>('');

  // ── Pagination state ──────────────────────────────────────────────────────
  readonly currentPage  = signal(0);
  readonly pageSize     = 20;

  // ── Data state ────────────────────────────────────────────────────────────
  readonly loading  = signal(false);
  readonly error    = signal<string | null>(null);
  readonly result   = signal<PagedResponse<OrderResponse> | null>(null);

  /** The currently expanded order row (for the inline detail panel). */
  readonly expandedOrderId = signal<number | null>(null);

  // ── Derived ───────────────────────────────────────────────────────────────
  readonly orders     = computed(() => this.result()?.content ?? []);
  readonly totalPages = computed(() => this.result()?.totalPages ?? 0);
  readonly totalItems = computed(() => this.result()?.totalElements ?? 0);
  readonly isFirst    = computed(() => this.result()?.first ?? true);
  readonly isLast     = computed(() => this.result()?.last ?? true);

  /** The expanded order object (null when nothing is expanded). */
  readonly expandedOrder = computed<OrderResponse | null>(() => {
    const id = this.expandedOrderId();
    if (id === null) return null;
    return this.orders().find((o) => o.id === id) ?? null;
  });

  // ── Lifecycle ────────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.fetch();
  }

  // ── Public actions ────────────────────────────────────────────────────────

  /**
   * Toggles a status chip. Resets pagination to page 0 and re-fetches.
   *
   * @param status the status to toggle
   */
  toggleStatus(status: OrderStatus): void {
    const next = new Set(this.selectedStatuses());
    next.has(status) ? next.delete(status) : next.add(status);
    this.selectedStatuses.set(next);
    this.resetAndFetch();
  }

  /**
   * Selects a type filter. Passing {@code null} clears the type filter.
   *
   * @param type the order type to filter by, or null for all
   */
  selectType(type: OrderType | null): void {
    this.selectedType.set(type);
    this.resetAndFetch();
  }

  /** Called when either date input changes. */
  onDateChange(): void {
    this.resetAndFetch();
  }

  /** Clears all active filters and re-fetches. */
  clearFilters(): void {
    this.selectedStatuses.set(new Set());
    this.selectedType.set(null);
    this.dateFrom.set('');
    this.dateTo.set('');
    this.resetAndFetch();
  }

  /** Navigates to the previous page. */
  prevPage(): void {
    if (this.isFirst()) return;
    this.currentPage.update((p) => p - 1);
    this.fetch();
  }

  /** Navigates to the next page. */
  nextPage(): void {
    if (this.isLast()) return;
    this.currentPage.update((p) => p + 1);
    this.fetch();
  }

  /**
   * Expands or collapses the inline detail panel for an order row.
   *
   * @param orderId the order to toggle
   */
  toggleExpand(orderId: number): void {
    this.expandedOrderId.update((current) => (current === orderId ? null : orderId));
  }

  /** Returns true when any filter is active. Used to show the "Limpiar" button. */
  hasActiveFilters = computed(
    () =>
      this.selectedStatuses().size > 0 ||
      this.selectedType() !== null ||
      !!this.dateFrom() ||
      !!this.dateTo()
  );

  // ── Status / type display helpers ────────────────────────────────────────

  /** Returns a human-readable Spanish label for a given order status. */
  statusLabel(status: OrderStatus): string {
    const map: Record<string, string> = {
      DRAFT:          'Borrador',
      PENDING:        'Pendiente',
      IN_PREPARATION: 'En preparación',
      READY:          'Listo',
      SERVED:         'Servido',
      FINISHED:       'Finalizado',
      CANCELLED:      'Cancelado',
    };
    return map[status] ?? status;
  }

  /** Returns Tailwind CSS classes for the status badge. */
  statusClass(status: OrderStatus): string {
    const map: Record<string, string> = {
      DRAFT:          'bg-black/8 text-black/50',
      PENDING:        'bg-yellow-100 text-yellow-800',
      IN_PREPARATION: 'bg-blue-100 text-blue-800',
      READY:          'bg-green-100 text-green-800',
      SERVED:         'bg-teal-100 text-teal-700',
      FINISHED:       'bg-purple-100 text-purple-700',
      CANCELLED:      'bg-red-100 text-red-600',
    };
    return map[status] ?? 'bg-gray-100 text-gray-600';
  }

  /** Returns a human-readable Spanish label for a given order type. */
  typeLabel(type: OrderType): string {
    const map: Record<string, string> = {
      DINE_IN:  'Mesa',
      TAKE_AWAY: 'Recogida',
      DELIVERY: 'Delivery',
    };
    return map[type] ?? type;
  }

  /** Returns Tailwind CSS classes for the type badge. */
  typeClass(type: OrderType): string {
    const map: Record<string, string> = {
      DINE_IN:   'bg-stone-100 text-stone-700',
      TAKE_AWAY: 'bg-orange-100 text-orange-700',
      DELIVERY:  'bg-indigo-100 text-indigo-700',
    };
    return map[type] ?? 'bg-gray-100 text-gray-600';
  }

  /** Customer/table display name for a row. */
  displayName(order: OrderResponse): string {
    if (order.type === 'DINE_IN')   return order.tableId ? `Mesa ${order.tableId}` : '—';
    if (order.type === 'TAKE_AWAY') return order.pickup?.name ?? '—';
    if (order.type === 'DELIVERY')  return order.delivery?.name ?? '—';
    return '—';
  }

  // ── TrackBy helpers ───────────────────────────────────────────────────────
  trackOrder   = (_: number, o: OrderResponse) => o.id;
  trackStatus  = (_: number, s: string) => s;
  trackType    = (_: number, t: string) => t;
  trackItem    = (_: number, i: { id: number }) => i.id;
  trackNote    = (_: number, n: { id: number }) => n.id;
  /** Used for static number arrays such as the loading skeleton. */
  trackByIndex = (index: number) => index;

  // ── Private helpers ───────────────────────────────────────────────────────

  private resetAndFetch(): void {
    this.currentPage.set(0);
    this.expandedOrderId.set(null);
    this.fetch();
  }

  private fetch(): void {
    this.loading.set(true);
    this.error.set(null);

    const statuses   = [...this.selectedStatuses()];
    const statusParam = statuses.length > 0 ? statuses.join(',') : undefined;

    // Convert YYYY-MM-DD to ISO-8601 Instant strings the backend understands.
    const createdFrom = this.dateFrom() ? `${this.dateFrom()}T00:00:00Z` : undefined;
    const createdTo   = this.dateTo()   ? `${this.dateTo()}T23:59:59Z`   : undefined;

    this.api
      .list({
        status:      statusParam,
        type:        this.selectedType() ?? undefined,
        createdFrom,
        createdTo,
        page:        this.currentPage(),
        size:        this.pageSize,
        sort:        'createdAt,desc',
      })
      .subscribe({
        next:     (res) => this.result.set(res),
        error:    (err) => this.error.set(this.extractMessage(err)),
        complete: () => this.loading.set(false),
      });
  }

  private extractMessage(err: unknown): string {
    const e = err as Record<string, unknown>;
    const details = (e?.['error'] as Record<string, unknown>)?.['error'] as Record<string, unknown>;
    if (details?.['message']) return String(details['message']);
    return 'No se pudieron cargar los pedidos.';
  }
}
