// src/app/features/admin/admin-api.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '@app/environment/environment';
import { map, Observable } from 'rxjs';
import {
  AdminChangeEmailRequest,
  CategoryResponse,
  CreateCategoryRequest,
  CreateProductRequest,
  CreateTableRequest,
  CreateUserRequest,
  ProductResponse,
  TableResponse,
  UpdateCategoryRequest,
  UpdateProductRequest,
  UpdateTableRequest,
  UserListParams,
  UserResponse,
} from './admin.types';

/** Minimal wrapper around the paginated API response shape. */
interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

/** Generic success envelope returned by the API. */
interface ApiResponse<T> {
  ok: boolean;
  message: string;
  data: T;
}

/**
 * Centralised HTTP client for all admin and manager endpoints.
 *
 * Follows the same thin-client pattern used by `StaffOrdersListApi`:
 * maps the API envelope so consumers receive the payload directly.
 */
@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private readonly http = inject(HttpClient);
  private readonly API = environment.apiBase;

  // ── Users (ADMIN only) ──────────────────────────────────────────────────

  /**
   * Lists users with optional filters.
   */
  listUsers(params: UserListParams = {}): Observable<PagedResponse<UserResponse>> {
    let httpParams = new HttpParams();
    if (params.role != null)   httpParams = httpParams.set('role',   params.role);
    if (params.active != null) httpParams = httpParams.set('active', String(params.active));
    if (params.q != null && params.q.trim())
      httpParams = httpParams.set('q',      params.q.trim());
    httpParams = httpParams
      .set('page', String(params.page  ?? 0))
      .set('size', String(params.size  ?? 20))
      .set('sort', params.sort ?? 'createdAt,desc');

    return this.http.get<PagedResponse<UserResponse>>(
      `${this.API}/api/admin/users`,
      { params: httpParams }
    );
  }

  /** Creates a new user and triggers the invite email. */
  createUser(req: CreateUserRequest): Observable<void> {
    return this.http
      .post<ApiResponse<void>>(`${this.API}/api/admin/users`, req)
      .pipe(map(() => void 0));
  }

  /** Deactivates a user account. */
  deactivateUser(id: number): Observable<void> {
    return this.http
      .post<ApiResponse<void>>(`${this.API}/api/admin/users/${id}/actions/deactivate`, {})
      .pipe(map(() => void 0));
  }

  /** Reactivates a user account. */
  reactivateUser(id: number): Observable<void> {
    return this.http
      .post<ApiResponse<void>>(`${this.API}/api/admin/users/${id}/actions/reactivate`, {})
      .pipe(map(() => void 0));
  }

  /** Admin override to change a user's email. */
  changeUserEmail(id: number, req: AdminChangeEmailRequest): Observable<void> {
    return this.http
      .put<ApiResponse<void>>(`${this.API}/api/admin/users/${id}/email`, req)
      .pipe(map(() => void 0));
  }

  /** Forces a password-reset email to be sent. */
  forcePasswordReset(id: number): Observable<void> {
    return this.http
      .post<ApiResponse<void>>(`${this.API}/api/admin/users/${id}/force-password-reset`, {})
      .pipe(map(() => void 0));
  }

  /** Resends the set-password invite email. */
  resendInvite(id: number): Observable<void> {
    return this.http
      .post<ApiResponse<void>>(`${this.API}/api/admin/users/${id}/resend-invite`, {})
      .pipe(map(() => void 0));
  }

  // ── Products (MANAGER+) ─────────────────────────────────────────────────

  listProducts(active?: boolean, page = 0, size = 50): Observable<PagedResponse<ProductResponse>> {
    let httpParams = new HttpParams()
      .set('page', String(page))
      .set('size', String(size));
    if (active != null) httpParams = httpParams.set('active', String(active));
    return this.http.get<PagedResponse<ProductResponse>>(
      `${this.API}/api/manager/products`,
      { params: httpParams }
    );
  }

  createProduct(req: CreateProductRequest): Observable<ProductResponse> {
    return this.http
      .post<ApiResponse<ProductResponse>>(`${this.API}/api/manager/products`, req)
      .pipe(map(r => r.data));
  }

  updateProduct(id: number, req: UpdateProductRequest): Observable<ProductResponse> {
    return this.http
      .patch<ApiResponse<ProductResponse>>(`${this.API}/api/manager/products/${id}`, req)
      .pipe(map(r => r.data));
  }

  discontinueProduct(id: number): Observable<void> {
    return this.http
      .post<ApiResponse<void>>(`${this.API}/api/manager/products/${id}/actions/discontinue`, {})
      .pipe(map(() => void 0));
  }

  reactivateProduct(id: number): Observable<void> {
    return this.http
      .post<ApiResponse<void>>(`${this.API}/api/manager/products/${id}/actions/reactivate`, {})
      .pipe(map(() => void 0));
  }

  // ── Categories (MANAGER+) ───────────────────────────────────────────────

  listCategories(): Observable<CategoryResponse[]> {
    return this.http
      .get<ApiResponse<CategoryResponse[]>>(`${this.API}/api/manager/categories`)
      .pipe(map(r => r.data));
  }

  createCategory(req: CreateCategoryRequest): Observable<CategoryResponse> {
    return this.http
      .post<ApiResponse<CategoryResponse>>(`${this.API}/api/manager/categories`, req)
      .pipe(map(r => r.data));
  }

  updateCategory(id: number, req: UpdateCategoryRequest): Observable<CategoryResponse> {
    return this.http
      .patch<ApiResponse<CategoryResponse>>(`${this.API}/api/manager/categories/${id}`, req)
      .pipe(map(r => r.data));
  }

  deleteCategory(id: number): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`${this.API}/api/manager/categories/${id}`)
      .pipe(map(() => void 0));
  }

  // ── Tables (MANAGER+) ───────────────────────────────────────────────────

  listTables(): Observable<TableResponse[]> {
    return this.http
      .get<ApiResponse<TableResponse[]>>(`${this.API}/api/manager/tables`)
      .pipe(map(r => r.data));
  }

  createTable(req: CreateTableRequest): Observable<TableResponse> {
    return this.http
      .post<ApiResponse<TableResponse>>(`${this.API}/api/manager/tables`, req)
      .pipe(map(r => r.data));
  }

  updateTable(id: number, req: UpdateTableRequest): Observable<TableResponse> {
    return this.http
      .patch<ApiResponse<TableResponse>>(`${this.API}/api/manager/tables/${id}`, req)
      .pipe(map(r => r.data));
  }

  deleteTable(id: number): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`${this.API}/api/manager/tables/${id}`)
      .pipe(map(() => void 0));
  }
}
