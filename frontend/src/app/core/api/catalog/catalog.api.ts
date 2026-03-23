// src/app/core/api/catalog/catalog.api.ts
import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CatalogCategoryDto, CatalogProductDto } from './catalog.models';

/**
 * HTTP client for the public catalog endpoints.
 * No authentication required — these endpoints are open to all.
 */
@Injectable({ providedIn: 'root' })
export class CatalogApi {
  private readonly http = inject(HttpClient);

  /**
   * Returns all categories that have at least one active product,
   * with their products embedded. One call gives the full menu tree.
   */
  listCategories(): Observable<CatalogCategoryDto[]> {
    return this.http.get<CatalogCategoryDto[]>('/api/catalog/categories');
  }

  /**
   * Returns active products, optionally filtered by category.
   *
   * @param params optional filter parameters
   */
  listProducts(params?: { categoryId?: number | null }): Observable<CatalogProductDto[]> {
    let httpParams = new HttpParams();
    if (params?.categoryId != null) {
      httpParams = httpParams.set('categoryId', String(params.categoryId));
    }
    return this.http.get<CatalogProductDto[]>('/api/catalog/products', { params: httpParams });
  }
}
