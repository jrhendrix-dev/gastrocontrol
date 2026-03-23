// src/app/core/api/customer/customer-checkout.api.ts
import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type CheckoutOrderType = 'TAKE_AWAY' | 'DELIVERY';

export interface CheckoutPickup {
  name: string;
  phone?: string | null;
  notes?: string | null;
}

export interface CheckoutDelivery {
  name: string;
  phone: string;
  addressLine1: string;
  addressLine2?: string | null;
  city: string;
  postalCode?: string | null;
  notes?: string | null;
}

export interface CheckoutItem {
  productId: number;
  quantity: number;
}

export interface CustomerCheckoutRequest {
  type: CheckoutOrderType;
  pickup?: CheckoutPickup | null;
  delivery?: CheckoutDelivery | null;
  items: CheckoutItem[];
}

export interface CustomerCheckoutResponse {
  orderId: number;
  /** Stripe Checkout URL — redirect the browser here to collect payment. */
  checkoutUrl: string;
  /** Opaque UUID for the public tracking URL: /track/{trackingToken} */
  trackingToken: string | null;
}

/**
 * HTTP client for the customer-facing checkout endpoint.
 * No authentication required.
 */
@Injectable({ providedIn: 'root' })
export class CustomerCheckoutApi {
  private readonly http = inject(HttpClient);

  /**
   * Creates an order and starts a Stripe Checkout session.
   *
   * @param req  order type, customer details, and cart items
   * @returns    the new order id and the Stripe Checkout redirect URL
   */
  startCheckout(req: CustomerCheckoutRequest): Observable<CustomerCheckoutResponse> {
    return this.http.post<CustomerCheckoutResponse>('/api/customer/orders/checkout', req);
  }

  /**
   * Creates a cash order, submits it to the kitchen immediately, and records
   * a pending MANUAL payment. No Stripe session is created.
   *
   * @param req  order type, customer details, and cart items
   * @returns    the new order id
   */
  cashCheckout(req: CustomerCheckoutRequest): Observable<{ orderId: number; trackingToken: string | null }> {
    return this.http.post<{ orderId: number; trackingToken: string | null }>('/api/customer/orders/cash-checkout', req);
  }
}
