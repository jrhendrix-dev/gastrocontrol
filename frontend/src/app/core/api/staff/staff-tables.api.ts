import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PagedResponse } from '../types/paged-response';
import { DiningTableResponse } from './staff.models';

@Injectable({ providedIn: 'root' })
export class StaffTablesApi {
  private readonly http = inject(HttpClient);

  list(params: {
    q?: string;
    page?: number;
    size?: number;
    sort?: string;
    includeActiveOrder?: boolean;
  }): Observable<PagedResponse<DiningTableResponse>> {
    let httpParams = new HttpParams();
    if (params.q) httpParams = httpParams.set('q', params.q);
    httpParams = httpParams.set('page', String(params.page ?? 0));
    httpParams = httpParams.set('size', String(params.size ?? 50));
    httpParams = httpParams.set('sort', params.sort ?? 'id,asc');
    if (params.includeActiveOrder) httpParams = httpParams.set('include', 'activeOrder');

    return this.http.get<PagedResponse<DiningTableResponse>>('/api/staff/tables', { params: httpParams });
  }
}
