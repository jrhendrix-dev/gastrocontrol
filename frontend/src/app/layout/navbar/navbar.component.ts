import { Component, ElementRef, HostListener, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { NgFor, NgIf } from '@angular/common';
import { AuthService } from '../../core/auth/auth.service';

type NavMode = 'public' | 'staff';

type NavItem = {
  label: string;
  route: string;
  mode?: NavMode; // if omitted -> available in all modes
  requiresAuth?: boolean;
  rolesAny?: string[]; // show only if user has any of these roles
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

  /** mobile menu */
  open = false;
  /** account dropdown */
  accountOpen = false;

  /**
   * Demo mode toggle.
   * If you want this to ALWAYS follow user roles, delete modeSig + toggleModeForDemo()
   * and replace mode() usage with inferredMode().
   */
  private modeSig = signal<NavMode>('public');
  mode = computed(() => this.modeSig());

  private readonly items: NavItem[] = [
    // Public/customer
    { label: 'Inicio', route: '/', mode: 'public' },
    { label: 'Menú', route: '/menu', mode: 'public' },
    { label: 'Mi pedido', route: '/order/current', mode: 'public', requiresAuth: true },

    // Staff/admin
    { label: 'POS', route: '/staff/pos', mode: 'staff', rolesAny: ['ROLE_STAFF', 'ROLE_MANAGER', 'ROLE_ADMIN'] },
    { label: 'Pedidos', route: '/staff/orders', mode: 'staff', rolesAny: ['ROLE_STAFF', 'ROLE_MANAGER', 'ROLE_ADMIN'] },
    { label: 'Cocina', route: '/staff/kitchen', mode: 'staff', rolesAny: ['ROLE_STAFF', 'ROLE_MANAGER', 'ROLE_ADMIN'] },
    { label: 'Admin', route: '/admin', mode: 'staff', rolesAny: ['ROLE_ADMIN'] },
  ];

  /** Infer mode from roles */
  inferredMode = computed<NavMode>(() => {
    const roles = this.auth.roles();
    return (roles.includes('ROLE_ADMIN') || roles.includes('ROLE_STAFF') || roles.includes('ROLE_MANAGER'))
      ? 'staff'
      : 'public';
  });

  /**
   * Visible nav items.
   * - If user is public, never show staff items.
   * - If user is staff, allow demo toggle to switch views.
   */
  visibleItems = computed<NavItem[]>(() => {
    const inferred = this.inferredMode();
    const mode = inferred === 'public' ? 'public' : this.modeSig(); // lock down if not staff
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
    // Only allow demo toggle if user is staff-capable
    if (this.inferredMode() === 'public') return;
    this.modeSig.set(this.modeSig() === 'staff' ? 'public' : 'staff');
  }

  toggleAccountMenu() {
    this.accountOpen = !this.accountOpen;
  }

  closeMenus() {
    this.accountOpen = false;
    this.open = false;
  }

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
    if (roles.includes('ROLE_ADMIN')) return '/admin';
    if (roles.includes('ROLE_STAFF') || roles.includes('ROLE_MANAGER')) return '/staff/pos';
    return null;
  }

  panelLabel(): string {
    const roles = this.auth.roles();
    if (roles.includes('ROLE_ADMIN')) return 'Admin';
    if (roles.includes('ROLE_STAFF') || roles.includes('ROLE_MANAGER')) return 'POS / Staff';
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
    if (target && !this.host.nativeElement.contains(target)) {
      this.accountOpen = false;
    }
  }
}
