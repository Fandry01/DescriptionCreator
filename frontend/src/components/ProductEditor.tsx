import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  ApiError,
  generateProductDescription,
  getDescriptionHistory,
  getProductCurrentState,
  publishProductDescription,
  restoreDescriptionVersion,
} from '../api/descriptionApi'
import type {
  DescriptionVersion,
  ProductDescriptionDraft,
  ProductSummary,
  PublishDescriptionResponse,
} from '../types/product'
import { ProductFacts } from './ProductFacts'
import { VersionHistory } from './VersionHistory'

interface ProductEditorProps {
  product: ProductSummary
  onBack: () => void
  onPublished: (handle: string, publishedDescriptionHtml: string) => void
  onGenerated: () => void
}

function htmlToPlainText(html: string | null): string {
  if (!html) return ''
  const document = new DOMParser().parseFromString(html, 'text/html')
  return document.body.textContent?.trim() ?? ''
}

function countWords(value: string): number {
  const trimmed = value.trim()
  return trimmed ? trimmed.split(/\s+/).length : 0
}

export function ProductEditor({
  product,
  onBack,
  onPublished,
  onGenerated,
}: ProductEditorProps) {
  const [draft, setDraft] = useState<ProductDescriptionDraft | null>(null)
  const [description, setDescription] = useState('')
  const [generating, setGenerating] = useState(false)
  const [generationError, setGenerationError] = useState<string | null>(null)
  const [publishing, setPublishing] = useState(false)
  const [publishError, setPublishError] = useState<string | null>(null)
  const [conflict, setConflict] = useState(false)
  const [published, setPublished] =
    useState<PublishDescriptionResponse | null>(null)
  const [currentDescriptionHtml, setCurrentDescriptionHtml] =
    useState<string | null>(product.descriptionHtml)
  const [currentDescriptionKnown, setCurrentDescriptionKnown] = useState(true)
  const [history, setHistory] = useState<DescriptionVersion[]>([])
  const [historyLoading, setHistoryLoading] = useState(true)
  const [historyError, setHistoryError] = useState<string | null>(null)
  const [restoringVersionId, setRestoringVersionId] = useState<number | null>(null)
  const [restoreConflict, setRestoreConflict] = useState(false)
  const [restoreError, setRestoreError] = useState<string | null>(null)
  const [restoreSuccess, setRestoreSuccess] = useState<string | null>(null)
  const [refreshingCurrent, setRefreshingCurrent] = useState(false)

  const loadHistory = useCallback(async (signal?: AbortSignal) => {
    setHistoryLoading(true)
    setHistoryError(null)

    try {
      setHistory(await getDescriptionHistory(product.handle, signal))
    } catch {
      if (!signal?.aborted) {
        setHistoryError('Version history could not be loaded. Please try again.')
      }
    } finally {
      if (!signal?.aborted) setHistoryLoading(false)
    }
  }, [product.handle])

  const loadDraft = useCallback(async (signal?: AbortSignal) => {
    try {
      const generatedDraft = await generateProductDescription(product.handle, signal)
      if (signal?.aborted) return
      setDraft(generatedDraft)
      setDescription(generatedDraft.generatedDescription)
      setCurrentDescriptionHtml(generatedDraft.existingDescriptionHtml)
      setCurrentDescriptionKnown(true)
      setPublished(null)
      setConflict(false)
      onGenerated()
    } catch (error) {
      if (signal?.aborted) return
      setGenerationError(error instanceof ApiError && error.status === 429
        ? 'Your monthly description generation limit has been reached.'
        : 'A description could not be generated. Please try again.')
    } finally {
      if (!signal?.aborted) setGenerating(false)
    }
  }, [onGenerated, product.handle])

  function generateDraft() {
    setGenerating(true)
    setGenerationError(null)
    setPublishError(null)
    void loadDraft()
  }

  useEffect(() => {
    const controller = new AbortController()
    let active = true

    getDescriptionHistory(product.handle, controller.signal)
      .then((versions) => {
        if (active) setHistory(versions)
      })
      .catch(() => {
        if (active) {
          setHistoryError('Version history could not be loaded. Please try again.')
        }
      })
      .finally(() => {
        if (active) setHistoryLoading(false)
      })

    return () => {
      active = false
      controller.abort()
    }
  }, [product.handle])

  const existingDescription = useMemo(
    () => htmlToPlainText(currentDescriptionHtml),
    [currentDescriptionHtml],
  )
  const wordCount = useMemo(() => countWords(description), [description])

  async function handlePublish() {
    if (!draft || !description.trim() || publishing || !currentDescriptionKnown) return

    setPublishing(true)
    setPublishError(null)
    setConflict(false)
    setPublished(null)
    setRestoreSuccess(null)

    try {
      const result = await publishProductDescription(draft.handle, {
        description,
        expectedExistingDescriptionHtml: currentDescriptionHtml,
      })
      setPublished(result)
      setCurrentDescriptionHtml(result.publishedDescriptionHtml)
      setCurrentDescriptionKnown(true)
      onPublished(draft.handle, result.publishedDescriptionHtml)
      void loadHistory()
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        setConflict(true)
      } else {
        setPublishError(
          'The description could not be published. Please try again.',
        )
      }
    } finally {
      setPublishing(false)
    }
  }

  async function handleRestore(version: DescriptionVersion): Promise<boolean> {
    if (
      restoringVersionId !== null
      || !currentDescriptionKnown
      || version.publishedDescriptionHtml === currentDescriptionHtml
    ) return false

    setRestoringVersionId(version.id)
    setRestoreConflict(false)
    setRestoreError(null)
    setRestoreSuccess(null)
    setPublished(null)

    try {
      const result = await restoreDescriptionVersion(draft?.handle ?? product.handle, version.id, {
        expectedExistingDescriptionHtml: currentDescriptionHtml,
      })
      setCurrentDescriptionHtml(result.publishedDescriptionHtml)
      setCurrentDescriptionKnown(true)
      setRestoreSuccess(`Version #${version.id} was restored and published to Shopify.`)
      await loadHistory()
      return true
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        setRestoreConflict(true)
      } else if (error instanceof ApiError && error.status === 404) {
        setRestoreError('This version is no longer available for this product.')
      } else {
        setRestoreError('The version could not be restored. Please try again.')
      }
      return false
    } finally {
      setRestoringVersionId(null)
    }
  }

  async function refreshCurrentProductState() {
    if (refreshingCurrent) return
    setRefreshingCurrent(true)
    setRestoreError(null)

    try {
      const currentProduct = await getProductCurrentState(product.handle)
      setCurrentDescriptionHtml(currentProduct.descriptionHtml)
      setCurrentDescriptionKnown(true)
      setRestoreConflict(false)
      setRestoreSuccess(null)
    } catch {
      setRestoreError(
        'The current Shopify description could not be refreshed. Return to the product list and try again.',
      )
    } finally {
      setRefreshingCurrent(false)
    }
  }

  return (
    <section className="editor-page" aria-labelledby="editor-heading">
      <button className="back-link" onClick={onBack} type="button">
        <span aria-hidden="true">←</span> Back to products
      </button>

      <div className="editor-heading">
        <p className="eyebrow">Description editor</p>
        <h1 id="editor-heading">{draft?.title ?? product.title}</h1>
        <p>Review every detail and edit the draft before publishing.</p>
      </div>

      {generationError && (
        <div className="state-panel state-panel-error" role="alert">
          <div>
            <h2>Draft generation failed</h2>
            <p>{generationError}</p>
          </div>
          <button className="button button-secondary" onClick={generateDraft} type="button">
            Try again
          </button>
        </div>
      )}

      {draft && (
          <section className="facts-section" aria-labelledby="facts-heading">
            <div className="section-heading-inline">
              <div>
                <p className="section-kicker">Shopify details</p>
                <h2 id="facts-heading">Product information</h2>
              </div>
              <span className="read-only-label">Read only</span>
            </div>
            <ProductFacts facts={draft.facts} />
          </section>
      )}

      <div className="comparison-grid">
            <section className="comparison-panel" aria-labelledby="existing-heading">
              <div className="comparison-heading">
                <div>
                  <span className="comparison-number">01</span>
                  <h2 id="existing-heading">Existing description</h2>
                </div>
                <span className="read-only-label">Read only</span>
              </div>
              <div className={`existing-copy ${existingDescription ? '' : 'empty-copy'}`}>
                {existingDescription || 'No existing description.'}
              </div>
            </section>

            <section className="comparison-panel comparison-panel-new" aria-labelledby="new-heading">
              <div className="comparison-heading">
                <div>
                  <span className="comparison-number">02</span>
                  <h2 id="new-heading">New description</h2>
                </div>
                <span className="editable-label">Editable</span>
              </div>
              {draft ? <><label className="editor-field">
                <span className="sr-only">New product description</span>
                <textarea
                  aria-describedby="description-count"
                  onChange={(event) => setDescription(event.target.value)}
                  rows={12}
                  value={description}
                />
              </label>
              <div className="description-count" id="description-count">
                <span>{wordCount} {wordCount === 1 ? 'word' : 'words'}</span>
                <span>{description.length} characters</span>
              </div></> : (
                <div className="generate-empty-state">
                  {generating ? <>
                    <span className="spinner spinner-large" aria-hidden="true" />
                    <h3>Creating a new description</h3>
                    <p>Writing a polished draft from the available product details.</p>
                  </> : <>
                    <h3>Ready to create a draft?</h3>
                    <p>Generation only starts when you choose it and will count towards monthly usage.</p>
                    <button className="button button-primary" onClick={generateDraft} type="button">
                      Generate description
                    </button>
                  </>}
                </div>
              )}
            </section>
          </div>

          {published && (
            <div className="success-banner" role="status">
              <span aria-hidden="true">✓</span>
              <div>
                <strong>Description published</strong>
                <p>The approved description is now live in Shopify.</p>
              </div>
            </div>
          )}

          {conflict && (
            <div className="conflict-panel" role="alert">
              <div>
                <h2>Shopify description changed</h2>
                <p>
                  The Shopify description changed after this draft was generated.
                  Generate a new draft before publishing.
                </p>
              </div>
              <button className="button button-secondary" onClick={generateDraft} type="button">
                Generate new draft
              </button>
            </div>
          )}

          {publishError && (
            <div className="inline-error" role="alert">
              {publishError}
            </div>
          )}

          {draft && <div className="editor-actions">
            <button className="button button-quiet" onClick={onBack} type="button">
              Back without publishing
            </button>
            <button
              className="button button-primary"
              disabled={!description.trim() || publishing || conflict || !currentDescriptionKnown}
              onClick={handlePublish}
              type="button"
            >
              {publishing ? (
                <><span className="button-spinner" aria-hidden="true" /> Publishing…</>
              ) : (
                'Publish description'
              )}
            </button>
          </div>}

          {restoreSuccess && (
            <div className="success-banner" role="status">
              <span aria-hidden="true">✓</span>
              <div><strong>Version restored</strong><p>{restoreSuccess}</p></div>
            </div>
          )}

          {restoreError && <div className="inline-error" role="alert">{restoreError}</div>}

          <VersionHistory
            conflict={restoreConflict}
            currentDescriptionHtml={currentDescriptionHtml}
            currentDescriptionKnown={currentDescriptionKnown}
            error={historyError}
            loading={historyLoading}
            onRefreshCurrent={refreshCurrentProductState}
            onRestore={handleRestore}
            onRetry={() => void loadHistory()}
            refreshingCurrent={refreshingCurrent}
            restoringVersionId={restoringVersionId}
            versions={history}
          />
    </section>
  )
}
