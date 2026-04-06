/**
 * Represents an active demo session stored in localStorage.
 */
export interface DemoSession {
  /** The session ID returned by the backend — sent as X-Demo-Session header. */
  sessionId: string;
  /** The role the user selected on the demo page. */
  role: 'ADMIN' | 'MANAGER' | 'STAFF';
  /** ISO timestamp when this session expires (2 hours from creation). */
  expiresAt: string;
}
