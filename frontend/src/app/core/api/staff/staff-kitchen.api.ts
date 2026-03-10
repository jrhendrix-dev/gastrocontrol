// src/app/core/api/staff/staff-kitchen.api.ts
import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { PagedResponse } from '../types/paged-response';
import { KitchenOrderResponse, ChangeOrderStatusResult } from './kitchen.models';
import { ApiResponse } from '../types/api-response';

/**
 * API client for the Kitchen Display System (KDS).
 *
 * <p>All requests hit the existing {@code /api/staff/orders} endpoints — no
 * new backend routes are required. The kitchen board fetches only the statuses
 * it cares about and delegates status transitions to the shared change-status
 * endpoint.</p>
 */
@Injectable({ providedIn: 'root' })
export class StaffKitchenApi {
  private readonly http = inject(HttpClient);

  /**
   * Fetches active kitchen orders: PENDING, IN_PREPARATION, and READY.
   * DRAFT orders are excluded — they have not been submitted yet.
   *
   * @param page  zero-based page index
   * @param size  max number of orders per page (default 50 covers most services)
   * @returns paginated kitchen orders sorted oldest-first so urgent tickets surface at the top
   */
  listKitchenOrders(page = 0, size = 50): Observable<PagedResponse<KitchenOrderResponse>> {
    const params = new HttpParams()
      .set('status', 'PENDING,IN_PREPARATION,READY')
      .set('page', String(page))
      .set('size', String(size))
      .set('sort', 'createdAt,asc'); // oldest first — most urgent at top of each column

    return this.http.get<PagedResponse<KitchenOrderResponse>>('/api/staff/orders', { params });
  }

  /**
   * Advances an order to the next kitchen status.
   *
   * <ul>
   *   <li>PENDING → IN_PREPARATION (kitchen picks it up)</li>
   *   <li>IN_PREPARATION → READY (dish is ready for service)</li>
   *   <li>READY → SERVED (waiter has delivered)</li>
   * </ul>
   *
   * @param orderId   the order to advance
   * @param newStatus the target status
   * @returns the change result containing old and new status
   */
  advanceStatus(orderId: number, newStatus: string): Observable<ChangeOrderStatusResult> {
    return this.http
      .patch<ApiResponse<ChangeOrderStatusResult>>(
        `/api/staff/orders/${orderId}/status`,
        { newStatus }
      )
      .pipe(
        // data is always present on a successful 200 response from this endpoint.
        // The optional typing on ApiResponse<T>.data is a conservative default for
        // error shapes — the backend always populates it on success.
        map((res) => res.data as ChangeOrderStatusResult)
      );
  }
}
