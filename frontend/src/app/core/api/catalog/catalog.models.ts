export interface CatalogCategoryDto {
  id: number;
  name: string;
  // add fields if your DTO has them (e.g., sortOrder)
}

export interface CatalogProductDto {
  id: number;
  name: string;
  priceCents: number;
  categoryId: number | null;
  categoryName?: string | null;
}
