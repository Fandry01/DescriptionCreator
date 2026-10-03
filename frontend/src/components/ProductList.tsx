import { useMemo, useState } from 'react'
import type { ProductSummary } from '../types/product'

interface ProductListProps {
  filter: 'missing' | 'all'
  products: ProductSummary[]
  loading: boolean
  error: string | null
  onRetry: () => void
  onFilterChange: (filter: 'missing' | 'all') => void
  onSelect: (product: ProductSummary) => void
}

function isDescriptionMissing(descriptionHtml: string | null): boolean {
  if (!descriptionHtml?.trim()) return true
  const document = new DOMParser().parseFromString(descriptionHtml, 'text/html')
  return !(document.body.textContent ?? '').replace(/\u00a0/g, ' ').trim()
}

const dateFormatter = new Intl.DateTimeFormat('en-GB', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
})

function formatDate(value: string): string {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? 'Unknown' : dateFormatter.format(date)
}

export function ProductList({
  filter,
  products,
  loading,
  error,
  onRetry,
  onFilterChange,
  onSelect,
}: ProductListProps) {
  const [search, setSearch] = useState('')
  const filteredProducts = useMemo(() => {
    const query = search.trim().toLocaleLowerCase()
    if (!query) return products

    return products.filter(
      (product) =>
        product.title.toLocaleLowerCase().includes(query) ||
        product.handle.toLocaleLowerCase().includes(query),
    )
  }, [products, search])

  return (
    <section className="page-section" aria-labelledby="products-heading">
      <div className="page-heading-row">
        <div>
          <p className="eyebrow">Catalogue workspace</p>
          <h1 id="products-heading">Product descriptions</h1>
          <p className="page-subtitle">
            {filter === 'missing'
              ? 'Create polished descriptions for products that are currently missing them in Shopify.'
              : 'Open any recent Shopify product to create copy or inspect its version history.'}
          </p>
        </div>
        {!loading && !error && products.length > 0 && (
          <span className="product-count">
            {products.length} {products.length === 1 ? 'product' : 'products'}
          </span>
        )}
      </div>

      <div className="product-filter" aria-label="Filter products" role="group">
        <button
          aria-pressed={filter === 'missing'}
          className={filter === 'missing' ? 'active' : ''}
          onClick={() => onFilterChange('missing')}
          type="button"
        >
          Missing descriptions
        </button>
        <button
          aria-pressed={filter === 'all'}
          className={filter === 'all' ? 'active' : ''}
          onClick={() => onFilterChange('all')}
          type="button"
        >
          All products
        </button>
      </div>

      {loading && (
        <div className="state-panel" role="status">
          <span className="spinner" aria-hidden="true" />
          <div>
            <h2>Loading products</h2>
            <p>Checking Shopify for products without descriptions.</p>
          </div>
        </div>
      )}

      {!loading && error && (
        <div className="state-panel state-panel-error" role="alert">
          <div>
            <h2>Products could not be loaded</h2>
            <p>{error}</p>
          </div>
          <button className="button button-secondary" onClick={onRetry} type="button">
            Try again
          </button>
        </div>
      )}

      {!loading && !error && products.length === 0 && (
        <div className="state-panel state-panel-empty">
          <div className="state-mark" aria-hidden="true">✓</div>
          <div>
            <h2>Everything is up to date</h2>
            <p>No products are currently missing a description.</p>
          </div>
        </div>
      )}

      {!loading && !error && products.length > 0 && (
        <>
          <label className="search-field">
            <span className="sr-only">Search products</span>
            <svg aria-hidden="true" viewBox="0 0 24 24">
              <path d="m21 21-4.6-4.6m2.1-5.15a7.25 7.25 0 1 1-14.5 0 7.25 7.25 0 0 1 14.5 0Z" />
            </svg>
            <input
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Search by title or handle"
              type="search"
              value={search}
            />
          </label>

          {filteredProducts.length === 0 ? (
            <div className="no-results">
              <h2>No matching products</h2>
              <p>Try a different title or handle.</p>
            </div>
          ) : (
            <div className="product-list">
              <div className="product-list-header" aria-hidden="true">
                <span>Product</span>
                <span>Last updated</span>
                <span>Status</span>
                <span />
              </div>
              {filteredProducts.map((product) => {
                const missingDescription = isDescriptionMissing(product.descriptionHtml)

                return <article className="product-row" key={product.id}>
                  <div className="product-identity">
                    <h2>{product.title}</h2>
                    <p>/{product.handle}</p>
                  </div>
                  <div className="product-meta">
                    <span className="mobile-label">Last updated</span>
                    <time dateTime={product.updatedAt}>
                      {formatDate(product.updatedAt)}
                    </time>
                  </div>
                  <div>
                    <span className={`status-badge ${missingDescription ? '' : 'status-badge-available'}`}>
                      <span aria-hidden="true" />
                      {missingDescription ? 'Missing description' : 'Description available'}
                    </span>
                  </div>
                  <button
                    className="button button-secondary row-action"
                    onClick={() => onSelect(product)}
                    type="button"
                  >
                    {missingDescription ? 'Create description' : 'Open product'}
                  </button>
                </article>
              })}
            </div>
          )}
        </>
      )}
    </section>
  )
}
