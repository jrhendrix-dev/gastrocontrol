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
 * Signal-based cart service shared between the menu page and checkout flow.
 *
 * <p>The cart is held in memory for the current session. It is intentionally
 * not persisted to localStorage — if the customer refreshes, the cart resets.
 * Persistence can be added in a future phase.</p>
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

  /** The raw list of cart lines. */
  private readonly _items = signal<CartItem[]>([]);

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

  /**
   * Adds one unit of a product to the cart.
   * If the product is already in the cart its quantity is incremented.
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
  }

  /**
   * Removes one unit of a product from the cart.
   * If quantity reaches zero the line is removed entirely.
   *
   * @param productId the product to decrement
   */
  decrement(productId: number): void {
    this._items.update(items =>
      items
        .map(i => i.product.id === productId ? { ...i, quantity: i.quantity - 1 } : i)
        .filter(i => i.quantity > 0)
    );
  }

  /**
   * Removes a product line from the cart entirely regardless of quantity.
   *
   * @param productId the product to remove
   */
  remove(productId: number): void {
    this._items.update(items => items.filter(i => i.product.id !== productId));
  }

  /** Returns the current quantity of a product in the cart, or 0 if not present. */
  quantityOf(productId: number): number {
    return this._items().find(i => i.product.id === productId)?.quantity ?? 0;
  }

  /** Empties the cart completely. */
  clear(): void {
    this._items.set([]);
  }
}
