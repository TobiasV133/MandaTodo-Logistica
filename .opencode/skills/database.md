# Skill: Configuración de Base de Datos MySQL

## Restricciones UNIQUE
- Toda entidad de usuario o cliente debe garantizar unicidad en DNI/CUIT y Email usando `@Table(uniqueConstraints = ...)` y `@Column(unique = true)`.