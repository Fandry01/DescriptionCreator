import { useCallback, useEffect, useState } from 'react'
import { getProducts } from './api/descriptionApi'
import { ApiError } from './api/descriptionApi'
import { getCurrentUser, getUsage, logout } from './api/authApi'
import { LoginPage } from './components/LoginPage'
import { ProductEditor } from './components/ProductEditor'
import { ProductList } from './components/ProductList'
import type { ProductSummary } from './types/product'
import type { AuthUser, UsageStatus } from './types/auth'
import './App.css'

function App() {
  const [authUser, setAuthUser] = useState<AuthUser | null>(null)
  const [checkingAuth, setCheckingAuth] = useState(true)
  const [usage, setUsage] = useState<UsageStatus | null>(null)
  const [productFilter, setProductFilter] = useState<'missing' | 'all'>('missing')
  const [products, setProducts] = useState<ProductSummary[]>([])
  const [selectedProduct, setSelectedProduct] =
    useState<ProductSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refreshUsage = useCallback(async () => {
    try { setUsage(await getUsage()) } catch { setUsage(null) }
  }, [])

  useEffect(() => {
    getCurrentUser()
      .then((user) => {
        setAuthUser(user)
        void refreshUsage()
      })
      .catch((failure) => {
        if (!(failure instanceof ApiError && failure.status === 401)) {
          setError('Authentication could not be checked. Please try again.')
        }
      })
      .finally(() => setCheckingAuth(false))
  }, [refreshUsage])

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
    if (!authUser) return
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
  }, [authUser, productFilter])

  function handleAuthenticated(user: AuthUser) {
    setAuthUser(user)
    setLoading(true)
    setError(null)
    void refreshUsage()
  }

  async function handleLogout() {
    try { await logout() } finally {
      setAuthUser(null)
      setUsage(null)
      setProducts([])
      setSelectedProduct(null)
    }
  }

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

  if (checkingAuth) {
    return <div className="auth-loading" role="status"><span className="spinner" /> Checking session…</div>
  }

  if (!authUser) return <LoginPage onAuthenticated={handleAuthenticated} />

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
        <div className="header-account">
          {usage && <span className="usage-indicator">{usage.used} / {usage.limit} generations</span>}
          <span className="account-name">{authUser.displayName || authUser.email}</span>
          <button className="button button-quiet" onClick={() => void handleLogout()} type="button">Logout</button>
        </div>
      </header>

      <main className="app-main">
        {selectedProduct ? (
          <ProductEditor
            onGenerated={() => void refreshUsage()}
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
