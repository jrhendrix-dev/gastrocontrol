import { Routes } from '@angular/router';
import { AppShellComponent } from './layout/app-shell.component';
import { HomePage } from './features/home/home.page';
import { LoginPage } from './features/auth/login.page';
import { PlaceholderPage } from './features/placeholder/placeholder.page';
import { MePage } from './features/me/pages/me.page';

export const routes: Routes = [
  // Standalone pages (no navbar/footer)
  { path: 'login', component: LoginPage },

  // Shell-wrapped pages
  {
    path: '',
    component: AppShellComponent,
    children: [
      { path: '', component: HomePage },

      // My profile
      { path: 'me', component: MePage },

      // temporary routes
      { path: 'staff/pos', component: PlaceholderPage },
      { path: 'menu', component: PlaceholderPage },
    ],
  },

  { path: '**', redirectTo: '' },
];
