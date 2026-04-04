import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Prepends /gastrocontrol to all relative /api/ requests.
 * Skips requests that already contain /gastrocontrol (e.g. from AuthService).
 * Skips absolute URLs (http/https).
 */
export const baseUrlInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.url.startsWith('/api/') && !req.url.includes('/gastrocontrol')) {
    return next(req.clone({ url: `/gastrocontrol${req.url}` }));
  }
  return next(req);
};
