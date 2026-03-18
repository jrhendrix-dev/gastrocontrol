// src/app/features/staff/staff.guard.ts
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

/**
 * Route guard for staff-only routes (/staff/*).
 *
 * Allows: STAFF, MANAGER, ADMIN.
 * Redirects unauthenticated users to /login.
 * Redirects unauthorised users (CUSTOMER) to /.
 */
export const staffGuard: CanActivateFn = () => {
  const auth   = inject(AuthService);
  const router = inject(Router);

  if (!auth.loggedIn()) {
    return router.createUrlTree(['/login']);
  }

  const roles = auth.roles();
  const allowed =
    roles.includes('ROLE_STAFF') ||
    roles.includes('ROLE_MANAGER') ||
    roles.includes('ROLE_ADMIN');

  return allowed ? true : router.createUrlTree(['/']);
};
