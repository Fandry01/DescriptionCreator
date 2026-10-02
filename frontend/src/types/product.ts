export interface ProductSummary {
  id: string
  title: string
  handle: string
  descriptionHtml: string | null
  createdAt: string
  updatedAt: string
}

export interface ProductFacts {
  brand: string
  model: string
  color: string
  grade: string
  gradeLabel: string
  material: string
  hardware: string
  includes: string
  size: string
  dimensions: string
  chainLength: string
  strapLength: string
  year: string
  signsOfWear: string
}

export interface ProductDescriptionDraft {
  productId: string
  title: string
  handle: string
  existingDescriptionHtml: string | null
  generatedDescription: string
  facts: ProductFacts
}

export interface PublishDescriptionRequest {
  description: string
  expectedExistingDescriptionHtml: string | null
}

export interface PublishDescriptionResponse {
  productId: string
  handle: string
  publishedDescriptionHtml: string
}
