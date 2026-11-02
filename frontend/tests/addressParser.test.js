import assert from 'node:assert/strict'
import test from 'node:test'

import {
  convertirAltura,
  normalizarCalle,
  normalizarDireccionIngresada,
  normalizarLocalidad,
  parsearDireccion,
} from '../src/utils/addressParser.js'

const casosDireccion = [
  // Alias de CABA.
  [
    'Corrientes 1234 capfed',
    { calle: 'Corrientes', altura: '1234', localidad: 'CABA', pisoDepto: '' },
  ],
  [
    'Corrientes 1234 CapFed',
    { calle: 'Corrientes', altura: '1234', localidad: 'CABA', pisoDepto: '' },
  ],
  [
    'Corrientes 1234 capital federal',
    { calle: 'Corrientes', altura: '1234', localidad: 'CABA', pisoDepto: '' },
  ],

  // Ruta Provincial / Nacional: el número es parte de la calle.
  [
    'Ruta Provincial 4 1800 montegrande',
    {
      calle: 'Ruta Provincial 4',
      altura: '1800',
      localidad: 'Monte Grande',
      pisoDepto: '',
    },
  ],
  [
    'ruta provincial 4 1800 montegrande',
    {
      calle: 'Ruta Provincial 4',
      altura: '1800',
      localidad: 'Monte Grande',
      pisoDepto: '',
    },
  ],
  [
    'Ruta Nacional 3 500 CABA',
    {
      calle: 'Ruta Nacional 3',
      altura: '500',
      localidad: 'CABA',
      pisoDepto: '',
    },
  ],
  [
    'Ruta Provincial 4 1800',
    { calle: 'Ruta Provincial 4', altura: '1800', localidad: '', pisoDepto: '' },
  ],

  // Traza de kilómetros pegada o desglosada.
  [
    'ruta8km20 1500 estebanecheverria',
    {
      calle: 'Ruta 8 Km 20',
      altura: '1500',
      localidad: 'Esteban Echeverría',
      pisoDepto: '',
    },
  ],
  [
    'Ruta 8 Km 20 1500 Esteban Echeverría',
    {
      calle: 'Ruta 8 Km 20',
      altura: '1500',
      localidad: 'Esteban Echeverría',
      pisoDepto: '',
    },
  ],
  [
    'ruta 8 km20 1500 CABA',
    {
      calle: 'Ruta 8 Km 20',
      altura: '1500',
      localidad: 'CABA',
      pisoDepto: '',
    },
  ],
  [
    'ruta8km20 1500',
    { calle: 'Ruta 8 Km 20', altura: '1500', localidad: '', pisoDepto: '' },
  ],
  [
    'ruta 8 km 20',
    { calle: 'Ruta 8 Km 20', altura: '', localidad: '', pisoDepto: '' },
  ],

  // Piso / depto complejo.
  [
    'corrientes 1234 1er piso dpto b caba',
    {
      calle: 'Corrientes',
      altura: '1234',
      localidad: 'CABA',
      pisoDepto: '1º B',
    },
  ],
  [
    '1er piso dpto b',
    { calle: '', altura: '', localidad: '', pisoDepto: '1º B' },
  ],
  [
    'corrientes 1234 1º piso depto b CABA',
    {
      calle: 'Corrientes',
      altura: '1234',
      localidad: 'CABA',
      pisoDepto: '1º B',
    },
  ],
  [
    'corrientes 1234 3bis caba',
    {
      calle: 'Corrientes',
      altura: '1234',
      localidad: 'CABA',
      pisoDepto: '3 bis',
    },
  ],
  [
    'corrientes 1234 3bis',
    { calle: 'Corrientes', altura: '1234', localidad: '', pisoDepto: '3 bis' },
  ],
  [
    'papa juan xxiii 1500 3er piso capfed',
    {
      calle: 'Papa Juan Xxiii',
      altura: '1500',
      localidad: 'CABA',
      pisoDepto: '3º',
    },
  ],
  [
    '25 de mayo 450 3bis vicentelopez',
    {
      calle: '25 de Mayo',
      altura: '450',
      localidad: 'Vicente López',
      pisoDepto: '3 bis',
    },
  ],
  [
    'corrientes 1234 3b caba',
    {
      calle: 'Corrientes',
      altura: '1234',
      localidad: 'CABA',
      pisoDepto: '3 B',
    },
  ],
  [
    'corrientes 1234 4a caba',
    {
      calle: 'Corrientes',
      altura: '1234',
      localidad: 'CABA',
      pisoDepto: '4 A',
    },
  ],
  [
    'corrientes 1234 1b caba',
    {
      calle: 'Corrientes',
      altura: '1234',
      localidad: 'CABA',
      pisoDepto: '1 B',
    },
  ],
  [
    'ruta8km20 1500 3b estebanecheverria',
    {
      calle: 'Ruta 8 Km 20',
      altura: '1500',
      localidad: 'Esteban Echeverría',
      pisoDepto: '3 B',
    },
  ],

  // Calle numerada con sufijo bis y número sobrante antes de la altura.
  [
    'calle 50bis 450 1234 3er piso b lomasdezamora',
    {
      calle: 'Calle 50bis',
      altura: '1234',
      localidad: 'Lomas de Zamora',
      pisoDepto: '3º B',
    },
  ],
  [
    'calle 50bis 450 1234',
    { calle: 'Calle 50bis', altura: '1234', localidad: '', pisoDepto: '' },
  ],

  // Planta baja / PB con departamento numérico o alfabético.
  [
    '3 de febrero 800 planta baja 3 bsas',
    {
      calle: '3 de Febrero',
      altura: '800',
      localidad: 'Buenos Aires',
      pisoDepto: 'Planta Baja 3',
    },
  ],
  [
    '3 de febrero 800 PB 3 bsas',
    {
      calle: '3 de Febrero',
      altura: '800',
      localidad: 'Buenos Aires',
      pisoDepto: 'PB 3',
    },
  ],
  [
    '3 de febrero 800 planta baja b bsas',
    {
      calle: '3 de Febrero',
      altura: '800',
      localidad: 'Buenos Aires',
      pisoDepto: 'Planta Baja B',
    },
  ],
  [
    'planta baja 3',
    { calle: '', altura: '', localidad: '', pisoDepto: 'Planta Baja 3' },
  ],
  [
    'PB 3',
    { calle: '', altura: '', localidad: '', pisoDepto: 'PB 3' },
  ],

  // Córdoba Capital no debe confundirse con CABA.
  [
    'Mariano Moreno 88, Piso 1, Depto D, Córdoba Capital',
    {
      calle: 'Mariano Moreno',
      altura: '88',
      localidad: 'Córdoba',
      pisoDepto: 'Piso 1 Depto D',
    },
  ],
  [
    'Córdoba Capital',
    { calle: '', altura: '', localidad: 'Córdoba', pisoDepto: '' },
  ],

  // Altura sin número.
  [
    'Sarmiento S/N Piso 7 Depto F Bahía Blanca',
    {
      calle: 'Sarmiento',
      altura: 'S/N',
      localidad: 'Bahía Blanca',
      pisoDepto: 'Piso 7 Depto F',
    },
  ],
  [
    'Sarmiento s/n Piso 7 Depto F Bahía Blanca',
    {
      calle: 'Sarmiento',
      altura: 'S/N',
      localidad: 'Bahía Blanca',
      pisoDepto: 'Piso 7 Depto F',
    },
  ],
  [
    'Sarmiento sin numero Piso 7 Depto F Bahía Blanca',
    {
      calle: 'Sarmiento',
      altura: 'S/N',
      localidad: 'Bahía Blanca',
      pisoDepto: 'Piso 7 Depto F',
    },
  ],
  [
    'Sarmiento sin número Piso 7 Depto F Bahía Blanca',
    {
      calle: 'Sarmiento',
      altura: 'S/N',
      localidad: 'Bahía Blanca',
      pisoDepto: 'Piso 7 Depto F',
    },
  ],

  // Altura cero válida.
  [
    'Sarmiento 0 Piso 7 Depto F Bahía Blanca',
    {
      calle: 'Sarmiento',
      altura: '0',
      localidad: 'Bahía Blanca',
      pisoDepto: 'Piso 7 Depto F',
    },
  ],

  // Regresiones de pisos, fechas, calles numeradas y localidades.
  [
    'corrientes 1234 4to a caba',
    {
      calle: 'Corrientes',
      altura: '1234',
      localidad: 'CABA',
      pisoDepto: '4º A',
    },
  ],
  [
    'corrientes 1234 2da caba',
    {
      calle: 'Corrientes',
      altura: '1234',
      localidad: 'CABA',
      pisoDepto: '2ª',
    },
  ],
  [
    '3 de febrero 800 piso 2 depto 4 bsas',
    {
      calle: '3 de Febrero',
      altura: '800',
      localidad: 'Buenos Aires',
      pisoDepto: 'Piso 2 Depto 4',
    },
  ],
  [
    'Avenida 12 de Octubre 450 1234 quilmes',
    {
      calle: 'Avenida 12 de Octubre',
      altura: '1234',
      localidad: 'Quilmes',
      pisoDepto: '',
    },
  ],
  [
    '9 de Julio 450 1234 CABA',
    {
      calle: '9 de Julio',
      altura: '1234',
      localidad: 'CABA',
      pisoDepto: '',
    },
  ],
  [
    'Calle 50 1234 3 bis CABA',
    {
      calle: 'Calle 50',
      altura: '1234',
      localidad: 'CABA',
      pisoDepto: '3 bis',
    },
  ],
  [
    'Calle 50bis 1234 CABA',
    {
      calle: 'Calle 50bis',
      altura: '1234',
      localidad: 'CABA',
      pisoDepto: '',
    },
  ],
  [
    'Av. Corrientes 1234 3º B CABA',
    {
      calle: 'Avenida Corrientes',
      altura: '1234',
      localidad: 'CABA',
      pisoDepto: '3º B',
    },
  ],
  [
    'Avenida San Martín',
    {
      calle: 'Avenida San Martín',
      altura: '',
      localidad: '',
      pisoDepto: '',
    },
  ],
  [
    'Av. Corrientes, Villa Ballester',
    {
      calle: 'Avenida Corrientes',
      altura: '',
      localidad: 'Villa Ballester',
      pisoDepto: '',
    },
  ],
  [
    'S/N',
    { calle: 'S/N', altura: '', localidad: '', pisoDepto: '' },
  ],
]

for (const [entrada, esperado] of casosDireccion) {
  test(`parsearDireccion: ${entrada}`, () => {
    assert.deepEqual(parsearDireccion(entrada), esperado)
  })
}

test('normalizarLocalidad reconoce los alias de CABA', () => {
  assert.equal(normalizarLocalidad('capfed'), 'CABA')
  assert.equal(normalizarLocalidad('CapFed'), 'CABA')
  assert.equal(normalizarLocalidad('capital federal'), 'CABA')
  assert.equal(normalizarLocalidad('C.A.B.A.'), 'CABA')
  assert.equal(normalizarLocalidad('Capital'), 'CABA')
})

test('normalizarLocalidad resuelve Córdoba Capital sin derivar a CABA', () => {
  assert.equal(normalizarLocalidad('Córdoba Capital'), 'Córdoba')
  assert.equal(normalizarLocalidad('cordoba capital'), 'Córdoba')
})

test('normalizarLocalidad conserva la grafía canónica del GBA', () => {
  assert.equal(normalizarLocalidad('estebanecheverria'), 'Esteban Echeverría')
  assert.equal(normalizarLocalidad('vicentelopez'), 'Vicente López')
  assert.equal(normalizarLocalidad('bahia blanca'), 'Bahía Blanca')
})

test('normalizarCalle expande abreviaturas y respeta conectores', () => {
  assert.equal(normalizarCalle('av. corrientes'), 'Avenida Corrientes')
  assert.equal(normalizarCalle('gral. san martín'), 'General San Martín')
  assert.equal(normalizarCalle('pje. 3 de febrero'), 'Pasaje 3 de Febrero')
})

test('convertirAltura mantiene las regresiones de palabras numéricas', () => {
  assert.equal(convertirAltura('mil doscientos treinta'), '1230')
  assert.equal(convertirAltura('Mildoscientostreintaycuatro'), '1234')
  assert.equal(convertirAltura('1 2 34'), '1234')
  assert.equal(convertirAltura('S/N'), 'S/N')
  assert.equal(convertirAltura('sin número'), 'S/N')
  assert.equal(convertirAltura('0'), '0')
  assert.equal(convertirAltura('1234 bis'), '1234 bis')
})

test('normalizarDireccionIngresada no sobrescribe campos manuales', () => {
  const { direccion, cambios } = normalizarDireccionIngresada({
    calle: 'corrientes 1234 3bis capfed',
    altura: '999',
    localidad: 'Rosario',
    pisoDepto: 'A',
  })

  assert.deepEqual(direccion, {
    calle: 'Corrientes',
    altura: '999',
    localidad: 'Rosario',
    pisoDepto: 'A',
  })
  assert.deepEqual(cambios, ['calle “corrientes 1234 3bis capfed” → “Corrientes”'])
})

test('normalizarDireccionIngresada conserva altura 0 y S/N', () => {
  const cero = normalizarDireccionIngresada({
    calle: 'Sarmiento 999',
    altura: '0',
    localidad: '',
    pisoDepto: '',
  }).direccion

  assert.equal(cero.altura, '0')

  const sinNumero = normalizarDireccionIngresada({
    calle: 'Sarmiento S/N Piso 7 Depto F Bahía Blanca',
    altura: '',
    localidad: '',
    pisoDepto: '',
  }).direccion

  assert.deepEqual(sinNumero, {
    calle: 'Sarmiento',
    altura: 'S/N',
    localidad: 'Bahía Blanca',
    pisoDepto: 'Piso 7 Depto F',
  })
})

test('normalizarDireccionIngresada completa el ejemplo de CABA con 3er piso', () => {
  const { direccion } = normalizarDireccionIngresada({
    calle: 'papa juan xxiii 1500 3er piso capfed',
    altura: '',
    localidad: '',
    pisoDepto: '',
  })

  assert.deepEqual(direccion, {
    calle: 'Papa Juan Xxiii',
    altura: '1500',
    localidad: 'CABA',
    pisoDepto: '3º',
  })
})
