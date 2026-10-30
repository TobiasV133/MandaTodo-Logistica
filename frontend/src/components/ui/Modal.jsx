import { useEffect, useId, useState } from 'react'
import { createPortal } from 'react-dom'

/**
 * Modal accesible basado en las clases de Bootstrap 5.
 * No requiere el bundle JS de Bootstrap.
 */
export default function Modal({ open, title, children, footer, onClose }) {
  const titleId = useId()
  const [scrollInicial] = useState(() => window.scrollY)

  useEffect(() => {
    if (!open) return undefined

    const onKeyDown = (event) => {
      if (event.key === 'Escape') onClose?.()
    }

    document.addEventListener('keydown', onKeyDown)

    return () => {
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [open, onClose])

  if (!open) return null

  return createPortal(
    <>
      <div className="modal-backdrop fade show" aria-hidden="true" onClick={onClose} />
      <div
        className="modal fade show d-block portal-modal"
        style={{ top: scrollInicial }}
        tabIndex={-1}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        onClick={(event) => {
          if (event.target === event.currentTarget) onClose?.()
        }}
      >
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content">
            <div className="modal-header">
              <h2 className="modal-title h5" id={titleId}>
                {title}
              </h2>
              <button
                type="button"
                className="btn-close"
                aria-label="Cerrar"
                onClick={onClose}
              />
            </div>
            <div className="modal-body">{children}</div>
            {footer && <div className="modal-footer">{footer}</div>}
          </div>
        </div>
      </div>
    </>,
    document.body,
  )
}
