// src/app/features/order/track.page.ts
import {
  ChangeDetectionStrategy, Component, computed, inject,
  OnDestroy, OnInit, signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { interval, Subscription, switchMap, startWith, catchError, of } from 'rxjs';

type OrderStatus = 'PENDING'|'IN_PREPARATION'|'READY'|'SERVED'|'FINISHED'|'CANCELLED';
type OrderType   = 'TAKE_AWAY'|'DELIVERY';

interface TrackingItem  { name: string; quantity: number; }
interface TrackingOrder {
  id: number; type: OrderType; status: OrderStatus; createdAt: string;
  estimatedMinutesRemaining: number|null;
  pickupName: string|null; deliveryAddressLine1: string|null; deliveryCity: string|null;
  items: TrackingItem[];
}

const POLL_MS = 5000;
const TERMINAL: OrderStatus[] = ['FINISHED','CANCELLED'];
const STATUS_ORDER: OrderStatus[] = ['PENDING','IN_PREPARATION','READY','SERVED','FINISHED'];
const STEPS = [
  { status: 'PENDING'        as OrderStatus, label: 'Pedido recibido',    icon: '📋' },
  { status: 'IN_PREPARATION' as OrderStatus, label: 'En preparación',     icon: '👨‍🍳' },
  { status: 'READY'          as OrderStatus, label: 'Listo para recoger', icon: '✅' },
  { status: 'FINISHED'       as OrderStatus, label: 'Entregado',          icon: '🎉' },
];

/**
 * Real-time order tracking page.
 * Polls GET /api/customer/orders/{id}/track every 5 s.
 * Stops automatically on terminal status (FINISHED or CANCELLED).
 */
@Component({
  selector: 'app-track-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="track-page">

      @if (loading()) {
        <div class="loading-state">
          <div class="pulse-ring"></div>
          <p>Buscando tu pedido…</p>
        </div>
      } @else if (error()) {
        <div class="error-state">
          <span>⚠️</span>
          <h2>Pedido no encontrado</h2>
          <p>{{ error() }}</p>
          <a routerLink="/menu" class="btn-primary">Volver al menú</a>
        </div>
      } @else if (order()) {
        <div class="track-layout">

          <div class="track-header">
            <div class="order-meta">
              <span class="order-num">Pedido #{{ order()!.id }}</span>
              <span class="order-type-badge">
                {{ order()!.type === 'TAKE_AWAY' ? '🥡 Recogida' : '🛵 Delivery' }}
              </span>
            </div>
            @if (!isTerminal()) {
              <div class="polling-indicator">
                <span class="dot"></span><span>Actualizando en tiempo real</span>
              </div>
            }
          </div>

          @if (order()!.status === 'CANCELLED') {
            <div class="cancelled-card">
              <span>❌</span>
              <div>
                <strong>Pedido cancelado</strong>
                <p>Si tienes dudas, contacta con el restaurante.</p>
              </div>
            </div>
          } @else {
            <div class="timeline-card">
              <div class="timeline">
                @for (step of steps; track step.status; let i = $index) {
                  <div class="timeline-step"
                       [class.completed]="isStepCompleted(step.status)"
                       [class.active]="isStepActive(step.status)">
                    <div class="step-indicator">
                      @if (isStepCompleted(step.status)) { <span class="step-check">✓</span> }
                      @else if (isStepActive(step.status)) { <span class="step-active-dot"></span> }
                      @else { <span class="step-num">{{ i + 1 }}</span> }
                    </div>
                    <div class="step-content">
                      <span class="step-icon">{{ step.icon }}</span>
                      <span class="step-label">{{ step.label }}</span>
                    </div>
                    @if (i < steps.length - 1) {
                      <div class="step-connector" [class.filled]="isStepCompleted(step.status)"></div>
                    }
                  </div>
                }
              </div>

              <div class="status-message" [class.ready]="order()!.status === 'READY'">
                <span class="status-icon">{{ currentStatusIcon() }}</span>
                <div>
                  <div class="status-text">{{ currentStatusText() }}</div>
                  @if (order()!.estimatedMinutesRemaining) {
                    <div class="eta">⏱ Tiempo estimado: {{ order()!.estimatedMinutesRemaining }} min</div>
                  }
                </div>
              </div>
            </div>

            @if (order()!.type === 'TAKE_AWAY' && order()!.pickupName) {
              <div class="info-card">
                <span class="info-icon">🥡</span>
                <div>
                  <div class="info-label">Recogida para</div>
                  <div class="info-value">{{ order()!.pickupName }}</div>
                </div>
              </div>
            }
            @if (order()!.type === 'DELIVERY' && order()!.deliveryAddressLine1) {
              <div class="info-card">
                <span class="info-icon">📍</span>
                <div>
                  <div class="info-label">Dirección de entrega</div>
                  <div class="info-value">
                    {{ order()!.deliveryAddressLine1 }}{{ order()!.deliveryCity ? ', ' + order()!.deliveryCity : '' }}
                  </div>
                </div>
              </div>
            }
          }

          <div class="items-card">
            <h3 class="items-title">Tu pedido</h3>
            <div class="items-list">
              @for (item of order()!.items; track item.name) {
                <div class="item-row">
                  <span class="item-qty">{{ item.quantity }}×</span>
                  <span class="item-name">{{ item.name }}</span>
                </div>
              }
            </div>
          </div>

          <a routerLink="/menu" class="btn-menu">Hacer otro pedido</a>
        </div>
      }
    </div>
  `,
  styles: [`
    .track-page {
      min-height: calc(100vh - 60px); background: #faf8f5;
      font-family: system-ui, sans-serif; padding: 2rem 1.5rem 4rem;
      display: flex; justify-content: center;
    }
    .loading-state {
      display: flex; flex-direction: column; align-items: center;
      justify-content: center; gap: 1.5rem; color: #888; font-size: 0.9rem;
    }
    .pulse-ring {
      width: 48px; height: 48px; border-radius: 50%;
      border: 3px solid rgba(26,46,26,0.15); border-top-color: #1a2e1a;
      animation: spin 0.8s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
    .error-state {
      display: flex; flex-direction: column; align-items: center;
      gap: 0.75rem; padding: 5rem 1.5rem; text-align: center;
      span { font-size: 2.5rem; }
      h2 { font-size: 1.25rem; font-weight: 700; color: #1a2e1a; margin: 0; font-family: 'Georgia', serif; }
      p  { color: #888; margin: 0; font-size: 0.875rem; }
    }
    .track-layout { width: 100%; max-width: 560px; display: flex; flex-direction: column; gap: 1rem; }
    .track-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 0.5rem; }
    .order-meta   { display: flex; align-items: center; gap: 0.75rem; }
    .order-num    { font-size: 1.25rem; font-weight: 700; color: #1a2e1a; font-family: 'Georgia', serif; }
    .order-type-badge {
      font-size: 0.775rem; font-weight: 600;
      background: rgba(26,46,26,0.07); color: #1a2e1a;
      padding: 0.2rem 0.625rem; border-radius: 999px;
    }
    .polling-indicator { display: flex; align-items: center; gap: 0.4rem; font-size: 0.75rem; color: #aaa; }
    .dot { width: 7px; height: 7px; border-radius: 50%; background: #22c55e; animation: blink 1.5s ease-in-out infinite; }
    @keyframes blink { 0%,100% { opacity:1; } 50% { opacity:0.3; } }

    .cancelled-card {
      background: rgba(220,38,38,0.05); border: 1px solid rgba(220,38,38,0.15);
      border-radius: 14px; padding: 1.25rem; display: flex; align-items: flex-start; gap: 1rem; font-size: 0.875rem;
      strong { color: #b91c1c; display: block; margin-bottom: 0.25rem; }
      p { color: #888; margin: 0; }
    }

    .timeline-card {
      background: white; border-radius: 16px; border: 1px solid rgba(0,0,0,0.07);
      padding: 1.5rem; display: flex; flex-direction: column; gap: 1.5rem;
    }
    .timeline { display: flex; align-items: flex-start; }
    .timeline-step { display: flex; flex-direction: column; align-items: center; flex: 1; position: relative; gap: 0.5rem; }
    .step-indicator {
      width: 36px; height: 36px; border-radius: 50%; display: grid; place-items: center;
      background: rgba(0,0,0,0.05); border: 2px solid rgba(0,0,0,0.1);
      font-size: 0.75rem; font-weight: 700; color: #bbb; transition: all 0.3s; z-index: 1;
    }
    .timeline-step.completed .step-indicator { background: #1a2e1a; border-color: #1a2e1a; color: white; }
    .timeline-step.active    .step-indicator { background: white; border-color: #c8a96e; border-width: 3px; box-shadow: 0 0 0 4px rgba(200,169,110,0.15); }
    .step-check { font-size: 0.875rem; }
    .step-active-dot { width: 10px; height: 10px; border-radius: 50%; background: #c8a96e; animation: blink 1.2s ease-in-out infinite; }
    .step-num { color: #ccc; }
    .step-content { display: flex; flex-direction: column; align-items: center; gap: 0.15rem; text-align: center; }
    .step-icon  { font-size: 1.1rem; }
    .step-label { font-size: 0.68rem; font-weight: 500; color: #bbb; line-height: 1.3; transition: color 0.3s; }
    .timeline-step.completed .step-label,
    .timeline-step.active    .step-label { color: #1a2e1a; font-weight: 600; }
    .step-connector {
      position: absolute; top: 17px; left: 50%; right: -50%;
      height: 2px; background: rgba(0,0,0,0.1); transition: background 0.3s; z-index: 0;
      &.filled { background: #1a2e1a; }
    }
    .status-message {
      display: flex; align-items: flex-start; gap: 1rem;
      background: rgba(26,46,26,0.04); border-radius: 10px;
      padding: 0.875rem 1rem; border: 1px solid rgba(26,46,26,0.08);
      &.ready { background: rgba(200,169,110,0.08); border-color: rgba(200,169,110,0.25); }
    }
    .status-icon { font-size: 1.5rem; flex-shrink: 0; }
    .status-text { font-size: 0.9rem; font-weight: 600; color: #1a2e1a; }
    .eta         { font-size: 0.8rem; color: #888; margin-top: 0.2rem; }

    .info-card {
      background: white; border-radius: 12px; border: 1px solid rgba(0,0,0,0.07);
      padding: 1rem 1.25rem; display: flex; align-items: center; gap: 1rem;
    }
    .info-icon  { font-size: 1.5rem; flex-shrink: 0; }
    .info-label { font-size: 0.75rem; color: #aaa; font-weight: 500; margin-bottom: 0.15rem; }
    .info-value { font-size: 0.9rem; font-weight: 600; color: #1a2e1a; }

    .items-card { background: white; border-radius: 12px; border: 1px solid rgba(0,0,0,0.07); padding: 1.25rem; }
    .items-title { font-size: 0.875rem; font-weight: 700; color: #1a2e1a; margin: 0 0 0.875rem; font-family: 'Georgia', serif; }
    .items-list  { display: flex; flex-direction: column; gap: 0.5rem; }
    .item-row    { display: flex; align-items: baseline; gap: 0.5rem; font-size: 0.875rem; }
    .item-qty    { color: #aaa; font-size: 0.8rem; width: 24px; flex-shrink: 0; }
    .item-name   { color: #333; }

    .btn-primary, .btn-menu {
      display: inline-block; padding: 0.75rem 1.5rem;
      background: #1a2e1a; color: #c8a96e; border-radius: 10px; text-decoration: none;
      font-weight: 700; font-size: 0.875rem; text-align: center; transition: background 0.15s;
      &:hover { background: #243d24; }
    }
  `],
})
export class TrackPage implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly http  = inject(HttpClient);

  protected readonly order   = signal<TrackingOrder | null>(null);
  protected readonly loading = signal(true);
  protected readonly error   = signal<string | null>(null);
  protected readonly steps   = STEPS;

  protected readonly isTerminal = computed(() => {
    const s = this.order()?.status;
    return s ? TERMINAL.includes(s) : false;
  });

  private pollSub?: Subscription;
  private orderId?: string;
  private errorCount = 0;
  private readonly MAX_ERRORS = 3;

  ngOnInit(): void {
    this.orderId = this.route.snapshot.paramMap.get('id') ?? undefined;
    if (!this.orderId) {
      this.error.set('No se encontró el identificador del pedido.');
      this.loading.set(false);
      return;
    }
    this.startPolling();
  }

  ngOnDestroy(): void { this.pollSub?.unsubscribe(); }

  private startPolling(): void {
    this.pollSub = interval(POLL_MS).pipe(
      startWith(0),
      switchMap(() =>
        this.http.get<TrackingOrder>(`/api/customer/orders/track/${this.orderId}`).pipe(
          catchError(() => of(null))
        )
      ),
    ).subscribe(result => {
      if (result === null) {
        this.errorCount++;
        if (this.loading() || this.errorCount >= this.MAX_ERRORS) {
          this.error.set('No se pudo cargar el pedido. Comprueba el enlace e inténtalo de nuevo.');
          this.loading.set(false);
          this.pollSub?.unsubscribe(); // stop hammering on persistent errors
        }
        return;
      }
      this.errorCount = 0; // reset on success
      this.order.set(result);
      this.loading.set(false);
      this.error.set(null);
      if (TERMINAL.includes(result.status)) this.pollSub?.unsubscribe();
    });
  }

  protected isStepCompleted(s: OrderStatus): boolean {
    const cur = this.order()?.status;
    if (!cur) return false;
    // Special case: treat SERVED and FINISHED as completing the FINISHED step
    if (s === 'FINISHED' && (cur === 'SERVED' || cur === 'FINISHED')) return true;
    return STATUS_ORDER.indexOf(cur) > STATUS_ORDER.indexOf(s);
  }

  protected isStepActive(s: OrderStatus): boolean {
    // SERVED uses the FINISHED step as its active marker
    if (s === 'FINISHED' && this.order()?.status === 'SERVED') return false;
    return this.order()?.status === s;
  }

  protected currentStatusText(): string {
    switch (this.order()?.status) {
      case 'PENDING':        return 'Hemos recibido tu pedido y está esperando confirmación de cocina.';
      case 'IN_PREPARATION': return 'Tu pedido está siendo preparado en cocina. ¡Ya queda poco!';
      case 'READY':          return this.order()?.type === 'TAKE_AWAY'
        ? '¡Tu pedido está listo! Puedes venir a recogerlo.'
        : '¡Tu pedido está listo y en camino!';
      case 'SERVED':         return 'Tu pedido ha sido entregado. ¡Buen provecho!';
      case 'FINISHED':       return '¡Pedido completado! Esperamos que lo hayas disfrutado.';
      default:               return 'Procesando tu pedido…';
    }
  }

  protected currentStatusIcon(): string {
    switch (this.order()?.status) {
      case 'PENDING':        return '📋';
      case 'IN_PREPARATION': return '👨‍🍳';
      case 'READY':          return '✅';
      case 'SERVED':         return '🛵';
      case 'FINISHED':       return '🎉';
      default:               return '⏳';
    }
  }
}
