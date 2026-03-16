// src/app/core/api/staff/staff-payment.api.ts
import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, switchMap } from 'rxjs';
import { ApiResponse } from '../types/api-response';
import { StaffOrdersApi } from './staff-orders.api';
import { OrderResponse } from './staff.models';

export interface ConfirmManualPaymentRequest {
  manualReference?: string | null;
}

export interface ConfirmManualPaymentResponse {
  orderId: number;
  provider: string;
  status: string;
  amountCents: number;
  currency: string;
  manualReference: string | null;
}

/**
 * API client for the POS payment and order-finalization flow.
 *
 * <p>Encapsulates the three-step sequence that closes a dine-in order:</p>
 * <ol>
 *   <li>Confirm manual payment → POST /api/staff/payments/orders/{id}/confirm-manual</li>
 *   <li>Finalize order status → PATCH /api/staff/orders/{id}/status (FINISHED)</li>
 *   <li>Reload full order → GET /api/staff/orders/{id}</li>
 * </ol>
 *
 * <p>Step 3 is necessary because the PATCH /status endpoint returns
 * ApiResponse&lt;ChangeOrderStatusResult&gt; — a slim result DTO, NOT a full
 * OrderResponse. Attempting to use it as one causes the POS signal to receive
 * an incomplete object and the loading spinner to never clear.
 * Reloading via getById guarantees a complete, server-authoritative object.</p>
 */
@Injectable({ providedIn: 'root' })
export class StaffPaymentApi {
  private readonly http      = inject(HttpClient);
  private readonly ordersApi = inject(StaffOrdersApi);

  /**
   * Confirms a manual payment (cash, card, Bizum) for the given order.
   *
   * @param orderId the order to confirm payment for
   * @param req     optional manual reference (method label)
   * @returns the confirm response including amount and provider
   */
  confirmManual(
    orderId: number,
    req: ConfirmManualPaymentRequest = {}
  ): Observable<ConfirmManualPaymentResponse> {
    return this.http.post<ConfirmManualPaymentResponse>(
      `/api/staff/payments/orders/${orderId}/confirm-manual`,
      req
    );
  }

  /**
   * Advances a SERVED order to FINISHED, then reloads the full order.
   *
   * <p>The PATCH /status endpoint returns ApiResponse&lt;ChangeOrderStatusResult&gt;,
   * not an OrderResponse. We discard that response and immediately call
   * GET /orders/{id} to get the complete, up-to-date order the POS needs.</p>
   *
   * @param orderId the order to finalize
   * @returns the reloaded OrderResponse with status FINISHED
   */
  finalize(orderId: number): Observable<OrderResponse> {
    return this.http
      .patch<ApiResponse<unknown>>(`/api/staff/orders/${orderId}/status`, { newStatus: 'FINISHED' })
      .pipe(
        switchMap(() => this.ordersApi.getById(orderId))
      );
  }

  /**
   * Convenience: confirms payment, finalizes status, and reloads the order
   * as a single Observable chain.
   *
   * @param orderId         the order to pay and close
   * @param manualReference human-readable method label stored on the payment row
   * @returns the finalized OrderResponse reloaded from the server
   */
  confirmAndFinalize(orderId: number, manualReference: string): Observable<OrderResponse> {
    return this.confirmManual(orderId, { manualReference }).pipe(
      switchMap(() => this.finalize(orderId))
    );
  }
}
