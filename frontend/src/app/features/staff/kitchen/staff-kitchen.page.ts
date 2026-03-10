// src/app/features/staff/kitchen/staff-kitchen.page.ts
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnDestroy,
  OnInit,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';

import { StaffKitchenApi } from '@app/app/core/api/staff/staff-kitchen.api';
import { ToastService } from '@app/app/core/ui/toast/toast.service';
import {
  ADVANCE_LABEL,
  KitchenOrderResponse,
  KitchenStatus,
  NEXT_STATUS,
  STATUS_LABEL,
} from '@app/app/core/api/staff/kitchen.models';

/** Statuses rendered as columns on the board, in left-to-right pipeline order. */
const KITCHEN_COLUMNS: KitchenStatus[] = ['PENDING', 'IN_PREPARATION', 'READY'];

/**
 * Kitchen Display System (KDS) page.
 *
 * <p>Renders a three-column Kanban board showing all active kitchen orders:
 * <em>Pendiente → En preparación → Listo</em>. Staff advance orders by tapping
 * the action button on each card. The board auto-refreshes every 15 seconds so
 * it can be left on a mounted screen without manual intervention.</p>
 *
 * <p>Uses Angular signals with {@code OnPush} change detection for minimal
 * re-rendering overhead on a frequently-polled view.</p>
 */
@Component({
  standalone: true,
  selector: 'gc-staff-kitchen-page',
  imports: [CommonModule],
  templateUrl: './staff-kitchen.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StaffKitchenPage implements OnInit, OnDestroy {
  private readonly kitchenApi = inject(StaffKitchenApi);
  private readonly toast = inject(ToastService);

  // ── State ────────────────────────────────────────────────────────────────────

  /** All active kitchen orders fetched from the backend. */
  readonly orders = signal<KitchenOrderResponse[]>([]);

  /** Whether the initial load is in progress. */
  readonly loading = signal(true);

  /** ID of an order whose status advance is currently in-flight. */
  readonly advancingId = signal<number | null>(null);

  /** Timestamp of the last successful refresh (for the "last updated" indicator). */
  readonly lastRefreshed = signal<Date | null>(null);

  // ── Computed column slices ────────────────────────────────────────────────

  /** Orders in the PENDING column (submitted, not yet started). */
  readonly pendingOrders = computed(() =>
    this.orders().filter((o) => o.status === 'PENDING')
  );

  /** Orders in the IN_PREPARATION column (kitchen is working on them). */
  readonly inPreparationOrders = computed(() =>
    this.orders().filter((o) => o.status === 'IN_PREPARATION')
  );

  /** Orders in the READY column (plated, awaiting collection by service staff). */
  readonly readyOrders = computed(() =>
    this.orders().filter((o) => o.status === 'READY')
  );

  // ── Static lookup maps exposed to the template ───────────────────────────

  /** Column definitions in pipeline order. */
  readonly columns = KITCHEN_COLUMNS;

  /** Human-readable column header labels. */
  readonly statusLabel = STATUS_LABEL;

  /** CTA label for the advance button on each card. */
  readonly advanceLabel = ADVANCE_LABEL;

  // ── Auto-poll ────────────────────────────────────────────────────────────

  private readonly pollIntervalMs = 15_000;
  private pollHandle: ReturnType<typeof setInterval> | null = null;

  // ── Lifecycle ────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.refresh();
    this.startPolling();
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  // ── Public actions ───────────────────────────────────────────────────────

  /**
   * Manually refreshes the board. Also resets the auto-poll timer so that the
   * next automatic refresh happens 15 s after the manual one, not sooner.
   */
  refresh(): void {
    this.loading.set(true);
    this.fetchOrders();
    this.resetPolling();
  }

  /**
   * Advances an order to its next pipeline status.
   *
   * <p>Optimistically removes the card from the current column so the UI feels
   * instant, then updates it with the real server response. On error the card
   * is restored and a toast is shown.</p>
   *
   * @param order      the order card the user acted on
   * @param fromStatus the column the card currently lives in
   */
  advance(order: KitchenOrderResponse, fromStatus: KitchenStatus): void {
    if (this.advancingId() !== null) return; // debounce: only one in-flight at a time

    const nextStatus = NEXT_STATUS[fromStatus];
    this.advancingId.set(order.id);

    // Optimistic update: move the card forward immediately
    this.orders.update((all) =>
      all.map((o) => (o.id === order.id ? { ...o, status: nextStatus } : o))
    );

    this.kitchenApi.advanceStatus(order.id, nextStatus).subscribe({
      next: (result) => {
        // Reconcile with server truth (status may differ if another client acted)
        this.orders.update((all) =>
          all
            .map((o) => (o.id === result.orderId ? { ...o, status: result.newStatus } : o))
            // Remove from board once SERVED — it's no longer a kitchen concern
            .filter((o) => !['SERVED', 'FINISHED', 'CANCELLED'].includes(o.status as string))
        );
        this.advancingId.set(null);
        this.toast.success(`Pedido #${order.id} → ${result.newStatus}`);
      },
      error: (err) => {
        // Roll back the optimistic update
        this.orders.update((all) =>
          all.map((o) => (o.id === order.id ? { ...o, status: fromStatus } : o))
        );
        this.advancingId.set(null);
        const msg = this.extractErrorMessage(err) ?? 'No se pudo cambiar el estado.';
        this.toast.error(msg);
      },
    });
  }

  /**
   * Returns the orders for a given column status.
   * Used in the template to avoid repeating the computed signal pattern.
   *
   * @param status the column status to retrieve
   */
  ordersFor(status: KitchenStatus): KitchenOrderResponse[] {
    switch (status) {
      case 'PENDING':        return this.pendingOrders();
      case 'IN_PREPARATION': return this.inPreparationOrders();
      case 'READY':          return this.readyOrders();
    }
  }

  /**
   * Formats an ISO-8601 timestamp into a human-friendly elapsed time string.
   * e.g. "hace 3 min" or "hace 1 h 5 min".
   *
   * @param isoString ISO-8601 date string from the backend
   */
  elapsed(isoString: string): string {
    const diffMs = Date.now() - new Date(isoString).getTime();
    const totalMinutes = Math.floor(diffMs / 60_000);

    if (totalMinutes < 1) return 'hace un momento';
    if (totalMinutes < 60) return `hace ${totalMinutes} min`;

    const hours = Math.floor(totalMinutes / 60);
    const mins = totalMinutes % 60;
    return mins > 0 ? `hace ${hours} h ${mins} min` : `hace ${hours} h`;
  }

  /**
   * Returns a CSS urgency class based on how long an order has been waiting.
   * Used to visually flag tickets that are taking too long.
   *
   * @param isoString ISO-8601 date string
   * @param status    current order status (READY orders don't age-warn)
   */
  urgencyClass(isoString: string, status: KitchenStatus): string {
    if (status === 'READY') return ''; // ready is fine — it's waiting for service

    const minutes = Math.floor((Date.now() - new Date(isoString).getTime()) / 60_000);
    if (minutes >= 20) return 'kds-urgent';
    if (minutes >= 12) return 'kds-warning';
    return '';
  }

  /**
   * TrackBy function for order lists — prevents full DOM re-render on poll.
   *
   * @param _ index (unused)
   * @param order the order item
   */
  trackOrder(_: number, order: KitchenOrderResponse): number {
    return order.id;
  }

  /**
   * TrackBy function for order item lines.
   *
   * @param index item index
   */
  trackItem(index: number): number {
    return index;
  }

  // ── Private helpers ───────────────────────────────────────────────────────

  private fetchOrders(): void {
    this.kitchenApi.listKitchenOrders().subscribe({
      next: (page) => {
        this.orders.set(page.content ?? []);
        this.lastRefreshed.set(new Date());
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.toast.error('No se pudo cargar la cocina. Reintentando...');
      },
    });
  }

  private startPolling(): void {
    this.pollHandle = setInterval(() => this.fetchOrders(), this.pollIntervalMs);
  }

  private stopPolling(): void {
    if (this.pollHandle !== null) {
      clearInterval(this.pollHandle);
      this.pollHandle = null;
    }
  }

  private resetPolling(): void {
    this.stopPolling();
    this.startPolling();
  }

  private extractErrorMessage(err: unknown): string | null {
    const e = err as any;
    const details = e?.error?.error?.details;
    if (details && typeof details === 'object') {
      const first = Object.keys(details)[0];
      if (first) return String(details[first]);
    }
    return e?.error?.message ?? e?.message ?? null;
  }
}
