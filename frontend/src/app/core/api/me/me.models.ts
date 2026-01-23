export type UserRole = 'ADMIN' | 'MANAGER' | 'STAFF' | 'CUSTOMER';

export interface MeResponse {
  id: number;
  email: string;
  role: UserRole;
  active: boolean;
  firstName: string | null;
  lastName: string | null;
  phone: string | null;
  createdAt: string;
  lastLoginAt: string | null;
}

export interface UpdateProfileRequest {
  firstName: string | null;
  lastName: string | null;
  phone: string | null;
}
