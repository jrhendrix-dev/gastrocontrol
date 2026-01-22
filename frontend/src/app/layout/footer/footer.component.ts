import { Component } from '@angular/core';

@Component({
  selector: 'gc-footer',
  standalone: true,
  template: `
  <footer class="bg-brand-green text-brand-gold">
    <div class="mx-auto grid max-w-6xl grid-cols-1 gap-6 px-4 py-10 md:grid-cols-2">
      <div>
        <h3 class="mb-3 text-xl font-semibold">Contáctanos</h3>
        <p class="mb-1"><i class="fa-solid fa-envelope text-brand-crimson mr-2"></i><a href="mailto:jrhendrixdev@gmail.com" class="hover:underline">jrhendrixdev@gmail.com</a></p>
        <p class="mb-1"><i class="fa-solid fa-phone text-brand-crimson mr-2"></i><a href="tel:+34635507365" class="hover:underline">+34 635 507 365</a></p>
        <p class="text-gray-300"><i class="fa-solid fa-map-marker-alt text-brand-crimson mr-2"></i>Sevilla, Andalucía, España</p>
        <p class="mt-3 text-sm text-gray-300 font-semibold">Este sitio es un proyecto ficticio con fines demostrativos.</p>
      </div>
    </div>
  </footer>
  `
})
export class FooterComponent {}
