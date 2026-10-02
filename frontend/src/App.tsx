import { useCallback, useEffect, useState } from 'react'
import { getMissingDescriptionProducts } from './api/descriptionApi'
import { ProductEditor } from './components/ProductEditor'
import { ProductList } from './components/ProductList'
import type { ProductSummary } from './types/product'
import './App.css'

function App() {
  const [products, setProducts] = useState<ProductSummary[]>([])
  const [selectedProduct, setSelectedProduct] =
    useState<ProductSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadProducts = useCallback(async () => {
    setLoading(true)
    setError(null)

    try {
      setProducts(await getMissingDescriptionProducts())
    } catch {
      setError('We could not connect to Shopify. Check the backend and try again.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    let active = true

    getMissingDescriptionProducts(controller.signal)
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
  }, [])

  function handlePublished(handle: string) {
    setProducts((current) =>
      current.filter((product) => product.handle !== handle),
    )
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
            loading={loading}
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
