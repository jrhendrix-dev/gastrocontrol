// src/app/app.routes.ts
import { Routes } from '@angular/router';
import { AppShellComponent } from './layout/app-shell.component';
import { CustomerShellComponent } from './layout/customer-shell.component';
import { HomePage } from './features/home/home.page';
import { LoginPage } from './features/auth/login.page';
import { StaffPosPage } from './features/staff/pos/staff-pos.page';
import { MePage } from './features/me/pages/me.page';
import { ConfirmEmailChangePage } from '@app/app/features/me/pages/confirm-email-change.page';
import { adminGuard } from './features/admin/admin.guard';
import { staffGuard } from './features/staff/staff.guard';

export const routes: Routes = [
  { path: 'login', component: LoginPage },
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

  // Staff shell
  {
    path: '',
    component: AppShellComponent,
    children: [
      { path: '', component: HomePage },
      { path: 'me', component: MePage },
      { path: 'confirm-email-change', component: ConfirmEmailChangePage },
      { path: 'staff/pos', component: StaffPosPage, canActivate: [staffGuard] },
      {
        path: 'staff/kitchen',
        canActivate: [staffGuard],
        loadComponent: () =>
          import('./features/staff/kitchen/staff-kitchen.page').then(m => m.StaffKitchenPage),
      },
      {
        path: 'staff/orders',
        canActivate: [staffGuard],
        loadComponent: () =>
          import('./features/staff/orders/staff-orders-list.page').then(m => m.StaffOrdersListPage),
      },
      {
        path: 'admin',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/admin/admin-panel.page').then(m => m.AdminPanelPage),
      },
    ],
  },

  // Customer shell
  {
    path: '',
    component: CustomerShellComponent,
    children: [
      {
        path: 'menu',
        loadComponent: () =>
          import('./features/menu/menu.page').then(m => m.MenuPage),
      },
      {
        path: 'order',
        loadComponent: () =>
          import('./features/order/order.page').then(m => m.OrderPage),
      },
      {
        path: 'order/confirm',
        loadComponent: () =>
          import('./features/order/order-confirm.page').then(m => m.OrderConfirmPage),
      },
      {
        path: 'track/:id',
        loadComponent: () =>
          import('./features/order/track.page').then(m => m.TrackPage),
      },
    ],
  },

  // Demo

  {
    path: 'demo',
    loadComponent: () =>
      import('./features/demo/demo.page').then(m => m.DemoPageComponent),
  },

  { path: '**', redirectTo: '' },
];
