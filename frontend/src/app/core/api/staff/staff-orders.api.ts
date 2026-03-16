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
  UpdateOrderNoteRequest,
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
  updateItemQuantity(
    orderId: number,
    itemId: number,
    req: UpdateOrderItemQuantityRequest
  ): Observable<OrderResponse> {
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

  /**
   * Edits the text of an existing note.
   *
   * The backend preserves the original text on the first edit (audit trail).
   * Editing is permitted regardless of order status.
   *
   * @param orderId the order the note belongs to
   * @param noteId  the note to edit
   * @param req     the replacement text (max 500 chars, must not be blank)
   */
  updateNote(orderId: number, noteId: number, req: UpdateOrderNoteRequest): Observable<OrderResponse> {
    return this.http.patch<OrderResponse>(`/api/staff/orders/${orderId}/notes/${noteId}`, req);
  }

  /**
   * Deletes a note from an order.
   *
   * Only permitted while the order is DRAFT or PENDING.
   * The backend rejects the request with 422 once the order reaches IN_PREPARATION.
   *
   * @param orderId the order the note belongs to
   * @param noteId  the note to delete
   */
  deleteNote(orderId: number, noteId: number): Observable<OrderResponse> {
    return this.http.delete<OrderResponse>(`/api/staff/orders/${orderId}/notes/${noteId}`);
  }

  /**
   * Changes the status of an order.
   *
   * <p>Used by the payment flow to finalize a SERVED order (SERVED → FINISHED).
   * The backend enforces all transition rules and the payment gate — a 422 is
   * returned if the payment is not yet SUCCEEDED.</p>
   *
   * @param orderId   the order to transition
   * @param newStatus the target status string
   * @returns the updated {@link OrderResponse}
   */
  changeStatus(orderId: number, newStatus: string): Observable<OrderResponse> {
    return this.http
      .patch<ApiResponse<OrderResponse>>(`/api/staff/orders/${orderId}/status`, { newStatus })
      .pipe(map((res) => res.data as OrderResponse));
  }
}
