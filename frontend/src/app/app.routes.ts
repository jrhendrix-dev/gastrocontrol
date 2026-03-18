// src/app/app.routes.ts
import { Routes } from '@angular/router';
import { AppShellComponent } from './layout/app-shell.component';
import { HomePage } from './features/home/home.page';
import { LoginPage } from './features/auth/login.page';
import { PlaceholderPage } from './features/placeholder/placeholder.page';
import { StaffPosPage } from './features/staff/pos/staff-pos.page';
import { MePage } from './features/me/pages/me.page';
import { ConfirmEmailChangePage } from '@app/app/features/me/pages/confirm-email-change.page';
import { adminGuard } from './features/admin/admin.guard';
import { staffGuard } from './features/staff/staff.guard';

export const routes: Routes = [
  // ── Standalone pages (no navbar / shell) ────────────────────────────────
  { path: 'login', component: LoginPage },

  // ── Token-based auth flows (no shell, no auth required) ─────────────────
  {
    path: 'set-password',
    loadComponent: () =>
      import('./features/auth/set-password.page').then(m => m.SetPasswordPage),
  },
  {
    path: 'reset-password',
    loadComponent: () =>
      import('./features/auth/reset-password.page').then(m => m.ResetPasswordPage),
  },

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
      { path: 'staff/pos',     component: StaffPosPage, canActivate: [staffGuard] },

      // Staff – Kitchen Display (lazy-loaded)
      {
        path: 'staff/kitchen',
        canActivate: [staffGuard],
        loadComponent: () =>
          import('./features/staff/kitchen/staff-kitchen.page').then(m => m.StaffKitchenPage),
      },

      // Staff – Orders List (lazy-loaded)
      {
        path: 'staff/orders',
        canActivate: [staffGuard],
        loadComponent: () =>
          import('./features/staff/orders/staff-orders-list.page').then(m => m.StaffOrdersListPage),
      },

      // Admin Panel – MANAGER and ADMIN roles only (lazy-loaded, guarded)
      {
        path: 'admin',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/admin/admin-panel.page').then(
            (m) => m.AdminPanelPage
          ),
      },

      // Public menu (placeholder)
      { path: 'menu', component: PlaceholderPage },
    ],
  },

  { path: '**', redirectTo: '' },
];
