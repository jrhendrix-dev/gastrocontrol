import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { ToastService } from '../../../core/ui/toast/toast.service';

@Component({
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="mx-auto max-w-xl px-4 py-10 text-center">
      <h1 class="text-2xl font-semibold">Confirmando cambio de email…</h1>

      <p class="mt-3 opacity-75" *ngIf="status() === 'loading'">
        Un momento, estamos validando tu enlace.
      </p>

      <div
        *ngIf="status() === 'ok'"
        class="mt-6 rounded-md border border-emerald-300 bg-emerald-50 p-4"
      >
        Email actualizado correctamente. Redirigiendo a inicio de sesión…
      </div>

      <div
        *ngIf="status() === 'error'"
        class="mt-6 rounded-md border border-red-300 bg-red-50 p-4"
      >
        No se pudo confirmar el cambio de email.
      </div>

      <div class="mt-6">
        <button class="rounded-md border px-4 py-2" (click)="goLogin()">
          Ir a login
        </button>
      </div>
    </div>
  `,
})
export class ConfirmEmailChangePage {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private auth = inject(AuthService);
  private toast = inject(ToastService);

  status = signal<'loading' | 'ok' | 'error'>('loading');

  constructor() {
    const token = (this.route.snapshot.queryParamMap.get('token') ?? '').trim();

    if (!token) {
      this.status.set('error');
      this.toast.error('Token inválido');
      return;
    }

    // If not logged in, redirect to login while preserving token.
    if (!this.auth.loggedIn()) {
      const returnUrl = this.router
        .createUrlTree(['/confirm-email-change'], { queryParams: { token } })
        .toString();

      void this.router.navigate(['/login'], { queryParams: { returnUrl } });
      return;
    }

    // Logged in -> confirm via JSON body { token }
    this.auth
      .confirmEmailChange({ token })
      .pipe(finalize(() => void 0))
      .subscribe({
        next: (res: any) => {
          this.status.set('ok');

          // Backend revokes refresh tokens after email change.
          // Best UX: force re-login with the new email.
          this.toast.success('Email actualizado. Vuelve a iniciar sesión.');

          this.auth.clearAuthLocal();

          // Optional: prefill login email if your login page supports it.
          // If you don't handle it yet, keep only navigate(['/login']).
          const newEmail: string | undefined = this.extractNewEmailFromTokenlessWorld();

          void this.router.navigate(['/login'], {
            queryParams: newEmail ? { email: newEmail } : undefined,
          });
        },
        error: () => {
          this.status.set('error');
          this.toast.error('No se pudo confirmar el cambio de email');
        },
      });
  }

  goLogin() {
    void this.router.navigateByUrl('/login');
  }

  /**
   * We don't actually have the new email in this page unless you:
   * - store it somewhere client-side during request, or
   * - include it in the confirm response (not recommended).
   * So by default we return undefined.
   */
  private extractNewEmailFromTokenlessWorld(): string | undefined {
    return undefined;
  }
}
