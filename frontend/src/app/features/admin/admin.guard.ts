import { CanActivateFn } from '@angular/router';
import { waitForAuthThen } from '../../core/auth/auth.guard.utils';

/**
 * Route guard for admin/manager routes (/admin/*).
 *
 * Allows: MANAGER, ADMIN.
 * Redirects unauthenticated users to /login.
 * Redirects unauthorised users to /.
 *
 * Waits for the auth bootstrap to complete before evaluating,
 * preventing false logouts on page refresh.
 */
export const adminGuard: CanActivateFn = () =>
  waitForAuthThen(['ROLE_ADMIN', 'ROLE_MANAGER']);
