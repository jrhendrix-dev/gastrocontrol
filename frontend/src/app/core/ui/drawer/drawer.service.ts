import { Injectable, TemplateRef, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class DrawerService {
  private _open = signal(false);
  private _heading = signal('');
  private _contentTpl = signal<TemplateRef<any> | null>(null);
  private _context = signal<any>({});

  open = this._open.asReadonly();
  heading = this._heading.asReadonly();
  contentTpl = this._contentTpl.asReadonly();
  context = this._context.asReadonly();

  show(opts: { heading: string; tpl: TemplateRef<any>; context?: any }) {
    this._heading.set(opts.heading);
    this._contentTpl.set(opts.tpl);
    this._context.set(opts.context ?? {});
    this._open.set(true);
  }

  close() {
    this._open.set(false);
  }
}
