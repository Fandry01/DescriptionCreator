import { useState } from 'react'
import type { DescriptionVersion } from '../types/product'

interface VersionHistoryProps {
  versions: DescriptionVersion[]
  loading: boolean
  error: string | null
  onRetry: () => void
  onRestore: (version: DescriptionVersion) => Promise<boolean>
  restoringVersionId: number | null
  currentDescriptionHtml: string | null
  currentDescriptionKnown: boolean
  conflict: boolean
  onRefreshCurrent: () => void
  refreshingCurrent: boolean
}

function htmlToPlainText(html: string): string {
  const document = new DOMParser().parseFromString(html, 'text/html')
  return document.body.textContent?.trim() ?? ''
}

function formatDateTime(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}

export function VersionHistory({
  versions,
  loading,
  error,
  onRetry,
  onRestore,
  restoringVersionId,
  currentDescriptionHtml,
  currentDescriptionKnown,
  conflict,
  onRefreshCurrent,
  refreshingCurrent,
}: VersionHistoryProps) {
  const [selectedVersion, setSelectedVersion] =
    useState<DescriptionVersion | null>(null)

  async function confirmRestore() {
    if (!selectedVersion) return
    const restored = await onRestore(selectedVersion)
    if (restored) setSelectedVersion(null)
  }

  const selectedCurrentlyLive = selectedVersion !== null
    && currentDescriptionKnown
    && selectedVersion.publishedDescriptionHtml === currentDescriptionHtml

  return (
    <section className="history-section" aria-labelledby="history-heading">
      <div className="history-heading">
        <div>
          <p className="section-kicker">Published copy</p>
          <h2 id="history-heading">Version history</h2>
          <p>Previously published descriptions for this product.</p>
        </div>
        {!loading && !error && (
          <span className="history-count">
            {versions.length} {versions.length === 1 ? 'version' : 'versions'}
          </span>
        )}
      </div>

      {conflict && (
        <div className="conflict-panel history-conflict" role="alert">
          <div>
            <h2>Shopify description changed</h2>
            <p>
              The Shopify description changed after this page was loaded. Refresh
              the current product state before restoring.
            </p>
          </div>
          <button
            className="button button-secondary"
            disabled={refreshingCurrent}
            onClick={onRefreshCurrent}
            type="button"
          >
            {refreshingCurrent ? 'Refreshing…' : 'Refresh product state'}
          </button>
        </div>
      )}

      {selectedVersion && (
        <div className="restore-confirmation" role="alertdialog" aria-modal="false">
          <div>
            <h3>Restore this version?</h3>
            <p>
              This will replace the current Shopify description with version #{selectedVersion.id}.
              The current description will remain in version history.
            </p>
          </div>
          <div className="restore-confirmation-actions">
            <button
              className="button button-quiet"
              disabled={restoringVersionId !== null}
              onClick={() => setSelectedVersion(null)}
              type="button"
            >
              Cancel
            </button>
            <button
              className="button button-primary"
              disabled={restoringVersionId !== null || !currentDescriptionKnown || selectedCurrentlyLive}
              onClick={confirmRestore}
              type="button"
            >
              {selectedCurrentlyLive
                ? 'Currently live'
                : restoringVersionId === selectedVersion.id
                  ? 'Restoring…'
                  : 'Restore version'}
            </button>
          </div>
        </div>
      )}

      {loading && (
        <div className="history-state" role="status">
          <span className="spinner" aria-hidden="true" /> Loading version history…
        </div>
      )}

      {!loading && error && (
        <div className="history-state history-state-error" role="alert">
          <span>{error}</span>
          <button className="button button-secondary" onClick={onRetry} type="button">
            Try again
          </button>
        </div>
      )}

      {!loading && !error && versions.length === 0 && (
        <div className="history-state">No published versions yet.</div>
      )}

      {!loading && !error && versions.length > 0 && (
        <ol className="history-list">
          {versions.map((version) => {
            const currentlyLive = currentDescriptionKnown
              && version.publishedDescriptionHtml === currentDescriptionHtml
            const restoring = restoringVersionId === version.id
            const preview = htmlToPlainText(version.publishedDescriptionHtml)

            return (
              <li className="history-item" key={version.id}>
                <div className="history-item-meta">
                  <span className={`history-action history-action-${version.action.toLowerCase()}`}>
                    {version.action === 'PUBLISH' ? 'Published' : 'Restored'}
                  </span>
                  <span>Version #{version.id}</span>
                  <time dateTime={version.createdAt}>{formatDateTime(version.createdAt)}</time>
                </div>
                <p className="history-preview">{preview || 'Empty description'}</p>
                {version.action === 'RESTORE' && version.restoredFromVersionId !== null && (
                  <p className="history-origin">
                    Restored from version #{version.restoredFromVersionId}
                  </p>
                )}
                <button
                  className="button button-secondary history-restore-button"
                  disabled={currentlyLive || restoringVersionId !== null || !currentDescriptionKnown}
                  onClick={() => setSelectedVersion(version)}
                  type="button"
                >
                  {currentlyLive ? 'Currently live' : restoring ? 'Restoring…' : 'Restore this version'}
                </button>
              </li>
            )
          })}
        </ol>
      )}
    </section>
  )
}
