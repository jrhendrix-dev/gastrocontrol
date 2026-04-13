// src/app/core/cart/cart.service.ts
import { Injectable, computed, signal } from '@angular/core';
import { CatalogProductDto } from '../api/catalog/catalog.models';

/**
 * A single line in the customer's cart.
 */
export interface CartItem {
  product: CatalogProductDto;
  quantity: number;
}

/**
 * Internal shape persisted to localStorage.
 *
 * <p>We store a version field so we can safely migrate or discard
 * incompatible data in the future, and an {@code expiresAt} ISO timestamp
 * so stale carts (e.g. from a previous day) are discarded automatically.</p>
 */
interface PersistedCart {
  /** Schema version — bump if CartItem shape changes. */
  version: 1;
  /** ISO timestamp; cart is discarded after this point. */
  expiresAt: string;
  items: CartItem[];
}

/** localStorage key used for cart persistence. */
const STORAGE_KEY = 'gc_cart';

/** How long a persisted cart remains valid (24 hours). */
const TTL_MS = 24 * 60 * 60 * 1000;

/**
 * Signal-based cart service shared between the menu page and checkout flow.
 *
 * <p>The cart is persisted to {@code localStorage} under the key
 * {@code gc_cart} so that it survives page refreshes. Carts older than
 * 24 hours are discarded automatically to avoid stale price data.</p>
 *
 * <p>Provided at root so the same instance is shared across the
 * menu page, cart drawer, and checkout page without any extra setup.</p>
 */
@Injectable({ providedIn: 'root' })
export class CartService {

  /**
   * Controls the cart drawer visibility.
   * Shared between the menu page (opens it) and the customer shell (renders it).
   */
  readonly drawerOpen = signal(false);

  /** The raw list of cart lines, seeded from localStorage on construction. */
  private readonly _items = signal<CartItem[]>(this.loadFromStorage());

  /** Read-only view of the current cart lines. */
  readonly items = this._items.asReadonly();

  /** Total number of individual items in the cart (sum of quantities). */
  readonly totalQuantity = computed(() =>
    this._items().reduce((sum, i) => sum + i.quantity, 0)
  );

  /** Total price in cents. */
  readonly totalCents = computed(() =>
    this._items().reduce((sum, i) => sum + i.product.priceCents * i.quantity, 0)
  );

  /** True when the cart has at least one item. */
  readonly hasItems = computed(() => this._items().length > 0);

  // ── Public mutations ────────────────────────────────────────────────────

  /**
   * Adds one unit of a product to the cart.
   * If the product is already present its quantity is incremented.
   * Persists the new state to localStorage.
   *
   * @param product the product to add
   */
  add(product: CatalogProductDto): void {
    this._items.update(items => {
      const existing = items.find(i => i.product.id === product.id);
      if (existing) {
        return items.map(i =>
          i.product.id === product.id
            ? { ...i, quantity: i.quantity + 1 }
            : i
        );
      }
      return [...items, { product, quantity: 1 }];
    });
    this.persist();
  }

  /**
   * Removes one unit of a product from the cart.
   * If the quantity reaches zero the line is removed entirely.
   * Persists the new state to localStorage.
   *
   * @param productId the ID of the product to decrement
   */
  decrement(productId: number): void {
    this._items.update(items =>
      items
        .map(i => i.product.id === productId ? { ...i, quantity: i.quantity - 1 } : i)
        .filter(i => i.quantity > 0)
    );
    this.persist();
  }

  /**
   * Removes a product line from the cart entirely regardless of quantity.
   * Persists the new state to localStorage.
   *
   * @param productId the ID of the product to remove
   */
  remove(productId: number): void {
    this._items.update(items => items.filter(i => i.product.id !== productId));
    this.persist();
  }

  /**
   * Returns the current quantity of a product in the cart, or {@code 0}
   * if the product is not present.
   *
   * @param productId the ID of the product to query
   */
  quantityOf(productId: number): number {
    return this._items().find(i => i.product.id === productId)?.quantity ?? 0;
  }

  /**
   * Empties the cart and removes it from localStorage.
   * Call this after a successful order submission.
   */
  clear(): void {
    this._items.set([]);
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch { /* ignore — Safari private mode, etc. */ }
  }

  // ── Private helpers ─────────────────────────────────────────────────────

  /**
   * Writes the current cart state to localStorage.
   *
   * <p>Failures (e.g. storage quota exceeded, Safari private mode) are
   * caught and silently ignored — the cart still works in memory.</p>
   */
  private persist(): void {
    try {
      const payload: PersistedCart = {
        version: 1,
        expiresAt: new Date(Date.now() + TTL_MS).toISOString(),
        items: this._items(),
      };
      localStorage.setItem(STORAGE_KEY, JSON.stringify(payload));
    } catch { /* ignore */ }
  }

  /**
   * Attempts to restore cart items from localStorage.
   *
   * <p>Returns an empty array if:</p>
   * <ul>
   *   <li>There is nothing stored under the cart key.</li>
   *   <li>The stored data fails to parse.</li>
   *   <li>The schema version does not match.</li>
   *   <li>The cart has expired (older than {@link TTL_MS}).</li>
   * </ul>
   *
   * @returns the restored {@link CartItem} array, or {@code []}
   */
  private loadFromStorage(): CartItem[] {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return [];

      const parsed: PersistedCart = JSON.parse(raw);

      // Discard incompatible schema versions
      if (parsed.version !== 1) {
        localStorage.removeItem(STORAGE_KEY);
        return [];
      }

      // Discard expired carts
      if (new Date(parsed.expiresAt) <= new Date()) {
        localStorage.removeItem(STORAGE_KEY);
        return [];
      }

      // Basic shape validation — guard against corrupted data
      if (!Array.isArray(parsed.items)) {
        localStorage.removeItem(STORAGE_KEY);
        return [];
      }

      return parsed.items;
    } catch {
      localStorage.removeItem(STORAGE_KEY);
      return [];
    }
  }
}
