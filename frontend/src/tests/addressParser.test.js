import assert from 'node:assert/strict'
import test from 'node:test'

import { parsearDireccion } from '../utils/addressParser.js'

test('parsearDireccion reconoce piso/depto entre comillas y conserva la localidad', () => {
  const esperado = {
    calle: 'Avenida Cabildo',
    altura: '2341',
    localidad: 'Belgrano',
    pisoDepto: 'Piso 4 Depto B',
  }

  assert.deepEqual(
    parsearDireccion('Av. Cabildo 2341, Piso 4, Depto "B", Belgrano'),
    esperado,
  )
  assert.deepEqual(
    parsearDireccion("Av. Cabildo 2341, Piso 4, Depto 'B', Belgrano"),
    esperado,
  )
  assert.deepEqual(
    parsearDireccion('Av. Cabildo 2341, Piso 4, Depto “B”, Belgrano'),
    esperado,
  )
})
