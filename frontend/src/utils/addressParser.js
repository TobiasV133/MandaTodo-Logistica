/**
 * Normalización inteligente de direcciones para la carga de pedidos.
 *
 * Resuelve tres casos frecuentes del wizard:
 *   1. Altura escrita con palabras ("mil doscientos treinta" -> "1230").
 *   2. Dirección completa pegada en el campo Calle
 *      ("av. corrientes 1234 caba" -> calle "Avenida Corrientes",
 *      altura "1234", localidad "CABA").
 *   3. Abreviaturas de calle ("av." -> "Avenida", "gral." -> "General").
 *
 * El módulo es puro (sin React ni acceso al DOM) para poder probarlo de forma
 * aislada. Las funciones que reciben la dirección completa respetan los campos
 * ya cargados por el operario: altura, localidad y piso/depto solo se completan
 * cuando están vacíos.
 */

const CONECTORES = new Set(['de', 'del', 'la', 'las', 'los', 'y', 'e', 'el', 'al', 'en'])

const MESES = new Set([
  'enero',
  'febrero',
  'marzo',
  'abril',
  'mayo',
  'junio',
  'julio',
  'agosto',
  'septiembre',
  'setiembre',
  'octubre',
  'noviembre',
  'diciembre',
  // Abreviaturas frecuentes en direcciones ("12 de Oct.").
  'ene',
  'feb',
  'mar',
  'abr',
  'may',
  'jun',
  'jul',
  'ago',
  'sep',
  'set',
  'oct',
  'nov',
  'dic',
])

/**
 * Palabras que, dentro de una dirección completa, indican que el número que
 * sigue pertenece al nombre de la calle y no a la altura.
 * Ej.: "Calle 50 1234", "Ruta 8 km 20".
 */
const DESIGNadores = new Set([
  'calle',
  'ruta',
  'provincial',
  'nacional',
  'km',
  'kilometro',
  'kilometros',
  'piso',
  'planta',
  'baja',
  'pb',
  'depto',
  'dpto',
  'dto',
  'departamento',
  'casa',
  'lote',
  'manzana',
  'mz',
  'torre',
])

/** Palabras que introducen el piso en una dirección compuesta. */
const DESIGNACIONES_PISO = new Set(['piso', 'planta'])

/** Palabras que introducen el departamento. */
const DESIGNACIONES_DEPTO = new Set([
  'depto',
  'dpto',
  'dto',
  'departamento',
  'unidad',
])

/**
 * Ordinales de piso escritos con letras ("4to", "2da") y su forma canónica.
 * La clave es el texto normalizado (sin tildes ni mayúsculas).
 */
const ORDINALES_PISO = new Map([
  ['1ro', '1º'],
  ['1ero', '1º'],
  ['1er', '1º'],
  ['1ra', '1ª'],
  ['1era', '1ª'],
  ['2do', '2º'],
  ['2da', '2ª'],
  ['3ro', '3º'],
  ['3ero', '3º'],
  ['3er', '3º'],
  ['3ra', '3ª'],
  ['3era', '3ª'],
  ['4to', '4º'],
  ['4ta', '4ª'],
  ['5to', '5º'],
  ['5ta', '5ª'],
  ['6to', '6º'],
  ['6ta', '6ª'],
  ['7mo', '7º'],
  ['7ma', '7ª'],
  ['8vo', '8º'],
  ['8va', '8ª'],
  ['9no', '9º'],
  ['9na', '9ª'],
  ['10mo', '10º'],
  ['10ma', '10ª'],
])

const ABREVIATURAS = new Map([
  ['av', 'Avenida'],
  ['avda', 'Avenida'],
  ['avenida', 'Avenida'],
  ['bv', 'Boulevard'],
  ['bvd', 'Boulevard'],
  ['blvd', 'Boulevard'],
  ['boulevard', 'Boulevard'],
  ['pje', 'Pasaje'],
  ['psje', 'Pasaje'],
  ['pasaje', 'Pasaje'],
  ['gral', 'General'],
  ['cnel', 'Coronel'],
  ['tte', 'Teniente'],
  ['sgt', 'Sargento'],
  ['cabo', 'Cabo'],
  ['dr', 'Doctor'],
  ['dra', 'Doctora'],
  ['ing', 'Ingeniero'],
  ['lic', 'Licenciado'],
  ['prof', 'Profesor'],
  ['arq', 'Arquitecto'],
  ['sta', 'Santa'],
  ['sto', 'Santo'],
  ['nva', 'Nueva'],
  ['nvo', 'Nuevo'],
  ['pte', 'Presidente'],
  ['vte', 'Vicente'],
  ['mtro', 'Maestro'],
  ['pres', 'Presidente'],
])

const ALIAS_LOCALIDAD = new Map([
  ['caba', 'CABA'],
  ['capfed', 'CABA'],
  ['capital', 'CABA'],
  ['cordoba capital', 'Córdoba'],
  ['capital federal', 'CABA'],
  ['ciudad autonoma de buenos aires', 'CABA'],
  ['ciudad de buenos aires', 'CABA'],
  ['bs as', 'Buenos Aires'],
  ['bsas', 'Buenos Aires'],
  ['baires', 'Buenos Aires'],
  ['buenosaires', 'Buenos Aires'],
  ['gba', 'GBA'],
  // Formas pegadas de localidades compuestas del GBA.
  ['montegrande', 'Monte Grande'],
  ['lomasdezamora', 'Lomas de Zamora'],
  ['estebanecheverria', 'Esteban Echeverría'],
  ['vicentelopez', 'Vicente López'],
  ['sanmartin', 'San Martín'],
])

/**
 * Canonización de localidades frecuentes: devuelve la grafía con tildes y
 * mayúsculas correctas cuando el operario escribe todo en minúsculas.
 */
const LOCALIDADES_CANONICAS = new Map([
  ['buenos aires', 'Buenos Aires'],
  ['bahia blanca', 'Bahía Blanca'],
  ['cordoba', 'Córdoba'],
  ['rio cuarto', 'Río Cuarto'],
  ['la plata', 'La Plata'],
  ['monte grande', 'Monte Grande'],
  ['lanus', 'Lanús'],
  ['avellaneda', 'Avellaneda'],
  ['quilmes', 'Quilmes'],
  ['berazategui', 'Berazategui'],
  ['florencio varela', 'Florencio Varela'],
  ['lomas de zamora', 'Lomas de Zamora'],
  ['banfield', 'Banfield'],
  ['temperley', 'Temperley'],
  ['adrogue', 'Adrogué'],
  ['ezeiza', 'Ezeiza'],
  ['esteban echeverria', 'Esteban Echeverría'],
  ['moron', 'Morón'],
  ['ituzaingo', 'Ituzaingó'],
  ['hurlingham', 'Hurlingham'],
  ['san isidro', 'San Isidro'],
  ['vicente lopez', 'Vicente López'],
  ['san martin', 'San Martín'],
  ['tres de febrero', 'Tres de Febrero'],
  ['san miguel', 'San Miguel'],
  ['jose c paz', 'José C. Paz'],
  ['malvinas argentinas', 'Malvinas Argentinas'],
  ['tigre', 'Tigre'],
  ['escobar', 'Escobar'],
  ['pilar', 'Pilar'],
  ['moreno', 'Moreno'],
  ['merlo', 'Merlo'],
  ['rosario', 'Rosario'],
  ['mar del plata', 'Mar del Plata'],
  ['mendoza', 'Mendoza'],
  ['salta', 'Salta'],
  ['tucuman', 'Tucumán'],
  ['neuquen', 'Neuquén'],
])

/**
 * Claves de localidad ordenadas de mayor a menor longitud. El orden permite
 * reconocer primero las localidades compuestas ("buenos aires") antes que una
 * coincidencia parcial más corta.
 */
const CLAVES_LOCALIDAD = [
  ...new Set([...ALIAS_LOCALIDAD.keys(), ...LOCALIDADES_CANONICAS.keys()]),
].sort((a, b) => b.length - a.length)

/**
 * Índice de localidades sin espacios. Permite canonicalizar formas pegadas o
 * con espaciado inconsistente ("lomasdezamora", "lomas dezamora") sin agregar
 * una entrada por cada variante.
 */
const LOCALIDAD_POR_CLAVE_PEGADA = new Map()
for (const [clave, valor] of [...LOCALIDADES_CANONICAS, ...ALIAS_LOCALIDAD]) {
  LOCALIDAD_POR_CLAVE_PEGADA.set(clave.replace(/\s+/g, ''), valor)
}

/** Busca una localidad por clave exacta o por su forma sin espacios. */
function buscarLocalidadCanonica(clave) {
  if (!clave) return null

  return (
    ALIAS_LOCALIDAD.get(clave) ??
    LOCALIDADES_CANONICAS.get(clave) ??
    LOCALIDAD_POR_CLAVE_PEGADA.get(clave.replace(/\s+/g, '')) ??
    null
  )
}

const UNIDADES = new Map([
  ['cero', 0],
  ['un', 1],
  ['uno', 1],
  ['una', 1],
  ['dos', 2],
  ['tres', 3],
  ['cuatro', 4],
  ['cinco', 5],
  ['seis', 6],
  ['siete', 7],
  ['ocho', 8],
  ['nueve', 9],
])

const ESPECIALES = new Map([
  ['diez', 10],
  ['once', 11],
  ['doce', 12],
  ['trece', 13],
  ['catorce', 14],
  ['quince', 15],
  ['dieciseis', 16],
  ['diecisiete', 17],
  ['dieciocho', 18],
  ['diecinueve', 19],
  ['veinte', 20],
  ['veintiuno', 21],
  ['veintiun', 21],
  ['veintiuna', 21],
  ['veintidos', 22],
  ['veintitres', 23],
  ['veinticuatro', 24],
  ['veinticinco', 25],
  ['veintiseis', 26],
  ['veintisiete', 27],
  ['veintiocho', 28],
  ['veintinueve', 29],
])

const DECENAS = new Map([
  ['treinta', 30],
  ['cuarenta', 40],
  ['cincuenta', 50],
  ['sesenta', 60],
  ['setenta', 70],
  ['ochenta', 80],
  ['noventa', 90],
])

const CENTENAS = new Map([
  ['cien', 100],
  ['ciento', 100],
  ['doscientos', 200],
  ['doscientas', 200],
  ['trescientos', 300],
  ['trescientas', 300],
  ['cuatrocientos', 400],
  ['cuatrocientas', 400],
  ['quinientos', 500],
  ['quinientas', 500],
  ['seiscientos', 600],
  ['seiscientas', 600],
  ['setecientos', 700],
  ['setecientas', 700],
  ['ochocientos', 800],
  ['ochocientas', 800],
  ['novecientos', 900],
  ['novecientas', 900],
])

const MULTIPLICADORES = new Map([
  ['mil', 1000],
  ['miles', 1000],
  ['millon', 1000000],
  ['millones', 1000000],
])

const PALABRAS_NUMERO = new Map([
  ...UNIDADES,
  ...ESPECIALES,
  ...DECENAS,
  ...CENTENAS,
])

const LIMITE_ALTURA = 999999

/**
 * Guiones y guiones bajos que el operario usa como separadores al escribir
 * números con palabras ("Mil_doscientos_treinta_y_cuatro").
 */
const SEPARADORES_PALABRAS = /[_\-\u2010-\u2015]+/g

/**
 * Palabras numéricas ordenadas de mayor a menor longitud. Ese orden permite
 * segmentar texto pegado ("mildoscientostreintaycuatro") probando primero la
 * coincidencia más larga y retrocediendo si el resto no se puede segmentar.
 */
const PALABRAS_SEGMENTABLES = [
  ...new Set([...PALABRAS_NUMERO.keys(), ...MULTIPLICADORES.keys(), 'y']),
].sort((a, b) => b.length - a.length)

/** Tope defensivo para no segmentar cadenas arbitrariamente largas. */
const LONGITUD_MAXIMA_PEGADA = 40

/** Quita tildes, mayúsculas y puntuación para comparar sin ambigüedad. */
function normalizarTexto(valor) {
  return String(valor ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[º°]/g, '')
    .replace(/\./g, '')
    .trim()
}

function tokenizar(texto) {
  return String(texto ?? '')
    .trim()
    .split(/[\s,]+/)
    .map((raw) => raw.replace(/^[-–—]+|[-–—]+$/g, ''))
    .filter(Boolean)
    .map((raw) => ({ raw, norm: normalizarTexto(raw) }))
    .filter((token) => token.norm)
}

/** Devuelve el valor numérico de un token de dígitos, o null. */
function valorNumerico(norm) {
  if (!/^\d{1,6}$/.test(norm)) return null
  return Number(norm)
}

function esPalabraNumerica(token) {
  if (!token) return false
  return (
    token.norm === 'y' ||
    PALABRAS_NUMERO.has(token.norm) ||
    MULTIPLICADORES.has(token.norm)
  )
}

/** Convierte una frase de palabras ("mil doscientos treinta") a número. */
function parsearFraseNumerica(tokens) {
  let total = 0
  let parcial = 0
  let encontrado = false

  for (const token of tokens) {
    if (token.norm === 'y') continue

    if (MULTIPLICADORES.has(token.norm)) {
      const factor = MULTIPLICADORES.get(token.norm)
      total += (parcial === 0 ? 1 : parcial) * factor
      parcial = 0
      encontrado = true
      continue
    }

    const valor = PALABRAS_NUMERO.get(token.norm)
    if (valor == null) return null

    parcial += valor
    encontrado = true
  }

  return encontrado ? total + parcial : null
}

/**
 * Segmenta una palabra pegada en palabras numéricas conocidas.
 * "mildoscientostreintaycuatro" -> ["mil", "doscientos", "treinta", "y", "cuatro"].
 * Devuelve null si no se puede descomponer por completo.
 */
function segmentarPalabra(norm, indice = 0) {
  if (indice >= norm.length) return []

  const resto = norm.slice(indice)
  for (const palabra of PALABRAS_SEGMENTABLES) {
    if (!resto.startsWith(palabra)) continue
    const cola = segmentarPalabra(norm, indice + palabra.length)
    if (cola) return [palabra, ...cola]
  }

  return null
}

/**
 * Expande tokens que vienen pegados ("milquinientos") o unidos por guiones ya
 * normalizados a espacios. Si algún token no es numérico devuelve null para
 * conservar el valor original (por ejemplo "1234 bis").
 */
function expandirPalabrasNumericas(tokens) {
  const expandidos = []

  for (const token of tokens) {
    if (esPalabraNumerica(token)) {
      expandidos.push(token)
      continue
    }

    if (token.norm.length > LONGITUD_MAXIMA_PEGADA) return null
    const segmentos = segmentarPalabra(token.norm)
    if (!segmentos) return null

    expandidos.push(...segmentos.map((segmento) => ({ raw: segmento, norm: segmento })))
  }

  return expandidos
}

/**
 * Colapsa dígitos escritos sueltos ("1 2 3 4" o "1 2 34") en un único número.
 * Solo aplica cuando TODOS los tokens son grupos de dígitos; así "1234 bis"
 * conserva su valor original.
 */
function unirDigitosEspaciados(tokens) {
  if (tokens.length < 2) return null
  if (!tokens.every((token) => /^\d{1,6}$/.test(token.norm))) return null

  const unido = tokens
    .map((token) => token.norm)
    .join('')
    .replace(/^0+(?=\d)/, '')

  if (Number(unido) > LIMITE_ALTURA) return null
  return unido
}

/**
 * Separa una localidad conocida que quedó pegada al final de la cadena
 * ("Corrientes 1234caba" -> "Corrientes 1234 caba"). Solo corta cuando el
 * prefijo es numérico o una palabra numérica segmentable, para no romper
 * nombres de calles que terminan con el mismo texto.
 */
function separarLocalidadPegada(texto) {
  const tokens = tokenizar(texto)
  if (tokens.length === 0) return texto

  const ultimo = tokens[tokens.length - 1]
  const previos = tokens.slice(0, -1).map((token) => token.raw)

  for (const clave of CLAVES_LOCALIDAD) {
    const pegado = clave.replace(/\s+/g, '')
    if (!ultimo.norm.endsWith(pegado)) continue

    const prefijo = ultimo.norm.slice(0, ultimo.norm.length - pegado.length)
    if (prefijo.length === 0) {
      // "buenosaires" -> "buenos aires"; "caba" ya es una palabra completa.
      if (clave.includes(' ')) {
        return [...previos, ...clave.split(' ')].join(' ')
      }
      continue
    }

    if (!/^\d+$/.test(prefijo) && !segmentarPalabra(prefijo)) continue

    return [...previos, prefijo, ...clave.split(' ')].join(' ')
  }

  return texto
}

/**
 * Colapsa corridas de dígitos sueltos dentro de la cadena completa
 * ("Corrientes 1 2 3 4" -> "Corrientes 1234"). No toca los números que
 * pertenecen al nombre de la calle ("Calle 50 1234").
 */
function colapsarDigitosEspaciados(texto) {
  const tokens = tokenizar(texto)
  if (tokens.length === 0) return texto

  const salida = []
  let indice = 0

  while (indice < tokens.length) {
    if (!/^\d+$/.test(tokens[indice].norm)) {
      salida.push(tokens[indice].raw)
      indice += 1
      continue
    }

    let fin = indice
    while (fin < tokens.length && /^\d+$/.test(tokens[fin].norm)) fin += 1

    const anterior = tokens[indice - 1]?.norm
    if (anterior && DESIGNadores.has(anterior)) {
      // El primer número es parte del nombre de la calle; se emite suelto y
      // se reevalúa el resto de la corrida ("Calle 50 1 2 3 4").
      salida.push(tokens[indice].raw)
      indice += 1
      continue
    }

    const corrida = tokens.slice(indice, fin)
    const todosDeUnDigito = corrida.every((token) => token.norm.length === 1)
    const colapsable =
      corrida.length >= 3 || (corrida.length >= 2 && todosDeUnDigito)

    if (colapsable) {
      const unido = corrida
        .map((token) => token.norm)
        .join('')
        .replace(/^0+(?=\d)/, '')

      if (Number(unido) <= LIMITE_ALTURA) {
        salida.push(unido)
      } else {
        salida.push(...corrida.map((token) => token.raw))
      }
    } else {
      salida.push(...corrida.map((token) => token.raw))
    }

    indice = fin
  }

  return salida.join(' ')
}

/**
 * Expande palabras numéricas pegadas en cualquier posición de la cadena
 * ("Corrientes Mildoscientostreintaycuatro" -> "Corrientes mil doscientos
 * treinta y cuatro"). Los tokens que no se pueden segmentar quedan intactos.
 */
function expandirPalabrasPegadas(texto) {
  const tokens = tokenizar(texto)
  if (tokens.length === 0) return texto

  const palabras = tokens.map((token) => {
    if (esPalabraNumerica(token)) return token.raw
    if (token.norm.length > LONGITUD_MAXIMA_PEGADA) return token.raw

    const segmentos = segmentarPalabra(token.norm)
    return segmentos ? segmentos.join(' ') : token.raw
  })

  return palabras.join(' ')
}

/**
 * Separa la traza de kilómetros de los números que la rodean:
 * "ruta8km20" -> "ruta8 km 20", "km20" -> "km 20".
 *
 * Se hace antes de separarLetrasNumeros para que "8km20" no termine
 * interpretado como calle "8km" y altura "20".
 */
function separarKilometros(texto) {
  return String(texto ?? '')
    .replace(/(\d)\s*km\s*(\d)/gi, '$1 km $2')
    .replace(/(\d)\s*km\b/gi, '$1 km')
    .replace(/\bkm\s*(\d)/gi, 'km $1')
}

/**
 * Separa letras y dígitos pegados ("Corrientes1234" -> "Corrientes 1234").
 *
 * Solo corta cuando una letra precede a un dígito. Así quedan intactos:
 *   - ordinales ("1º", "4to"), donde el número va primero;
 *   - "bis" y "depto", que no tienen dígitos pegados del lado derecho;
 *   - localidades pegadas al final ("1234caba"), que resuelve
 *     separarLocalidadPegada.
 */
function separarLetrasNumeros(texto) {
  return String(texto ?? '').replace(/(\p{L})(\d)/gu, '$1 $2')
}

/** Preprocesa la cadena completa antes de extraer las piezas. */
function preprocesarDireccion(texto) {
  const base = String(texto ?? '').replace(SEPARADORES_PALABRAS, ' ').trim()
  if (!base) return ''

  return expandirPalabrasPegadas(
    colapsarDigitosEspaciados(
      separarLocalidadPegada(separarLetrasNumeros(separarKilometros(base))),
    ),
  )
}

/**
 * Extrae la localidad si el final de la cadena coincide con un alias o una
 * localidad canónica. Devuelve los tokens restantes para seguir parseando.
 */
function extraerLocalidadFinal(tokens) {
  let fin = tokens.length
  while (fin > 1 && tokens[fin - 1].norm === 'argentina') fin -= 1

  const utiles = tokens.slice(0, fin)
  const maximo = Math.min(5, utiles.length)

  for (let cantidad = maximo; cantidad >= 1; cantidad -= 1) {
    const inicio = utiles.length - cantidad
    const clave = utiles
      .slice(inicio)
      .map((token) => token.norm)
      .join(' ')
    const valor = buscarLocalidadCanonica(clave)
    if (!valor) continue

    let prefijo = utiles.slice(0, inicio)
    // "Corrientes 1234 De CABA": el "de" es un conector, no parte de la calle.
    if (prefijo.length > 1 && prefijo[prefijo.length - 1].norm === 'de') {
      prefijo = prefijo.slice(0, -1)
    }
    if (prefijo.length === 0) {
      // Una localidad compuesta sola ("Córdoba Capital", "Buenos Aires") se
      // reconoce completa; una clave de una sola palabra puede ser una calle.
      if (clave.includes(' ')) {
        return { localidad: valor, tokens: [] }
      }
      continue
    }

    // "Calle Salta" o "Avenida San Martín": el prefijo es solo una designación,
    // no una calle completa.
    if (prefijo.length === 1) {
      const norm = prefijo[0].norm
      if (ABREVIATURAS.has(norm) || DESIGNadores.has(norm)) continue
    }

    return { localidad: valor, tokens: prefijo }
  }

  return { localidad: '', tokens }
}

/**
 * Indica si el número que sigue a un candidato es otra altura (y no un piso).
 * Se usa solo cuando la calle ya contenía una fecha o un número protegido:
 * en "12 de Octubre 450 1234", el 450 es parte del nombre y 1234 la altura.
 */
function esNumeroQueNoEsPiso(tokens, inicio, digitosAltura) {
  const token = tokens[inicio]
  if (!token || !/^\d+$/.test(token.norm)) return false

  const siguiente = tokens[inicio + 1]?.norm
  if (siguiente === 'bis' || /^[a-z]$/.test(siguiente ?? '')) return false

  // Un número más corto que el candidato se interpreta como piso/depto
  // ("Calle 50 1234 3" -> altura 1234, piso 3).
  if (token.norm.length <= 3 && token.norm.length < digitosAltura) return false

  return true
}

/**
 * Reconoce una altura sin número escrita en la dirección.
 * Requiere que haya una calle antes para no romper el caso "S/N" usado como
 * nombre de calle.
 */
function leerAlturaSinNumero(tokens, indice) {
  if (indice <= 0) return null

  const token = tokens[indice]
  if (!token) return null

  // "Calle S/N" puede ser el nombre de la calle; no lo tomamos como altura.
  if (DESIGNadores.has(tokens[indice - 1]?.norm)) return null

  if (token.norm === 's/n' || token.norm === 'sn') {
    return { texto: 'S/N', fin: indice }
  }

  if (token.norm === 'sin' && tokens[indice + 1]?.norm === 'numero') {
    return { texto: 'S/N', fin: indice + 1 }
  }

  return null
}

/**
 * Busca la altura dentro de una dirección completa.
 * Descarta números que forman parte del nombre de la calle:
 *   - "Calle 50 1234" -> 50 se descarta, 1234 es la altura.
 *   - "9 de Julio 1234" -> 9 se descarta por el "de" + mes.
 *   - "12 de Octubre 450 1234" -> 12 y 450 son parte de la calle, 1234 la altura.
 *   - "Ruta 8 km 20" -> no hay altura reconocible.
 */
function buscarAltura(tokens) {
  const extras = []
  let huboProtegido = false

  for (let indice = 0; indice < tokens.length; indice += 1) {
    const token = tokens[indice]
    const anteriorToken = tokens[indice - 1]?.norm

    // "Calle 50bis": el número con sufijo es parte del nombre de la calle,
    // no una altura. Marca la calle como protegida para que "450 1234" se
    // resuelva como número sobrante + altura real.
    if (
      /^\d+bis$/.test(token.norm) &&
      anteriorToken &&
      DESIGNadores.has(anteriorToken)
    ) {
      huboProtegido = true
      continue
    }

    const sinNumero = leerAlturaSinNumero(tokens, indice)
    if (sinNumero) {
      return { indice, fin: sinNumero.fin, valor: sinNumero.texto, extras }
    }

    let fin = indice
    let valor = valorNumerico(token.norm)

    if (valor == null && token.norm !== 'y' && esPalabraNumerica(token)) {
      let cursor = indice
      const frase = []
      while (cursor < tokens.length && esPalabraNumerica(tokens[cursor])) {
        frase.push(tokens[cursor])
        cursor += 1
      }
      valor = parsearFraseNumerica(frase)
      fin = cursor - 1
    }

    if (valor == null || valor < 0 || valor > LIMITE_ALTURA) continue

    const anterior = tokens[indice - 1]?.norm
    const siguiente = tokens[fin + 1]?.norm
    const despuesDe = tokens[fin + 2]?.norm

    if (anterior && DESIGNadores.has(anterior)) {
      huboProtegido = true
      continue
    }
    if (siguiente === 'de' && (indice === 0 || MESES.has(despuesDe))) {
      huboProtegido = true
      continue
    }

    // Si la calle ya tenía una fecha o un número, y este candidato está
    // seguido por otro número que no parece piso, el candidato pertenece al
    // nombre de la calle y la altura real es la del final.
    if (huboProtegido && esNumeroQueNoEsPiso(tokens, fin + 1, String(valor).length)) {
      extras.push({ indice, fin })
      continue
    }

    return { indice, fin, valor, extras }
  }

  return null
}

/** Lee un piso numérico u ordinal ("3", "3º", "4to", "2da"). */
function leerPiso(tokens, indice) {
  const token = tokens[indice]
  if (!token) return null

  const ordinal = ORDINALES_PISO.get(token.norm)
  if (ordinal) return { texto: ordinal, fin: indice + 1 }

  // "3bis": el indicador viene pegado al número.
  const bisPegado = /^(\d{1,3})bis$/.exec(token.norm)
  if (bisPegado) {
    return { texto: `${bisPegado[1]} bis`, fin: indice + 1 }
  }

  // "3b", "4a", "1b": piso y letra de departamento pegados.
  const letraPegada = /^(\d{1,3})([a-z])$/.exec(token.norm)
  if (letraPegada) {
    return {
      texto: `${letraPegada[1]} ${letraPegada[2].toUpperCase()}`,
      fin: indice + 1,
    }
  }

  // "3º" / "2ª": normalizarTexto quita el "º", pero el raw lo conserva.
  const escrito = /^(\d{1,3})\s*([º°ª])$/.exec(token.raw)
  if (escrito) {
    const indicador = escrito[2] === 'ª' ? 'ª' : 'º'
    return { texto: `${escrito[1]}${indicador}`, fin: indice + 1 }
  }

  if (/^\d{1,3}$/.test(token.norm)) {
    return { texto: token.norm, fin: indice + 1 }
  }

  return null
}

/** Lee el valor de un departamento (letra, número corto u ordinal). */
function leerDepto(tokens, indice) {
  const token = tokens[indice]
  if (!token) return null

  const ordinal = ORDINALES_PISO.get(token.norm)
  if (ordinal) return { texto: ordinal, fin: indice + 1 }

  if (/^\d{1,4}$/.test(token.norm)) {
    return { texto: token.norm, fin: indice + 1 }
  }

  // Una letra suelta es el depto; "y"/"e" se reservan para conectores.
  if (/^[a-z]$/.test(token.norm) && token.norm !== 'y' && token.norm !== 'e') {
    return { texto: token.norm.toUpperCase(), fin: indice + 1 }
  }

  return null
}

/**
 * Separa un piso/depto del resto de la dirección.
 *
 * Reconoce:
 *   - "3º B", "3 B", "3 bis", "PB", "planta baja";
 *   - ordinales con letras: "4to A" -> "4º A", "2da" -> "2ª";
 *   - estructuras compuestas: "piso 2 depto 4" -> "Piso 2 Depto 4";
 *   - "piso 2 y depto 4" (el conector no se toma como depto).
 */
function extraerPiso(tokens) {
  if (tokens.length === 0) return { piso: '', resto: [] }

  const partes = []
  let cursor = 0
  let pisoConPalabraPosterior = false
  const primero = tokens[cursor]?.norm
  const esPlantaBaja =
    primero === 'pb' ||
    (primero === 'planta' && tokens[cursor + 1]?.norm === 'baja')

  if (primero === 'pb') {
    partes.push('PB')
    cursor += 1
  } else if (DESIGNACIONES_PISO.has(primero)) {
    partes.push(primero === 'planta' ? 'Planta' : 'Piso')
    cursor += 1

    if (tokens[cursor]?.norm === 'baja') {
      partes.push('Baja')
      cursor += 1
    } else {
      const valor = leerPiso(tokens, cursor)
      if (valor) {
        partes.push(valor.texto)
        cursor = valor.fin
        if (tokens[cursor]?.norm === 'bis') {
          partes.push('bis')
          cursor += 1
        }
      }
    }
  } else {
    // Piso sin palabra clave: "4to A", "3º B", "2da".
    const esNumeroDeCalle =
      tokens[cursor + 1]?.norm === 'de' && MESES.has(tokens[cursor + 2]?.norm)
    const valor = esNumeroDeCalle ? null : leerPiso(tokens, cursor)
    if (valor) {
      partes.push(valor.texto)
      cursor = valor.fin
      if (tokens[cursor]?.norm === 'bis') {
        partes.push('bis')
        cursor += 1
      }

      // "1er piso ..." / "1º piso ...": el número ya aportó el piso y la
      // palabra "piso" solo actúa como separador hacia el depto.
      if (DESIGNACIONES_PISO.has(tokens[cursor]?.norm)) {
        pisoConPalabraPosterior = true
        cursor += 1
      }
    }
  }

  // "piso 2 y depto 4": el conector no es parte del depto.
  if (
    (tokens[cursor]?.norm === 'y' || tokens[cursor]?.norm === 'e') &&
    DESIGNACIONES_DEPTO.has(tokens[cursor + 1]?.norm)
  ) {
    cursor += 1
  }

  if (DESIGNACIONES_DEPTO.has(tokens[cursor]?.norm)) {
    if (!pisoConPalabraPosterior) partes.push('Depto')
    cursor += 1

    const valor = leerDepto(tokens, cursor)
    if (valor) {
      partes.push(valor.texto)
      cursor = valor.fin
      if (tokens[cursor]?.norm === 'bis') {
        partes.push('bis')
        cursor += 1
      }
    }
  } else if (partes.length > 0) {
    const token = tokens[cursor]

    // "Planta Baja 3" / "PB 3": el número o la letra que sigue es el depto.
    if (esPlantaBaja) {
      const valor = leerDepto(tokens, cursor)
      if (valor) {
        partes.push(valor.texto)
        cursor = valor.fin
        if (tokens[cursor]?.norm === 'bis') {
          partes.push('bis')
          cursor += 1
        }
      }
    } else if (
      token &&
      /^[a-z]$/.test(token.norm) &&
      token.norm !== 'y' &&
      token.norm !== 'e'
    ) {
      // Letra de depto pegada al piso ("3º B", "Piso 2 B").
      partes.push(token.norm.toUpperCase())
      cursor += 1
      if (tokens[cursor]?.norm === 'bis') {
        partes.push('bis')
        cursor += 1
      }
    }
  }

  return { piso: partes.join(' '), resto: tokens.slice(cursor) }
}

/**
 * Indica si una cadena sin altura parece ser exclusivamente un piso/depto.
 * Evita interpretar como piso una calle que empieza con un número ("3 B").
 */
function parecePisoSinAltura(tokens) {
  if (tokens.length === 0) return false

  const primero = tokens[0]
  if (primero.norm === 'pb') return true
  if (ORDINALES_PISO.has(primero.norm)) return true
  if (/^\d{1,3}\s*[º°ª]$/.test(primero.raw)) return true

  return (
    DESIGNACIONES_PISO.has(primero.norm) ||
    DESIGNACIONES_DEPTO.has(primero.norm)
  )
}

/** Aplica mayúsculas correctas y deja los conectores en minúscula. */
function formatearTitulo(palabras) {
  return palabras
    .map((palabra, indice) => {
      const norm = normalizarTexto(palabra)
      if (norm === 's/n') return 'S/N'
      if (/^\d+$/.test(norm)) return norm
      if (indice > 0 && CONECTORES.has(norm)) return norm
      if (/^[A-Z]{2,3}$/.test(palabra)) return palabra

      const minuscula = palabra.toLocaleLowerCase('es-AR')
      return minuscula.charAt(0).toLocaleUpperCase('es-AR') + minuscula.slice(1)
    })
    .join(' ')
}

/** Normaliza la calle: expande abreviaturas y corrige mayúsculas. */
export function normalizarCalle(texto) {
  const tokens = tokenizar(texto)
  if (tokens.length === 0) return ''

  const palabras = tokens.map((token) => {
    if (token.norm === 's/n' || token.norm === 'sn') return 'S/N'
    if (ABREVIATURAS.has(token.norm)) return ABREVIATURAS.get(token.norm)
    if (/^\d+$/.test(token.norm)) return token.norm
    return token.raw
  })

  return formatearTitulo(palabras)
}

/** Normaliza la localidad: alias conocidos y grafía canónica. */
export function normalizarLocalidad(texto) {
  const tokens = tokenizar(texto)
  while (tokens.length > 1 && tokens[tokens.length - 1].norm === 'argentina') {
    tokens.pop()
  }
  if (tokens.length === 0) return ''

  // "1234 de CABA" -> el "de" es un conector, no parte del nombre.
  if (tokens[0]?.norm === 'de' && tokens.length > 1) {
    const sinDe = tokens.slice(1).map((token) => token.norm).join(' ')
    if (buscarLocalidadCanonica(sinDe)) {
      tokens.shift()
    }
  }

  const clave = tokens.map((token) => token.norm).join(' ')
  const canonica = buscarLocalidadCanonica(clave)
  if (canonica) return canonica

  return formatearTitulo(tokens.map((token) => token.raw))
}

/**
 * Traduce una altura escrita con palabras a dígitos.
 * Si el valor ya es numérico, o no se puede interpretar, se devuelve igual.
 */
export function convertirAltura(valor) {
  const original = String(valor ?? '').trim()
  if (!original) return ''

  const norm = normalizarTexto(original)
  if (['s/n', 'sn', 'sin numero'].includes(norm)) return 'S/N'
  if (/^\d+$/.test(norm)) return norm

  // Guiones y guiones bajos actúan como separadores de palabras.
  const tokens = tokenizar(original.replace(SEPARADORES_PALABRAS, ' '))
  if (tokens.length === 0) return original

  // "1 2 3 4" / "1 2 34" -> "1234"
  const digitosUnidos = unirDigitosEspaciados(tokens)
  if (digitosUnidos != null) return digitosUnidos

  // "mildoscientostreintaycuatro" -> "mil doscientos treinta y cuatro"
  const expandidos = expandirPalabrasNumericas(tokens)
  const numero = expandidos ? parsearFraseNumerica(expandidos) : null
  if (numero == null || numero < 0 || numero > LIMITE_ALTURA) return original

  return String(numero)
}

/**
 * Parsea una dirección completa escrita en un solo campo.
 *
 * Orden de resolución:
 *   1. Preprocesa la cadena (separadores, letras/dígitos pegados, dígitos
 *      espaciados, palabras pegadas).
 *   2. Extrae la localidad si coincide con un alias conocido al final.
 *   3. Extrae la altura (numérica o en palabras).
 *   4. Asigna el sobrante inicial a la calle y el secundario a piso/depto.
 *
 * Devuelve las cuatro piezas por separado; no decide qué pisa y qué no.
 */
export function parsearDireccion(texto) {
  const preparado = preprocesarDireccion(texto)
  const tokens = tokenizar(preparado)
  if (tokens.length === 0) {
    return { calle: '', altura: '', localidad: '', pisoDepto: '' }
  }

  const { localidad, tokens: sinLocalidad } = extraerLocalidadFinal(tokens)

  const candidato = buscarAltura(sinLocalidad)

  if (!candidato) {
    // Direcciones que son solo piso/depto ("1er piso dpto b", "Piso 2").
    if (parecePisoSinAltura(sinLocalidad)) {
      const { piso, resto } = extraerPiso(sinLocalidad)
      if (piso) {
        return {
          calle: normalizarCalle(resto.map((token) => token.raw).join(' ')),
          altura: '',
          localidad,
          pisoDepto: piso,
        }
      }
    }

    // Sin altura reconocible: si hay coma y no extrajimos localidad, el último
    // segmento puede ser una localidad no listada ("Av. Corrientes, Villa Ballester").
    if (!localidad) {
      const partes = String(texto ?? '')
        .split(',')
        .map((parte) => parte.trim())
        .filter(Boolean)

      if (partes.length >= 2) {
        const ultima = partes[partes.length - 1]
        if (!/\d/.test(ultima) && ultima.split(/\s+/).length <= 4) {
          return {
            calle: normalizarCalle(partes.slice(0, -1).join(' ')),
            altura: '',
            localidad: normalizarLocalidad(ultima),
            pisoDepto: '',
          }
        }
      }
    }

    return {
      calle: normalizarCalle(sinLocalidad.map((token) => token.raw).join(' ')),
      altura: '',
      localidad,
      pisoDepto: '',
    }
  }

  const indicesExtra = new Set()
  for (const rango of candidato.extras) {
    for (let i = rango.indice; i <= rango.fin; i += 1) indicesExtra.add(i)
  }

  const calleTokens = sinLocalidad
    .slice(0, candidato.indice)
    .filter((_, indice) => !indicesExtra.has(indice))
  const restoTokens = sinLocalidad.slice(candidato.fin + 1)
  const { piso, resto } = extraerPiso(restoTokens)

  return {
    calle: normalizarCalle(calleTokens.map((token) => token.raw).join(' ')),
    altura: String(candidato.valor),
    localidad: localidad || normalizarLocalidad(resto.map((token) => token.raw).join(' ')),
    pisoDepto: piso,
  }
}

/**
 * Normaliza la dirección que el operario está cargando.
 *
 * Regla de negocio del wizard: la altura, la localidad y el piso/depto que el
 * operario ya escribió a mano NUNCA se sobreescriben. Solo se completan cuando
 * están vacíos. La calle sí se normaliza siempre (es el campo de origen).
 *
 * Devuelve `{ direccion, cambios }`, donde `cambios` es una lista de textos
 * legibles para mostrar en pantalla.
 */
export function normalizarDireccionIngresada(actual = {}) {
  const calleOriginal = String(actual.calle ?? '')
  const alturaOriginal = String(actual.altura ?? '')
  const localidadOriginal = String(actual.localidad ?? '')
  const pisoOriginal = String(actual.pisoDepto ?? '')

  const parseada = parsearDireccion(calleOriginal)

  const calle = parseada.calle
  const altura = alturaOriginal.trim()
    ? convertirAltura(alturaOriginal)
    : parseada.altura
  const localidad = localidadOriginal.trim()
    ? normalizarLocalidad(localidadOriginal)
    : parseada.localidad
  const pisoDepto = pisoOriginal.trim() ? pisoOriginal : parseada.pisoDepto

  const cambios = []
  if (calle !== calleOriginal) {
    cambios.push(`calle “${calleOriginal}” → “${calle}”`)
  }
  if (altura !== alturaOriginal) {
    cambios.push(`altura “${alturaOriginal || 'vacía'}” → “${altura}”`)
  }
  if (localidad !== localidadOriginal) {
    cambios.push(`localidad “${localidadOriginal || 'vacía'}” → “${localidad}”`)
  }
  if (pisoDepto !== pisoOriginal) {
    cambios.push(`piso/depto “${pisoOriginal || 'vacío'}” → “${pisoDepto}”`)
  }

  return {
    direccion: { calle, altura, localidad, pisoDepto },
    cambios,
  }
}
