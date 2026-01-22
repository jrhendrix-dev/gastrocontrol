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
  id: string | number;
  email: string;
  firstName?: string;
  lastName?: string;
  roles: string[];
};
