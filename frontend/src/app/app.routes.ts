// src/app/app.routes.ts
import { Routes } from '@angular/router';
import { AppShellComponent } from './layout/app-shell.component';
import { HomePage } from './features/home/home.page';
import { LoginPage } from './features/auth/login.page';
import { PlaceholderPage } from './features/placeholder/placeholder.page';
import { StaffPosPage } from './features/staff/pos/staff-pos.page';
import { MePage } from './features/me/pages/me.page';
import { ConfirmEmailChangePage } from '@app/app/features/me/pages/confirm-email-change.page';

export const routes: Routes = [
  // ── Standalone pages (no navbar / shell) ────────────────────────────────
  { path: 'login', component: LoginPage },

  // ── Shell-wrapped pages ──────────────────────────────────────────────────
  {
    path: '',
    component: AppShellComponent,
    children: [
      { path: '', component: HomePage },

      // Profile
      { path: 'me', component: MePage },
      { path: 'confirm-email-change', component: ConfirmEmailChangePage },

      // Staff – POS (eager, used constantly)
      { path: 'staff/pos', component: StaffPosPage },

      // Staff – Kitchen Display (lazy-loaded: only kitchen staff need it)
      {
        path: 'staff/kitchen',
        loadComponent: () =>
          import('./features/staff/kitchen/staff-kitchen.page').then(
            (m) => m.StaffKitchenPage
          ),
      },

      // Staff – Orders List (lazy-loaded: only kitchen staff need it)
      {
        path: 'staff/orders',
        loadComponent: () =>
          import('./features/staff/orders/staff-orders-list.page').then(
            (m) => m.StaffOrdersListPage
          ),
      },

      // Placeholders (to be replaced in future phases)
      { path: 'admin', component: PlaceholderPage },
      { path: 'menu', component: PlaceholderPage },
    ],
  },

  { path: '**', redirectTo: '' },
];
