export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data?: T;
  timestamp?: string;
}

export interface ApiErrorResponse {
  error: {
    code: string;
    details?: Record<string, string>;
  };
}
