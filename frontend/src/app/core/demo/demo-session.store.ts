import { Injectable, signal, computed } from '@angular/core';
import { DemoSession } from './demo-session.model';

const STORAGE_KEY = 'gc_demo_session';

@Injectable({ providedIn: 'root' })
export class DemoSessionStore {

  private readonly _session = signal<DemoSession | null>(this.loadFromStorage());

  /**
   * Incremented every minute by DemoBannerComponent to drive
   * time-dependent computed signals like minutesRemaining.
   */
  readonly _tick = signal(0);

  readonly session = this._session.asReadonly();

  readonly isActive = computed(() => {
    this._tick(); // depend on tick so it recomputes every minute
    const s = this._session();
    if (!s) return false;
    return new Date(s.expiresAt) > new Date();
  });

  readonly sessionId = computed(() =>
    this.isActive() ? this._session()!.sessionId : null
  );

  readonly role = computed(() =>
    this.isActive() ? this._session()!.role : null
  );

  readonly minutesRemaining = computed(() => {
    this._tick(); // depend on tick so it recomputes every minute
    const s = this._session();
    if (!s || !this.isActive()) return 0;
    const diff = new Date(s.expiresAt).getTime() - Date.now();
    return Math.max(0, Math.floor(diff / 60_000));
  });

  /** Called by DemoBannerComponent every minute to trigger recomputation. */
  tick(): void {
    this._tick.update(n => n + 1);
  }

  activate(sessionId: string, role: DemoSession['role']): void {
    const expiresAt = new Date(Date.now() + 2 * 60 * 60 * 1000).toISOString();
    const session: DemoSession = { sessionId, role, expiresAt };
    this._session.set(session);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
  }

  deactivate(): void {
    this._session.set(null);
    localStorage.removeItem(STORAGE_KEY);
  }

  private loadFromStorage(): DemoSession | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return null;
      const session: DemoSession = JSON.parse(raw);
      if (new Date(session.expiresAt) <= new Date()) {
        localStorage.removeItem(STORAGE_KEY);
        return null;
      }
      return session;
    } catch {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
  }
}
