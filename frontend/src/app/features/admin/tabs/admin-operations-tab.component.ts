// src/app/features/admin/tabs/admin-operations-tab.component.ts
import {
  ChangeDetectionStrategy, Component, inject, OnInit, signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { StaffOrdersListApi, ListOrdersParams } from '@app/app/core/api/staff/staff-orders-list.api';
import { OrderResponse, OrderStatus } from '@app/app/core/api/staff/staff.models';
import { environment } from '@app/environment/environment';

type ModalType = 'reopen' | 'forceCancel' | 'overrideStatus' | 'processAdjustment' | 'refund' | null;

interface AdjustmentResult {
  paidAmountCents: number;
  newTotalCents: number;
  deltaCents: number;
  adjustmentType: string;
  providerReference: string | null;
}

interface RefundResult {
  orderId: number;
  originalTotal: number;
  refundedCents: number;
  reason: string;
  manualReference: string;
}

const REOPEN_REASONS = [
  { value: 'CORRECTION',       label: 'Corrección' },
  { value: 'CUSTOMER_REQUEST', label: 'Solicitud del cliente' },
  { value: 'KITCHEN_ERROR',    label: 'Error de cocina' },
] as const;

const STATUS_OPTIONS: OrderStatus[] = [
  'PENDING', 'IN_PREPARATION', 'READY', 'SERVED', 'FINISHED', 'CANCELLED',
];

const STATUS_LABELS: Record<string, string> = {
  PENDING:        'Pendiente',
  IN_PREPARATION: 'En preparación',
  READY:          'Listo',
  SERVED:         'Servido',
  FINISHED:       'Finalizado',
  CANCELLED:      'Cancelado',
  DRAFT:          'Borrador',
};

const TYPE_LABELS: Record<string, string> = {
  DINE_IN:  'Mesa',
  TAKE_AWAY: 'Recogida',
  DELIVERY:  'Delivery',
};

/**
 * Pestaña de Operaciones del panel de administración.
 *
 * Acciones disponibles para Manager y Admin:
 * - Órdenes reabiertas resaltadas con acción "Procesar ajuste"
 * - Reabrir orden (con ventana de 30 min y detección de conflicto de mesa)
 * - Cancelación forzada (cualquier estado no terminal)
 * - Cambio de estado libre (sin restricciones de estado)
 * - Reembolso directo (sin reabrir, solo para órdenes FINALIZADAS)
 */
@Component({
  selector: 'app-admin-operations-tab',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="ops-tab">

      <!-- Barra de herramientas -->
      <div class="toolbar">
        <div class="filters">
          <select class="gc-input filter-select" [(ngModel)]="filterStatus" (ngModelChange)="load()">
            <option value="">Todos los estados</option>
            <option value="DRAFT">Borrador</option>
            <option value="PENDING">Pendiente</option>
            <option value="IN_PREPARATION">En preparación</option>
            <option value="READY">Listo</option>
            <option value="SERVED">Servido</option>
            <option value="FINISHED">Finalizado</option>
            <option value="CANCELLED">Cancelado</option>
          </select>
          <select class="gc-input filter-select" [(ngModel)]="filterType" (ngModelChange)="load()">
            <option value="">Todos los tipos</option>
            <option value="DINE_IN">Mesa</option>
            <option value="TAKE_AWAY">Recogida</option>
            <option value="DELIVERY">Delivery</option>
          </select>
        </div>
        <button class="btn btn-outline btn-sm" (click)="load()">↻ Actualizar</button>
      </div>

      @if (error()) {
        <div class="error-banner">{{ error() }}</div>
      }

      <!-- Tabla de órdenes -->
      <div class="gc-card table-wrapper">
        <table class="ops-table">
          <thead>
            <tr>
              <th>#</th>
              <th>Tipo</th>
              <th>Mesa</th>
              <th>Estado</th>
              <th>Total</th>
              <th>Creado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            @if (loading()) {
              <tr><td colspan="7" class="empty-cell">Cargando…</td></tr>
            } @else if (orders().length === 0) {
              <tr><td colspan="7" class="empty-cell">No se encontraron órdenes.</td></tr>
            } @else {
              @for (order of orders(); track order.id) {
                <tr [class.reopened-row]="order.reopened">
                  <td class="cell-id">
                    #{{ order.id }}
                    @if (order.reopened) {
                      <span class="reopened-badge">⚠ Reabierto</span>
                    }
                  </td>
                  <td>
                    <span class="type-chip type-{{ order.type.toLowerCase() }}">
                      {{ typeLabel(order.type) }}
                    </span>
                  </td>
                  <td class="cell-muted">{{ order.tableId ?? '—' }}</td>
                  <td>
                    <span class="status-chip status-{{ order.status.toLowerCase() }}">
                      {{ statusLabel(order.status) }}
                    </span>
                  </td>
                  <td class="cell-price">{{ formatPrice(order.totalCents) }}</td>
                  <td class="cell-muted">{{ order.createdAt | date:'dd MMM HH:mm' }}</td>
                  <td class="actions-cell">
                    <div class="action-menu">
                      <!-- Procesar ajuste: solo para órdenes reabiertas -->
                      @if (order.reopened) {
                        <button class="btn-adjustment btn-sm" (click)="openProcessAdjustment(order)">
                          Procesar ajuste
                        </button>
                      }
                      <!-- Reabrir: solo FINALIZADO o CANCELADO (sin flag reabierto) -->
                      @if (!order.reopened && (order.status === 'FINISHED' || order.status === 'CANCELLED')) {
                        <button class="btn btn-outline btn-sm" (click)="openReopen(order)">Reabrir</button>
                      }
                      <!-- Reembolso directo: solo FINALIZADO sin flag reabierto -->
                      @if (!order.reopened && order.status === 'FINISHED') {
                        <button class="btn btn-outline btn-sm" (click)="openRefund(order)">Reembolso</button>
                      }
                      <!-- Cancelación forzada: cualquier estado activo -->
                      @if (!order.reopened && order.status !== 'FINISHED' && order.status !== 'CANCELLED') {
                        <button class="btn-danger-soft btn-sm" (click)="openForceCancel(order)">Cancelar</button>
                      }
                      <!-- Cambiar estado: siempre disponible -->
                      <button class="btn btn-outline btn-sm" (click)="openOverrideStatus(order)">Estado</button>
                    </div>
                  </td>
                </tr>
              }
            }
          </tbody>
        </table>
      </div>

      <!-- Paginación -->
      @if (totalPages() > 1) {
        <div class="pagination">
          <button class="btn btn-outline btn-sm"
                  [disabled]="currentPage() === 0"
                  (click)="goToPage(currentPage() - 1)">← Anterior</button>
          <span class="page-info">Página {{ currentPage() + 1 }} de {{ totalPages() }}</span>
          <button class="btn btn-outline btn-sm"
                  [disabled]="currentPage() >= totalPages() - 1"
                  (click)="goToPage(currentPage() + 1)">Siguiente →</button>
        </div>
      }
    </div>

    <!-- ── Modal: Procesar ajuste ─────────────────────────────────────────── -->
    @if (modalType() === 'processAdjustment') {
      <div class="modal-backdrop" (click)="closeModal()">
        <div class="modal gc-card" (click)="$event.stopPropagation()">
          @if (!adjustmentResult()) {
            <h2 class="modal-title">Procesar ajuste — Pedido #{{ selectedOrder()?.id }}</h2>
            <p class="modal-sub">
              El sistema calculará automáticamente la diferencia entre el importe
              pagado y el nuevo total tras las modificaciones.
            </p>
            <div class="adjustment-summary">
              <div class="adj-row">
                <span>Total original</span>
                <span class="adj-val">{{ formatPrice(selectedOrder()?.totalCents ?? 0) }}</span>
              </div>
              <div class="adj-row adj-note">
                <span>Nuevo total</span>
                <span class="adj-val muted">Se calculará al confirmar</span>
              </div>
            </div>
            <div class="form-field">
              <label>Referencia (opcional)</label>
              <input class="gc-input" type="text" [(ngModel)]="adjustmentReference"
                     placeholder="Ej. Efectivo devuelto al cliente, Terminal #A-4421…" />
            </div>
            @if (modalError()) { <div class="error-banner">{{ modalError() }}</div> }
            <div class="modal-actions">
              <button class="btn btn-outline" (click)="closeModal()">Cancelar</button>
              <button class="btn btn-primary" [disabled]="modalLoading()" (click)="submitProcessAdjustment()">
                {{ modalLoading() ? 'Procesando…' : 'Confirmar ajuste' }}
              </button>
            </div>
          } @else {
            <h2 class="modal-title">Ajuste procesado ✓</h2>
            <div class="adjustment-summary">
              <div class="adj-row">
                <span>Importe pagado</span>
                <span class="adj-val">{{ formatPrice(adjustmentResult()!.paidAmountCents) }}</span>
              </div>
              <div class="adj-row">
                <span>Nuevo total</span>
                <span class="adj-val">{{ formatPrice(adjustmentResult()!.newTotalCents) }}</span>
              </div>
              <div class="adj-row adj-delta"
                   [class.positive]="adjustmentResult()!.deltaCents > 0"
                   [class.negative]="adjustmentResult()!.deltaCents < 0"
                   [class.zero]="adjustmentResult()!.deltaCents === 0">
                <span>Diferencia</span>
                <span class="adj-val">
                  {{ adjustmentResult()!.deltaCents > 0 ? '+' : '' }}{{ formatPrice(adjustmentResult()!.deltaCents) }}
                  — {{ adjustmentResult()!.adjustmentType }}
                </span>
              </div>
            </div>
            <p class="modal-sub">
              El pedido ya no está bloqueado. El staff puede finalizar el cobro desde el TPV.
            </p>
            <div class="modal-actions">
              <button class="btn btn-primary" (click)="closeModal()">Cerrar</button>
            </div>
          }
        </div>
      </div>
    }

    <!-- ── Modal: Reembolso directo ───────────────────────────────────────── -->
    @if (modalType() === 'refund') {
      <div class="modal-backdrop" (click)="closeModal()">
        <div class="modal gc-card" (click)="$event.stopPropagation()">
          @if (!refundResult()) {
            <h2 class="modal-title">Reembolso — Pedido #{{ selectedOrder()?.id }}</h2>
            <p class="modal-sub">
              Reembolso sin reabrir el pedido. Útil para compensaciones,
              reclamaciones o errores de cobro. El pedido permanece finalizado.
            </p>
            <div class="adjustment-summary">
              <div class="adj-row">
                <span>Total del pedido</span>
                <span class="adj-val">{{ formatPrice(selectedOrder()?.totalCents ?? 0) }}</span>
              </div>
            </div>
            <div class="form-row">
              <div class="form-field">
                <label>Importe a reembolsar (€) *</label>
                <input class="gc-input" type="number" min="0.01" step="0.01"
                       [(ngModel)]="refundEuros"
                       placeholder="0.00" />
              </div>
            </div>
            <div class="form-field">
              <label>Motivo *</label>
              <input class="gc-input" type="text" [(ngModel)]="refundReason"
                     placeholder="Ej. Reclamación del cliente, plato en mal estado…" />
            </div>
            <div class="form-field">
              <label>Referencia (opcional)</label>
              <input class="gc-input" type="text" [(ngModel)]="refundReference"
                     placeholder="Ej. Efectivo devuelto, Terminal #B-1234…" />
            </div>
            @if (modalError()) { <div class="error-banner">{{ modalError() }}</div> }
            <div class="modal-actions">
              <button class="btn btn-outline" (click)="closeModal()">Cancelar</button>
              <button class="btn btn-danger"
                      [disabled]="modalLoading() || !refundEuros || !refundReason.trim()"
                      (click)="submitRefund()">
                {{ modalLoading() ? 'Procesando…' : 'Emitir reembolso' }}
              </button>
            </div>
          } @else {
            <h2 class="modal-title">Reembolso procesado ✓</h2>
            <div class="adjustment-summary">
              <div class="adj-row">
                <span>Total original</span>
                <span class="adj-val">{{ formatPrice(refundResult()!.originalTotal) }}</span>
              </div>
              <div class="adj-row adj-delta negative">
                <span>Reembolsado</span>
                <span class="adj-val">-{{ formatPrice(refundResult()!.refundedCents) }}</span>
              </div>
              <div class="adj-row adj-note">
                <span>Motivo</span>
                <span class="adj-val muted">{{ refundResult()!.reason }}</span>
              </div>
            </div>
            <p class="modal-sub">El reembolso ha sido registrado en el historial del pedido.</p>
            <div class="modal-actions">
              <button class="btn btn-primary" (click)="closeModal()">Cerrar</button>
            </div>
          }
        </div>
      </div>
    }

    <!-- ── Modal: Reabrir orden ───────────────────────────────────────────── -->
    @if (modalType() === 'reopen') {
      <div class="modal-backdrop" (click)="closeModal()">
        <div class="modal gc-card" (click)="$event.stopPropagation()">
          <h2 class="modal-title">Reabrir pedido #{{ selectedOrder()?.id }}</h2>
          <p class="modal-sub">
            El pedido volverá a estado <strong>PENDIENTE</strong> y se podrán modificar artículos.
            Solo es posible dentro de los <strong>30 minutos</strong> posteriores al cierre.
            @if (selectedOrder()?.tableId) {
              Si la mesa {{ selectedOrder()!.tableId }} está ocupada, se desvinculará automáticamente.
            }
          </p>
          <div class="form-field">
            <label>Motivo *</label>
            <select class="gc-input" [(ngModel)]="reopenReason">
              <option value="">Seleccionar motivo…</option>
              @for (r of reopenReasons; track r.value) {
                <option [value]="r.value">{{ r.label }}</option>
              }
            </select>
          </div>
          <div class="form-field">
            <label>Observaciones (opcional)</label>
            <input class="gc-input" type="text" [(ngModel)]="reopenMessage"
                   placeholder="Ej. Cliente solicita artículo adicional" />
          </div>
          @if (modalError()) { <div class="error-banner">{{ modalError() }}</div> }
          <div class="modal-actions">
            <button class="btn btn-outline" (click)="closeModal()">Cancelar</button>
            <button class="btn btn-primary" [disabled]="modalLoading() || !reopenReason"
                    (click)="submitReopen()">
              {{ modalLoading() ? 'Reabriendo…' : 'Reabrir pedido' }}
            </button>
          </div>
        </div>
      </div>
    }

    <!-- ── Modal: Cancelación forzada ────────────────────────────────────── -->
    @if (modalType() === 'forceCancel') {
      <div class="modal-backdrop" (click)="closeModal()">
        <div class="modal modal-sm gc-card" (click)="$event.stopPropagation()">
          <h2 class="modal-title">Cancelar pedido #{{ selectedOrder()?.id }}</h2>
          <p class="modal-sub">
            Se cancelará el pedido independientemente de su estado actual
            (<strong>{{ statusLabel(selectedOrder()?.status ?? '') }}</strong>).
          </p>
          <div class="form-field">
            <label>Motivo (opcional)</label>
            <input class="gc-input" type="text" [(ngModel)]="cancelMessage"
                   placeholder="Ej. Cliente se fue, error de cocina…" />
          </div>
          @if (modalError()) { <div class="error-banner">{{ modalError() }}</div> }
          <div class="modal-actions">
            <button class="btn btn-outline" (click)="closeModal()">Volver</button>
            <button class="btn btn-danger" [disabled]="modalLoading()" (click)="submitForceCancel()">
              {{ modalLoading() ? 'Cancelando…' : 'Cancelar pedido' }}
            </button>
          </div>
        </div>
      </div>
    }

    <!-- ── Modal: Cambiar estado ──────────────────────────────────────────── -->
    @if (modalType() === 'overrideStatus') {
      <div class="modal-backdrop" (click)="closeModal()">
        <div class="modal modal-sm gc-card" (click)="$event.stopPropagation()">
          <h2 class="modal-title">Cambiar estado — Pedido #{{ selectedOrder()?.id }}</h2>
          <p class="modal-sub">
            Estado actual: <strong>{{ statusLabel(selectedOrder()?.status ?? '') }}</strong>.
            Selecciona cualquier estado para aplicarlo directamente.
          </p>
          <div class="form-field">
            <label>Nuevo estado *</label>
            <select class="gc-input" [(ngModel)]="newStatus">
              <option value="">Seleccionar estado…</option>
              @for (s of statusOptions; track s) {
                <option [value]="s" [disabled]="s === selectedOrder()?.status">{{ statusLabel(s) }}</option>
              }
            </select>
          </div>
          <div class="form-field">
            <label>Motivo (opcional)</label>
            <input class="gc-input" type="text" [(ngModel)]="statusMessage"
                   placeholder="Motivo del cambio manual…" />
          </div>
          @if (modalError()) { <div class="error-banner">{{ modalError() }}</div> }
          <div class="modal-actions">
            <button class="btn btn-outline" (click)="closeModal()">Cancelar</button>
            <button class="btn btn-primary" [disabled]="modalLoading() || !newStatus"
                    (click)="submitOverrideStatus()">
              {{ modalLoading() ? 'Aplicando…' : 'Aplicar estado' }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .ops-tab { display: flex; flex-direction: column; gap: 1.25rem; }
    .toolbar { display: flex; align-items: center; justify-content: space-between; gap: 1rem; }
    .filters { display: flex; gap: 0.5rem; }
    .filter-select { width: 160px; }

    .error-banner {
      background: rgba(220,38,38,0.06); border: 1px solid rgba(220,38,38,0.2);
      color: #b91c1c; border-radius: 0.5rem; padding: 0.75rem 1rem; font-size: 0.875rem;
    }

    .table-wrapper { overflow-x: auto; padding: 0; }
    .ops-table {
      width: 100%; border-collapse: collapse; font-size: 0.875rem;
      th {
        background: var(--gc-brand-analogous, #1a4a37);
        color: rgba(255,255,255,0.85); font-weight: 600; font-size: 0.72rem;
        text-transform: uppercase; letter-spacing: 0.05em;
        padding: 0.65rem 1rem; text-align: left; white-space: nowrap;
      }
      td {
        padding: 0.75rem 1rem; border-top: 1px solid rgba(0,0,0,0.05);
        color: var(--gc-ink); vertical-align: middle;
      }
      tr:hover td { background: rgba(0,0,0,0.015); }
    }

    .reopened-row td { background: rgba(234,179,8,0.06) !important; border-top-color: rgba(234,179,8,0.2); }
    .reopened-row:hover td { background: rgba(234,179,8,0.1) !important; }
    .reopened-badge {
      display: inline-block; margin-left: 0.4rem;
      font-size: 0.65rem; font-weight: 700; color: #92400e;
      background: rgba(234,179,8,0.15); border: 1px solid rgba(234,179,8,0.3);
      padding: 0.1rem 0.4rem; border-radius: 4px; white-space: nowrap;
    }

    .cell-id    { font-family: monospace; font-size: 0.82rem; font-weight: 600; }
    .cell-muted { color: var(--gc-ink-muted); font-size: 0.82rem; }
    .cell-price { font-weight: 600; white-space: nowrap; }
    .empty-cell { text-align: center; color: var(--gc-ink-muted); padding: 2rem !important; }

    .type-chip {
      font-size: 0.68rem; font-weight: 700; letter-spacing: 0.06em;
      text-transform: uppercase; padding: 0.15rem 0.5rem; border-radius: 4px;
      &.type-dine_in   { background: rgba(15,47,36,0.08);  color: var(--gc-brand); }
      &.type-take_away { background: rgba(161,98,7,0.08);  color: #92400e; }
      &.type-delivery  { background: rgba(2,132,199,0.08); color: #0369a1; }
    }
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
    }

    .actions-cell { white-space: nowrap; }
    .action-menu { display: flex; gap: 0.35rem; flex-wrap: wrap; }
    .btn-adjustment {
      background: rgba(234,179,8,0.12); color: #92400e;
      border: 1px solid rgba(234,179,8,0.4); border-radius: 0.375rem;
      padding: 0.3rem 0.65rem; font-size: 0.75rem; font-weight: 600; cursor: pointer;
      transition: background 0.15s;
      &:hover { background: rgba(234,179,8,0.22); }
    }
    .btn-danger-soft {
      background: rgba(220,38,38,0.07); color: #b91c1c;
      border: 1px solid rgba(220,38,38,0.2); border-radius: 0.375rem;
      padding: 0.3rem 0.65rem; font-size: 0.75rem; font-weight: 500; cursor: pointer;
      transition: background 0.15s;
      &:hover:not(:disabled) { background: rgba(220,38,38,0.14); }
      &:disabled { opacity: 0.45; cursor: not-allowed; }
    }

    .pagination { display: flex; align-items: center; justify-content: center; gap: 1rem; }
    .page-info  { font-size: 0.875rem; color: var(--gc-ink-muted); }

    .modal-backdrop {
      position: fixed; inset: 0; background: rgba(0,0,0,0.4);
      display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 1rem;
    }
    .modal {
      width: 100%; max-width: 460px; padding: 1.75rem;
      display: flex; flex-direction: column; gap: 1rem;
      &.modal-sm { max-width: 380px; }
    }
    .modal-title { font-size: 1.1rem; font-weight: 700; color: var(--gc-ink); margin: 0; }
    .modal-sub   { font-size: 0.875rem; color: var(--gc-ink-muted); margin: 0; line-height: 1.5; }
    .form-field {
      display: flex; flex-direction: column; gap: 0.375rem;
      label { font-size: 0.8rem; color: var(--gc-ink-muted); font-weight: 500; }
    }
    .form-row { display: flex; gap: 0.75rem; }
    .modal-actions { display: flex; justify-content: flex-end; gap: 0.5rem; margin-top: 0.25rem; }

    .adjustment-summary {
      background: rgba(0,0,0,0.03); border: 1px solid rgba(0,0,0,0.07);
      border-radius: 8px; padding: 0.875rem 1rem;
      display: flex; flex-direction: column; gap: 0.5rem;
    }
    .adj-row {
      display: flex; justify-content: space-between; align-items: center;
      font-size: 0.875rem; color: var(--gc-ink);
    }
    .adj-val { font-weight: 600; }
    .adj-val.muted { color: var(--gc-ink-muted); font-weight: 400; font-style: italic; }
    .adj-note { border-top: 1px solid rgba(0,0,0,0.06); padding-top: 0.5rem; }
    .adj-delta {
      border-top: 1px solid rgba(0,0,0,0.06); padding-top: 0.5rem; font-weight: 600;
      &.positive { color: #b91c1c; }
      &.negative { color: #166534; }
      &.zero     { color: var(--gc-ink-muted); }
    }
  `],
})
export class AdminOperationsTabComponent implements OnInit {
  private readonly ordersApi = inject(StaffOrdersListApi);
  private readonly http      = inject(HttpClient);
  private readonly API       = environment.apiBase;

  protected readonly orders      = signal<OrderResponse[]>([]);
  protected readonly loading     = signal(false);
  protected readonly error       = signal<string | null>(null);
  protected readonly currentPage = signal(0);
  protected readonly totalPages  = signal(0);

  protected filterStatus = '';
  protected filterType   = '';

  protected readonly modalType        = signal<ModalType>(null);
  protected readonly modalLoading     = signal(false);
  protected readonly modalError       = signal<string | null>(null);
  protected readonly selectedOrder    = signal<OrderResponse | null>(null);
  protected readonly adjustmentResult = signal<AdjustmentResult | null>(null);
  protected readonly refundResult     = signal<RefundResult | null>(null);

  protected reopenReason        = '';
  protected reopenMessage       = '';
  protected cancelMessage       = '';
  protected newStatus           = '';
  protected statusMessage       = '';
  protected adjustmentReference = '';
  protected refundEuros: number | null = null;
  protected refundReason        = '';
  protected refundReference     = '';

  protected readonly reopenReasons = REOPEN_REASONS;
  protected readonly statusOptions = STATUS_OPTIONS;

  protected statusLabel(s: string): string { return STATUS_LABELS[s] ?? s; }
  protected typeLabel(t: string): string   { return TYPE_LABELS[t]   ?? t; }

  protected formatPrice(cents: number): string {
    return (cents / 100).toLocaleString('es-ES', { style: 'currency', currency: 'EUR' });
  }

  ngOnInit(): void { this.load(); }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    const params: ListOrdersParams = { page: this.currentPage(), size: 25, sort: 'createdAt,desc' };
    if (this.filterStatus) params.status = this.filterStatus;
    if (this.filterType)   params.type   = this.filterType as any;

    this.ordersApi.list(params).subscribe({
      next: page => { this.orders.set(page.content); this.totalPages.set(page.totalPages); this.loading.set(false); },
      error: () => { this.error.set('Error al cargar las órdenes.'); this.loading.set(false); },
    });
  }

  protected goToPage(page: number): void { this.currentPage.set(page); this.load(); }

  // ── Modal openers ─────────────────────────────────────────────────────────

  protected openProcessAdjustment(order: OrderResponse): void {
    this.selectedOrder.set(order); this.adjustmentReference = ''; this.adjustmentResult.set(null);
    this.openModal('processAdjustment');
  }
  protected openReopen(order: OrderResponse): void {
    this.selectedOrder.set(order); this.reopenReason = ''; this.reopenMessage = '';
    this.openModal('reopen');
  }
  protected openForceCancel(order: OrderResponse): void {
    this.selectedOrder.set(order); this.cancelMessage = '';
    this.openModal('forceCancel');
  }
  protected openOverrideStatus(order: OrderResponse): void {
    this.selectedOrder.set(order); this.newStatus = ''; this.statusMessage = '';
    this.openModal('overrideStatus');
  }
  protected openRefund(order: OrderResponse): void {
    this.selectedOrder.set(order);
    this.refundEuros = null; this.refundReason = ''; this.refundReference = '';
    this.refundResult.set(null);
    this.openModal('refund');
  }

  // ── Submissions ───────────────────────────────────────────────────────────

  protected submitProcessAdjustment(): void {
    const order = this.selectedOrder();
    if (!order) return;
    this.modalLoading.set(true); this.modalError.set(null);
    this.http.post<{ data: AdjustmentResult }>(
      `${this.API}/api/staff/orders/${order.id}/actions/process-adjustment`,
      { provider: 'MANUAL', manualReference: this.adjustmentReference.trim() || 'Ajuste procesado por manager' }
    ).subscribe({
      next: res => { this.adjustmentResult.set(res.data); this.modalLoading.set(false); this.load(); },
      error: err => this.handleModalError(err),
    });
  }

  protected submitRefund(): void {
    const order = this.selectedOrder();
    if (!order || !this.refundEuros || !this.refundReason.trim()) return;
    const amountCents = Math.round(this.refundEuros * 100);
    this.modalLoading.set(true); this.modalError.set(null);
    this.http.post<{ data: RefundResult }>(
      `${this.API}/api/manager/orders/${order.id}/actions/refund`,
      { amountCents, reason: this.refundReason.trim(), manualReference: this.refundReference.trim() || null }
    ).subscribe({
      next: res => { this.refundResult.set(res.data); this.modalLoading.set(false); },
      error: err => this.handleModalError(err),
    });
  }

  protected submitReopen(): void {
    const order = this.selectedOrder();
    if (!order || !this.reopenReason) return;
    this.modalLoading.set(true); this.modalError.set(null);
    this.http.post(`${this.API}/api/manager/orders/${order.id}/actions/reopen`,
      { reasonCode: this.reopenReason, message: this.reopenMessage || null }
    ).subscribe({
      next: () => { this.closeModal(); this.load(); },
      error: err => this.handleModalError(err),
    });
  }

  protected submitForceCancel(): void {
    const order = this.selectedOrder();
    if (!order) return;
    this.modalLoading.set(true); this.modalError.set(null);
    this.http.post(`${this.API}/api/manager/orders/${order.id}/actions/force-cancel`,
      { message: this.cancelMessage || null }
    ).subscribe({
      next: () => { this.closeModal(); this.load(); },
      error: err => this.handleModalError(err),
    });
  }

  protected submitOverrideStatus(): void {
    const order = this.selectedOrder();
    if (!order || !this.newStatus) return;
    this.modalLoading.set(true); this.modalError.set(null);
    this.http.patch(`${this.API}/api/manager/orders/${order.id}/status`,
      { newStatus: this.newStatus, message: this.statusMessage || null }
    ).subscribe({
      next: () => { this.closeModal(); this.load(); },
      error: err => this.handleModalError(err),
    });
  }

  protected closeModal(): void {
    this.modalType.set(null); this.modalLoading.set(false); this.modalError.set(null);
    this.selectedOrder.set(null); this.adjustmentResult.set(null); this.refundResult.set(null);
  }

  private openModal(type: ModalType): void {
    this.modalError.set(null); this.modalLoading.set(false);
    this.adjustmentResult.set(null); this.refundResult.set(null);
    this.modalType.set(type);
  }

  private handleModalError(err: unknown): void {
    this.modalLoading.set(false);
    const details = (err as any)?.error?.error?.details;
    if (details) { this.modalError.set(Object.values(details).join(' ')); return; }
    this.modalError.set((err as any)?.error?.error?.message ?? 'Ha ocurrido un error inesperado.');
  }
}
