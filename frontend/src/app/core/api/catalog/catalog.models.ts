// src/app/core/api/catalog/catalog.models.ts

/**
 * A single active product as returned by the public catalog endpoint.
 * Safe for unauthenticated use — never includes cost or admin fields.
 */
export interface CatalogProductDto {
  id: number;
  name: string;
  description: string | null;
  /** Server-relative URL of the product image, or null if none uploaded. */
  imageUrl: string | null;
  priceCents: number;
  categoryId: number | null;
  categoryName: string | null;
}

/**
 * A category with its active products embedded.
 * Returned by GET /api/catalog/categories — one call gives the full menu tree.
 */
export interface CatalogCategoryDto {
  id: number;
  name: string;
  /** All active products belonging to this category, sorted by name. */
  products: CatalogProductDto[];
}
