import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { AuthService, UpdateProfileRequest } from '../../../core/auth/auth.service';
import { ToastService } from '../../../core/ui/toast/toast.service';

type ApiErrorResponse = {
  error: { code: string; details?: Record<string, string> };
};

@Component({
  standalone: true,
  selector: 'gc-me-page',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './me.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MePage {
  private auth = inject(AuthService);
  private fb = inject(FormBuilder);
  private toast = inject(ToastService);

  loading = signal(false);
  saving = signal(false);

  successMsg = signal<string | null>(null);
  formError = signal<string | null>(null);
  fieldErrors = signal<Record<string, string>>({});

  form = this.fb.group({
    firstName: this.fb.control<string | null>(null, [Validators.maxLength(80)]),
    lastName: this.fb.control<string | null>(null, [Validators.maxLength(120)]),
    phone: this.fb.control<string | null>(null, [Validators.maxLength(30)]),
  });

  constructor() {
    this.load();
  }

  me() {
    return this.auth.meSig();
  }

  load() {
    this.loading.set(true);
    this.successMsg.set(null);
    this.formError.set(null);
    this.fieldErrors.set({});

    this.auth
      .me()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (u) => {
          this.formError.set(null); // IMPORTANT
          this.form.patchValue({
            firstName: u.firstName ?? null,
            lastName: u.lastName ?? null,
            phone: u.phone ?? null,
          });
          this.form.markAsPristine();
        },

        error: () => this.formError.set('No se pudo cargar tu perfil.'),
      });
  }

  save() {
    this.fieldErrors.set({});

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const req = this.form.getRawValue();

    this.saving.set(true);
    this.auth.updateProfile(req)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => {
          this.toast.success('Perfil actualizado');
          this.form.markAsPristine();
        },
        error: (err) => this.applyApiError(err),
      });
  }


  private applyApiError(err: any) {
    const body = err?.error;

    if (body?.error?.code === 'VALIDATION_FAILED' && body.error.details) {
      this.fieldErrors.set(body.error.details);
      Object.keys(body.error.details).forEach(k =>
        this.form.get(k)?.markAsTouched()
      );
      return;
    }

    this.toast.error('No se pudo guardar el perfil');
  }

}
