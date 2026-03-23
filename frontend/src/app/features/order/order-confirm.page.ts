// src/app/features/order/order-confirm.page.ts
import {
  ChangeDetectionStrategy, Component, inject, OnInit, signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

/**
 * Landing page after Stripe Checkout completes.
 *
 * Stripe redirects to: /order/confirm?orderId=X&status=success
 * (or status=cancel if the customer abandoned the checkout)
 *
 * We read the orderId from query params and show the appropriate state.
 * The actual payment reconciliation is handled by the Stripe webhook on
 * the backend — we just show a friendly confirmation here.
 */
@Component({
  selector: 'app-order-confirm-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="confirm-page">
      @if (status() === 'success') {
        <div class="confirm-card success">
          <div class="confirm-icon">✅</div>
          <h1 class="confirm-title">¡Pedido recibido!</h1>
          <p class="confirm-sub">
            Hemos recibido tu pago y tu pedido ya está en camino a cocina.
          </p>
          @if (isNumericOrderId()) {
            <div class="order-ref">
              Pedido <strong>#{{ orderId() }}</strong>
            </div>
          }
          <div class="confirm-actions">
            @if (trackingToken()) {
              <a [routerLink]="['/track', trackingToken()]" class="btn-track">
                📍 Seguir mi pedido
              </a>
            } @else {
              <a routerLink="/menu" class="btn-track">
                Volver al menú
              </a>
            }
            <a routerLink="/menu" class="btn-ghost">Volver al menú</a>
          </div>
        </div>
      } @else if (status() === 'cancel') {
        <div class="confirm-card cancel">
          <div class="confirm-icon">❌</div>
          <h1 class="confirm-title">Pago cancelado</h1>
          <p class="confirm-sub">
            No se ha realizado ningún cargo. Puedes volver al menú
            e intentarlo de nuevo cuando quieras.
          </p>
          <div class="confirm-actions">
            <a routerLink="/menu" class="btn-track">Volver al menú</a>
          </div>
        </div>
      } @else {
        <div class="confirm-card">
          <div class="confirm-icon">⏳</div>
          <h1 class="confirm-title">Verificando pago…</h1>
          <p class="confirm-sub">Espera un momento.</p>
        </div>
      }
    </div>
  `,
  styles: [`
    .confirm-page {
      min-height: calc(100vh - 60px);
      background: #faf8f5;
      display: flex; align-items: center; justify-content: center;
      padding: 2rem 1.5rem;
      font-family: system-ui, sans-serif;
    }
    .confirm-card {
      background: white; border-radius: 20px;
      border: 1px solid rgba(0,0,0,0.07);
      padding: 3rem 2.5rem;
      max-width: 460px; width: 100%;
      display: flex; flex-direction: column; align-items: center;
      gap: 1rem; text-align: center;
      box-shadow: 0 8px 40px rgba(0,0,0,0.07);
    }
    .confirm-icon { font-size: 3rem; }
    .confirm-title {
      font-size: 1.625rem; font-weight: 700; color: #1a2e1a;
      margin: 0; font-family: 'Georgia', serif; letter-spacing: -0.02em;
    }
    .confirm-sub { font-size: 0.9rem; color: #888; margin: 0; line-height: 1.6; }
    .order-ref {
      background: rgba(26,46,26,0.05);
      border: 1px solid rgba(26,46,26,0.1);
      border-radius: 8px; padding: 0.5rem 1.25rem;
      font-size: 0.875rem; color: #555;
      strong { color: #1a2e1a; }
    }
    .confirm-actions {
      display: flex; flex-direction: column; align-items: center; gap: 0.75rem;
      margin-top: 0.5rem; width: 100%;
    }
    .btn-track {
      display: block; width: 100%;
      padding: 0.875rem;
      background: #1a2e1a; color: #c8a96e;
      border-radius: 10px; text-decoration: none;
      font-weight: 700; font-size: 0.9rem;
      text-align: center;
      transition: background 0.15s;
      &:hover { background: #243d24; }
    }
    .btn-ghost {
      font-size: 0.875rem; color: #aaa;
      text-decoration: none;
      &:hover { color: #1a2e1a; }
    }
  `],
})
export class OrderConfirmPage implements OnInit {
  private readonly route = inject(ActivatedRoute);

  protected readonly orderId       = signal<string | null>(null);
  protected readonly trackingToken = signal<string | null>(null);
  protected readonly status        = signal<'success' | 'cancel' | 'pending'>('pending');

  /** True when orderId is a numeric order id, not a Stripe session id. */
  protected isNumericOrderId(): boolean {
    const id = this.orderId();
    return id !== null && /^\d+$/.test(id);
  }

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    this.orderId.set(params.get('orderId'));

    // Token comes from query param (cash flow) or sessionStorage (Stripe flow)
    const tokenFromParams = params.get('token');
    const tokenFromStorage = sessionStorage.getItem('gc_tracking_token');
    const token = tokenFromParams ?? tokenFromStorage;
    if (token) {
      this.trackingToken.set(token);
      sessionStorage.removeItem('gc_tracking_token'); // clean up
    }

    // Stripe appends ?payment_status=paid when returning from a successful session.
    // Our cancel_url passes ?status=cancel.
    // Stripe also appends ?session_id=... which we ignore (webhook handles reconciliation).
    const stripePaymentStatus = params.get('payment_status');
    const ourStatus           = params.get('status');

    if (stripePaymentStatus === 'paid' || ourStatus === 'success') {
      this.status.set('success');
    } else if (ourStatus === 'cancel') {
      this.status.set('cancel');
    } else if (params.get('session_id') && !ourStatus) {
      // Stripe redirected with a session_id but no explicit status —
      // treat as success since Stripe only redirects to success_url on payment.
      this.status.set('success');
    } else {
      this.status.set(this.orderId() ? 'success' : 'pending');
    }
  }
}
