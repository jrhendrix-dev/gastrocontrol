// src/app/layout/navbar/navbar.component.ts
import { Component, ElementRef, HostListener, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { NgFor, NgIf } from '@angular/common';
import { AuthService } from '../../core/auth/auth.service';

type NavMode = 'public' | 'staff';

type NavItem = {
  label: string;
  route: string;
  mode?: NavMode;
  requiresAuth?: boolean;
  rolesAny?: string[];
};

@Component({
  selector: 'gc-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, NgIf, NgFor],
  templateUrl: './navbar.component.html',
})
export class NavbarComponent {
  auth = inject(AuthService);
  private router = inject(Router);
  private host = inject(ElementRef<HTMLElement>);

  open = false;
  accountOpen = false;

  private modeSig = signal<NavMode | null>(null);
  mode = computed<NavMode>(() => this.modeSig() ?? this.inferredMode());

  private readonly items: NavItem[] = [
    // Public/customer
    { label: 'Inicio',      route: '/',              mode: 'public' },
    { label: 'Menú',        route: '/menu',           mode: 'public' },
    { label: 'Mi pedido',   route: '/order/current',  mode: 'public', requiresAuth: true },

    // Staff
    { label: 'POS',     route: '/staff/pos',     mode: 'staff', rolesAny: ['ROLE_STAFF', 'ROLE_MANAGER', 'ROLE_ADMIN'] },
    { label: 'Pedidos', route: '/staff/orders',  mode: 'staff', rolesAny: ['ROLE_STAFF', 'ROLE_MANAGER', 'ROLE_ADMIN'] },
    { label: 'Cocina',  route: '/staff/kitchen', mode: 'staff', rolesAny: ['ROLE_STAFF', 'ROLE_MANAGER', 'ROLE_ADMIN'] },

    // Admin panel — visible to MANAGER and ADMIN
    { label: 'Admin',   route: '/admin',          mode: 'staff', rolesAny: ['ROLE_MANAGER', 'ROLE_ADMIN'] },
  ];

  inferredMode = computed<NavMode>(() => {
    const roles = this.auth.roles();
    return (roles.includes('ROLE_ADMIN') || roles.includes('ROLE_STAFF') || roles.includes('ROLE_MANAGER'))
      ? 'staff'
      : 'public';
  });

  visibleItems = computed<NavItem[]>(() => {
    const inferred = this.inferredMode();
    const mode = inferred === 'public' ? 'public' : this.mode();
    const roles = this.auth.roles();
    const loggedIn = this.auth.loggedIn();

    return this.items.filter((i) => {
      if (i.mode && i.mode !== mode) return false;
      if (i.requiresAuth && !loggedIn) return false;
      if (i.rolesAny?.length) return i.rolesAny.some((r) => roles.includes(r));
      return true;
    });
  });

  trackItem = (_: number, item: NavItem) => item.route;

  toggleModeForDemo() {
    if (this.inferredMode() === 'public') return;
    this.modeSig.set(this.mode() === 'staff' ? 'public' : 'staff');
  }

  toggleAccountMenu() { this.accountOpen = !this.accountOpen; }

  closeMenus() { this.accountOpen = false; this.open = false; }

  goHome(ev: MouseEvent) {
    const path = this.router.url.replace(/[?#].*$/, '');
    if (path === '/') {
      ev.preventDefault();
      window.scrollTo({ top: 0, behavior: 'smooth' });
      if (this.open) this.open = false;
    }
  }

  panelRoute(): string | null {
    const roles = this.auth.roles();
    if (roles.includes('ROLE_ADMIN') || roles.includes('ROLE_MANAGER')) return '/admin';
    if (roles.includes('ROLE_STAFF')) return '/staff/pos';
    return null;
  }

  panelLabel(): string {
    const roles = this.auth.roles();
    if (roles.includes('ROLE_ADMIN')) return 'Admin';
    if (roles.includes('ROLE_MANAGER')) return 'Manager';
    if (roles.includes('ROLE_STAFF')) return 'Staff';
    return 'Panel';
  }

  logout() {
    this.closeMenus();
    this.auth.logout().subscribe({ next: () => void this.router.navigateByUrl('/login') });
  }

  @HostListener('window:keydown', ['$event'])
  onKeydown(ev: KeyboardEvent) {
    if (ev.key === 'Escape') this.closeMenus();
  }

  @HostListener('document:click', ['$event'])
  onDocClick(ev: MouseEvent) {
    if (!this.accountOpen) return;
    const target = ev.target as Node | null;
    if (target && !this.host.nativeElement.contains(target)) this.accountOpen = false;
  }
}
