import { CanActivateFn } from '@angular/router';
import { waitForAuthThen } from '../../core/auth/auth.guard.utils';

/**
 * Route guard for staff-only routes (/staff/*).
 *
 * Allows: STAFF, MANAGER, ADMIN.
 * Redirects unauthenticated users to /login.
 * Redirects unauthorised users (CUSTOMER) to /.
 *
 * Waits for the auth bootstrap to complete before evaluating,
 * preventing false logouts on page refresh.
 */
export const staffGuard: CanActivateFn = () =>
  waitForAuthThen(['ROLE_STAFF', 'ROLE_MANAGER', 'ROLE_ADMIN']);
