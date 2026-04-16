import { Component, inject, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { DemoSessionStore } from '../../core/demo/demo-session.store';
import { AuthService } from '../../core/auth/auth.service';

/**
 * Persistent banner shown at the bottom of the app when a demo session is active.
 * Displays the current role, time remaining, and an exit button.
 */
@Component({
  selector: 'app-demo-banner',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (demoStore.isActive()) {
      <div class="demo-banner">
        <span class="demo-badge">DEMO</span>
        <span class="demo-info">
          Sesión de demo activa como
          <strong>{{ demoStore.role() }}</strong>
          · expira en <strong>{{ minutesDisplay() }}</strong>
        </span>
        <button class="demo-exit" (click)="exitDemo()">
          Salir del demo
        </button>
      </div>
    }
  `,
  styles: [`
    .demo-banner {
      position: fixed;
      bottom: 0;
      left: 0;
      right: 0;
      z-index: 9999;
      background: #1a2e1a;
      color: white;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 1rem;
      padding: 0.5rem 1rem;
      font-size: 0.875rem;
      box-shadow: 0 -2px 10px rgba(0,0,0,0.2);
    }
    .demo-badge {
      background: #f0a500;
      color: #1a2e1a;
      font-weight: 800;
      padding: 0.2rem 0.5rem;
      border-radius: 0.25rem;
      font-size: 0.75rem;
      letter-spacing: 0.05em;
    }
    .demo-info {
      color: #ccc;
    }
    .demo-info strong {
      color: white;
    }
    .demo-exit {
      background: transparent;
      border: 1px solid #ccc;
      color: white;
      padding: 0.25rem 0.75rem;
      border-radius: 0.25rem;
      cursor: pointer;
      font-size: 0.8rem;
      margin-left: 1rem;
      transition: background 0.2s;
    }
    .demo-exit:hover {
      background: rgba(255,255,255,0.1);
    }
  `]
})
export class DemoBannerComponent implements OnInit, OnDestroy {
  readonly demoStore = inject(DemoSessionStore);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly minutesDisplay = signal('');
  private timer: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    // Only run the expiry timer if a demo session is actually active.
    // Without this guard, the timer fires for real users and calls
    // exitDemo() which clears auth and redirects to landing page.
    if (!this.demoStore.isActive()) return;
    this.updateDisplay();
    this.timer = setInterval(() => {
      this.demoStore.tick(); // ← triggers signal recomputation
      this.updateDisplay();
      if (!this.demoStore.isActive()) {
        this.exitDemo();
      }
    }, 60_000);
  }

  ngOnDestroy(): void {
    if (this.timer) clearInterval(this.timer);
  }

  exitDemo(): void {
    this.demoStore.deactivate();
    this.authService.clearAuthLocal();
    this.router.navigate(['/']);
  }

  private updateDisplay(): void {
    const mins = this.demoStore.minutesRemaining();
    if (mins >= 60) {
      const h = Math.floor(mins / 60);
      const m = mins % 60;
      this.minutesDisplay.set(m > 0 ? `${h}h ${m}m` : `${h}h`);
    } else {
      this.minutesDisplay.set(`${mins}m`);
    }
  }
}
