// src/app/layout/app-shell.component.ts
import { CommonModule, NgTemplateOutlet } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { NavbarComponent } from './navbar/navbar.component';
import { FooterComponent } from './footer/footer.component';
import { ToastContainerComponent } from '../core/ui/toast/toast-container.component';
import { DrawerComponent } from '../core/ui/drawer/drawer.component';
import { DrawerService } from '../core/ui/drawer/drawer.service';
import { AuthService } from '../core/auth/auth.service';

@Component({
  standalone: true,
  selector: 'gc-app-shell',
  imports: [
    CommonModule,
    NgTemplateOutlet,
    RouterOutlet,
    NavbarComponent,
    FooterComponent,
    ToastContainerComponent,
    DrawerComponent,
  ],
  template: `
    <gc-navbar />

    <main class="min-h-[calc(100vh-var(--gc-nav-height))]">
      <router-outlet />
    </main>

    <gc-footer />
    <gc-toast-container />

    <gc-drawer
      [open]="drawer.open()"
      [heading]="drawer.heading()"
      [offsetVar]="'--gc-nav-height'"
      (close)="drawer.close()"
    >
      <ng-container *ngIf="drawer.contentTpl() as tpl">
        <ng-container
          [ngTemplateOutlet]="tpl"
          [ngTemplateOutletContext]="drawer.context()"
        ></ng-container>
      </ng-container>
    </gc-drawer>
  `,
})
export class AppShellComponent {
  drawer = inject(DrawerService);
  private auth = inject(AuthService);

  constructor() {
    // hydrate session on hard refresh
    this.auth.bootstrap().subscribe();
  }
}
