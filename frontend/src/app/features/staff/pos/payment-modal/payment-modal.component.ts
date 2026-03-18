// src/app/features/staff/pos/payment-modal/payment-modal.component.ts
import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  signal,
  computed,
} from '@angular/core';
import { CurrencyPipe, NgClass, NgFor, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';

export type PaymentMethod = 'Efectivo' | 'Tarjeta' | 'Bizum';

export interface PaymentConfirmedEvent {
  method: PaymentMethod;
  reference: string;
}

/**
 * Self-contained payment confirmation modal for the POS.
 *
 * <p>When {@code reopened} is {@code true} the modal enters a blocked state:
 * the payment form is replaced by a clear message instructing staff to ask
 * a manager to process the financial adjustment first. This prevents the
 * raw backend error that would otherwise surface.</p>
 */
@Component({
  selector: 'gc-payment-modal',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgIf, NgFor, NgClass, CurrencyPipe, FormsModule],
  templateUrl: './payment-modal.component.html',
})
export class PaymentModalComponent implements OnChanges {
  @Input() open = false;
  @Input() totalCents = 0;
  @Input() loading = false;
  @Input() error: string | null = null;

  /**
   * When true, the order was reopened by a manager and is pending a financial
   * adjustment. Payment cannot proceed until the manager resolves it via the
   * Operations panel. The modal shows a blocking info message instead of the form.
   */
  @Input() reopened = false;

  @Output() confirmed = new EventEmitter<PaymentConfirmedEvent>();
  @Output() closed    = new EventEmitter<void>();

  readonly methods: PaymentMethod[] = ['Efectivo', 'Tarjeta', 'Bizum'];

  readonly methodIcon: Record<PaymentMethod, string> = {
    Efectivo: '💵',
    Tarjeta:  '💳',
    Bizum:    '📱',
  };

  readonly selectedMethod = signal<PaymentMethod | null>(null);
  referenceValue = '';

  readonly referencePlaceholder = computed<string>(() => {
    switch (this.selectedMethod()) {
      case 'Tarjeta': return 'Últimos 4 dígitos (opcional)';
      case 'Bizum':   return 'Teléfono Bizum (opcional)';
      default:        return 'Referencia (opcional)';
    }
  });

  /** Blocked when order is in the reopen edit window. */
  readonly canConfirm = computed(
    () => this.selectedMethod() !== null && !this.loading && !this.reopened
  );

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open']?.currentValue === true) {
      this.selectedMethod.set(null);
      this.referenceValue = '';
    }
  }

  selectMethod(method: PaymentMethod): void {
    if (this.reopened) return;
    this.selectedMethod.set(method);
  }

  confirm(): void {
    const method = this.selectedMethod();
    if (!method || this.loading || this.reopened) return;
    this.confirmed.emit({ method, reference: this.referenceValue.trim() });
  }

  dismiss(): void {
    if (this.loading) return;
    this.closed.emit();
  }

  trackMethod = (_: number, m: PaymentMethod) => m;
}
