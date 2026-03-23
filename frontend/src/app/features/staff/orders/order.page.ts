// src/app/features/order/order.page.ts
import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * Customer checkout page — Phase 2.
 * Placeholder until the full checkout flow is implemented.
 */
@Component({
  selector: 'app-order-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div style="padding:4rem 2rem; text-align:center; color:#888; font-family:system-ui,sans-serif;">
      <p style="font-size:2rem;">🛒</p>
      <p>Checkout — próximamente.</p>
    </div>
  `,
})
export class OrderPage {}
