// src/app/layout/customer-shell.component.ts
import { Component, computed, inject, signal } from '@angular/core';
import { RouterOutlet, RouterLink, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { CartService } from '../core/cart/cart.service';
import { AuthService } from '../core/auth/auth.service';

/**
 * Shell wrapper for all customer-facing pages.
 *
 * Visually distinct from the staff AppShellComponent:
 * - Warm cream background, brand-green accents
 * - Mobile-first top navbar with cart badge
 * - No staff navigation links
 */
@Component({
  selector: 'gc-customer-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, CommonModule],
  template: `
    <div class="customer-shell">

      <!-- ── Top navbar ─────────────────────────────────────────────── -->
      <header class="customer-nav">
        <div class="nav-inner">

          <!-- Logo -->
          <a routerLink="/" class="nav-logo">
            <span class="logo-mark">GC</span>
            <span class="logo-text">Gastrocontrol</span>
          </a>

          <!-- Right side -->
          <div class="nav-right">
            <!-- Menu link -->
            <a routerLink="/menu" class="nav-link">Menú</a>

            <!-- Account -->
            @if (auth.loggedIn()) {
              <a routerLink="/me" class="nav-link">Mi cuenta</a>
            } @else {
              <a routerLink="/login" class="nav-link">Entrar</a>
            }

            <!-- Cart button -->
            <button class="cart-btn" (click)="cart.drawerOpen.set(true)" [class.has-items]="cart.hasItems()">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                   stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/>
                <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/>
              </svg>
              @if (cart.totalQuantity() > 0) {
                <span class="cart-badge">{{ cart.totalQuantity() }}</span>
              }
            </button>
          </div>
        </div>
      </header>

      <!-- ── Page content ───────────────────────────────────────────── -->
      <main class="customer-main">
        <router-outlet />
      </main>

      <!-- ── Cart drawer ────────────────────────────────────────────── -->
      @if (cart.drawerOpen()) {
        <div class="drawer-backdrop" (click)="cart.drawerOpen.set(false)">
          <aside class="cart-drawer" (click)="$event.stopPropagation()">

            <div class="drawer-header">
              <h2 class="drawer-title">Tu pedido</h2>
              <button class="drawer-close" (click)="cart.drawerOpen.set(false)" aria-label="Cerrar">✕</button>
            </div>

            @if (!cart.hasItems()) {
              <div class="cart-empty">
                <span class="cart-empty-icon">🛒</span>
                <p>Tu carrito está vacío.</p>
                <p class="cart-empty-sub">Añade algo del menú para empezar.</p>
              </div>
            } @else {
              <div class="cart-lines">
                @for (item of cart.items(); track item.product.id) {
                  <div class="cart-line">
                    <div class="line-info">
                      <span class="line-name">{{ item.product.name }}</span>
                      <span class="line-price">
                        {{ formatPrice(item.product.priceCents * item.quantity) }}
                      </span>
                    </div>
                    <div class="line-qty">
                      <button class="qty-btn" (click)="cart.decrement(item.product.id)">−</button>
                      <span class="qty-value">{{ item.quantity }}</span>
                      <button class="qty-btn" (click)="cart.add(item.product)">+</button>
                    </div>
                  </div>
                }
              </div>

              <div class="cart-footer">
                <div class="cart-total">
                  <span>Total</span>
                  <span class="total-price">{{ formatPrice(cart.totalCents()) }}</span>
                </div>
                <button class="btn-checkout" (click)="goToCheckout()">
                  Ir al pago →
                </button>
              </div>
            }

          </aside>
        </div>
      }

    </div>
  `,
  styles: [`
    /* ── Shell layout ───────────────────────────────────────────────── */
    .customer-shell {
      min-height: 100vh;
      background: #faf8f5;
      font-family: 'Georgia', 'Times New Roman', serif;
    }

    /* ── Navbar ─────────────────────────────────────────────────────── */
    .customer-nav {
      position: sticky; top: 0; z-index: 100;
      background: #1a2e1a;
      border-bottom: 1px solid rgba(255,255,255,0.06);
    }
    .nav-inner {
      max-width: 1200px; margin: 0 auto;
      padding: 0 1.5rem;
      height: 60px;
      display: flex; align-items: center; justify-content: space-between;
    }
    .nav-logo {
      display: flex; align-items: center; gap: 0.625rem;
      text-decoration: none;
    }
    .logo-mark {
      width: 32px; height: 32px;
      background: #c8a96e;
      border-radius: 6px;
      display: grid; place-items: center;
      font-size: 0.7rem; font-weight: 800;
      color: #1a2e1a; letter-spacing: 0.05em;
    }
    .logo-text {
      font-size: 1rem; font-weight: 600;
      color: rgba(255,255,255,0.92);
      letter-spacing: -0.02em;
    }
    .nav-right {
      display: flex; align-items: center; gap: 1.5rem;
    }
    .nav-link {
      font-size: 0.875rem; color: rgba(255,255,255,0.65);
      text-decoration: none; transition: color 0.15s;
      font-family: system-ui, sans-serif;
      &:hover { color: rgba(255,255,255,0.95); }
    }

    /* Cart button */
    .cart-btn {
      position: relative;
      background: none; border: none; cursor: pointer;
      color: rgba(255,255,255,0.65);
      padding: 0.375rem;
      border-radius: 8px;
      transition: color 0.15s, background 0.15s;
      display: flex; align-items: center;
      &:hover { color: white; background: rgba(255,255,255,0.08); }
      &.has-items { color: #c8a96e; }
    }
    .cart-badge {
      position: absolute; top: -4px; right: -4px;
      background: #c8a96e; color: #1a2e1a;
      font-size: 0.62rem; font-weight: 800;
      width: 17px; height: 17px;
      border-radius: 50%;
      display: grid; place-items: center;
      font-family: system-ui, sans-serif;
    }

    /* ── Main ───────────────────────────────────────────────────────── */
    .customer-main { min-height: calc(100vh - 60px); }

    /* ── Cart drawer ────────────────────────────────────────────────── */
    .drawer-backdrop {
      position: fixed; inset: 0; z-index: 500;
      background: rgba(0,0,0,0.45);
      backdrop-filter: blur(2px);
    }
    .cart-drawer {
      position: fixed; top: 0; right: 0; bottom: 0;
      width: min(400px, 100vw);
      background: #faf8f5;
      display: flex; flex-direction: column;
      box-shadow: -8px 0 40px rgba(0,0,0,0.18);
    }
    .drawer-header {
      display: flex; align-items: center; justify-content: space-between;
      padding: 1.25rem 1.5rem;
      border-bottom: 1px solid rgba(0,0,0,0.08);
      background: #1a2e1a;
    }
    .drawer-title {
      font-size: 1.1rem; font-weight: 700;
      color: rgba(255,255,255,0.92); margin: 0;
      letter-spacing: -0.02em;
    }
    .drawer-close {
      background: rgba(255,255,255,0.1); border: none; cursor: pointer;
      color: rgba(255,255,255,0.7);
      width: 30px; height: 30px; border-radius: 50%;
      display: grid; place-items: center; font-size: 0.85rem;
      transition: background 0.15s, color 0.15s;
      &:hover { background: rgba(255,255,255,0.2); color: white; }
    }

    /* Empty state */
    .cart-empty {
      flex: 1; display: flex; flex-direction: column;
      align-items: center; justify-content: center; gap: 0.5rem;
      padding: 2rem; text-align: center;
      color: #666;
      p { margin: 0; font-size: 0.9rem; }
    }
    .cart-empty-icon { font-size: 2.5rem; }
    .cart-empty-sub { font-size: 0.8rem !important; color: #999; }

    /* Cart lines */
    .cart-lines {
      flex: 1; overflow-y: auto;
      padding: 1rem 1.5rem;
      display: flex; flex-direction: column; gap: 0.875rem;
    }
    .cart-line {
      display: flex; flex-direction: column; gap: 0.375rem;
      padding-bottom: 0.875rem;
      border-bottom: 1px solid rgba(0,0,0,0.07);
      &:last-child { border-bottom: none; }
    }
    .line-info {
      display: flex; justify-content: space-between; align-items: baseline;
    }
    .line-name {
      font-size: 0.9rem; font-weight: 500; color: #1a2e1a;
    }
    .line-price {
      font-size: 0.9rem; font-weight: 600; color: #1a2e1a;
      font-family: system-ui, sans-serif;
    }
    .line-qty {
      display: flex; align-items: center; gap: 0.625rem;
    }
    .qty-btn {
      width: 28px; height: 28px;
      background: #f0ede8; border: 1px solid rgba(0,0,0,0.1);
      border-radius: 6px; cursor: pointer;
      font-size: 1rem; font-weight: 600;
      color: #1a2e1a;
      display: grid; place-items: center;
      transition: background 0.1s;
      &:hover { background: #e5e0d8; }
    }
    .qty-value {
      min-width: 24px; text-align: center;
      font-size: 0.9rem; font-weight: 600;
      font-family: system-ui, sans-serif;
      color: #1a2e1a;
    }

    /* Footer */
    .cart-footer {
      padding: 1.25rem 1.5rem;
      border-top: 2px solid rgba(0,0,0,0.08);
      display: flex; flex-direction: column; gap: 1rem;
      background: white;
    }
    .cart-total {
      display: flex; justify-content: space-between; align-items: baseline;
      font-size: 0.875rem; color: #666;
    }
    .total-price {
      font-size: 1.25rem; font-weight: 700; color: #1a2e1a;
      font-family: system-ui, sans-serif;
    }
    .btn-checkout {
      width: 100%; padding: 0.875rem;
      background: #1a2e1a; color: #c8a96e;
      border: none; border-radius: 10px; cursor: pointer;
      font-size: 1rem; font-weight: 700;
      letter-spacing: 0.02em;
      transition: background 0.15s, transform 0.1s;
      font-family: system-ui, sans-serif;
      &:hover { background: #243d24; }
      &:active { transform: scale(0.99); }
    }
  `],
})
export class CustomerShellComponent {
  protected readonly cart   = inject(CartService);
  protected readonly auth   = inject(AuthService);
  private   readonly router = inject(Router);

  protected formatPrice(cents: number): string {
    return (cents / 100).toLocaleString('es-ES', { style: 'currency', currency: 'EUR' });
  }

  protected goToCheckout(): void {
    this.cart.drawerOpen.set(false);
    void this.router.navigateByUrl('/order');
  }
}
