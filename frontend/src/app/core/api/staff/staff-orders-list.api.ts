// src/app/core/api/staff/staff-orders-list.api.ts
import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PagedResponse } from '../types/paged-response';
import { OrderResponse, OrderStatus, OrderType } from './staff.models';

/**
 * Query parameters accepted by GET /api/staff/orders.
 * All fields are optional.
 */
export interface ListOrdersParams {
  /** Comma-separated status values, e.g. 'PENDING,IN_PREPARATION'. */
  status?: string;
  /** Filter by order type. */
  type?: OrderType;
  /** Filter by table id (DINE_IN only). */
  tableId?: number;
  /** ISO-8601 lower bound for createdAt (when order was placed). */
  createdFrom?: string;
  /** ISO-8601 upper bound for createdAt. */
  createdTo?: string;
  /**
   * ISO-8601 lower bound for closedAt (when order was FINISHED/CANCELLED).
   * Use this for revenue dashboards — captures orders finalized today
   * regardless of when they were originally placed.
   */
  closedFrom?: string;
  /** ISO-8601 upper bound for closedAt. */
  closedTo?: string;
  /** Zero-based page index. Defaults to 0. */
  page?: number;
  /** Page size. Defaults to 20. */
  size?: number;
  /** Sort expression, e.g. 'createdAt,desc'. */
  sort?: string;
}

/**
 * HTTP API client for the Orders List feature.
 */
@Injectable({ providedIn: 'root' })
export class StaffOrdersListApi {
  private readonly http = inject(HttpClient);

  /**
   * Fetches a paginated, filtered list of orders.
   *
   * @param params filter and pagination options
   * @returns a paged response containing matching OrderResponse objects
   */
  list(params: ListOrdersParams = {}): Observable<PagedResponse<OrderResponse>> {
    let httpParams = new HttpParams();

    if (params.status)      httpParams = httpParams.set('status',      params.status);
    if (params.type)        httpParams = httpParams.set('type',        params.type);
    if (params.tableId)     httpParams = httpParams.set('tableId',     String(params.tableId));
    if (params.createdFrom) httpParams = httpParams.set('createdFrom', params.createdFrom);
    if (params.createdTo)   httpParams = httpParams.set('createdTo',   params.createdTo);
    if (params.closedFrom)  httpParams = httpParams.set('closedFrom',  params.closedFrom);
    if (params.closedTo)    httpParams = httpParams.set('closedTo',    params.closedTo);

    httpParams = httpParams
      .set('page', String(params.page ?? 0))
      .set('size', String(params.size ?? 20))
      .set('sort', params.sort ?? 'createdAt,desc');

    return this.http.get<PagedResponse<OrderResponse>>('/api/staff/orders', { params: httpParams });
  }
}
