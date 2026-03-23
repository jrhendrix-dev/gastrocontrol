// src/app/features/order/track.page.ts
import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * Real-time order tracking page — Phase 3.
 * Placeholder until the tracking flow is implemented.
 */
@Component({
  selector: 'app-track-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div style="padding:4rem 2rem; text-align:center; color:#888; font-family:system-ui,sans-serif;">
      <p style="font-size:2rem;">📍</p>
      <p>Seguimiento del pedido — próximamente.</p>
    </div>
  `,
})
export class TrackPage {}
