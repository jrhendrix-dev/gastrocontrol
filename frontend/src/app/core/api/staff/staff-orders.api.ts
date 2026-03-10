// src/app/core/api/staff/staff-orders.api.ts
import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiResponse } from '../types/api-response';
import {
  AddOrderItemRequest,
  AddOrderNoteRequest,
  CreateDraftOrderRequest,
  OrderResponse,
  UpdateOrderItemQuantityRequest,
} from './staff.models';

/**
 * HTTP client for the staff order endpoints.
 *
 * <p>All methods return typed Observables that complete after a single emission.
 * Error handling (toast/field errors) is the responsibility of the calling component.</p>
 */
@Injectable({ providedIn: 'root' })
export class StaffOrdersApi {
  private readonly http = inject(HttpClient);

  /** Fetches a single order by id, including its items and notes. */
  getById(orderId: number): Observable<OrderResponse> {
    return this.http.get<OrderResponse>(`/api/staff/orders/${orderId}`);
  }

  /** Creates a new DRAFT order (POS ticket). */
  createDraft(req: CreateDraftOrderRequest): Observable<OrderResponse> {
    return this.http.post<OrderResponse>('/api/staff/orders/drafts', req);
  }

  /** Adds a product to an existing order, merging quantity if already present. */
  addItem(orderId: number, req: AddOrderItemRequest): Observable<OrderResponse> {
    return this.http.post<OrderResponse>(`/api/staff/orders/${orderId}/items`, req);
  }

  /** Updates the quantity of a specific order line item. */
  updateItemQuantity(orderId: number, itemId: number, req: UpdateOrderItemQuantityRequest): Observable<OrderResponse> {
    return this.http.patch<OrderResponse>(`/api/staff/orders/${orderId}/items/${itemId}`, req);
  }

  /** Removes a specific line item from the order. */
  removeItem(orderId: number, itemId: number): Observable<OrderResponse> {
    return this.http.delete<OrderResponse>(`/api/staff/orders/${orderId}/items/${itemId}`);
  }

  /** Submits a DRAFT order to the kitchen (DRAFT → PENDING). */
  submit(orderId: number): Observable<void> {
    return this.http
      .post<ApiResponse<unknown>>(`/api/staff/orders/${orderId}/actions/submit`, {})
      .pipe(map(() => void 0));
  }

  /** Cancels an order. Only permitted for DRAFT and PENDING orders by the backend. */
  cancel(orderId: number): Observable<void> {
    return this.http
      .post<ApiResponse<unknown>>(`/api/staff/orders/${orderId}/actions/cancel`, {})
      .pipe(map(() => void 0));
  }

  /**
   * Adds a free-text note to an order.
   *
   * Notes can be added at any point during the active lifecycle.
   * The returned response includes the full updated notes list.
   *
   * @param orderId the target order
   * @param req     the note text (max 500 chars, must not be blank)
   */
  addNote(orderId: number, req: AddOrderNoteRequest): Observable<OrderResponse> {
    return this.http.post<OrderResponse>(`/api/staff/orders/${orderId}/notes`, req);
  }
}
