import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { DemoSessionStore } from '../demo/demo-session.store';

const STORAGE_KEY = 'gc_demo_session';

export const baseUrlInterceptor: HttpInterceptorFn = (req, next) => {
  const demoStore = inject(DemoSessionStore);

  // Read from signal first, fall back to localStorage directly
  // to handle the case where the signal hasn't propagated yet
  let sessionId = demoStore.sessionId();
  if (!sessionId) {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) {
        const parsed = JSON.parse(raw);
        if (parsed?.sessionId && new Date(parsed.expiresAt) > new Date()) {
          sessionId = parsed.sessionId;
        }
      }
    } catch { /* ignore */ }
  }

  let outReq = req;

  // Prepend /gastrocontrol to relative /api/ calls
  if (req.url.startsWith('/api/') && !req.url.includes('/gastrocontrol')) {
    outReq = outReq.clone({ url: `/gastrocontrol${req.url}` });
  }

  // Attach demo session header if active
  if (sessionId) {
    outReq = outReq.clone({
      setHeaders: { 'X-Demo-Session': sessionId },
    });
  }

  return next(outReq);
};
