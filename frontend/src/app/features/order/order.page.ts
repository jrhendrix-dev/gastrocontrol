// src/app/features/order/order.page.ts
import {
  ChangeDetectionStrategy, Component, inject, OnInit, signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CartService } from '../../core/cart/cart.service';
import { CustomerCheckoutApi } from '../../core/api/customer/customer-checkout.api';

type OrderType = 'TAKE_AWAY' | 'DELIVERY';
type PaymentMethod = 'STRIPE' | 'CASH';
type Step = 'type' | 'details' | 'submitting';

/**
 * Customer checkout page — Step 1: order type, Step 2: details, Step 3: Stripe.
 */
@Component({
  selector: 'app-order-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `<!-- src/app/features/order/order.page.html -->
  <div class="checkout-page">

    <!-- ── Empty cart guard ───────────────────────────────────────────── -->
    @if (!cart.hasItems()) {
      <div class="empty-cart">
        <span class="empty-icon">🛒</span>
        <h2>Tu carrito está vacío</h2>
        <p>Añade productos desde el menú para continuar.</p>
        <a routerLink="/menu" class="btn-back-menu">Ver el menú</a>
      </div>
    } @else {

      <div class="checkout-layout">

        <!-- ── Left: Steps ────────────────────────────────────────────── -->
        <div class="checkout-main">

          <!-- Step indicator -->
          <div class="steps">
            <div class="step" [class.active]="step() === 'type'" [class.done]="step() !== 'type'">
              <span class="step-num">1</span>
              <span class="step-label">Tipo de pedido</span>
            </div>
            <div class="step-divider"></div>
            <div class="step"
                 [class.active]="step() === 'details' || step() === 'submitting'">
              <span class="step-num">2</span>
              <span class="step-label">Tus datos</span>
            </div>
            <div class="step-divider"></div>
            <div class="step">
              <span class="step-num">3</span>
              <span class="step-label">Pago</span>
            </div>
          </div>

          <!-- ── Step 1: Order type ──────────────────────────────────── -->
          @if (step() === 'type') {
            <div class="step-panel">
              <h2 class="panel-title">¿Cómo quieres recibir tu pedido?</h2>

              <div class="type-cards">

                <button class="type-card" [class.selected]="selectedType() === 'TAKE_AWAY'"
                        (click)="selectType('TAKE_AWAY')">
                  <span class="type-icon">🥡</span>
                  <div class="type-info">
                    <div class="type-name">Recoger en local</div>
                    <div class="type-desc">Prepara tu pedido y recógelo cuando esté listo.</div>
                  </div>
                  <span class="type-check" [class.visible]="selectedType() === 'TAKE_AWAY'">✓</span>
                </button>

                <button class="type-card" [class.selected]="selectedType() === 'DELIVERY'"
                        (click)="selectType('DELIVERY')">
                  <span class="type-icon">🛵</span>
                  <div class="type-info">
                    <div class="type-name">Envío a domicilio</div>
                    <div class="type-desc">Te lo llevamos donde nos digas.</div>
                  </div>
                  <span class="type-check" [class.visible]="selectedType() === 'DELIVERY'">✓</span>
                </button>

              </div>

              <div class="step-actions">
                <a routerLink="/menu" class="btn-ghost">← Volver al menú</a>
                <button class="btn-continue" [disabled]="!selectedType()" (click)="goToDetails()">
                  Continuar →
                </button>
              </div>
            </div>
          }

          <!-- ── Step 2a: Take Away details ──────────────────────────── -->
          @if (step() === 'details' && selectedType() === 'TAKE_AWAY') {
            <div class="step-panel">
              <h2 class="panel-title">Datos de recogida</h2>
              <p class="panel-sub">Te avisaremos cuando tu pedido esté listo.</p>

              <div class="form-grid">
                <div class="form-field full">
                  <label>Nombre *</label>
                  <input class="gc-input" type="text" [(ngModel)]="pickup.name"
                         placeholder="Tu nombre" autocomplete="name" />
                  @if (fieldError('name')) {
                    <span class="field-error">{{ fieldError('name') }}</span>
                  }
                </div>
                <div class="form-field full">
                  <label>Teléfono (opcional)</label>
                  <input class="gc-input" type="tel" [(ngModel)]="pickup.phone"
                         placeholder="Para avisarte cuando esté listo" autocomplete="tel" />
                </div>
                <div class="form-field full">
                  <label>Notas para cocina (opcional)</label>
                  <textarea class="gc-input" rows="2" [(ngModel)]="pickup.notes"
                            placeholder="Alergias, preferencias…"></textarea>
                </div>
              </div>

              @if (submitError()) {
                <div class="submit-error">{{ submitError() }}</div>
              }

              <div class="step-actions">
                <button class="btn-ghost" (click)="step.set('type')">← Atrás</button>
                <button class="btn-primary-action" [disabled]="step() === 'submitting'"
                        (click)="submit()">
                  @if (step() === 'submitting') {
                    <span class="spinner"></span> Procesando…
                  } @else if (paymentMethod() === 'CASH') {
                    💵 Confirmar pedido
                  } @else {
                    🔒 Pagar con Stripe
                  }
                </button>
              </div>
            </div>
          }

          <!-- ── Step 2b: Delivery details ─────────────────────────── -->
          @if (step() === 'details' && selectedType() === 'DELIVERY') {
            <div class="step-panel">
              <h2 class="panel-title">Dirección de entrega</h2>
              <p class="panel-sub">Indica dónde quieres recibir tu pedido.</p>

              <div class="form-grid">
                <div class="form-field">
                  <label>Nombre *</label>
                  <input class="gc-input" type="text" [(ngModel)]="delivery.name"
                         placeholder="Tu nombre" autocomplete="name" />
                  @if (fieldError('name')) {
                    <span class="field-error">{{ fieldError('name') }}</span>
                  }
                </div>
                <div class="form-field">
                  <label>Teléfono *</label>
                  <input class="gc-input" type="tel" [(ngModel)]="delivery.phone"
                         placeholder="Para coordinar la entrega" autocomplete="tel" />
                  @if (fieldError('phone')) {
                    <span class="field-error">{{ fieldError('phone') }}</span>
                  }
                </div>
                <div class="form-field full">
                  <label>Dirección *</label>
                  <input class="gc-input" type="text" [(ngModel)]="delivery.addressLine1"
                         placeholder="Calle y número" autocomplete="address-line1" />
                  @if (fieldError('addressLine1')) {
                    <span class="field-error">{{ fieldError('addressLine1') }}</span>
                  }
                </div>
                <div class="form-field full">
                  <label>Piso / puerta (opcional)</label>
                  <input class="gc-input" type="text" [(ngModel)]="delivery.addressLine2"
                         placeholder="Ej. 2º B" autocomplete="address-line2" />
                </div>
                <div class="form-field">
                  <label>Ciudad *</label>
                  <input class="gc-input" type="text" [(ngModel)]="delivery.city"
                         placeholder="Ciudad" autocomplete="address-level2" />
                  @if (fieldError('city')) {
                    <span class="field-error">{{ fieldError('city') }}</span>
                  }
                </div>
                <div class="form-field">
                  <label>Código postal (opcional)</label>
                  <input class="gc-input" type="text" [(ngModel)]="delivery.postalCode"
                         placeholder="00000" autocomplete="postal-code" />
                </div>
                <div class="form-field full">
                  <label>Instrucciones de entrega (opcional)</label>
                  <textarea class="gc-input" rows="2" [(ngModel)]="delivery.notes"
                            placeholder="Ej. Portero automático 3B, llamar al llegar…"></textarea>
                </div>
              </div>

              @if (submitError()) {
                <div class="submit-error">{{ submitError() }}</div>
              }

              <div class="step-actions">
                <button class="btn-ghost" (click)="step.set('type')">← Atrás</button>
                <button class="btn-primary-action" [disabled]="step() === 'submitting'"
                        (click)="submit()">
                  @if (step() === 'submitting') {
                    <span class="spinner"></span> Procesando…
                  } @else if (paymentMethod() === 'CASH') {
                    💵 Confirmar pedido
                  } @else {
                    🔒 Pagar con Stripe
                  }
                </button>
              </div>
            </div>
          }

        </div>

        <!-- ── Right: Order summary ──────────────────────────────────── -->
        <aside class="order-summary">
          <div class="summary-card">
            <h3 class="summary-title">Resumen del pedido</h3>

            <div class="summary-lines">
              @for (item of cart.items(); track item.product.id) {
                <div class="summary-line">
                  <span class="summary-qty">{{ item.quantity }}×</span>
                  <span class="summary-name">{{ item.product.name }}</span>
                  <span class="summary-price">
                  {{ formatPrice(item.product.priceCents * item.quantity) }}
                </span>
                </div>
              }
            </div>

            <div class="summary-divider"></div>

            <div class="summary-total">
              <span>Total</span>
              <span class="total-amount">{{ formatPrice(cart.totalCents()) }}</span>
            </div>

            @if (selectedType()) {
              <div class="summary-type">
                {{ selectedType() === 'TAKE_AWAY' ? '🥡 Recoger en local' : '🛵 Envío a domicilio' }}
              </div>
            }

            @if (step() === 'details' || step() === 'submitting') {
              <div class="payment-method-section">
                <div class="payment-method-label">Método de pago</div>
                <div class="payment-methods">
                  <button class="payment-method-btn"
                          [class.selected]="paymentMethod() === 'STRIPE'"
                          (click)="paymentMethod.set('STRIPE')">
                    <span>💳</span>
                    <span>Tarjeta (Stripe)</span>
                  </button>
                  <button class="payment-method-btn"
                          [class.selected]="paymentMethod() === 'CASH'"
                          (click)="paymentMethod.set('CASH')">
                    <span>💵</span>
                    <span>Efectivo</span>
                  </button>
                </div>
                @if (paymentMethod() === 'CASH') {
                  <p class="cash-note">
                    Paga en efectivo al recoger o al recibir tu pedido.
                  </p>
                }
              </div>
            }
          </div>

          <p class="stripe-note" [style.display]="paymentMethod() === 'CASH' ? 'none' : 'block'">
            🔒 El pago se procesa de forma segura a través de Stripe.
            No almacenamos datos de tu tarjeta.
          </p>
        </aside>

      </div>
    }

  </div>`,
  styles: [`.checkout-page {
    min-height: calc(100vh - 60px);
    background: #faf8f5;
    font-family: system-ui, sans-serif;
    padding: 2rem 1.5rem 4rem;
  }

  /* Empty cart */
  .empty-cart {
    display: flex; flex-direction: column; align-items: center;
    justify-content: center; gap: 1rem;
    padding: 5rem 1.5rem; text-align: center;
  }
  .empty-icon { font-size: 3rem; }
  .empty-cart h2 {
    font-size: 1.25rem; font-weight: 700; color: #1a2e1a; margin: 0;
    font-family: 'Georgia', serif;
  }
  .empty-cart p { color: #888; margin: 0; font-size: 0.9rem; }
  .btn-back-menu {
    display: inline-block; margin-top: 0.5rem;
    padding: 0.75rem 1.5rem;
    background: #1a2e1a; color: #c8a96e;
    border-radius: 10px; text-decoration: none;
    font-weight: 700; font-size: 0.9rem;
  }

  /* Layout */
  .checkout-layout {
    max-width: 1000px; margin: 0 auto;
    display: grid; grid-template-columns: 1fr 340px;
    gap: 2rem; align-items: start;
  }

  /* Steps */
  .steps {
    display: flex; align-items: center; margin-bottom: 2rem;
  }
  .step { display: flex; align-items: center; gap: 0.5rem; flex-shrink: 0; }
  .step-num {
    width: 28px; height: 28px; border-radius: 50%;
    background: rgba(0,0,0,0.08); color: #999;
    display: grid; place-items: center;
    font-size: 0.75rem; font-weight: 700;
  }
  .step.active .step-num { background: #1a2e1a; color: #c8a96e; }
  .step.done .step-num   { background: #c8a96e; color: #1a2e1a; }
  .step-label { font-size: 0.8rem; color: #aaa; font-weight: 500; }
  .step.active .step-label { color: #1a2e1a; font-weight: 600; }
  .step-divider { flex: 1; height: 1px; background: rgba(0,0,0,0.1); margin: 0 0.75rem; }

  /* Panel */
  .step-panel {
    background: white; border-radius: 16px;
    border: 1px solid rgba(0,0,0,0.07);
    padding: 1.75rem;
    display: flex; flex-direction: column; gap: 1.5rem;
  }
  .panel-title {
    font-size: 1.25rem; font-weight: 700; color: #1a2e1a;
    margin: 0; font-family: 'Georgia', serif;
  }
  .panel-sub { font-size: 0.875rem; color: #888; margin: -0.75rem 0 0; }

  /* Type cards */
  .type-cards { display: flex; flex-direction: column; gap: 0.875rem; }
  .type-card {
    display: flex; align-items: center; gap: 1rem;
    padding: 1.25rem; background: #faf8f5;
    border: 2px solid rgba(0,0,0,0.08);
    border-radius: 12px; cursor: pointer; text-align: left; width: 100%;
    transition: border-color 0.15s, background 0.15s;
    &:hover { border-color: rgba(26,46,26,0.3); background: white; }
    &.selected { border-color: #1a2e1a; background: white; box-shadow: 0 0 0 1px #1a2e1a; }
  }
  .type-icon { font-size: 2rem; flex-shrink: 0; }
  .type-info { flex: 1; }
  .type-name { font-size: 0.975rem; font-weight: 700; color: #1a2e1a; }
  .type-desc { font-size: 0.8rem; color: #888; margin-top: 0.2rem; }
  .type-check {
    width: 24px; height: 24px; background: #1a2e1a; color: #c8a96e;
    border-radius: 50%; display: grid; place-items: center;
    font-size: 0.75rem; font-weight: 800;
    opacity: 0; transition: opacity 0.15s; flex-shrink: 0;
    &.visible { opacity: 1; }
  }

  /* Form */
  .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
  .form-field {
    display: flex; flex-direction: column; gap: 0.35rem;
    &.full { grid-column: 1 / -1; }
    label { font-size: 0.8rem; font-weight: 600; color: #555; }
    textarea { resize: vertical; }
  }
  .field-error { font-size: 0.75rem; color: #b91c1c; }
  .submit-error {
    background: rgba(220,38,38,0.06); border: 1px solid rgba(220,38,38,0.2);
    color: #b91c1c; border-radius: 8px; padding: 0.75rem 1rem; font-size: 0.875rem;
  }

  /* Actions */
  .step-actions { display: flex; justify-content: space-between; align-items: center; }
  .btn-ghost {
    background: none; border: none; cursor: pointer;
    color: #888; font-size: 0.875rem; padding: 0.5rem 0;
    font-family: system-ui, sans-serif; text-decoration: none;
    &:hover { color: #1a2e1a; }
  }
  .btn-continue, .btn-primary-action {
    padding: 0.75rem 1.75rem;
    background: #1a2e1a; color: #c8a96e;
    border: none; border-radius: 10px; cursor: pointer;
    font-size: 0.9rem; font-weight: 700;
    display: flex; align-items: center; gap: 0.5rem;
    font-family: system-ui, sans-serif;
    transition: background 0.15s;
    &:hover:not(:disabled) { background: #243d24; }
    &:disabled { opacity: 0.55; cursor: not-allowed; }
  }
  .spinner {
    width: 16px; height: 16px;
    border: 2px solid rgba(200,169,110,0.3);
    border-top-color: #c8a96e;
    border-radius: 50%;
    animation: spin 0.7s linear infinite;
  }
  @keyframes spin { to { transform: rotate(360deg); } }

  /* Summary */
  .order-summary { position: sticky; top: 80px; display: flex; flex-direction: column; gap: 1rem; }
  .summary-card {
    background: white; border-radius: 16px;
    border: 1px solid rgba(0,0,0,0.07); padding: 1.5rem;
  }
  .summary-title {
    font-size: 1rem; font-weight: 700; color: #1a2e1a;
    margin: 0 0 1rem; font-family: 'Georgia', serif;
  }
  .summary-lines { display: flex; flex-direction: column; gap: 0.625rem; }
  .summary-line { display: flex; align-items: baseline; gap: 0.5rem; font-size: 0.875rem; }
  .summary-qty { color: #aaa; font-size: 0.8rem; flex-shrink: 0; width: 24px; }
  .summary-name { flex: 1; color: #333; }
  .summary-price { font-weight: 600; color: #1a2e1a; white-space: nowrap; }
  .summary-divider { height: 1px; background: rgba(0,0,0,0.07); margin: 1rem 0; }
  .summary-total { display: flex; justify-content: space-between; align-items: baseline; font-size: 0.875rem; color: #666; }
  .total-amount { font-size: 1.375rem; font-weight: 800; color: #1a2e1a; }
  .summary-type { margin-top: 0.75rem; padding-top: 0.75rem; border-top: 1px solid rgba(0,0,0,0.06); font-size: 0.8rem; color: #888; }
  .stripe-note { font-size: 0.75rem; color: #aaa; margin: 0; text-align: center; line-height: 1.5; }

  /* Payment method selector */
  .payment-method-section {
    margin-top: 1rem; padding-top: 1rem;
    border-top: 1px solid rgba(0,0,0,0.06);
    display: flex; flex-direction: column; gap: 0.625rem;
  }
  .payment-method-label {
    font-size: 0.75rem; font-weight: 600; color: #888;
    text-transform: uppercase; letter-spacing: 0.05em;
  }
  .payment-methods { display: flex; gap: 0.5rem; }
  .payment-method-btn {
    flex: 1; display: flex; align-items: center; justify-content: center; gap: 0.4rem;
    padding: 0.6rem 0.5rem;
    background: #faf8f5; border: 1.5px solid rgba(0,0,0,0.1);
    border-radius: 8px; cursor: pointer; font-size: 0.8rem; font-weight: 500;
    color: #555; transition: all 0.15s; font-family: system-ui, sans-serif;
    &:hover { border-color: rgba(26,46,26,0.3); background: white; }
    &.selected {
      border-color: #1a2e1a; background: white;
      color: #1a2e1a; font-weight: 700;
      box-shadow: 0 0 0 1px #1a2e1a;
    }
  }
  .cash-note {
    font-size: 0.75rem; color: #888; margin: 0; line-height: 1.5;
    background: rgba(200,169,110,0.08); border: 1px solid rgba(200,169,110,0.2);
    border-radius: 6px; padding: 0.5rem 0.75rem;
  }

  @media (max-width: 768px) {
    .checkout-page { padding: 1rem 1rem 4rem; }
    .checkout-layout { grid-template-columns: 1fr; }
    .order-summary { position: static; order: -1; }
    .form-grid { grid-template-columns: 1fr; }
    .form-field.full { grid-column: unset; }
  }`],
})
export class OrderPage implements OnInit {
  protected readonly cart = inject(CartService);
  private  readonly api  = inject(CustomerCheckoutApi);
  private  readonly router = inject(Router);

  protected readonly step          = signal<Step>('type');
  protected readonly selectedType  = signal<OrderType | null>(null);
  protected readonly paymentMethod = signal<PaymentMethod>('STRIPE');
  protected readonly submitError  = signal<string | null>(null);
  protected readonly fieldErrors  = signal<Record<string, string>>({});

  protected pickup = { name: '', phone: '', notes: '' };
  protected delivery = {
    name: '', phone: '', addressLine1: '', addressLine2: '',
    city: '', postalCode: '', notes: '',
  };

  ngOnInit(): void {}

  protected selectType(type: OrderType): void { this.selectedType.set(type); }

  protected goToDetails(): void {
    if (!this.selectedType()) return;
    this.step.set('details');
    this.submitError.set(null);
    this.fieldErrors.set({});
  }

  protected fieldError(field: string): string | null {
    return this.fieldErrors()[field] ?? null;
  }

  protected submit(): void {
    const type = this.selectedType();
    if (!type) return;

    const errors: Record<string, string> = {};
    if (type === 'TAKE_AWAY') {
      if (!this.pickup.name.trim()) errors['name'] = 'El nombre es obligatorio.';
    }
    if (type === 'DELIVERY') {
      if (!this.delivery.name.trim())         errors['name']         = 'El nombre es obligatorio.';
      if (!this.delivery.phone.trim())        errors['phone']        = 'El teléfono es obligatorio.';
      if (!this.delivery.addressLine1.trim()) errors['addressLine1'] = 'La dirección es obligatoria.';
      if (!this.delivery.city.trim())         errors['city']         = 'La ciudad es obligatoria.';
    }
    if (Object.keys(errors).length > 0) { this.fieldErrors.set(errors); return; }

    this.fieldErrors.set({});
    this.submitError.set(null);
    this.step.set('submitting');

    const req = {
      type,
      pickup: type === 'TAKE_AWAY' ? {
        name: this.pickup.name.trim(),
        phone: this.pickup.phone?.trim() || null,
        notes: this.pickup.notes?.trim() || null,
      } : null,
      delivery: type === 'DELIVERY' ? {
        name: this.delivery.name.trim(),
        phone: this.delivery.phone.trim(),
        addressLine1: this.delivery.addressLine1.trim(),
        addressLine2: this.delivery.addressLine2?.trim() || null,
        city: this.delivery.city.trim(),
        postalCode: this.delivery.postalCode?.trim() || null,
        notes: this.delivery.notes?.trim() || null,
      } : null,
      items: this.cart.items().map(i => ({ productId: i.product.id, quantity: i.quantity })),
    };

    if (this.paymentMethod() === 'CASH') {
      // Cash payment: single backend call — order created, submitted to kitchen,
      // and MANUAL payment recorded in one transaction.
      this.api.cashCheckout(req).subscribe({
        next: res => {
          this.cart.clear();
          void this.router.navigate(['/order/confirm'], {
            queryParams: {
              orderId: res.orderId,
              token: res.trackingToken,
              payment_status: 'paid',
            }
          });
        },
        error: err => {
          this.step.set('details');
          const details = err?.error?.error?.details;
          if (details && typeof details === 'object') {
            const mapped: Record<string, string> = {};
            for (const [k, v] of Object.entries(details)) mapped[k] = String(v);
            this.fieldErrors.set(mapped);
            return;
          }
          this.submitError.set(err?.error?.error?.message ?? 'No se pudo procesar el pedido. Inténtalo de nuevo.');
        },
      });
    } else {
      this.api.startCheckout(req).subscribe({
        next: res => {
          this.cart.clear();
          // Store token in sessionStorage so confirm page can build the tracking URL
          if (res.trackingToken) {
            sessionStorage.setItem('gc_tracking_token', res.trackingToken);
          }
          window.location.href = res.checkoutUrl;
        },
        error: err => {
          this.step.set('details');
          const details = err?.error?.error?.details;
          if (details && typeof details === 'object') {
            const mapped: Record<string, string> = {};
            for (const [k, v] of Object.entries(details)) mapped[k] = String(v);
            this.fieldErrors.set(mapped);
            return;
          }
          this.submitError.set(err?.error?.error?.message ?? 'No se pudo procesar el pedido. Inténtalo de nuevo.');
        },
      });
    }
  }

  protected formatPrice(cents: number): string {
    return (cents / 100).toLocaleString('es-ES', { style: 'currency', currency: 'EUR' });
  }
}
