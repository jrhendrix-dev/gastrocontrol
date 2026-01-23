import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth/auth.service';

@Component({
  standalone: true,
  imports: [RouterOutlet],
  selector: 'gc-root',
  template: `<router-outlet />`,
})
export class AppComponent {
  private auth = inject(AuthService);

  constructor() {
    this.auth.hydrate();
  }
}
