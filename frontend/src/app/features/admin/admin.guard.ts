// src/app/features/admin/admin.guard.ts
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '@app/app/core/auth/auth.service';

/**
 * Route guard that allows access to the admin panel for MANAGER and ADMIN roles only.
 *
 * Redirects unauthenticated users to /login and unauthorised users to /.
 */
export const adminGuard: CanActivateFn = () => {
  const auth   = inject(AuthService);
  const router = inject(Router);

  if (!auth.loggedIn()) {
    return router.createUrlTree(['/login']);
  }

  const roles = auth.roles();
  const allowed = roles.includes('ROLE_ADMIN') || roles.includes('ROLE_MANAGER');
  if (!allowed) {
    return router.createUrlTree(['/']);
  }

  return true;
};
