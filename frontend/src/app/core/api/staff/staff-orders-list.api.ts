// src/app/core/api/staff/staff-orders-list.api.ts
import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PagedResponse } from '../types/paged-response';
import { OrderResponse, OrderStatus, OrderType } from './staff.models';

/**
 * Query parameters accepted by {@code GET /api/staff/orders}.
 *
 * <p>All fields are optional. Omitting a field means "no filter on that dimension".</p>
 */
export interface ListOrdersParams {
  /** Comma-separated status values, e.g. {@code 'PENDING,IN_PREPARATION'}. */
  status?: string;
  /** Filter by order type. */
  type?: OrderType;
  /** Filter by table id (DINE_IN only). */
  tableId?: number;
  /** ISO-8601 lower bound for {@code createdAt}. */
  createdFrom?: string;
  /** ISO-8601 upper bound for {@code createdAt}. */
  createdTo?: string;
  /** Zero-based page index. Defaults to 0. */
  page?: number;
  /** Page size. Defaults to 20. */
  size?: number;
  /** Sort expression, e.g. {@code 'createdAt,desc'}. */
  sort?: string;
}

/**
 * HTTP API client dedicated to the Orders List feature.
 *
 * <p>Wraps {@code GET /api/staff/orders} with a strongly-typed params object
 * so the page component never has to think about raw HTTP param names.</p>
 */
@Injectable({ providedIn: 'root' })
export class StaffOrdersListApi {
  private readonly http = inject(HttpClient);

  /**
   * Fetches a paginated, filtered list of orders.
   *
   * @param params filter and pagination options
   * @returns a paged response containing matching {@link OrderResponse} objects
   */
  list(params: ListOrdersParams = {}): Observable<PagedResponse<OrderResponse>> {
    let httpParams = new HttpParams();

    if (params.status)      httpParams = httpParams.set('status',      params.status);
    if (params.type)        httpParams = httpParams.set('type',        params.type);
    if (params.tableId)     httpParams = httpParams.set('tableId',     String(params.tableId));
    if (params.createdFrom) httpParams = httpParams.set('createdFrom', params.createdFrom);
    if (params.createdTo)   httpParams = httpParams.set('createdTo',   params.createdTo);

    httpParams = httpParams
      .set('page', String(params.page ?? 0))
      .set('size', String(params.size ?? 20))
      .set('sort', params.sort ?? 'createdAt,desc');

    return this.http.get<PagedResponse<OrderResponse>>('/api/staff/orders', { params: httpParams });
  }
}
