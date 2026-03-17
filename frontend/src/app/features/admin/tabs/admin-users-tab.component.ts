// src/app/features/admin/tabs/admin-users-tab.component.ts
import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../admin-api.service';
import { CreateUserRequest, UserListParams, UserResponse, UserRole } from '../admin.types';

type ModalType = 'create' | 'changeEmail' | 'confirmAction' | null;

interface ConfirmAction {
  title: string;
  message: string;
  confirmLabel: string;
  danger: boolean;
  action: () => void;
}

/**
 * Admin Users tab — paginated list with full user management actions.
 *
 * Features: list/filter, create (invite email), deactivate/reactivate,
 * force password reset, resend invite, change email.
 */
@Component({
  selector: 'app-admin-users-tab',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="users-tab">

      <!-- Toolbar -->
      <div class="toolbar">
        <div class="filters">
          <input class="gc-input filter-search" type="text"
                 placeholder="Buscar por email…"
                 [(ngModel)]="searchQuery" (ngModelChange)="onSearchChange()" />
          <select class="gc-input filter-select" [(ngModel)]="filterRole" (ngModelChange)="load()">
            <option value="">Todos los roles</option>
            <option value="ADMIN">Admin</option>
            <option value="MANAGER">Manager</option>
            <option value="STAFF">Staff</option>
            <option value="CUSTOMER">Customer</option>
          </select>
          <select class="gc-input filter-select" [(ngModel)]="filterActive" (ngModelChange)="load()">
            <option value="">Todos los estados</option>
            <option value="true">Activos</option>
            <option value="false">Inactivos</option>
          </select>
        </div>
        <button class="btn btn-primary" (click)="openCreateModal()">+ Nuevo usuario</button>
      </div>

      <!-- Error banner -->
      @if (error()) {
        <div class="error-banner">{{ error() }}</div>
      }

      <!-- Table -->
      <div class="table-wrapper gc-card-inner">
        <table class="users-table">
          <thead>
          <tr>
            <th>Email</th>
            <th>Nombre</th>
            <th>Rol</th>
            <th>Estado</th>
            <th>Alta</th>
            <th>Último acceso</th>
            <th>Acciones</th>
          </tr>
          </thead>
          <tbody>
            @if (loading()) {
              <tr><td colspan="7" class="empty-cell">Cargando…</td></tr>
            } @else if (users().length === 0) {
              <tr><td colspan="7" class="empty-cell">No se encontraron usuarios.</td></tr>
            } @else {
              @for (user of users(); track user.id) {
                <tr [class.row-inactive]="!user.active">
                  <td class="cell-email">{{ user.email }}</td>
                  <td>{{ fullName(user) }}</td>
                  <td>
                    <span class="role-chip role-{{ user.role.toLowerCase() }}">
                      {{ user.role }}
                    </span>
                  </td>
                  <td>
                    <span class="status-chip" [class.active]="user.active">
                      {{ user.active ? 'Activo' : 'Inactivo' }}
                    </span>
                  </td>
                  <td class="cell-date">{{ user.createdAt | date:'dd MMM yyyy' }}</td>
                  <td class="cell-date">
                    {{ user.lastLoginAt ? (user.lastLoginAt | date:'dd MMM yyyy') : '—' }}
                  </td>
                  <td class="cell-actions">
                    <div class="action-row">
                      @if (user.active) {
                        <button class="btn btn-sm btn-danger" (click)="confirmDeactivate(user)">
                          Desactivar
                        </button>
                      } @else {
                        <button class="btn btn-sm btn-success" (click)="confirmReactivate(user)">
                          Reactivar
                        </button>
                      }
                      <button class="btn btn-sm btn-outline" (click)="openChangeEmailModal(user)">
                        Cambiar email
                      </button>
                      <button class="btn btn-sm btn-outline" (click)="confirmForceReset(user)">
                        Reset contraseña
                      </button>
                      <button class="btn btn-sm btn-outline" (click)="confirmResendInvite(user)">
                        Reenviar invitación
                      </button>
                    </div>
                  </td>
                </tr>
              }
            }
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      @if (totalPages() > 1) {
        <div class="pagination">
          <button class="btn btn-outline btn-sm"
                  [disabled]="currentPage() === 0"
                  (click)="goToPage(currentPage() - 1)">← Anterior</button>
          <span class="page-info">Página {{ currentPage() + 1 }} de {{ totalPages() }}</span>
          <button class="btn btn-outline btn-sm"
                  [disabled]="currentPage() >= totalPages() - 1"
                  (click)="goToPage(currentPage() + 1)">Siguiente →</button>
        </div>
      }
    </div>

    <!-- ── Create User Modal ──────────────────────────────────────────── -->
    @if (modalType() === 'create') {
      <div class="modal-backdrop" (click)="closeModal()">
        <div class="modal gc-card" (click)="$event.stopPropagation()">
          <h2 class="modal-title">Crear usuario</h2>
          <p class="modal-sub">Se enviará un email de invitación para establecer la contraseña.</p>

          <div class="field">
            <label>Email</label>
            <input class="gc-input" type="email" [(ngModel)]="createForm.email"
                   placeholder="usuario@ejemplo.com" />
          </div>
          <div class="field">
            <label>Rol</label>
            <select class="gc-input" [(ngModel)]="createForm.role">
              <option value="STAFF">Staff</option>
              <option value="MANAGER">Manager</option>
              <option value="ADMIN">Admin</option>
            </select>
          </div>
          <div class="field field-row">
            <input type="checkbox" id="active-chk" [(ngModel)]="createForm.active" />
            <label for="active-chk">Activo inmediatamente</label>
          </div>

          @if (modalError()) {
            <div class="modal-error">{{ modalError() }}</div>
          }

          <div class="modal-actions">
            <button class="btn btn-ghost" (click)="closeModal()">Cancelar</button>
            <button class="btn btn-primary" [disabled]="modalLoading()" (click)="submitCreate()">
              {{ modalLoading() ? 'Creando…' : 'Crear y enviar invitación' }}
            </button>
          </div>
        </div>
      </div>
    }

    <!-- ── Change Email Modal ─────────────────────────────────────────── -->
    @if (modalType() === 'changeEmail') {
      <div class="modal-backdrop" (click)="closeModal()">
        <div class="modal gc-card" (click)="$event.stopPropagation()">
          <h2 class="modal-title">Cambiar email</h2>
          <p class="modal-sub">Cambio administrativo — no se requiere confirmación por email.</p>

          <div class="field">
            <label>Email actual</label>
            <input class="gc-input" type="text" [value]="selectedUser()?.email" disabled />
          </div>
          <div class="field">
            <label>Nuevo email</label>
            <input class="gc-input" type="email" [(ngModel)]="changeEmailValue"
                   placeholder="nuevo@ejemplo.com" />
          </div>

          @if (modalError()) {
            <div class="modal-error">{{ modalError() }}</div>
          }

          <div class="modal-actions">
            <button class="btn btn-ghost" (click)="closeModal()">Cancelar</button>
            <button class="btn btn-primary" [disabled]="modalLoading()" (click)="submitChangeEmail()">
              {{ modalLoading() ? 'Guardando…' : 'Guardar' }}
            </button>
          </div>
        </div>
      </div>
    }

    <!-- ── Confirm Action Modal ───────────────────────────────────────── -->
    @if (modalType() === 'confirmAction' && pendingAction()) {
      <div class="modal-backdrop" (click)="closeModal()">
        <div class="modal modal-sm gc-card" (click)="$event.stopPropagation()">
          <h2 class="modal-title">{{ pendingAction()!.title }}</h2>
          <p class="modal-sub">{{ pendingAction()!.message }}</p>

          @if (modalError()) {
            <div class="modal-error">{{ modalError() }}</div>
          }

          <div class="modal-actions">
            <button class="btn btn-ghost" (click)="closeModal()">Cancelar</button>
            <button [class]="pendingAction()!.danger ? 'btn btn-danger' : 'btn btn-primary'"
                    [disabled]="modalLoading()"
                    (click)="executeAction()">
              {{ modalLoading() ? 'Procesando…' : pendingAction()!.confirmLabel }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .users-tab { display: flex; flex-direction: column; gap: 1.25rem; }

    /* Toolbar */
    .toolbar {
      display: flex; align-items: center;
      justify-content: space-between; gap: 1rem;
    }
    .filters { display: flex; gap: 0.5rem; align-items: center; flex-wrap: nowrap; }
    .filter-search { width: 200px; }
    .filter-select { width: 140px; }

    /* Error */
    .error-banner {
      background: rgba(220,38,38,0.06);
      border: 1px solid rgba(220,38,38,0.2);
      color: #b91c1c;
      border-radius: 0.5rem;
      padding: 0.75rem 1rem;
      font-size: 0.875rem;
    }

    /* Table */
    .table-wrapper { overflow-x: auto; }
    .users-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 0.875rem;

      th {
        background: var(--gc-brand-analogous);
        color: rgba(255,255,255,0.85);
        font-weight: 600;
        font-size: 0.75rem;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        padding: 0.75rem 1rem;
        text-align: left;
        white-space: nowrap;
      }

      td {
        padding: 0.875rem 1rem;
        border-top: 1px solid rgba(0,0,0,0.05);
        color: var(--gc-ink);
        vertical-align: middle;
      }

      tr:hover td { background: rgba(0,0,0,0.02); }
    }

    .row-inactive td { opacity: 0.5; }
    .cell-email { font-family: monospace; font-size: 0.8rem; }
    .cell-date { font-size: 0.8rem; color: var(--gc-ink-analogous); white-space: nowrap; }
    .empty-cell { text-align: center; color: var(--gc-ink-analogous); padding: 2rem !important; }

    /* Role chips */
    .role-chip {
      font-size: 0.7rem; font-weight: 600;
      letter-spacing: 0.07em; text-transform: uppercase;
      padding: 0.2rem 0.55rem; border-radius: 4px;
      border: 1px solid transparent;

      &.role-admin    { background: rgba(220,38,38,0.08);  color: #b91c1c;  border-color: rgba(220,38,38,0.15); }
      &.role-manager  { background: rgba(161,98,7,0.08);   color: #92400e;  border-color: rgba(161,98,7,0.15); }
      &.role-staff    { background: rgba(15,47,36,0.08);   color: var(--gc-brand); border-color: rgba(15,47,36,0.15); }
      &.role-customer { background: rgba(2,132,199,0.08);  color: #0369a1;  border-color: rgba(2,132,199,0.15); }
    }

    /* Status chip */
    .status-chip {
      font-size: 0.75rem; font-weight: 500;
      padding: 0.2rem 0.55rem; border-radius: 4px;
      background: rgba(220,38,38,0.08);
      color: #b91c1c;
      border: 1px solid rgba(220,38,38,0.15);

      &.active {
        background: rgba(22,163,74,0.08);
        color: #15803d;
        border-color: rgba(22,163,74,0.15);
      }
    }

    /* Actions */
    .cell-actions { white-space: nowrap; }
    .action-row { display: flex; gap: 0.35rem; flex-wrap: wrap; }

    /* Pagination */
    .pagination {
      display: flex; align-items: center;
      justify-content: center; gap: 1rem;
    }
    .page-info { font-size: 0.875rem; color: var(--gc-ink-analogous); }

    /* Modal */
    .modal-backdrop {
      position: fixed; inset: 0;
      background: rgba(0,0,0,0.35);
      display: flex; align-items: center; justify-content: center;
      z-index: 1000; padding: 1rem;
    }
    .modal {
      width: 100%; max-width: 440px;
      padding: 1.75rem;
      display: flex; flex-direction: column; gap: 1rem;
      &.modal-sm { max-width: 360px; }
    }
    .modal-title { font-size: 1.125rem; font-weight: 700; color: var(--gc-ink); margin: 0; }
    .modal-sub   { font-size: 0.875rem; color: var(--gc-ink-anologous); margin: 0; }

    .field {
      display: flex; flex-direction: column; gap: 0.375rem;
      label { font-size: 0.8rem; font-weight: 500; color: var(--gc-ink-analogous); }
    }
    .field-row { flex-direction: row !important; align-items: center; gap: 0.5rem !important; }

    .modal-error {
      background: rgba(220,38,38,0.06);
      border: 1px solid rgba(220,38,38,0.2);
      color: #b91c1c;
      border-radius: 0.5rem;
      padding: 0.625rem 0.75rem;
      font-size: 0.8rem;
    }
    .modal-actions {
      display: flex; justify-content: flex-end;
      gap: 0.5rem; margin-top: 0.25rem;
    }
  `],
})
export class AdminUsersTabComponent implements OnInit {
  private readonly api = inject(AdminApiService);

  protected readonly users       = signal<UserResponse[]>([]);
  protected readonly loading     = signal(false);
  protected readonly error       = signal<string | null>(null);
  protected readonly currentPage = signal(0);
  protected readonly totalPages  = signal(0);

  protected searchQuery  = '';
  protected filterRole   = '';
  protected filterActive = '';

  private searchDebounce: ReturnType<typeof setTimeout> | null = null;

  protected readonly modalType     = signal<ModalType>(null);
  protected readonly modalLoading  = signal(false);
  protected readonly modalError    = signal<string | null>(null);
  protected readonly selectedUser  = signal<UserResponse | null>(null);
  protected readonly pendingAction = signal<ConfirmAction | null>(null);

  protected createForm: CreateUserRequest = { email: '', role: 'STAFF', active: true };
  protected changeEmailValue = '';

  ngOnInit(): void { this.load(); }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);

    const params: UserListParams = { page: this.currentPage(), size: 20 };
    if (this.filterRole)   params.role   = this.filterRole as UserRole;
    if (this.filterActive) params.active = this.filterActive === 'true';
    if (this.searchQuery.trim()) params.q = this.searchQuery.trim();

    this.api.listUsers(params).subscribe({
      next: page => {
        this.users.set(page.content);
        this.totalPages.set(page.totalPages);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar los usuarios. Inténtalo de nuevo.');
        this.loading.set(false);
      },
    });
  }

  protected onSearchChange(): void {
    if (this.searchDebounce) clearTimeout(this.searchDebounce);
    this.searchDebounce = setTimeout(() => { this.currentPage.set(0); this.load(); }, 350);
  }

  protected goToPage(page: number): void { this.currentPage.set(page); this.load(); }

  protected fullName(user: UserResponse): string {
    const parts = [user.firstName, user.lastName].filter(Boolean);
    return parts.length ? parts.join(' ') : '—';
  }

  protected openCreateModal(): void {
    this.createForm = { email: '', role: 'STAFF', active: true };
    this.openModal('create');
  }

  protected openChangeEmailModal(user: UserResponse): void {
    this.selectedUser.set(user);
    this.changeEmailValue = '';
    this.openModal('changeEmail');
  }

  protected confirmDeactivate(user: UserResponse): void {
    this.selectedUser.set(user);
    this.pendingAction.set({
      title: 'Desactivar usuario',
      message: `${user.email} no podrá iniciar sesión.`,
      confirmLabel: 'Desactivar',
      danger: true,
      action: () => this.api.deactivateUser(user.id).subscribe({
        next: () => { this.closeModal(); this.load(); },
        error: err => this.handleModalError(err),
      }),
    });
    this.openModal('confirmAction');
  }

  protected confirmReactivate(user: UserResponse): void {
    this.selectedUser.set(user);
    this.pendingAction.set({
      title: 'Reactivar usuario',
      message: `${user.email} podrá volver a iniciar sesión.`,
      confirmLabel: 'Reactivar',
      danger: false,
      action: () => this.api.reactivateUser(user.id).subscribe({
        next: () => { this.closeModal(); this.load(); },
        error: err => this.handleModalError(err),
      }),
    });
    this.openModal('confirmAction');
  }

  protected confirmForceReset(user: UserResponse): void {
    this.selectedUser.set(user);
    this.pendingAction.set({
      title: 'Forzar restablecimiento de contraseña',
      message: `Se enviará un email de restablecimiento a ${user.email}.`,
      confirmLabel: 'Enviar email',
      danger: false,
      action: () => this.api.forcePasswordReset(user.id).subscribe({
        next: () => this.closeModal(),
        error: err => this.handleModalError(err),
      }),
    });
    this.openModal('confirmAction');
  }

  protected confirmResendInvite(user: UserResponse): void {
    this.selectedUser.set(user);
    this.pendingAction.set({
      title: 'Reenviar invitación',
      message: `Se enviará una nueva invitación a ${user.email}.`,
      confirmLabel: 'Enviar invitación',
      danger: false,
      action: () => this.api.resendInvite(user.id).subscribe({
        next: () => this.closeModal(),
        error: err => this.handleModalError(err),
      }),
    });
    this.openModal('confirmAction');
  }

  protected submitCreate(): void {
    if (!this.createForm.email.trim()) { this.modalError.set('El email es obligatorio.'); return; }
    this.modalLoading.set(true);
    this.modalError.set(null);
    this.api.createUser(this.createForm).subscribe({
      next: () => { this.closeModal(); this.load(); },
      error: err => this.handleModalError(err),
    });
  }

  protected submitChangeEmail(): void {
    if (!this.changeEmailValue.trim()) { this.modalError.set('El nuevo email es obligatorio.'); return; }
    this.modalLoading.set(true);
    this.modalError.set(null);
    this.api.changeUserEmail(this.selectedUser()!.id, { newEmail: this.changeEmailValue.trim() }).subscribe({
      next: () => { this.closeModal(); this.load(); },
      error: err => this.handleModalError(err),
    });
  }

  protected executeAction(): void {
    this.modalLoading.set(true);
    this.modalError.set(null);
    this.pendingAction()?.action();
  }

  protected closeModal(): void {
    this.modalType.set(null);
    this.modalLoading.set(false);
    this.modalError.set(null);
    this.selectedUser.set(null);
    this.pendingAction.set(null);
  }

  private openModal(type: ModalType): void {
    this.modalError.set(null);
    this.modalLoading.set(false);
    this.modalType.set(type);
  }

  private handleModalError(err: unknown): void {
    this.modalLoading.set(false);
    const body    = (err as { error?: { error?: { details?: Record<string, string>; message?: string } } })?.error;
    const details = body?.error?.details;
    if (details) {
      this.modalError.set(Object.values(details).join(' '));
    } else {
      this.modalError.set(body?.error?.message ?? 'Se produjo un error inesperado.');
    }
  }
}
