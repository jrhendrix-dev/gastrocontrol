// src/app/core/api/staff/staff-orders.api.ts
import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiResponse } from '../types/api-response';
import {
  AddOrderItemRequest,
  CreateDraftOrderRequest,
  OrderResponse,
  UpdateOrderItemQuantityRequest,
} from './staff.models';

@Injectable({ providedIn: 'root' })
export class StaffOrdersApi {
  private readonly http = inject(HttpClient);

  getById(orderId: number): Observable<OrderResponse> {
    return this.http.get<OrderResponse>(`/api/staff/orders/${orderId}`);
  }

  createDraft(req: CreateDraftOrderRequest): Observable<OrderResponse> {
    return this.http.post<OrderResponse>('/api/staff/orders/drafts', req);
  }

  addItem(orderId: number, req: AddOrderItemRequest): Observable<OrderResponse> {
    return this.http.post<OrderResponse>(`/api/staff/orders/${orderId}/items`, req);
  }

  updateItemQuantity(orderId: number, itemId: number, req: UpdateOrderItemQuantityRequest): Observable<OrderResponse> {
    return this.http.patch<OrderResponse>(`/api/staff/orders/${orderId}/items/${itemId}`, req);
  }

  removeItem(orderId: number, itemId: number): Observable<OrderResponse> {
    return this.http.delete<OrderResponse>(`/api/staff/orders/${orderId}/items/${itemId}`);
  }

  submit(orderId: number): Observable<void> {
    return this.http
      .post<ApiResponse<unknown>>(`/api/staff/orders/${orderId}/actions/submit`, {})
      .pipe(map(() => void 0));
  }

  /**
   * Cancels an order (STAFF only allowed for DRAFT/PENDING).
   */
  cancel(orderId: number): Observable<void> {
    return this.http
      .post<ApiResponse<unknown>>(`/api/staff/orders/${orderId}/actions/cancel`, {})
      .pipe(map(() => void 0));
  }
}
