export default function Field({
  label,
  htmlFor,
  required = false,
  error,
  hint,
  children,
  className = '',
}) {
  return (
    <div className={`mb-3 ${className}`}>
      <label className="form-label field-label" htmlFor={htmlFor}>
        {label}
        {required && <span className="field-required">*</span>}
      </label>
      {children}
      {hint && !error && <div className="form-text field-hint">{hint}</div>}
      {error && <div className="field-error">{error}</div>}
    </div>
  )
}
