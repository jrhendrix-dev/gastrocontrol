// src/app/core/api/catalog/catalog.models.ts

export interface CatalogProductDto {
  id: number;
  name: string;
  description: string | null;
  imageUrl: string | null;
  priceCents: number;
  categoryId: number | null;
  categoryName: string | null;
}

export interface CatalogCategoryDto {
  id: number;
  name: string;
  products: CatalogProductDto[];
}
