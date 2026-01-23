import { Injectable, signal } from '@angular/core';

const LS_KEY = 'gc_access_token';

@Injectable({ providedIn: 'root' })
export class TokenStore {
  private tokenSig = signal<string | null>(localStorage.getItem(LS_KEY));

  accessToken(): string | null {
    const t = this.tokenSig();
    return t && t.trim().length ? t : null;
  }

  setAccessToken(token: string) {
    this.tokenSig.set(token);
    localStorage.setItem(LS_KEY, token);
  }

  clear() {
    this.tokenSig.set(null);
    localStorage.removeItem(LS_KEY);
  }
}
