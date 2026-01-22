import { Injectable, signal } from '@angular/core';

/**
 * Holds auth tokens in-memory (and optionally persists them).
 * This service MUST NOT depend on HttpClient to avoid DI cycles with interceptors.
 */
@Injectable({ providedIn: 'root' })
export class TokenStore {
  private _accessToken = signal<string | null>(null);

  accessToken(): string | null {
    return this._accessToken();
  }

  setAccessToken(token: string | null): void {
    this._accessToken.set(token);
  }

  clear(): void {
    this._accessToken.set(null);
  }
}
