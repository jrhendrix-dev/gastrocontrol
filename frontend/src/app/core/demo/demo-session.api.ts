import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

interface DemoSessionResponse {
  success: boolean;
  data: { sessionId: string };
}

/**
 * HTTP client for the demo session provisioning endpoint.
 * This is a public endpoint — no auth required.
 */
@Injectable({ providedIn: 'root' })
export class DemoSessionApi {
  private readonly http = inject(HttpClient);

  /**
   * Provisions a new isolated demo schema on the backend
   * and returns the session ID to use as X-Demo-Session header.
   */
  createSession(): Observable<string> {
    return this.http
      .post<DemoSessionResponse>('/api/demo/session', {})
      .pipe(map(res => res.data.sessionId));
  }
}
