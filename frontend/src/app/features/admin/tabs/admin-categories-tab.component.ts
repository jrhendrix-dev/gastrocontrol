// src/app/features/admin/tabs/admin-categories-tab.component.ts
import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * Manager Categories tab — placeholder until full category management is built.
 */
@Component({
  selector: 'app-admin-categories-tab',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="stub">
      <span class="stub-icon">🗂️</span>
      <h3>Categories</h3>
      <p>Category management coming soon.</p>
    </div>
  `,
  styles: [`
    .stub {
      display: flex; flex-direction: column; align-items: center;
      justify-content: center; gap: 0.5rem; padding: 4rem 2rem;
      color: #6b7280; text-align: center;
    }
    .stub-icon { font-size: 2.5rem; }
    h3 { color: #9ca3af; margin: 0; font-size: 1.125rem; }
    p { margin: 0; font-size: 0.875rem; }
  `],
})
export class AdminCategoriesTabComponent {}
