import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';


import { AuthService } from '../../core/auth/auth.service';
import { FormErrorMapper } from '@app/app/core/error/form-error/form-error-mapper.service';
import { ToastService } from '../../core/ui/toast/toast.service';

@Component({
  standalone: true,
  selector: 'gc-login-page',
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './login.page.html',
})
export class LoginPage {
  private fb = inject(FormBuilder);
  auth = inject(AuthService);
  private router = inject(Router);
  private mapper = inject(FormErrorMapper);
  private toast = inject(ToastService);

  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  submit() {
    if (this.form.invalid) return;

    this.auth.login(this.form.getRawValue() as any).subscribe({
      next: () => {
        this.toast.success('Login correcto');
        void this.router.navigateByUrl('/');
      },
      error: (err: HttpErrorResponse) => {
        const map = this.mapper.applyToForm(
          this.form as any,
          { email: 'email', password: 'password' },
          err
        );
        if (!map.applied) this.mapper.toastFromDetails(map.norm, this.toast, err);
      },
    });
  }
}
