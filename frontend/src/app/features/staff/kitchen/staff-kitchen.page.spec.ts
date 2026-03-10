// src/app/features/staff/kitchen/staff-kitchen.page.spec.ts
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { StaffKitchenPage } from './staff-kitchen.page';
import { StaffKitchenApi } from '@app/app/core/api/staff/staff-kitchen.api';
import { ToastService } from '@app/app/core/ui/toast/toast.service';
import { KitchenOrderResponse } from '@app/app/core/api/staff/kitchen.models';

// ── Helpers ───────────────────────────────────────────────────────────────────

function makeOrder(
  id: number,
  status: KitchenOrderResponse['status'],
  minutesAgo = 5
): KitchenOrderResponse {
  const created = new Date(Date.now() - minutesAgo * 60_000).toISOString();
  return {
    id,
    type: 'DINE_IN',
    tableId: 1,
    status,
    totalCents: 1000,
    createdAt: created,
    items: [{ id: 99, productId: 1, name: 'Burger', quantity: 2, unitPriceCents: 500 }],
  };
}

function makePage(orders: KitchenOrderResponse[]) {
  return { content: orders, totalElements: orders.length, totalPages: 1, number: 0, size: 50 };
}

// ── Test suite ────────────────────────────────────────────────────────────────

describe('StaffKitchenPage', () => {
  let component: StaffKitchenPage;
  let kitchenApi: jasmine.SpyObj<StaffKitchenApi>;
  let toastService: jasmine.SpyObj<ToastService>;

  const pendingOrder  = makeOrder(1, 'PENDING', 3);
  const prepOrder     = makeOrder(2, 'IN_PREPARATION', 10);
  const readyOrder    = makeOrder(3, 'READY', 1);

  beforeEach(() => {
    kitchenApi = jasmine.createSpyObj<StaffKitchenApi>('StaffKitchenApi', [
      'listKitchenOrders',
      'advanceStatus',
    ]);
    toastService = jasmine.createSpyObj<ToastService>('ToastService', [
      'success',
      'error',
    ]);

    kitchenApi.listKitchenOrders.and.returnValue(
      of(makePage([pendingOrder, prepOrder, readyOrder]))
    );

    TestBed.configureTestingModule({
      imports: [StaffKitchenPage],
      providers: [
        { provide: StaffKitchenApi, useValue: kitchenApi },
        { provide: ToastService, useValue: toastService },
      ],
    });

    component = TestBed.createComponent(StaffKitchenPage).componentInstance;
  });

  afterEach(() => {
    component.ngOnDestroy(); // clear poll interval
  });

  // ── Initialisation ─────────────────────────────────────────────────────────

  it('should load orders on init', () => {
    component.ngOnInit();
    expect(kitchenApi.listKitchenOrders).toHaveBeenCalledTimes(1);
    expect(component.orders().length).toBe(3);
    expect(component.loading()).toBeFalse();
  });

  it('should populate column computed signals correctly', () => {
    component.ngOnInit();
    expect(component.pendingOrders().length).toBe(1);
    expect(component.inPreparationOrders().length).toBe(1);
    expect(component.readyOrders().length).toBe(1);
  });

  it('should set lastRefreshed after successful fetch', () => {
    component.ngOnInit();
    expect(component.lastRefreshed()).not.toBeNull();
  });

  // ── Auto-poll ──────────────────────────────────────────────────────────────

  it('should auto-poll every 15 s', fakeAsync(() => {
    component.ngOnInit();
    expect(kitchenApi.listKitchenOrders).toHaveBeenCalledTimes(1);

    tick(15_000);
    expect(kitchenApi.listKitchenOrders).toHaveBeenCalledTimes(2);

    tick(15_000);
    expect(kitchenApi.listKitchenOrders).toHaveBeenCalledTimes(3);

    component.ngOnDestroy(); // stop the timer
  }));

  it('should stop polling on destroy', fakeAsync(() => {
    component.ngOnInit();
    component.ngOnDestroy();
    const callsBefore = kitchenApi.listKitchenOrders.calls.count();

    tick(30_000);
    expect(kitchenApi.listKitchenOrders.calls.count()).toBe(callsBefore);
  }));

  // ── Status advance ─────────────────────────────────────────────────────────

  it('should optimistically move card to IN_PREPARATION and confirm on success', () => {
    component.ngOnInit();

    kitchenApi.advanceStatus.and.returnValue(
      of({ orderId: 1, oldStatus: 'PENDING', newStatus: 'IN_PREPARATION' })
    );

    component.advance(pendingOrder, 'PENDING');

    // Optimistic update applied synchronously
    const updated = component.orders().find((o) => o.id === 1);
    expect(updated?.status).toBe('IN_PREPARATION');
    expect(component.advancingId()).toBeNull(); // cleared after success
    expect(toastService.success).toHaveBeenCalled();
  });

  it('should remove card when advancing READY → SERVED', () => {
    component.ngOnInit();

    kitchenApi.advanceStatus.and.returnValue(
      of({ orderId: 3, oldStatus: 'READY', newStatus: 'SERVED' })
    );

    component.advance(readyOrder, 'READY');

    const removed = component.orders().find((o) => o.id === 3);
    expect(removed).toBeUndefined();
  });

  it('should roll back optimistic update on API error', () => {
    component.ngOnInit();

    kitchenApi.advanceStatus.and.returnValue(throwError(() => ({ message: 'Network error' })));

    component.advance(pendingOrder, 'PENDING');

    // Should be rolled back to original status
    const rolled = component.orders().find((o) => o.id === 1);
    expect(rolled?.status).toBe('PENDING');
    expect(component.advancingId()).toBeNull();
    expect(toastService.error).toHaveBeenCalled();
  });

  it('should not allow a second advance while one is in-flight', () => {
    component.ngOnInit();

    // Simulate in-flight by setting advancingId manually
    component['advancingId'].set(1);

    kitchenApi.advanceStatus.and.returnValue(
      of({ orderId: 2, oldStatus: 'IN_PREPARATION', newStatus: 'READY' })
    );

    component.advance(prepOrder, 'IN_PREPARATION');

    expect(kitchenApi.advanceStatus).not.toHaveBeenCalled();
  });

  // ── Elapsed / urgency ──────────────────────────────────────────────────────

  it('should return "hace un momento" for sub-minute timestamps', () => {
    const now = new Date().toISOString();
    expect(component.elapsed(now)).toBe('hace un momento');
  });

  it('should return minutes for orders under an hour old', () => {
    const tenMinAgo = new Date(Date.now() - 10 * 60_000).toISOString();
    expect(component.elapsed(tenMinAgo)).toBe('hace 10 min');
  });

  it('urgencyClass should return kds-urgent for 20+ minutes PENDING', () => {
    const old = new Date(Date.now() - 25 * 60_000).toISOString();
    expect(component.urgencyClass(old, 'PENDING')).toBe('kds-urgent');
  });

  it('urgencyClass should return kds-warning for 12-19 minutes PENDING', () => {
    const old = new Date(Date.now() - 15 * 60_000).toISOString();
    expect(component.urgencyClass(old, 'PENDING')).toBe('kds-warning');
  });

  it('urgencyClass should always return empty string for READY orders', () => {
    const old = new Date(Date.now() - 30 * 60_000).toISOString();
    expect(component.urgencyClass(old, 'READY')).toBe('');
  });

  // ── Error handling ─────────────────────────────────────────────────────────

  it('should show error toast and stop loading when fetch fails', () => {
    kitchenApi.listKitchenOrders.and.returnValue(throwError(() => new Error('500')));
    component.ngOnInit();
    expect(toastService.error).toHaveBeenCalled();
    expect(component.loading()).toBeFalse();
  });
});
