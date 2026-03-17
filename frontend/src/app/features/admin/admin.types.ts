// src/app/features/admin/admin.types.ts

export type UserRole = 'ADMIN' | 'MANAGER' | 'STAFF' | 'CUSTOMER';

export interface UserResponse {
  id: number;
  email: string;
  firstName: string | null;
  lastName: string | null;
  phone: string | null;
  role: UserRole;
  active: boolean;
  createdAt: string;
  lastLoginAt: string | null;
}

export interface CreateUserRequest {
  email: string;
  role: UserRole;
  active: boolean;
}

export interface AdminChangeEmailRequest {
  newEmail: string;
}

export interface UserListParams {
  role?: UserRole;
  active?: boolean;
  q?: string;
  page?: number;
  size?: number;
  sort?: string;
}

// ── Manager types ──────────────────────────────────────────────────────────

export interface ProductResponse {
  id: number;
  name: string;
  description: string | null;
  priceCents: number;
  active: boolean;
  categoryId: number | null;
  categoryName: string | null;
  discontinuedAt: string | null;
}

export interface CreateProductRequest {
  name: string;
  description?: string | null;
  priceCents: number;
  categoryId?: number | null;
}

export interface UpdateProductRequest {
  name?: string;
  description?: string | null;
  priceCents?: number;
  categoryId?: number | null;
}

export interface CategoryResponse {
  id: number;
  name: string;
}

export interface CreateCategoryRequest {
  name: string;
}

export interface UpdateCategoryRequest {
  name: string;
}

export interface TableResponse {
  id: number;
  label: string;
}

export interface CreateTableRequest {
  label: string;
}

export interface UpdateTableRequest {
  label: string;
}
