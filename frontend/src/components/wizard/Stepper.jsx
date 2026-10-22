import Icon from '../ui/Icon'

const PASOS = [
  { numero: 1, label: 'Cliente' },
  { numero: 2, label: 'Dirección' },
  { numero: 3, label: 'Detalle' },
  { numero: 4, label: 'Confirmar' },
]

export default function Stepper({ paso }) {
  return (
    <div className="wizard-stepper" aria-label={`Paso ${paso} de 4`}>
      {PASOS.map((item, index) => (
        <div className="wizard-stepper__item" key={item.numero}>
          <div
            className={`wizard-step ${paso === item.numero ? 'is-active' : ''} ${
              paso > item.numero ? 'is-done' : ''
            }`}
          >
            <span className="wizard-step__dot">
              {paso > item.numero ? <Icon name="check" size={16} strokeWidth={2.4} /> : item.numero}
            </span>
            <span className="wizard-step__label">{item.label}</span>
          </div>
          {index < PASOS.length - 1 && <span className="wizard-step__line" />}
        </div>
      ))}
    </div>
  )
}
