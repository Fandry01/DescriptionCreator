import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  ApiError,
  generateProductDescription,
  publishProductDescription,
} from '../api/descriptionApi'
import type {
  ProductDescriptionDraft,
  ProductSummary,
  PublishDescriptionResponse,
} from '../types/product'
import { ProductFacts } from './ProductFacts'

interface ProductEditorProps {
  product: ProductSummary
  onBack: () => void
  onPublished: (handle: string) => void
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
}: ProductEditorProps) {
  const [draft, setDraft] = useState<ProductDescriptionDraft | null>(null)
  const [description, setDescription] = useState('')
  const [generating, setGenerating] = useState(true)
  const [generationError, setGenerationError] = useState<string | null>(null)
  const [publishing, setPublishing] = useState(false)
  const [publishError, setPublishError] = useState<string | null>(null)
  const [conflict, setConflict] = useState(false)
  const [published, setPublished] =
    useState<PublishDescriptionResponse | null>(null)

  const loadDraft = useCallback(async () => {
    setGenerating(true)
    setGenerationError(null)
    setPublishError(null)
    setConflict(false)
    setDraft(null)

    try {
      const generatedDraft = await generateProductDescription(product.handle)
      setDraft(generatedDraft)
      setDescription(generatedDraft.generatedDescription)
    } catch {
      setGenerationError(
        'A description could not be generated. Please try again.',
      )
    } finally {
      setGenerating(false)
    }
  }, [product.handle])

  useEffect(() => {
    const controller = new AbortController()
    let active = true

    generateProductDescription(product.handle, controller.signal)
      .then((generatedDraft) => {
        if (!active) return
        setDraft(generatedDraft)
        setDescription(generatedDraft.generatedDescription)
      })
      .catch(() => {
        if (active) {
          setGenerationError(
            'A description could not be generated. Please try again.',
          )
        }
      })
      .finally(() => {
        if (active) setGenerating(false)
      })

    return () => {
      active = false
      controller.abort()
    }
  }, [product.handle])

  const existingDescription = useMemo(
    () => htmlToPlainText(draft?.existingDescriptionHtml ?? null),
    [draft?.existingDescriptionHtml],
  )
  const wordCount = useMemo(() => countWords(description), [description])

  async function handlePublish() {
    if (!draft || !description.trim() || publishing) return

    setPublishing(true)
    setPublishError(null)
    setConflict(false)

    try {
      const result = await publishProductDescription(draft.handle, {
        description,
        expectedExistingDescriptionHtml: draft.existingDescriptionHtml,
      })
      setPublished(result)
      onPublished(draft.handle)
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

  if (published) {
    return (
      <section className="success-view" aria-labelledby="success-heading">
        <div className="success-icon" aria-hidden="true">✓</div>
        <p className="eyebrow">Published to Shopify</p>
        <h1 id="success-heading">Description published</h1>
        <p>
          The new description for <strong>{product.title}</strong> is now live in
          Shopify.
        </p>
        <button className="button button-primary" onClick={onBack} type="button">
          Back to products
        </button>
      </section>
    )
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

      {generating && (
        <div className="generating-panel" role="status">
          <span className="spinner spinner-large" aria-hidden="true" />
          <h2>Creating a new description</h2>
          <p>Writing a polished draft from the available product details.</p>
        </div>
      )}

      {!generating && generationError && (
        <div className="state-panel state-panel-error" role="alert">
          <div>
            <h2>Draft generation failed</h2>
            <p>{generationError}</p>
          </div>
          <button className="button button-secondary" onClick={loadDraft} type="button">
            Try again
          </button>
        </div>
      )}

      {!generating && draft && (
        <>
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
              <label className="editor-field">
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
              </div>
            </section>
          </div>

          {conflict && (
            <div className="conflict-panel" role="alert">
              <div>
                <h2>Shopify description changed</h2>
                <p>
                  The Shopify description changed after this draft was generated.
                  Generate a new draft before publishing.
                </p>
              </div>
              <button className="button button-secondary" onClick={loadDraft} type="button">
                Generate new draft
              </button>
            </div>
          )}

          {publishError && (
            <div className="inline-error" role="alert">
              {publishError}
            </div>
          )}

          <div className="editor-actions">
            <button className="button button-quiet" onClick={onBack} type="button">
              Back without publishing
            </button>
            <button
              className="button button-primary"
              disabled={!description.trim() || publishing || conflict}
              onClick={handlePublish}
              type="button"
            >
              {publishing ? (
                <><span className="button-spinner" aria-hidden="true" /> Publishing…</>
              ) : (
                'Publish description'
              )}
            </button>
          </div>
        </>
      )}
    </section>
  )
}
