import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, finalize, map, of, switchMap, tap, throwError } from 'rxjs';
import { environment } from '@app/environment/environment';
import {
  ApiResponse,
  LoginRequest,
  LoginResponse,
  MeResponse,
  RefreshResponse,
} from './auth.types';
import { TokenStore } from './token-store.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private tokenStore = inject(TokenStore);
  private API = environment.apiBase;

  private _me = signal<MeResponse | null>(null);
  meSig = this._me;
  loading = signal(false);

  accessToken(): string | null {
    return this.tokenStore.accessToken();
  }

  private setAccessToken(token: string) {
    this.tokenStore.setAccessToken(token);
  }

  private clearAccessToken() {
    this.tokenStore.clear();
  }

  /**
   * Hydrate session:
   * - if token exists -> try /me
   * - if /me fails with 401 -> refresh once -> try /me again
   * - if still fails -> clear local auth
   */
  hydrate() {
    const t = this.accessToken();
    if (!t) return;

    // NOTE: keep it simple; app bootstrap can call this once.
    this.me().pipe(
      catchError(() =>
        this.refresh().pipe(
          switchMap(() => this.me()),
          catchError(() => {
            this.clearAuthLocal();
            return of(null);
          }),
        ),
      ),
    ).subscribe();
  }

  loggedIn(): boolean {
    return !!this.accessToken();
  }

  roles(): string[] {
    const me = this._me();
    return Array.isArray(me?.roles) ? me!.roles : [];
  }

  email(): string {
    return this._me()?.email ?? '';
  }

  fullName(): string {
    const u = this._me();
    const name = [u?.firstName, u?.lastName].filter(Boolean).join(' ').trim();
    return name || u?.email || 'Cuenta';
  }

  login(req: LoginRequest) {
    this.loading.set(true);

    return this.http
      .post<ApiResponse<LoginResponse>>(`${this.API}/api/auth/login`, req, {
        withCredentials: true,
      })
      .pipe(
        tap(res => this.setAccessToken(res.data.accessToken)),
        switchMap(() => this.me()), // load user after login
        tap(() => undefined),
        finalize(() => this.loading.set(false)),
        catchError(err => throwError(() => err)),
      );
  }

  me() {
    return this.http.get<ApiResponse<MeResponse>>(`${this.API}/api/me`).pipe(
      map(res => res.data),
      tap(u => this._me.set(u)),
    );
  }

  refresh() {
    return this.http
      .post<ApiResponse<RefreshResponse>>(
        `${this.API}/api/auth/refresh`,
        {},
        { withCredentials: true },
      )
      .pipe(tap(res => this.setAccessToken(res.data.accessToken)));
  }

  logout() {
    return this.http
      .post<ApiResponse<void>>(`${this.API}/api/auth/logout`, {}, {
        withCredentials: true,
      })
      .pipe(
        finalize(() => this.clearAuthLocal()),
        catchError(() => {
          this.clearAuthLocal();
          return of(null);
        }),
        map(() => null),
      );
  }

  clearAuthLocal() {
    this.clearAccessToken();
    this._me.set(null);
  }
}
