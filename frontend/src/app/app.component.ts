import { Component, inject, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth/auth.service';

/**
 * Root application component.
 *
 * Kicks off the auth bootstrap sequence on startup:
 * attempts a silent token refresh, then loads the current user.
 * The authReady signal is set to true once this settles (success or failure),
 * which unblocks the route guards from making their access decision.
 */
@Component({
  standalone: true,
  imports: [RouterOutlet],
  selector: 'gc-root',
  template: `<router-outlet />`,
})
export class AppComponent implements OnInit {
  private auth = inject(AuthService);

  ngOnInit(): void {
    this.auth.bootstrap().subscribe();
  }
}
