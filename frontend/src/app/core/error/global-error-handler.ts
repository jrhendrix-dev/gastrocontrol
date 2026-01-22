import { ErrorHandler, Injectable } from '@angular/core';

@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  handleError(error: unknown): void {
    // Raw dump. No processing.
    // eslint-disable-next-line no-console
    console.error('[GC][RAW_ERROR]', error);
  }
}
