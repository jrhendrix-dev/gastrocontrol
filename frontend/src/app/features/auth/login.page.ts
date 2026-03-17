// src/app/features/auth/login.page.ts
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';

/**
 * Login page.
 *
 * Error handling is intentionally kept simple and login-specific:
 * - Wrong credentials   → inline error below the form
 * - Deactivated account → inline error with contact-admin message
 * - Unexpected error    → generic inline message
 *
 * We intentionally do NOT use FormErrorMapper here because login errors
 * are authentication outcomes, not field-level validation errors.
 */
@Component({
  standalone: true,
  selector: 'gc-login-page',
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './login.page.html',
})
export class LoginPage {
  private fb     = inject(FormBuilder);
  auth           = inject(AuthService);
  private router = inject(Router);
  private route  = inject(ActivatedRoute);

  /** Inline error message shown below the form fields. */
  readonly loginError = signal<string | null>(null);

  form = this.fb.group({
    email:    ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  private getReturnUrl(): string {
    const raw = (this.route.snapshot.queryParamMap.get('returnUrl') ?? '').trim();
    if (!raw.startsWith('/')) return '/';
    if (raw.startsWith('//')) return '/';
    return raw;
  }

  submit(): void {
    if (this.form.invalid) return;
    this.loginError.set(null);

    this.auth.login(this.form.getRawValue() as any).subscribe({
      next: () => void this.router.navigateByUrl(this.getReturnUrl()),
      error: (err: HttpErrorResponse) => this.loginError.set(this.resolveLoginError(err)),
    });
  }

  /**
   * Maps a backend error response to a human-readable login message.
   *
   * Backend error shapes:
   *  - credentials key → wrong email/password (VALIDATION_FAILED)
   *  - account key     → deactivated account  (BUSINESS_RULE_VIOLATION)
   */
  private resolveLoginError(err: HttpErrorResponse): string {
    const inner   = err?.error?.error ?? err?.error ?? {};
    const details = inner?.details ?? {};

    if (details['account'] || inner?.code === 'BUSINESS_RULE_VIOLATION') {
      return 'Esta cuenta ha sido desactivada. Contacta con el administrador.';
    }

    if (details['credentials'] || inner?.code === 'VALIDATION_FAILED') {
      return 'Email o contraseña incorrectos.';
    }

    if (err.status === 0) {
      return 'No se pudo conectar con el servidor. Comprueba tu conexión.';
    }

    return 'Error al iniciar sesión. Inténtalo de nuevo.';
  }
}
