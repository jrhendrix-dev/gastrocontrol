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

export interface ProductResponse {
  id: number;
  name: string;
  description: string;
  priceCents: number;
  active: boolean;
  categoryId?: number | null;
  categoryName?: string | null;
}

export interface OrderItemResponse {
  id: number; // order_items.id
  productId: number;
  name: string;
  quantity: number;
  unitPriceCents: number;
}

export interface OrderResponse {
  id: number;
  type: OrderType;
  tableId?: number | null;
  totalCents: number;
  status: OrderStatus;
  items: OrderItemResponse[];
}

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
