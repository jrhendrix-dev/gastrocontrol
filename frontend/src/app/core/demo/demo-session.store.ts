import { Injectable, signal, computed } from '@angular/core';
import { DemoSession } from './demo-session.model';

const STORAGE_KEY = 'gc_demo_session';

/**
 * Signal-based store for the active demo session.
 *
 * We persist the session to localStorage so page refreshes don't
 * lose the session ID. On load we validate the expiry and clear
 * stale sessions automatically.
 */
@Injectable({ providedIn: 'root' })
export class DemoSessionStore {

  private readonly _session = signal<DemoSession | null>(this.loadFromStorage());

  /** The current demo session, or null if not in demo mode. */
  readonly session = this._session.asReadonly();

  /** True when a valid (non-expired) demo session is active. */
  readonly isActive = computed(() => {
    const s = this._session();
    if (!s) return false;
    return new Date(s.expiresAt) > new Date();
  });

  /** The session ID to send as X-Demo-Session header. */
  readonly sessionId = computed(() =>
    this.isActive() ? this._session()!.sessionId : null
  );

  /** The active demo role, or null. */
  readonly role = computed(() =>
    this.isActive() ? this._session()!.role : null
  );

  /** Minutes remaining before the session expires. */
  readonly minutesRemaining = computed(() => {
    const s = this._session();
    if (!s || !this.isActive()) return 0;
    const diff = new Date(s.expiresAt).getTime() - Date.now();
    return Math.max(0, Math.floor(diff / 60_000));
  });

  /**
   * Saves a new demo session to the store and localStorage.
   *
   * @param sessionId the ID returned by the backend
   * @param role the role the user selected
   */
  activate(sessionId: string, role: DemoSession['role']): void {
    const expiresAt = new Date(Date.now() + 2 * 60 * 60 * 1000).toISOString();
    const session: DemoSession = { sessionId, role, expiresAt };
    this._session.set(session);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
  }

  /**
   * Clears the demo session from the store and localStorage.
   */
  deactivate(): void {
    this._session.set(null);
    localStorage.removeItem(STORAGE_KEY);
  }

  /**
   * Loads the session from localStorage and validates it has not expired.
   * Returns null if absent or expired.
   */
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
