// src/app/core/api/staff/kitchen.models.ts

import { OrderNoteResponse, OrderStatus, OrderType, OrderItemResponse } from './staff.models';

/**
 * Represents a single order as displayed on the Kitchen Display System.
 *
 * <p>This is a projection of the full {@code OrderResponse} narrowed to only the
 * fields the kitchen board needs. We reuse the backend's existing staff response
 * shape — no extra backend work required.</p>
 */
export interface KitchenOrderResponse {
  id: number;
  type: OrderType;
  tableId: number | null;
  status: OrderStatus;
  totalCents: number;
  createdAt: string;
  items: OrderItemResponse[];
  pickup?: { name: string | null; phone: string | null; notes: string | null } | null;
  delivery?: {
    name: string | null;
    phone: string | null;
    addressLine1: string | null;
    city: string | null;
    notes: string | null;
  } | null;

  /** Staff-written operational notes, sorted oldest-first. Never null. */
  notes: OrderNoteResponse[];
}

/**
 * Result payload returned by the change-status endpoint.
 * Mirrors {@code ChangeOrderStatusResult} on the backend.
 */
export interface ChangeOrderStatusResult {
  orderId: number;
  oldStatus: OrderStatus;
  newStatus: OrderStatus;
}

/**
 * The three statuses visible on the kitchen board, in pipeline order.
 */
export type KitchenStatus = 'PENDING' | 'IN_PREPARATION' | 'READY';

/** Maps a kitchen status to its next step in the pipeline. */
export const NEXT_STATUS: Record<KitchenStatus, string> = {
  PENDING: 'IN_PREPARATION',
  IN_PREPARATION: 'READY',
  READY: 'SERVED',
};

/** Human-readable column labels shown on the KDS board. */
export const STATUS_LABEL: Record<KitchenStatus, string> = {
  PENDING: 'Pendiente',
  IN_PREPARATION: 'En preparación',
  READY: 'Listo',
};

/** Call-to-action label for the advance button on each card. */
export const ADVANCE_LABEL: Record<KitchenStatus, string> = {
  PENDING: 'Iniciar',
  IN_PREPARATION: 'Listo',
  READY: 'Servido',
};
