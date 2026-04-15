// src/app/core/api/staff/staff.models.ts

export type OrderType = 'DINE_IN' | 'TAKE_AWAY' | 'DELIVERY';

export type OrderStatus =
  | 'DRAFT'
  | 'PENDING'
  | 'IN_PREPARATION'
  | 'READY'
  | 'SERVED'
  | 'FINISHED'
  | 'CANCELLED'
  | string;

export type PaymentProvider = 'STRIPE' | 'MANUAL';

export type PaymentStatus   = 'REQUIRES_PAYMENT' | 'SUCCEEDED' | 'FAILED' | 'REFUNDED';

// ── Table models ──────────────────────────────────────────────────────────────

export interface DiningTableActiveOrderSummary {
  orderId: number;
  status: OrderStatus;
  totalCents: number;
}

export interface DiningTableResponse {
  id: number;
  label: string;
  activeOrder?: DiningTableActiveOrderSummary | null;
}

// ── Product models ────────────────────────────────────────────────────────────

export interface ProductResponse {
  id: number;
  name: string;
  description: string;
  /** Server-relative URL of the product image, or null if none uploaded. */
  imageUrl?: string | null;
  priceCents: number;
  active: boolean;
  categoryId?: number | null;
  categoryName?: string | null;
}

// ── Order models ──────────────────────────────────────────────────────────────

export interface OrderItemResponse {
  /** Primary key of the order line (order_items.id) — NOT the product id. */
  id: number;
  productId: number;
  name: string;
  quantity: number;
  unitPriceCents: number;
}

/**
 * A single staff-written note attached to an order.
 * Notes are sorted oldest-first by the backend.
 *
 * Edit audit fields:
 * - `originalNote`: the text before the very first edit; null if never edited.
 * - `editedAt`:     ISO-8601 timestamp of the most recent edit; null if never edited.
 */
export interface OrderNoteResponse {
  id: number;
  note: string;
  authorRole: string | null;
  createdAt: string; // ISO-8601

  /** Original text before first edit. null if note was never edited. */
  originalNote: string | null;

  /** Timestamp of most recent edit. null if note was never edited. */
  editedAt: string | null; // ISO-8601
}

export interface OrderResponse {
  id: number;
  type: OrderType;
  tableId?: number | null;
  totalCents: number;
  status: OrderStatus;

  /**
   * ISO-8601 timestamp of when the order was created.
   * Used by the Kitchen Display to compute elapsed time and urgency.
   */
  createdAt: string;

  /**
   * True when the order has been reopened by a manager and is pending
   * a financial adjustment. While true, the order cannot be FINISHED.
   */
  reopened?: boolean;

  /** Payment provider. 'STRIPE' for online orders, 'MANUAL' for cash/POS. null if no payment yet. */
  paymentProvider?: PaymentProvider | null;

  /** Current payment status. null if no payment row exists yet. */
  paymentStatus?: PaymentStatus | null;

  /** Present only for TAKE_AWAY orders. */
  pickup?: { name: string | null; phone: string | null; notes: string | null } | null;

  /** Present only for DELIVERY orders. */
  delivery?: {
    name: string | null;
    phone: string | null;
    addressLine1: string | null;
    city: string | null;
    notes: string | null;
  } | null;

  items: OrderItemResponse[];

  /** Staff notes attached to this order. Empty array if none. */
  notes: OrderNoteResponse[];
}

// ── Request models ────────────────────────────────────────────────────────────

export interface CreateDraftOrderRequest {
  type: OrderType;
  tableId: number;
}

export interface AddOrderItemRequest {
  productId: number;
  quantity: number;
}

export interface UpdateOrderItemQuantityRequest {
  quantity: number;
}

export interface ChangeOrderStatusRequest {
  newStatus: string;
  message?: string | null;
}

export interface AddOrderNoteRequest {
  note: string;
}

export interface UpdateOrderNoteRequest {
  note: string;
}
