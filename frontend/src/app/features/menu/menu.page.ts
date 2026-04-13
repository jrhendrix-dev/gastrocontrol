// src/app/features/menu/menu.page.ts
import {
  AfterViewInit, ChangeDetectionStrategy, Component, ElementRef,
  inject, OnInit, QueryList, signal, ViewChildren,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { CatalogApi } from '../../core/api/catalog/catalog.api';
import { CartService } from '../../core/cart/cart.service';
import { CatalogCategoryDto, CatalogProductDto } from '../../core/api/catalog/catalog.models';

/**
 * Public menu page — browsable without authentication.
 *
 * Layout:
 * - Sticky category sidebar (desktop) / horizontal scroll tabs (mobile)
 * - Product grid with add-to-cart controls
 * - Floating cart summary bar at bottom (mobile) when cart has items
 *
 * Data: single call to GET /api/catalog/categories which returns
 * the full category+product tree.
 */
@Component({
  selector: 'app-menu-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  template: `
    <div class="menu-page">

      <!-- ── Hero ─────────────────────────────────────────────────── -->
      <section class="menu-hero">
        <div class="hero-inner">
          <p class="hero-eyebrow">Bienvenido</p>
          <h1 class="hero-title">Nuestra carta</h1>
          <p class="hero-sub">Ingredientes frescos, recetas de siempre.</p>
        </div>
      </section>

      @if (loading()) {
        <div class="loading-state">
          <div class="loading-spinner"></div>
          <span>Cargando carta…</span>
        </div>
      } @else if (error()) {
        <div class="error-state">
          <p>{{ error() }}</p>
          <button class="retry-btn" (click)="load()">Reintentar</button>
        </div>
      } @else {

        <div class="menu-body">

          <!-- ── Category nav ──────────────────────────────────────── -->
          <nav class="category-nav" aria-label="Categorías">
            @for (cat of categories(); track cat.id) {
              <button class="cat-btn"
                      [class.active]="activeCategoryId() === cat.id"
                      (click)="scrollToCategory(cat.id)">
                {{ cat.name }}
                <span class="cat-count">{{ cat.products.length }}</span>
              </button>
            }
          </nav>

          <!-- ── Product sections ──────────────────────────────────── -->
          <div class="sections">
            @for (cat of categories(); track cat.id) {
              @if (cat.products.length > 0) {
                <section
                  class="category-section"
                  [attr.data-category-id]="cat.id"
                  #categorySection>

                  <h2 class="section-title">{{ cat.name }}</h2>

                  <div class="product-grid">
                    @for (product of cat.products; track product.id) {
                      <article class="product-card">

                        <!-- Product image with emoji placeholder fallback -->
                        <div class="card-image" [class.card-image--placeholder]="!product.imageUrl">
                          @if (product.imageUrl) {
                            <img
                              [src]="product.imageUrl"
                              [alt]="product.name"
                              class="card-img"
                              loading="lazy"
                              (error)="onImageError($event)" />
                          } @else {
                            <span class="card-image-placeholder">🍽️</span>
                          }
                        </div>

                        <div class="card-body">
                          <div class="card-info">
                            <h3 class="card-name">{{ product.name }}</h3>
                            @if (product.description) {
                              <p class="card-desc">{{ product.description }}</p>
                            }
                          </div>

                          <div class="card-footer">
                            <span class="card-price">
                              {{ formatPrice(product.priceCents) }}
                            </span>

                            @if (cart.quantityOf(product.id) === 0) {
                              <button class="add-btn" (click)="cart.add(product)"
                                      aria-label="Añadir {{ product.name }}">
                                <span>+</span> Añadir
                              </button>
                            } @else {
                              <div class="qty-control">
                                <button class="qty-btn" (click)="cart.decrement(product.id)"
                                        aria-label="Quitar uno">−</button>
                                <span class="qty-display">{{ cart.quantityOf(product.id) }}</span>
                                <button class="qty-btn qty-btn-add" (click)="cart.add(product)"
                                        aria-label="Añadir uno">+</button>
                              </div>
                            }
                          </div>
                        </div>

                      </article>
                    }
                  </div>
                </section>
              }
            }
          </div>

        </div>

      }

      <!-- ── Mobile floating cart bar ─────────────────────────────── -->
      @if (cart.hasItems()) {
        <div class="floating-cart" role="button" aria-label="Ver carrito" (click)="cart.drawerOpen.set(true)">
          <div class="floating-cart-inner">
            <span class="floating-qty">{{ cart.totalQuantity() }} artículo{{ cart.totalQuantity() !== 1 ? 's' : '' }}</span>
            <span class="floating-label">Ver pedido</span>
            <span class="floating-total">{{ formatPrice(cart.totalCents()) }}</span>
          </div>
        </div>
      }

    </div>
  `,
  styles: [`
    /* ── Page wrapper ───────────────────────────────────────────────── */
    .menu-page {
      min-height: 100vh;
      background: #faf8f5;
      font-family: 'Georgia', 'Times New Roman', serif;
    }

    /* ── Hero ───────────────────────────────────────────────────────── */
    .menu-hero {
      background: #1a2e1a;
      padding: 3rem 1.5rem 2.5rem;
      text-align: center;
      position: relative;
      overflow: hidden;

      &::before {
        content: '';
        position: absolute; inset: 0;
        background: radial-gradient(ellipse at 50% 120%, rgba(200,169,110,0.15) 0%, transparent 60%);
        pointer-events: none;
      }
    }
    .hero-inner { position: relative; z-index: 1; max-width: 600px; margin: 0 auto; }
    .hero-eyebrow {
      font-size: 0.75rem; font-weight: 600; letter-spacing: 0.18em;
      text-transform: uppercase; color: #c8a96e; margin: 0 0 0.75rem;
      font-family: system-ui, sans-serif;
    }
    .hero-title {
      font-size: clamp(2rem, 5vw, 3rem); font-weight: 700;
      color: rgba(255,255,255,0.95); margin: 0 0 0.75rem;
      letter-spacing: -0.03em; line-height: 1.1;
    }
    .hero-sub {
      font-size: 1rem; color: rgba(255,255,255,0.5); margin: 0;
      font-style: italic;
    }

    /* ── Loading / error ────────────────────────────────────────────── */
    .loading-state, .error-state {
      display: flex; flex-direction: column; align-items: center;
      justify-content: center; gap: 1rem;
      padding: 5rem 1.5rem; text-align: center; color: #888;
      font-family: system-ui, sans-serif; font-size: 0.9rem;
    }
    .loading-spinner {
      width: 32px; height: 32px;
      border: 3px solid rgba(0,0,0,0.08);
      border-top-color: #1a2e1a;
      border-radius: 50%;
      animation: spin 0.7s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
    .retry-btn {
      padding: 0.5rem 1.25rem; background: #1a2e1a; color: #c8a96e;
      border: none; border-radius: 8px; cursor: pointer; font-size: 0.875rem;
      font-family: system-ui, sans-serif;
    }

    /* ── Menu body layout ───────────────────────────────────────────── */
    .menu-body {
      max-width: 1200px; margin: 0 auto;
      display: grid;
      grid-template-columns: 220px 1fr;
      gap: 0;
      padding: 2rem 1.5rem 6rem;
      align-items: start;
    }

    /* ── Category nav (sidebar on desktop, horizontal on mobile) ─────── */
    .category-nav {
      position: sticky; top: 76px;
      display: flex; flex-direction: column; gap: 0.25rem;
      padding-right: 2rem;
    }
    .cat-btn {
      display: flex; align-items: center; justify-content: space-between;
      padding: 0.625rem 0.875rem;
      background: none; border: none; cursor: pointer;
      border-radius: 8px;
      font-size: 0.875rem; font-weight: 500; color: #666;
      text-align: left;
      font-family: system-ui, sans-serif;
      transition: background 0.15s, color 0.15s;

      &:hover { background: rgba(0,0,0,0.04); color: #1a2e1a; }
      &.active {
        background: #1a2e1a; color: #c8a96e;
        .cat-count { background: rgba(200,169,110,0.2); color: #c8a96e; }
      }
    }
    .cat-count {
      font-size: 0.7rem; font-weight: 700;
      background: rgba(0,0,0,0.06); color: #999;
      padding: 0.1rem 0.45rem; border-radius: 999px;
      font-family: system-ui, sans-serif;
      transition: background 0.15s, color 0.15s;
    }

    /* ── Sections ───────────────────────────────────────────────────── */
    .sections { min-width: 0; }
    .category-section {
      margin-bottom: 3rem;
      scroll-margin-top: 80px;
    }
    .section-title {
      font-size: 1.5rem; font-weight: 700; color: #1a2e1a;
      margin: 0 0 1.25rem; letter-spacing: -0.02em;
      padding-bottom: 0.75rem;
      border-bottom: 2px solid rgba(26,46,26,0.1);
    }

    /* ── Product grid ───────────────────────────────────────────────── */
    .product-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 1rem;
    }

    /* ── Product card ───────────────────────────────────────────────── */
    .product-card {
      background: white;
      border-radius: 14px;
      overflow: hidden;
      border: 1px solid rgba(0,0,0,0.07);
      transition: transform 0.18s, box-shadow 0.18s;
      display: flex; flex-direction: column;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 8px 24px rgba(0,0,0,0.09);
      }
    }
    .card-image {
      height: 140px;
      background: linear-gradient(135deg, #f0ede8 0%, #e8e3db 100%);
      display: grid; place-items: center;
      overflow: hidden;
    }
    .card-image--placeholder {
      /* keep the gradient only when there's no image */
    }
    .card-img {
      width: 100%; height: 100%;
      object-fit: cover;
      display: block;
    }
    .card-image-placeholder { font-size: 2.5rem; opacity: 0.4; }
    .card-info { flex: 1; }
    .card-name {
      font-size: 0.95rem; font-weight: 700; color: #1a2e1a;
      margin: 0 0 0.35rem; letter-spacing: -0.01em;
    }
    .card-desc {
      font-size: 0.8rem; color: #888; margin: 0;
      line-height: 1.5;
      display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
      overflow: hidden;
      font-family: system-ui, sans-serif;
    }
    .card-footer {
      display: flex; align-items: center; justify-content: space-between;
    }
    .card-price {
      font-size: 1.05rem; font-weight: 700; color: #1a2e1a;
      font-family: system-ui, sans-serif;
    }

    /* Add button */
    .add-btn {
      display: flex; align-items: center; gap: 0.35rem;
      padding: 0.45rem 0.875rem;
      background: #1a2e1a; color: #c8a96e;
      border: none; border-radius: 8px; cursor: pointer;
      font-size: 0.8rem; font-weight: 700;
      font-family: system-ui, sans-serif;
      transition: background 0.15s, transform 0.1s;
      letter-spacing: 0.02em;
      span { font-size: 1rem; line-height: 1; }
      &:hover { background: #243d24; }
      &:active { transform: scale(0.97); }
    }

    /* Qty control (replaces add button once item in cart) */
    .qty-control {
      display: flex; align-items: center; gap: 0.5rem;
    }
    .qty-btn {
      width: 30px; height: 30px;
      background: #f0ede8; border: 1px solid rgba(0,0,0,0.1);
      border-radius: 7px; cursor: pointer;
      font-size: 1.1rem; font-weight: 600; color: #1a2e1a;
      display: grid; place-items: center;
      transition: background 0.1s;
      &:hover { background: #e5e0d8; }
    }
    .qty-btn-add {
      background: #1a2e1a; color: #c8a96e; border-color: #1a2e1a;
      &:hover { background: #243d24; }
    }
    .qty-display {
      min-width: 20px; text-align: center;
      font-size: 0.9rem; font-weight: 700; color: #1a2e1a;
      font-family: system-ui, sans-serif;
    }

    /* ── Floating cart bar (mobile) ─────────────────────────────────── */
    .floating-cart {
      position: fixed; bottom: 1.25rem; left: 1rem; right: 1rem;
      z-index: 200;
      max-width: 500px; margin: 0 auto;
      cursor: pointer;
    }
    .floating-cart-inner {
      background: #1a2e1a;
      border-radius: 14px;
      padding: 1rem 1.25rem;
      display: flex; align-items: center; justify-content: space-between;
      box-shadow: 0 8px 32px rgba(0,0,0,0.25);
      transition: background 0.15s, transform 0.1s;
    }
    .floating-cart:hover .floating-cart-inner {
      background: #243d24;
      transform: translateY(-1px);
    }
    .floating-cart:active .floating-cart-inner {
      transform: scale(0.99);
    }
    .floating-qty {
      font-size: 0.8rem; color: rgba(255,255,255,0.55);
      font-family: system-ui, sans-serif;
      background: rgba(255,255,255,0.1);
      padding: 0.2rem 0.6rem; border-radius: 999px;
    }
    .floating-label {
      font-size: 0.95rem; font-weight: 700; color: white;
      font-family: system-ui, sans-serif;
    }
    .floating-total {
      font-size: 1rem; font-weight: 700; color: #c8a96e;
      font-family: system-ui, sans-serif;
    }

    /* ── Responsive ─────────────────────────────────────────────────── */
    @media (max-width: 768px) {
      .menu-body {
        grid-template-columns: 1fr;
        padding: 0 0 6rem;
      }
      .category-nav {
        position: sticky; top: 60px;
        flex-direction: row; overflow-x: auto;
        padding: 0.75rem 1rem;
        background: white;
        border-bottom: 1px solid rgba(0,0,0,0.07);
        gap: 0.5rem;
        scrollbar-width: none;
        &::-webkit-scrollbar { display: none; }
        z-index: 10;
      }
      .cat-btn {
        flex-shrink: 0;
        white-space: nowrap;
        padding: 0.45rem 0.875rem;
        border: 1px solid rgba(0,0,0,0.1);
        &.active { border-color: #1a2e1a; }
      }
      .sections { padding: 1.25rem 1rem; }
      .product-grid { grid-template-columns: 1fr; }
      .card-image { height: 100px; }
    }
  `],
})
export class MenuPage implements OnInit, AfterViewInit {
  private readonly catalogApi = inject(CatalogApi);
  protected readonly cart     = inject(CartService);

  protected readonly categories      = signal<CatalogCategoryDto[]>([]);
  protected readonly loading         = signal(true);
  protected readonly error           = signal<string | null>(null);
  protected readonly activeCategoryId = signal<number | null>(null);

  /**
   * Falls back to the placeholder emoji if the image URL 404s or fails to load.
   * Sets imageUrl to null on the product so the @if switches to the placeholder branch.
   */
  protected onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.style.display = 'none';
    // Show the parent's placeholder state
    img.closest('.card-image')?.classList.add('card-image--placeholder');
    const placeholder = document.createElement('span');
    placeholder.className = 'card-image-placeholder';
    placeholder.textContent = '🍽️';
    img.parentElement?.appendChild(placeholder);
  }

  @ViewChildren('categorySection')
  private sectionRefs!: QueryList<ElementRef<HTMLElement>>;

  private observer?: IntersectionObserver;

  ngOnInit(): void { this.load(); }

  ngAfterViewInit(): void {
    // Set up intersection observer to track which category is in view
    this.observer = new IntersectionObserver(
      entries => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            const id = Number((entry.target as HTMLElement).dataset['categoryId']);
            if (id) this.activeCategoryId.set(id);
          }
        }
      },
      { rootMargin: '-20% 0px -70% 0px', threshold: 0 }
    );

    this.sectionRefs.changes.subscribe(() => this.observeSections());
    this.observeSections();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.catalogApi.listCategories().subscribe({
      next: cats => {
        const withProducts = cats.filter(c => c.products.length > 0);
        this.categories.set(withProducts);
        if (withProducts.length > 0) this.activeCategoryId.set(withProducts[0].id);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar la carta. Comprueba tu conexión e inténtalo de nuevo.');
        this.loading.set(false);
      },
    });
  }

  protected scrollToCategory(categoryId: number): void {
    this.activeCategoryId.set(categoryId);
    const el = this.sectionRefs?.find(
      ref => Number(ref.nativeElement.dataset['categoryId']) === categoryId
    );
    el?.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  protected formatPrice(cents: number): string {
    return (cents / 100).toLocaleString('es-ES', { style: 'currency', currency: 'EUR' });
  }

  private observeSections(): void {
    if (!this.observer) return;
    this.sectionRefs.forEach(ref => this.observer!.observe(ref.nativeElement));
  }
}
