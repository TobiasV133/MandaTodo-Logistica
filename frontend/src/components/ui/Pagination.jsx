/**
 * Footer de paginación reutilizable, con el mismo diseño que Auditoría.
 */
export default function Pagination({
  page,
  totalItems,
  pageSize = 10,
  onPageChange,
  ariaLabel = 'Paginación',
}) {
  if (totalItems === 0) return null

  const totalPages = Math.max(1, Math.ceil(totalItems / pageSize))
  const currentPage = Math.min(page, totalPages)
  const start = (currentPage - 1) * pageSize

  return (
    <div className="audit-pagination">
      <span className="text-secondary small">
        Mostrando {start + 1}–{Math.min(start + pageSize, totalItems)} de {totalItems}
      </span>

      <nav aria-label={ariaLabel}>
        <ul className="pagination pagination-sm mb-0">
          <li className={`page-item ${currentPage <= 1 ? 'disabled' : ''}`}>
            <button
              type="button"
              className="page-link"
              onClick={() => onPageChange(Math.max(1, currentPage - 1))}
              disabled={currentPage <= 1}
            >
              Anterior
            </button>
          </li>
          <li className="page-item disabled">
            <span className="page-link">
              Página {currentPage} de {totalPages}
            </span>
          </li>
          <li className={`page-item ${currentPage >= totalPages ? 'disabled' : ''}`}>
            <button
              type="button"
              className="page-link"
              onClick={() => onPageChange(Math.min(totalPages, currentPage + 1))}
              disabled={currentPage >= totalPages}
            >
              Siguiente
            </button>
          </li>
        </ul>
      </nav>
    </div>
  )
}
