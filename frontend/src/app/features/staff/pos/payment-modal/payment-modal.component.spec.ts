// src/app/features/staff/pos/payment-modal/payment-modal.component.spec.ts
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PaymentModalComponent, PaymentMethod } from './payment-modal.component';

describe('PaymentModalComponent', () => {
  let component: PaymentModalComponent;
  let fixture: ComponentFixture<PaymentModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaymentModalComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(PaymentModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ── Visibility ─────────────────────────────────────────────────────────────

  it('should expose all three payment methods', () => {
    expect(component.methods).toContain('Efectivo');
    expect(component.methods).toContain('Tarjeta');
    expect(component.methods).toContain('Bizum');
  });

  // ── Method selection ───────────────────────────────────────────────────────

  it('should start with no method selected', () => {
    expect(component.selectedMethod()).toBeNull();
  });

  it('should select a method on selectMethod()', () => {
    component.selectMethod('Efectivo');
    expect(component.selectedMethod()).toBe('Efectivo');
  });

  it('should replace previous selection on second selectMethod()', () => {
    component.selectMethod('Tarjeta');
    component.selectMethod('Bizum');
    expect(component.selectedMethod()).toBe('Bizum');
  });

  // ── canConfirm ─────────────────────────────────────────────────────────────

  it('should not be confirmable with no method selected', () => {
    expect(component.canConfirm()).toBeFalse();
  });

  it('should be confirmable once a method is selected', () => {
    component.selectMethod('Efectivo');
    expect(component.canConfirm()).toBeTrue();
  });

  it('should not be confirmable while loading even if method is selected', () => {
    component.selectMethod('Tarjeta');
    component.loading = true;
    expect(component.canConfirm()).toBeFalse();
  });

  // ── confirm() ─────────────────────────────────────────────────────────────

  it('should emit confirmed event with method and reference on confirm()', () => {
    const spy = jasmine.createSpy('confirmed');
    component.confirmed.subscribe(spy);

    component.selectMethod('Tarjeta');
    component.referenceValue = '1234';
    component.confirm();

    expect(spy).toHaveBeenCalledWith({ method: 'Tarjeta', reference: '1234' });
  });

  it('should trim whitespace from reference on confirm()', () => {
    const spy = jasmine.createSpy('confirmed');
    component.confirmed.subscribe(spy);

    component.selectMethod('Efectivo');
    component.referenceValue = '  efectivo ref  ';
    component.confirm();

    expect(spy).toHaveBeenCalledWith({ method: 'Efectivo', reference: 'efectivo ref' });
  });

  it('should not emit confirmed when no method is selected', () => {
    const spy = jasmine.createSpy('confirmed');
    component.confirmed.subscribe(spy);

    component.confirm();

    expect(spy).not.toHaveBeenCalled();
  });

  it('should not emit confirmed when loading is true', () => {
    const spy = jasmine.createSpy('confirmed');
    component.confirmed.subscribe(spy);

    component.selectMethod('Bizum');
    component.loading = true;
    component.confirm();

    expect(spy).not.toHaveBeenCalled();
  });

  // ── dismiss() ─────────────────────────────────────────────────────────────

  it('should emit closed on dismiss()', () => {
    const spy = jasmine.createSpy('closed');
    component.closed.subscribe(spy);

    component.dismiss();

    expect(spy).toHaveBeenCalled();
  });

  it('should not emit closed while loading', () => {
    const spy = jasmine.createSpy('closed');
    component.closed.subscribe(spy);

    component.loading = true;
    component.dismiss();

    expect(spy).not.toHaveBeenCalled();
  });

  // ── ngOnChanges reset ─────────────────────────────────────────────────────

  it('should reset selection and reference when opened', () => {
    component.selectMethod('Efectivo');
    component.referenceValue = 'some ref';

    // Simulate parent setting open = true (modal re-opening)
    component.ngOnChanges({
      open: {
        currentValue: true,
        previousValue: false,
        firstChange: false,
        isFirstChange: () => false,
      },
    });

    expect(component.selectedMethod()).toBeNull();
    expect(component.referenceValue).toBe('');
  });

  it('should NOT reset when open changes to false', () => {
    component.selectMethod('Bizum');

    component.ngOnChanges({
      open: {
        currentValue: false,
        previousValue: true,
        firstChange: false,
        isFirstChange: () => false,
      },
    });

    // State preserved (irrelevant while closed but confirms no unintended side effects)
    expect(component.selectedMethod()).toBe('Bizum');
  });

  // ── Reference placeholder ─────────────────────────────────────────────────

  it('should show card-specific placeholder for Tarjeta', () => {
    component.selectMethod('Tarjeta');
    expect(component.referencePlaceholder()).toContain('4 dígitos');
  });

  it('should show Bizum-specific placeholder for Bizum', () => {
    component.selectMethod('Bizum');
    expect(component.referencePlaceholder()).toContain('Teléfono');
  });

  it('should show generic placeholder for Efectivo', () => {
    component.selectMethod('Efectivo');
    expect(component.referencePlaceholder()).toBe('Referencia (opcional)');
  });
});
