import { inject } from '@angular/core';
import { Router, UrlTree } from '@angular/router';
import { toObservable } from '@angular/core/rxjs-interop';
import { filter, map, take } from 'rxjs';
import { AuthService } from './auth.service';

/**
 * Waits for the auth bootstrap to complete before evaluating
 * whether the user is logged in and has the required roles.
 *
 * @param allowedRoles - ROLE_XXX strings that are permitted. If empty, any authenticated user passes.
 * @param fallbackUrl  - Where to redirect unauthorised (but authenticated) users.
 */
export function waitForAuthThen(
  allowedRoles: string[],
  fallbackUrl: string = '/'
) {
  const auth   = inject(AuthService);
  const router = inject(Router);

  return toObservable(auth.authReady).pipe(
    filter(ready => ready),
    take(1),
    map((): boolean | UrlTree => {
      // TEMPORARY DEBUG - remove after fix
      console.log('[Guard] authReady fired');
      console.log('[Guard] loggedIn:', auth.loggedIn());
      console.log('[Guard] roles:', auth.roles());
      console.log('[Guard] _me signal:', auth.meSig());
      console.log('[Guard] allowedRoles:', allowedRoles);

      if (!auth.loggedIn()) {
        return router.createUrlTree(['/login']);
      }

      if (allowedRoles.length === 0) return true;

      const hasRole = auth.roles().some(r => allowedRoles.includes(r));
      console.log('[Guard] hasRole:', hasRole);
      return hasRole ? true : router.createUrlTree([fallbackUrl]);
    })
  );
}
