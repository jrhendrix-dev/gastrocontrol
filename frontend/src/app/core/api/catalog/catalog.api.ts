import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CatalogCategoryDto, CatalogProductDto } from './catalog.models';

@Injectable({ providedIn: 'root' })
export class CatalogApi {
  private readonly http = inject(HttpClient);

  listCategories(): Observable<CatalogCategoryDto[]> {
    return this.http.get<CatalogCategoryDto[]>('/api/catalog/categories');
  }

  listProducts(params?: { categoryId?: number | null }): Observable<CatalogProductDto[]> {
    let httpParams = new HttpParams();
    if (params?.categoryId != null) httpParams = httpParams.set('categoryId', String(params.categoryId));
    return this.http.get<CatalogProductDto[]>('/api/catalog/products', { params: httpParams });
  }
}
