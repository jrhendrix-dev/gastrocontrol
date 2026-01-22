import { Component } from '@angular/core';

@Component({
  standalone: true,
  template: `
    <section class="mx-auto max-w-6xl px-4 py-10">
      <div class="rounded-2xl p-6 shadow"
           style="background: var(--gc-surface); color: var(--gc-ink); border: 1px solid rgba(0,0,0,0.06);">
        <h2 class="text-xl font-semibold">Placeholder</h2>
        <p class="mt-2 opacity-80">Esta ruta aún no está implementada.</p>
      </div>
    </section>
  `
})
export class PlaceholderPage {}
