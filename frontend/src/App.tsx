import { useCallback, useEffect, useState } from 'react'
import { getProducts } from './api/descriptionApi'
import { ProductEditor } from './components/ProductEditor'
import { ProductList } from './components/ProductList'
import type { ProductSummary } from './types/product'
import './App.css'

function App() {
  const [productFilter, setProductFilter] = useState<'missing' | 'all'>('missing')
  const [products, setProducts] = useState<ProductSummary[]>([])
  const [selectedProduct, setSelectedProduct] =
    useState<ProductSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadProducts = useCallback(async () => {
    setLoading(true)
    setError(null)

    try {
      setProducts(await getProducts(productFilter === 'missing'))
    } catch {
      setError('We could not connect to Shopify. Check the backend and try again.')
    } finally {
      setLoading(false)
    }
  }, [productFilter])

  useEffect(() => {
    const controller = new AbortController()
    let active = true

    getProducts(productFilter === 'missing', controller.signal)
      .then((loadedProducts) => {
        if (active) setProducts(loadedProducts)
      })
      .catch(() => {
        if (active) {
          setError('We could not connect to Shopify. Check the backend and try again.')
        }
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
      controller.abort()
    }
  }, [productFilter])

  function handlePublished(handle: string, publishedDescriptionHtml: string) {
    setProducts((current) => productFilter === 'missing'
      ? current.filter((product) => product.handle !== handle)
      : current.map((product) => product.handle === handle
        ? { ...product, descriptionHtml: publishedDescriptionHtml }
        : product),
    )
  }

  function handleProductFilterChange(filter: 'missing' | 'all') {
    if (filter === productFilter) return
    setProductFilter(filter)
    setLoading(true)
    setError(null)
  }

  return (
    <div className="app-shell">
      <header className="app-header">
        <button
          aria-label="Go to product descriptions"
          className="brand"
          onClick={() => setSelectedProduct(null)}
          type="button"
        >
          <span className="brand-monogram" aria-hidden="true">DS</span>
          <span>
            <strong>Designer Stories</strong>
            <small>Description Creator</small>
          </span>
        </button>
        <span className="environment-label">Internal workspace</span>
      </header>

      <main className="app-main">
        {selectedProduct ? (
          <ProductEditor
            onBack={() => setSelectedProduct(null)}
            onPublished={handlePublished}
            product={selectedProduct}
          />
        ) : (
          <ProductList
            error={error}
            filter={productFilter}
            loading={loading}
            onFilterChange={handleProductFilterChange}
            onRetry={loadProducts}
            onSelect={setSelectedProduct}
            products={products}
          />
        )}
      </main>

      <footer className="app-footer">
        <span>Designer Stories</span>
        <span>Description publishing is limited to approved copy only.</span>
      </footer>
    </div>
  )
}

export default App
