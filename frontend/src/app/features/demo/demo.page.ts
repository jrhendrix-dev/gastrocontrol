import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { DemoSessionApi } from '../../core/demo/demo-session.api';
import { DemoSessionStore } from '../../core/demo/demo-session.store';
import { DemoSession } from '../../core/demo/demo-session.model';
import { AuthService } from '../../core/auth/auth.service';
import { finalize } from 'rxjs';

interface RoleCard {
  role: DemoSession['role'];
  title: string;
  description: string;
  icon: string;
  color: string;
  credentials: { email: string; password: string };
}

/**
 * Demo landing page — lets the visitor pick a role, provisions
 * an isolated demo schema, and auto-logs in with demo credentials.
 */
@Component({
  selector: 'app-demo-page',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="demo-page">
      <div class="demo-header">
        <h1>GastroControl Demo</h1>
        <p class="demo-subtitle">
          Elige un rol para explorar la aplicación con datos de prueba aislados.
          Tu sesión expira en <strong>2 horas</strong>.
        </p>
      </div>

      <div class="role-cards">
        @for (card of roleCards; track card.role) {
          <div
            class="role-card"
            [class.loading]="loading() === card.role"
            (click)="startDemo(card)"
          >
            <div class="role-icon" [style.background]="card.color">
              {{ card.icon }}
            </div>
            <h2>{{ card.title }}</h2>
            <p>{{ card.description }}</p>

            @if (loading() === card.role) {
              <div class="role-spinner">Preparando demo...</div>
            } @else {
              <button class="role-btn" [style.background]="card.color">
                Entrar como {{ card.title }}
              </button>
            }
          </div>
        }
      </div>

      @if (error()) {
        <div class="demo-error">
          {{ error() }}
        </div>
      }

      <div class="demo-footer">
        <a routerLink="/gastrocontrol" class="back-link">← Volver al inicio</a>
      </div>
    </div>
  `,
  styles: [`
    .demo-page {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 2rem;
      background: #f5f0e8;
    }

    .demo-header {
      text-align: center;
      margin-bottom: 3rem;
    }

    .demo-header h1 {
      font-size: 2.5rem;
      color: #1a2e1a;
      margin-bottom: 0.75rem;
    }

    .demo-subtitle {
      color: #555;
      font-size: 1.1rem;
      max-width: 500px;
    }

    .role-cards {
      display: flex;
      gap: 2rem;
      flex-wrap: wrap;
      justify-content: center;
      margin-bottom: 2rem;
    }

    .role-card {
      background: white;
      border-radius: 1rem;
      padding: 2rem;
      width: 260px;
      text-align: center;
      cursor: pointer;
      box-shadow: 0 4px 20px rgba(0,0,0,0.08);
      transition: transform 0.2s, box-shadow 0.2s;
    }

    .role-card:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 30px rgba(0,0,0,0.15);
    }

    .role-card.loading {
      opacity: 0.7;
      pointer-events: none;
    }

    .role-icon {
      width: 64px;
      height: 64px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 2rem;
      margin: 0 auto 1rem;
    }

    .role-card h2 {
      font-size: 1.4rem;
      color: #1a2e1a;
      margin-bottom: 0.5rem;
    }

    .role-card p {
      color: #666;
      font-size: 0.9rem;
      margin-bottom: 1.5rem;
      line-height: 1.5;
    }

    .role-btn {
      color: white;
      border: none;
      padding: 0.75rem 1.5rem;
      border-radius: 0.5rem;
      font-size: 1rem;
      cursor: pointer;
      width: 100%;
      font-weight: 600;
    }

    .role-spinner {
      color: #888;
      font-size: 0.9rem;
      padding: 0.75rem;
    }

    .demo-error {
      background: #fee;
      color: #c00;
      padding: 1rem 2rem;
      border-radius: 0.5rem;
      margin-bottom: 1rem;
    }

    .demo-footer {
      margin-top: 2rem;
    }

    .back-link {
      color: #666;
      text-decoration: none;
      font-size: 0.9rem;
    }

    .back-link:hover {
      color: #1a2e1a;
    }
  `]
})
export class DemoPageComponent {
  private readonly demoApi = inject(DemoSessionApi);
  private readonly demoStore = inject(DemoSessionStore);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly loading = signal<DemoSession['role'] | null>(null);
  readonly error = signal<string | null>(null);

  readonly roleCards: RoleCard[] = [
    {
      role: 'ADMIN',
      title: 'Admin',
      icon: '👑',
      color: '#2d6a2d',
      description: 'Acceso completo: gestión de usuarios, productos, categorías, mesas y panel de administración.',
      credentials: { email: 'admin@gastro.demo', password: 'GastroControl1726!' },
    },
    {
      role: 'MANAGER',
      title: 'Manager',
      icon: '📊',
      color: '#1a5276',
      description: 'Panel de operaciones, gestión de pedidos, reportes y supervisión del equipo.',
      credentials: { email: 'manager@gastro.demo', password: 'GastroControl1726!' },
    },
    {
      role: 'STAFF',
      title: 'Staff',
      icon: '🍽️',
      color: '#7d3c12',
      description: 'POS terminal, gestión de mesas, pedidos y vista de cocina en tiempo real.',
      credentials: { email: 'staff@gastro.demo', password: 'GastroControl1726!' },
    },
  ];

  /**
   * Provisions a demo session, stores the session ID, then
   * auto-logs in with the demo credentials for the selected role.
   */
  startDemo(card: RoleCard): void {
    this.loading.set(card.role);
    this.error.set(null);

    this.demoApi.createSession().pipe(
      finalize(() => this.loading.set(null))
    ).subscribe({
      next: (sessionId) => {
        // Activate demo mode — interceptor will now send X-Demo-Session
        this.demoStore.activate(sessionId, card.role);

        // Auto-login with demo credentials
        this.authService.login(card.credentials).subscribe({
          next: () => this.redirectForRole(card.role),
          error: () => {
            this.demoStore.deactivate();
            this.error.set('Error al iniciar sesión demo. Inténtalo de nuevo.');
          }
        });
      },
      error: () => {
        this.error.set('Error al crear la sesión demo. Inténtalo de nuevo.');
      }
    });
  }

  private redirectForRole(role: DemoSession['role']): void {
    switch (role) {
      case 'ADMIN':
        this.router.navigate(['/admin']);
        break;
      case 'MANAGER':
        this.router.navigate(['/admin']);
        break;
      case 'STAFF':
        this.router.navigate(['/staff/pos']);
        break;
    }
  }
}
