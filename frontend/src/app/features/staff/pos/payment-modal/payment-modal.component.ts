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

/** The three manual payment methods supported at the POS. */
export type PaymentMethod = 'Efectivo' | 'Tarjeta' | 'Bizum';

/** Emitted when staff confirm a payment. */
export interface PaymentConfirmedEvent {
  /** Human-readable method label — stored as manualReference on the payment row. */
  method: PaymentMethod;
  /** Optional free-text reference (e.g. last 4 card digits, Bizum phone). */
  reference: string;
}

/**
 * Self-contained payment confirmation modal for the POS.
 *
 * <p>Displays the order total, lets staff select a payment method, optionally
 * enter a reference, and confirm. On confirm it emits {@link PaymentConfirmedEvent}
 * so the parent page can fire the API calls. The modal never calls the API itself —
 * it is a pure UI component.</p>
 *
 * <p>Visibility is controlled by the parent via the {@code open} input.
 * The modal emits {@code closed} when the user dismisses it, and
 * {@code confirmed} when the form is submitted.</p>
 *
 * <h3>Usage</h3>
 * <pre>
 * &lt;gc-payment-modal
 *   [open]="showPaymentModal()"
 *   [totalCents]="order().totalCents"
 *   [loading]="paymentLoading()"
 *   (confirmed)="onPaymentConfirmed($event)"
 *   (closed)="showPaymentModal.set(false)"
 * /&gt;
 * </pre>
 */
@Component({
  selector: 'gc-payment-modal',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgIf, NgFor, NgClass, CurrencyPipe, FormsModule],
  templateUrl: './payment-modal.component.html',
})
export class PaymentModalComponent implements OnChanges {
  /** Controls visibility. Parent sets to true/false. */
  @Input() open = false;

  /** The order total to display, in cents. */
  @Input() totalCents = 0;

  /** When true, disables the confirm button and shows a loading indicator. */
  @Input() loading = false;

  /** Error message to display inside the modal, or null. */
  @Input() error: string | null = null;

  /** Emitted when staff confirm the payment. */
  @Output() confirmed = new EventEmitter<PaymentConfirmedEvent>();

  /** Emitted when the modal is dismissed (backdrop click or Escape / cancel). */
  @Output() closed = new EventEmitter<void>();

  /** Available payment methods. */
  readonly methods: PaymentMethod[] = ['Efectivo', 'Tarjeta', 'Bizum'];

  /** Icon map for each method. */
  readonly methodIcon: Record<PaymentMethod, string> = {
    Efectivo: '💵',
    Tarjeta:  '💳',
    Bizum:    '📱',
  };

  /** The currently selected method signal. */
  readonly selectedMethod = signal<PaymentMethod | null>(null);

  /** Optional reference text (last 4 digits, phone, etc.). */
  referenceValue = '';

  /** The reference placeholder tailored to the selected method. */
  readonly referencePlaceholder = computed<string>(() => {
    switch (this.selectedMethod()) {
      case 'Tarjeta': return 'Últimos 4 dígitos (opcional)';
      case 'Bizum':   return 'Teléfono Bizum (opcional)';
      default:        return 'Referencia (opcional)';
    }
  });

  /** True when the form is ready to submit. */
  readonly canConfirm = computed(
    () => this.selectedMethod() !== null && !this.loading
  );

  // ── Lifecycle ─────────────────────────────────────────────────────────────

  ngOnChanges(changes: SimpleChanges): void {
    // Reset internal state every time the modal is opened fresh.
    if (changes['open']?.currentValue === true) {
      this.selectedMethod.set(null);
      this.referenceValue = '';
    }
  }

  // ── Actions ───────────────────────────────────────────────────────────────

  /**
   * Selects a payment method chip.
   *
   * @param method the method to select
   */
  selectMethod(method: PaymentMethod): void {
    this.selectedMethod.set(method);
  }

  /**
   * Emits the {@code confirmed} event with the selected method and reference.
   * No-ops if no method is selected or a request is already in flight.
   */
  confirm(): void {
    const method = this.selectedMethod();
    if (!method || this.loading) return;

    this.confirmed.emit({
      method,
      reference: this.referenceValue.trim(),
    });
  }

  /**
   * Emits the {@code closed} event. Only allowed when not loading
   * to prevent accidental dismissal mid-request.
   */
  dismiss(): void {
    if (this.loading) return;
    this.closed.emit();
  }

  // ── TrackBy ───────────────────────────────────────────────────────────────
  trackMethod = (_: number, m: PaymentMethod) => m;
}
