import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

window.addEventListener('error', (e) => {
  console.error('[WINDOW_ERROR]', e.error?.stack || e.message || e);
});

window.addEventListener('unhandledrejection', (e: PromiseRejectionEvent) => {
  const r: any = e.reason;
  console.error('[UNHANDLED_REJECTION]', r?.stack || r?.message || r);
});


bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
