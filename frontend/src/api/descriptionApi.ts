import type {
  DescriptionVersion,
  ProductDescriptionDraft,
  ProductSummary,
  PublishDescriptionRequest,
  PublishDescriptionResponse,
  RestoreDescriptionRequest,
  RestoreDescriptionResponse,
} from '../types/product'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

async function requestJson<T>(
  path: string,
  options?: RequestInit,
): Promise<T> {
  let response: Response

  try {
    response = await fetch(`${API_BASE_URL}${path}`, options)
  } catch {
    throw new ApiError(0, 'Unable to reach the server.')
  }

  if (!response.ok) {
    throw new ApiError(response.status, `Request failed with status ${response.status}.`)
  }

  try {
    return (await response.json()) as T
  } catch {
    throw new ApiError(response.status, 'The server returned an invalid response.')
  }
}

export function getMissingDescriptionProducts(
  signal?: AbortSignal,
): Promise<ProductSummary[]> {
  return requestJson<ProductSummary[]>('/api/products?missingDescription=true', {
    signal,
  })
}

export function generateProductDescription(
  handle: string,
  signal?: AbortSignal,
): Promise<ProductDescriptionDraft> {
  return requestJson<ProductDescriptionDraft>(
    `/api/products/handle/${encodeURIComponent(handle)}/generate-description`,
    { method: 'POST', signal },
  )
}

export function publishProductDescription(
  handle: string,
  request: PublishDescriptionRequest,
): Promise<PublishDescriptionResponse> {
  return requestJson<PublishDescriptionResponse>(
    `/api/products/handle/${encodeURIComponent(handle)}/publish-description`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    },
  )
}

export function getDescriptionHistory(
  handle: string,
  signal?: AbortSignal,
): Promise<DescriptionVersion[]> {
  return requestJson<DescriptionVersion[]>(
    `/api/products/handle/${encodeURIComponent(handle)}/description-history`,
    { signal },
  )
}

export function restoreDescriptionVersion(
  handle: string,
  versionId: number,
  request: RestoreDescriptionRequest,
): Promise<RestoreDescriptionResponse> {
  return requestJson<RestoreDescriptionResponse>(
    `/api/products/handle/${encodeURIComponent(handle)}/description-history/${versionId}/restore`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    },
  )
}

export async function getProductCurrentState(
  handle: string,
  signal?: AbortSignal,
): Promise<ProductSummary> {
  const products = await requestJson<ProductSummary[]>('/api/products', { signal })
  const product = products.find((candidate) => candidate.handle === handle)

  if (!product) {
    throw new ApiError(404, 'The product was not found in the current Shopify product list.')
  }

  return product
}
