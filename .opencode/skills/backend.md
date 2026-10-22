# Skill: Backend Spring Boot Architecture

## Patrón Estándar DTO - Service - Controller
- **DTO:** Clases inmutables para transporte de datos.
- **Service:** Lógica de negocio y cliente RestTemplate para consultar Nominatim con cabecera User-Agent.
- **Controller:** Exposición de endpoints REST con anotación `@CrossOrigin`.