import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgIf } from '@angular/common';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  standalone: true,
  selector: 'gc-home-page',
  imports: [RouterLink, NgIf],
  template: `
    <section class="mx-auto max-w-6xl px-4 py-10">
      <div class="rounded-2xl p-8 shadow"
           style="background: var(--gc-surface); color: var(--gc-ink); border: 1px solid rgba(0,0,0,0.06);">
        <h1 class="text-3xl font-semibold">GastroControl</h1>
        <p class="mt-2 opacity-80">
          Demo de POS + pedidos + pagos (modo staff y modo cliente).
        </p>

        <div class="mt-6 flex flex-wrap gap-3">
          <a routerLink="/login" class="btn btn-primary">Login</a>
          <a routerLink="/staff/pos" class="btn btn-outline">Ir a POS</a>
          <a routerLink="/menu" class="btn btn-outline">Ver menú</a>
        </div>

        <div class="mt-8 rounded-xl p-4"
             style="background: rgba(230,184,92,0.18); border: 1px solid rgba(230,184,92,0.35);">
          <p class="text-sm">
            <strong>Tip:</strong> El refresh token es una cookie httpOnly y el access token va en Authorization.
            Esto simula un setup realista para demo.
          </p>
        </div>

        <div class="mt-6 text-sm opacity-80" *ngIf="auth.loggedIn()">
          Sesión activa como: <span class="font-semibold">{{ auth.email() }}</span>
        </div>
      </div>
    </section>
  `
})
export class HomePage {
  auth = inject(AuthService);
}
