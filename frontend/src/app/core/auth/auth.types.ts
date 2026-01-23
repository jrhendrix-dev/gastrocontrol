export type ApiResponse<T> = {
  ok: boolean;
  message: string;
  data: T;
};

export type LoginRequest = {
  email: string;
  password: string;
};

export type LoginResponse = {
  accessToken: string;
  // include any other fields your DTO returns (e.g. expiresIn)
};

export type RefreshResponse = {
  accessToken: string;
};

export type MeResponse = {
  id: number;
  email: string;
  role: 'ADMIN' | 'MANAGER' | 'STAFF' | 'CUSTOMER' | string;
  active: boolean;
  firstName: string | null;
  lastName: string | null;
  phone: string | null;
  createdAt: string;
  lastLoginAt: string | null;
};


