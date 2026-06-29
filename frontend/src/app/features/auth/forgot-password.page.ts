import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '@app/environment/environment';

@Component({
  standalone: true,
  selector: 'gc-forgot-password',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="min-h-screen flex items-center justify-center px-4" style="background:var(--gc-bg)">
      <div class="w-full max-w-md rounded-2xl p-8 shadow" style="background:var(--gc-surface)">
        <a routerLink="/login" class="text-sm opacity-60 hover:opacity-100 mb-6 inline-block">← Volver al login</a>
        <h1 class="text-2xl font-semibold mb-2" style="color:var(--gc-ink)">Recuperar contraseña</h1>
        <p class="text-sm mb-6 opacity-70" style="color:var(--gc-ink)">
          Introduce tu email y te enviaremos un enlace para restablecerla.
        </p>
        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="mb-4">
            <label class="block text-sm mb-1 opacity-70" style="color:var(--gc-ink)">Email</label>
            <input type="email" formControlName="email"
                   class="w-full rounded-lg px-3 py-2 text-sm border focus:outline-none"
                   style="background:var(--gc-bg);color:var(--gc-ink);border-color:rgba(0,0,0,0.15)"
                   placeholder="tu@email.com" />
          </div>
          <div *ngIf="error()" class="mb-4 text-sm text-red-600">{{ error() }}</div>
          <div *ngIf="sent()" class="mb-4 text-sm text-emerald-600">
            Si el email existe, recibirás un enlace en breve.
          </div>
          <button type="submit" [disabled]="form.invalid || loading()"
                  class="w-full py-2.5 rounded-lg text-sm font-semibold"
                  style="background:var(--gc-green);color:#fff;opacity:{{form.invalid||loading()?0.5:1}}">
            {{ loading() ? 'Enviando…' : 'Enviar enlace' }}
          </button>
        </form>
      </div>
    </div>
  `,
})
export class ForgotPasswordPage {
  private fb   = inject(FormBuilder);
  private http = inject(HttpClient);

  form    = this.fb.group({ email: ['', [Validators.required, Validators.email]] });
  loading = signal(false);
  error   = signal<string | null>(null);
  sent    = signal(false);

  submit() {
    if (this.form.invalid || this.loading()) return;
    this.loading.set(true);
    this.error.set(null);
    this.http.post(`${environment.apiBase}/api/auth/forgot-password`, { email: this.form.value.email })
      .subscribe({
        next: () => { this.loading.set(false); this.sent.set(true); },
        error: (e) => {
          this.loading.set(false);
          this.error.set(e?.status === 429 ? 'Demasiados intentos.' : 'Algo salió mal.');
        },
      });
  }
}
