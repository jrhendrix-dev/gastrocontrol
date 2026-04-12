// src/app/features/admin/tabs/admin-dashboard-tab.component.ts
import {
  ChangeDetectionStrategy, Component, inject, OnInit, signal, computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin } from 'rxjs';
import { StaffOrdersListApi } from '@app/app/core/api/staff/staff-orders-list.api';
import { OrderResponse } from '@app/app/core/api/staff/staff.models';

interface DashboardStats {
  todayRevenueCents: number;
  todayOrderCount: number;
  finishedTodayCount: number;
  cancelledTodayCount: number;
  openOrderCount: number;
  openTableCount: number;
  avgOrderValueCents: number;
}

/**
 * Manager/Admin Dashboard tab.
 *
 * Pulls live data from existing staff order endpoints:
 * - Today's FINISHED orders  → revenue, order count, avg value
 * - All orders today         → cancelled count
 * - Active orders            → open order + open table count
 *
 * No new backend endpoints required.
 */
@Component({
  selector: 'app-admin-dashboard-tab',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  template: `
    <div class="dashboard">

      @if (loading()) {
        <div class="loading-state">
          <div class="spinner"></div>
          <span>Cargando resumen…</span>
        </div>
      } @else if (error()) {
        <div class="error-banner">{{ error() }}</div>
      } @else {

        <!-- ── Stats grid ──────────────────────────────────────────── -->
        <div class="stats-grid">

          <div class="stat-card primary">
            <div class="stat-label">Ingresos hoy</div>
            <div class="stat-value">{{ formatPrice(stats()!.todayRevenueCents) }}</div>
            <div class="stat-sub">{{ stats()!.finishedTodayCount }} pedidos completados</div>
          </div>

          <div class="stat-card">
            <div class="stat-label">Pedidos hoy</div>
            <div class="stat-value">{{ stats()!.todayOrderCount }}</div>
            <div class="stat-sub">{{ stats()!.cancelledTodayCount }} cancelados</div>
          </div>

          <div class="stat-card">
            <div class="stat-label">Ticket medio</div>
            <div class="stat-value">
              {{ stats()!.finishedTodayCount > 0 ? formatPrice(stats()!.avgOrderValueCents) : '—' }}
            </div>
            <div class="stat-sub">de pedidos completados</div>
          </div>

          <div class="stat-card">
            <div class="stat-label">Pedidos activos</div>
            <div class="stat-value">{{ stats()!.openOrderCount }}</div>
            <div class="stat-sub">{{ stats()!.openTableCount }} mesas ocupadas</div>
          </div>

        </div>

        <!-- ── Active orders list ──────────────────────────────────── -->
        <div class="section">
          <h2 class="section-title">Pedidos activos</h2>

          @if (activeOrders().length === 0) {
            <div class="empty-state">No hay pedidos activos ahora mismo.</div>
          } @else {
            <div class="gc-card table-wrapper">
              <table class="orders-table">
                <thead>
                <tr>
                  <th>#</th>
                  <th>Tipo</th>
                  <th>Mesa</th>
                  <th>Estado</th>
                  <th>Total</th>
                  <th>Hora</th>
                </tr>
                </thead>
                <tbody>
                  @for (order of activeOrders(); track order.id) {
                    <tr>
                      <td class="cell-id">#{{ order.id }}</td>
                      <td>
                        <span class="type-chip type-{{ order.type.toLowerCase() }}">{{ typeLabel(order.type) }}</span>
                      </td>
                      <td class="cell-muted">{{ order.tableId ?? '—' }}</td>
                      <td>
                        <span class="status-chip status-{{ order.status.toLowerCase() }}">{{ statusLabel(order.status) }}</span>
                      </td>
                      <td class="cell-price">{{ formatPrice(order.totalCents) }}</td>
                      <td class="cell-muted">{{ order.createdAt | date:'HH:mm' }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </div>

        <!-- ── Today's breakdown ───────────────────────────────────── -->
        <div class="section">
          <h2 class="section-title">Pedidos finalizados hoy</h2>

          @if (todayFinished().length === 0) {
            <div class="empty-state">Aún no hay pedidos finalizados hoy.</div>
          } @else {
            <div class="gc-card table-wrapper">
              <table class="orders-table">
                <thead>
                <tr>
                  <th>#</th>
                  <th>Tipo</th>
                  <th>Mesa</th>
                  <th>Artículos</th>
                  <th>Total</th>
                  <th>Hora</th>
                </tr>
                </thead>
                <tbody>
                  @for (order of todayFinished(); track order.id) {
                    <tr>
                      <td class="cell-id">#{{ order.id }}</td>
                      <td>
                        <span class="type-chip type-{{ order.type.toLowerCase() }}">{{ typeLabel(order.type) }}</span>
                      </td>
                      <td class="cell-muted">{{ order.tableId ?? '—' }}</td>
                      <td class="cell-muted">{{ order.items.length }} artículos</td>
                      <td class="cell-price">{{ formatPrice(order.totalCents) }}</td>
                      <td class="cell-muted">{{ order.createdAt | date:'HH:mm' }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </div>

      }
    </div>
  `,
  styles: [`
    .dashboard { display: flex; flex-direction: column; gap: 2rem; }

    /* Loading */
    .loading-state {
      display: flex; align-items: center; gap: 0.75rem;
      padding: 3rem; color: var(--gc-ink-muted); font-size: 0.875rem;
    }
    .spinner {
      width: 20px; height: 20px;
      border: 2px solid rgba(0,0,0,0.1);
      border-top-color: var(--gc-brand);
      border-radius: 50%;
      animation: spin 0.7s linear infinite;
      flex-shrink: 0;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    .error-banner {
      background: rgba(220,38,38,0.06); border: 1px solid rgba(220,38,38,0.2);
      color: #b91c1c; border-radius: 0.5rem; padding: 0.75rem 1rem; font-size: 0.875rem;
    }

    /* Stats grid */
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 1rem;
    }
    .stat-card {
      background: white;
      border: 1px solid rgba(0,0,0,0.08);
      border-radius: 12px;
      padding: 1.25rem 1.5rem;
      display: flex; flex-direction: column; gap: 0.25rem;
      &.primary {
        background: var(--gc-brand);
        border-color: transparent;
        .stat-label { color: rgba(255,255,255,0.7); }
        .stat-value { color: #fff; }
        .stat-sub   { color: rgba(255,255,255,0.6); }
      }
    }
    .stat-label { font-size: 0.75rem; font-weight: 600; text-transform: uppercase; letter-spacing: 0.05em; color: var(--gc-ink-muted); }
    .stat-value { font-size: 1.75rem; font-weight: 700; color: var(--gc-ink); letter-spacing: -0.03em; line-height: 1.1; }
    .stat-sub   { font-size: 0.8rem; color: var(--gc-ink-muted); }

    /* Section */
    .section { display: flex; flex-direction: column; gap: 0.75rem; }
    .section-title { font-size: 1rem; font-weight: 600; color: var(--gc-ink); margin: 0; }
    .empty-state { color: var(--gc-ink-muted); font-size: 0.875rem; padding: 1.5rem; text-align: center; background: white; border: 1px solid rgba(0,0,0,0.06); border-radius: 10px; }

    /* Table */
    .table-wrapper { overflow-x: auto; padding: 0; }
    .orders-table {
      width: 100%; border-collapse: collapse; font-size: 0.875rem;
      th {
        background: var(--gc-brand-analogous, #1a4a37);
        color: rgba(255,255,255,0.85);
        font-weight: 600; font-size: 0.72rem;
        text-transform: uppercase; letter-spacing: 0.05em;
        padding: 0.65rem 1rem; text-align: left; white-space: nowrap;
      }
      td {
        padding: 0.75rem 1rem;
        border-top: 1px solid rgba(0,0,0,0.05);
        color: var(--gc-ink); vertical-align: middle;
      }
      tr:hover td { background: rgba(0,0,0,0.015); }
    }
    .cell-id    { font-family: monospace; font-size: 0.82rem; font-weight: 600; }
    .cell-muted { color: var(--gc-ink-muted); font-size: 0.82rem; }
    .cell-price { font-weight: 600; white-space: nowrap; }

    /* Type chips */
    .type-chip {
      font-size: 0.68rem; font-weight: 700; letter-spacing: 0.06em;
      text-transform: uppercase; padding: 0.15rem 0.5rem; border-radius: 4px;
      &.type-dine_in   { background: rgba(15,47,36,0.08);  color: var(--gc-brand); }
      &.type-take_away { background: rgba(161,98,7,0.08);  color: #92400e; }
      &.type-delivery  { background: rgba(2,132,199,0.08); color: #0369a1; }
    }

    /* Status chips */
    .status-chip {
      font-size: 0.68rem; font-weight: 700; letter-spacing: 0.06em;
      text-transform: uppercase; padding: 0.15rem 0.5rem; border-radius: 4px;
      background: rgba(0,0,0,0.05); color: var(--gc-ink-muted);
      &.status-pending        { background: rgba(234,179,8,0.12);  color: #92400e; }
      &.status-in_preparation { background: rgba(2,132,199,0.12);  color: #0369a1; }
      &.status-ready          { background: rgba(15,47,36,0.1);    color: var(--gc-brand); }
      &.status-served         { background: rgba(124,58,237,0.1);  color: #6d28d9; }
      &.status-finished       { background: rgba(34,197,94,0.1);   color: #166534; }
      &.status-cancelled      { background: rgba(220,38,38,0.1);   color: #b91c1c; }
      &.status-draft          { background: rgba(0,0,0,0.05);      color: var(--gc-ink-muted); }
    }

    /* Wherever the outer wrapper is padded – if there's a section padding, add: */
    @media (max-width: 640px) {
      .stats-grid { grid-template-columns: 1fr 1fr; gap: 0.75rem; }
    }
  `],
})
export class AdminDashboardTabComponent implements OnInit {
  private readonly ordersApi = inject(StaffOrdersListApi);

  protected readonly loading = signal(true);
  protected readonly error   = signal<string | null>(null);
  protected readonly stats   = signal<DashboardStats | null>(null);
  protected readonly activeOrders  = signal<OrderResponse[]>([]);
  protected readonly todayFinished = signal<OrderResponse[]>([]);

  ngOnInit(): void { this.load(); }

  private load(): void {
    this.loading.set(true);
    this.error.set(null);

    const todayStart = this.todayStartIso();
    const todayEnd   = this.todayEndIso();

    forkJoin({
      // All of today's orders (any status) — for total count and cancelled count
      todayAll: this.ordersApi.list({
        createdFrom: todayStart, createdTo: todayEnd,
        size: 200, sort: 'createdAt,desc',
      }),
      // Today's FINISHED orders — filtered by closedAt (when finalized),
      // NOT createdAt (when placed). This correctly captures orders placed
      // days ago but finalized today (e.g. via manager Operations tab).
      todayFinished: this.ordersApi.list({
        status: 'FINISHED',
        closedFrom: todayStart, closedTo: todayEnd,
        size: 200, sort: 'createdAt,desc',
      }),
      // Active orders — for open order + table count
      active: this.ordersApi.list({
        status: 'DRAFT,PENDING,IN_PREPARATION,READY,SERVED',
        size: 100, sort: 'createdAt,desc',
      }),
    }).subscribe({
      next: ({ todayAll, todayFinished, active }) => {
        const finished  = todayFinished.content;
        const allToday  = todayAll.content;
        const activeArr = active.content;

        const revenueCents = finished.reduce((sum, o) => sum + o.totalCents, 0);
        const cancelledCount = allToday.filter(o => o.status === 'CANCELLED').length;
        const openTableIds = new Set(activeArr.filter(o => o.tableId).map(o => o.tableId));

        this.stats.set({
          todayRevenueCents:  revenueCents,
          todayOrderCount:    allToday.length,
          finishedTodayCount: finished.length,
          cancelledTodayCount: cancelledCount,
          openOrderCount:     activeArr.length,
          openTableCount:     openTableIds.size,
          avgOrderValueCents: finished.length > 0
            ? Math.round(revenueCents / finished.length)
            : 0,
        });

        this.activeOrders.set(activeArr);
        this.todayFinished.set(finished);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al cargar el resumen. Inténtalo de nuevo.');
        this.loading.set(false);
      },
    });
  }

  protected formatPrice(cents: number): string {
    return (cents / 100).toLocaleString('es-ES', { style: 'currency', currency: 'EUR' });
  }

  protected typeLabel(t: string): string {
    const labels: Record<string, string> = {
      DINE_IN: 'Mesa', TAKE_AWAY: 'Recogida', DELIVERY: 'Delivery',
    };
    return labels[t] ?? t;
  }

  protected statusLabel(s: string): string {
    const labels: Record<string, string> = {
      DRAFT: 'Borrador', PENDING: 'Pendiente', IN_PREPARATION: 'En preparación',
      READY: 'Listo', SERVED: 'Servido', FINISHED: 'Finalizado', CANCELLED: 'Cancelado',
    };
    return labels[s] ?? s;
  }

  private todayStartIso(): string {
    const d = new Date();
    d.setHours(0, 0, 0, 0);
    return d.toISOString();
  }

  private todayEndIso(): string {
    const d = new Date();
    d.setHours(23, 59, 59, 999);
    return d.toISOString();
  }
}
