// src/app/features/auth/reset-password.page.ts
import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '@app/environment/environment';

type PageState = 'form' | 'loading' | 'success' | 'error';

/**
 * Reset-password page — accepts the reset token from the URL query param
 * and calls POST /api/auth/reset-password to set a new password.
 */
@Component({
  selector: 'app-reset-password-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <section class="auth-page">
      <div class="auth-card gc-card">

        <div class="auth-brand">GastroControl</div>

        @switch (state()) {

          @case ('form') {
            <h1 class="auth-title">Restablecer contraseña</h1>
            <p class="auth-subtitle">Elige una nueva contraseña para tu cuenta.</p>

            <div class="field">
              <label>Nueva contraseña</label>
              <input class="gc-input" type="password" [(ngModel)]="password"
                     placeholder="Mínimo 8 caracteres" (keyup.enter)="submit()" />
            </div>
            <div class="field">
              <label>Confirmar contraseña</label>
              <input class="gc-input" type="password" [(ngModel)]="confirmPassword"
                     placeholder="Repite tu contraseña" (keyup.enter)="submit()" />
            </div>

            @if (formError()) {
              <div class="inline-error">
                <span>⚠</span><span>{{ formError() }}</span>
              </div>
            }

            <button class="btn btn-primary w-full" (click)="submit()">
              Restablecer contraseña
            </button>

            <a class="back-link" routerLink="/login">← Volver al login</a>
          }

          @case ('loading') {
            <div class="status-state">
              <div class="gc-spinner"></div>
              <p>Actualizando tu contraseña…</p>
            </div>
          }

          @case ('success') {
            <div class="status-state">
              <div class="status-icon success">✓</div>
              <h2>¡Contraseña actualizada!</h2>
              <p>Tu contraseña ha sido cambiada correctamente.</p>
              <a class="btn btn-primary w-full" routerLink="/login">Ir al login</a>
            </div>
          }

          @case ('error') {
            <div class="status-state">
              <div class="status-icon danger">✕</div>
              <h2>Enlace inválido o expirado</h2>
              <p>Este enlace ha expirado o ya fue usado.<br>
                Solicita uno nuevo desde la página de login.</p>
              <a class="btn btn-primary w-full" routerLink="/login">Ir al login</a>
            </div>
          }
        }

      </div>
    </section>
  `,
  styles: [`
    .auth-page {
      min-height: 100vh;
      background: var(--gc-surface);
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 1rem;
    }

    .auth-card {
      width: 100%;
      max-width: 420px;
      padding: 2.25rem;
      display: flex;
      flex-direction: column;
      gap: 1.125rem;
    }

    .auth-brand {
      font-size: 0.875rem;
      font-weight: 700;
      letter-spacing: -0.01em;
      color: var(--gc-brand);
    }

    .auth-title {
      font-size: 1.5rem;
      font-weight: 700;
      color: var(--gc-ink);
      margin: 0;
      letter-spacing: -0.02em;
    }

    .auth-subtitle {
      font-size: 0.875rem;
      color: var(--gc-ink-muted);
      margin: 0;
    }

    .field {
      display: flex;
      flex-direction: column;
      gap: 0.375rem;

      label {
        font-size: 0.8rem;
        font-weight: 500;
        color: var(--gc-ink-muted);
      }
    }

    .inline-error {
      display: flex;
      align-items: flex-start;
      gap: 0.5rem;
      border-radius: 0.5rem;
      border: 1px solid rgba(220,38,38,0.2);
      background: rgba(220,38,38,0.06);
      color: #b91c1c;
      font-size: 0.875rem;
      padding: 0.625rem 0.75rem;
    }

    .back-link {
      text-align: center;
      font-size: 0.8rem;
      color: var(--gc-ink-muted);
      text-decoration: none;
      &:hover { color: var(--gc-ink); }
    }

    .status-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.75rem;
      text-align: center;
      padding: 0.5rem 0;

      h2 { color: var(--gc-ink); margin: 0; font-size: 1.125rem; font-weight: 700; }
      p  { color: var(--gc-ink-muted); margin: 0; font-size: 0.875rem; line-height: 1.55; }
    }

    .gc-spinner {
      width: 36px; height: 36px;
      border: 3px solid rgba(0,0,0,0.1);
      border-top-color: var(--gc-brand);
      border-radius: 50%;
      animation: spin 0.7s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    .status-icon {
      width: 52px; height: 52px;
      border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      font-size: 1.5rem; font-weight: 700;

      &.success { background: rgba(22,163,74,0.1); color: #16a34a; }
      &.danger  { background: rgba(220,38,38,0.1); color: #dc2626; }
    }
  `],
})
export class ResetPasswordPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly http  = inject(HttpClient);
  private readonly API   = environment.apiBase;

  protected readonly state     = signal<PageState>('form');
  protected readonly formError = signal<string | null>(null);

  protected password        = '';
  protected confirmPassword = '';
  private   token           = '';

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';
    if (!this.token) this.state.set('error');
  }

  protected submit(): void {
    this.formError.set(null);

    if (this.password.length < 8) {
      this.formError.set('La contraseña debe tener al menos 8 caracteres.');
      return;
    }
    if (this.password !== this.confirmPassword) {
      this.formError.set('Las contraseñas no coinciden.');
      return;
    }

    this.state.set('loading');
    this.http.post(`${this.API}/api/auth/reset-password`, {
      token: this.token,
      newPassword: this.password,
    }).subscribe({
      next: () => this.state.set('success'),
      error: () => this.state.set('error'),
    });
  }
}
