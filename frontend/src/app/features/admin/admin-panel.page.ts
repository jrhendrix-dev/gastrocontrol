// src/app/features/admin/admin-panel.page.ts
import {
  ChangeDetectionStrategy, Component, computed, inject, signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '@app/app/core/auth/auth.service';
import { AdminDashboardTabComponent } from './tabs/admin-dashboard-tab.component';
import { AdminOperationsTabComponent } from './tabs/admin-operations-tab.component';
import { AdminUsersTabComponent } from './tabs/admin-users-tab.component';
import { AdminProductsTabComponent } from './tabs/admin-products-tab.component';
import { AdminCategoriesTabComponent } from './tabs/admin-categories-tab.component';
import { AdminTablesTabComponent } from './tabs/admin-tables-tab.component';

type TabId = 'dashboard' | 'operations' | 'products' | 'categories' | 'tables' | 'users';

interface Tab {
  id: TabId;
  label: string;
  icon: string;
  /** If true, only ADMIN can see this tab. MANAGER cannot. */
  adminOnly: boolean;
  /** If true, only MANAGER and ADMIN can see this tab. STAFF cannot. */
  managerOnly: boolean;
}

const ALL_TABS: Tab[] = [
  { id: 'dashboard',  label: 'Resumen',     icon: '📊', adminOnly: false, managerOnly: true  },
  { id: 'operations', label: 'Operaciones', icon: '⚙️', adminOnly: false, managerOnly: true  },
  { id: 'products',   label: 'Productos',   icon: '🍽️', adminOnly: false, managerOnly: true  },
  { id: 'categories', label: 'Categorías',  icon: '🗂️', adminOnly: false, managerOnly: true  },
  { id: 'tables',     label: 'Mesas',       icon: '🪑', adminOnly: false, managerOnly: true  },
  { id: 'users',      label: 'Usuarios',    icon: '👥', adminOnly: true,  managerOnly: true  },
];

/**
 * Single-page admin panel with role-aware tab visibility.
 *
 * MANAGER: Dashboard, Operations, Products, Categories, Tables.
 * ADMIN:   All tabs including Users.
 */
@Component({
  selector: 'app-admin-panel-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    AdminDashboardTabComponent,
    AdminOperationsTabComponent,
    AdminUsersTabComponent,
    AdminProductsTabComponent,
    AdminCategoriesTabComponent,
    AdminTablesTabComponent,
  ],
  template: `
    <div class="admin-panel">
      <div class="admin-header">
        <h1 class="admin-title">Panel de Administración</h1>
        <span class="admin-role-badge">{{ roleBadge() }}</span>
      </div>

      <nav class="admin-tabs" role="tablist">
        @for (tab of visibleTabs(); track tab.id) {
          <button class="admin-tab" role="tab"
                  [class.active]="activeTab() === tab.id"
                  [attr.aria-selected]="activeTab() === tab.id"
                  (click)="setTab(tab.id)">
            <span>{{ tab.icon }}</span>
            <span>{{ tab.label }}</span>
          </button>
        }
      </nav>

      <div class="admin-content" role="tabpanel">
        @switch (activeTab()) {
          @case ('dashboard')  { <app-admin-dashboard-tab /> }
          @case ('operations') { <app-admin-operations-tab /> }
          @case ('users')      { <app-admin-users-tab /> }
          @case ('products')   { <app-admin-products-tab /> }
          @case ('categories') { <app-admin-categories-tab /> }
          @case ('tables')     { <app-admin-tables-tab /> }
        }
      </div>
    </div>
  `,
  styles: [`
    .admin-panel { min-height: 100%; background: var(--gc-surface); color: var(--gc-ink); }

    .admin-header {
      display: flex; align-items: center; gap: 1rem;
      padding: 1.5rem 2rem 0;
      border-bottom: 1px solid rgba(0,0,0,0.08);
    }
    .admin-title {
      font-size: 1.375rem; font-weight: 700; color: var(--gc-ink);
      margin: 0 auto 0 0; letter-spacing: -0.02em;
    }
    .admin-role-badge {
      font-size: 0.7rem; font-weight: 600; letter-spacing: 0.08em;
      text-transform: uppercase; color: var(--gc-brand);
      background: rgba(15,47,36,0.08); border: 1px solid rgba(15,47,36,0.15);
      padding: 0.25rem 0.75rem; border-radius: 999px;
    }

    .admin-tabs {
      display: flex; padding: 0 2rem;
      background: var(--gc-brand-analogous, #1a4a37);
    }
    .admin-tab {
      display: flex; align-items: center; gap: 0.4rem;
      padding: 0.875rem 1.125rem; background: none; border: none;
      border-bottom: 2px solid transparent;
      color: rgba(255,255,255,0.6);
      font-size: 0.875rem; font-weight: 500; cursor: pointer;
      transition: color 0.15s, border-color 0.15s; white-space: nowrap;
      &:hover { color: rgba(255,255,255,0.9); }
      &.active { color: var(--gc-accent); border-bottom-color: var(--gc-accent); font-weight: 600; }
    }
    .admin-content { padding: 2rem; }
  `],
})
export class AdminPanelPage {
  private readonly auth = inject(AuthService);

  protected readonly activeTab = signal<TabId>('dashboard');

  protected readonly isAdmin   = computed(() => this.auth.roles().includes('ROLE_ADMIN'));
  protected readonly isManager = computed(() =>
    this.auth.roles().includes('ROLE_MANAGER') || this.auth.roles().includes('ROLE_ADMIN')
  );

  protected readonly roleBadge = computed(() =>
    this.isAdmin() ? 'Admin' : 'Manager'
  );

  protected readonly visibleTabs = computed(() =>
    ALL_TABS.filter(t => {
      if (t.adminOnly && !this.isAdmin()) return false;
      if (t.managerOnly && !this.isManager()) return false;
      return true;
    })
  );

  protected setTab(id: TabId): void { this.activeTab.set(id); }
}
