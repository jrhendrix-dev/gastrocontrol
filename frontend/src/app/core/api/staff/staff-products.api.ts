import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PagedResponse } from '../types/paged-response';
import { ProductResponse } from './staff.models';

@Injectable({ providedIn: 'root' })
export class StaffProductsApi {
  private readonly http = inject(HttpClient);

  list(params: {
    active?: boolean;
    categoryId?: number;
    q?: string;
    page?: number;
    size?: number;
    sort?: string;
  }): Observable<PagedResponse<ProductResponse>> {
    let httpParams = new HttpParams();
    if (params.active !== undefined) httpParams = httpParams.set('active', String(params.active));
    if (params.categoryId !== undefined) httpParams = httpParams.set('categoryId', String(params.categoryId));
    if (params.q) httpParams = httpParams.set('q', params.q);
    httpParams = httpParams.set('page', String(params.page ?? 0));
    httpParams = httpParams.set('size', String(params.size ?? 25));
    httpParams = httpParams.set('sort', params.sort ?? 'id,asc');

    return this.http.get<PagedResponse<ProductResponse>>('/api/staff/products', { params: httpParams });
  }
}
