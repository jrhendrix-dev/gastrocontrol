// src/app/core/ui/favicon.service.ts
import { Injectable } from '@angular/core';

/**
 * Manages dynamic favicon swapping.
 *
 * We swap the favicon to GastroControl's own icon when the app is running
 * under the /gastrocontrol portfolio sub-path, and restore the portfolio
 * favicon when leaving (though in practice the SPA never navigates away).
 */
@Injectable({ providedIn: 'root' })
export class FaviconService {

  private readonly GASTROCONTROL_FAVICON = 'assets/pics/favicon.png';
  private readonly PORTFOLIO_FAVICON     = '/logo/favicon.png';

  /**
   * Swaps the browser tab favicon to the GastroControl icon.
   * Call once at app bootstrap.
   */
  useGastroControlFavicon(): void {
    this.setFavicon(this.GASTROCONTROL_FAVICON);
  }

  /**
   * Restores the portfolio favicon.
   * Useful if the user navigates back to the portfolio shell.
   */
  usePortfolioFavicon(): void {
    this.setFavicon(this.PORTFOLIO_FAVICON);
  }

  private setFavicon(href: string): void {
    let link = document.querySelector<HTMLLinkElement>('link[rel~="icon"]');
    if (!link) {
      link = document.createElement('link');
      link.rel  = 'icon';
      document.head.appendChild(link);
    }
    link.href = href;
  }
}
