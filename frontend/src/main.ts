// src/main.ts
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';
import { FaviconService } from './app/core/ui/favicon.service';

window.addEventListener('error', (e) => {
  console.error('[WINDOW_ERROR]', e.error?.stack || e.message || e);
});

window.addEventListener('unhandledrejection', (e: PromiseRejectionEvent) => {
  const r: any = e.reason;
  console.error('[UNHANDLED_REJECTION]', r?.stack || r?.message || r);
});

bootstrapApplication(App, appConfig)
  .then(appRef => {
    appRef.injector.get(FaviconService).useGastroControlFavicon();
  })
  .catch((err) => console.error(err));
