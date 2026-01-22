import { Routes } from '@angular/router';
import { AppShellComponent } from './layout/app-shell.component';
import { HomePage } from './features/home/home.page';
import { LoginPage } from './features/auth/login.page';
import { PlaceholderPage } from './features/placeholder/placeholder.page';

export const routes: Routes = [
  // Standalone pages (no navbar/footer)
  { path: 'login', component: LoginPage },

  // Shell-wrapped pages
  {
    path: '',
    component: AppShellComponent,
    children: [
      { path: '', component: HomePage },

      // temporary routes
      { path: 'staff/pos', component: PlaceholderPage },
      { path: 'menu', component: PlaceholderPage },
    ],
  },

  { path: '**', redirectTo: '' },
];
