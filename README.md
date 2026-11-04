# Sistema web de gestión trazable de expedientes de solicitud de ciudadanía europea

Prototipo del Trabajo Final de Grado (Prototipado Tecnológico) de la Licenciatura en Informática,
Universidad Siglo 21. Autor: Pablo Hidalgo.

## Qué implementa

El núcleo del sistema descripto en el documento del TFG:

| Caso de uso | Funcionalidad |
|---|---|
| CU01 | Inicio de sesión con bloqueo de la cuenta al quinto intento fallido consecutivo |
| CU04 | Registro de expedientes y alta del cliente con enlace de activación de un solo uso (24 h) |
| CU05 | Carga de documentos (PDF, JPG o PNG de hasta 10 MB) con control de versiones |
| CU06 | Validación preliminar con las reglas de cada país (patrón Strategy: `ReglasItalia`, `ReglasEspana`) |
| CU07 y CU08 | Revisión documental, aprobación y registro de observaciones |
| CU09 | Cambio de estado del expediente (patrón State: `Iniciado`, `EnRevision`, `Completo`, `Presentado`, `Resuelto`) |
| CU10 | Registro inmutable de eventos de trazabilidad |
| CU11 | Consulta del expediente y de su historial |
| CU12 | Consulta de indicadores de seguimiento |

Los usuarios y los requisitos documentales (CU02 y CU03 en el sistema completo) se cargan como datos de
ejemplo al iniciar la aplicación con la base vacía. El envío de correos se simula: cada mensaje se escribe en
la consola de la aplicación con la marca `[CORREO SIMULADO]`.

## Requisitos

- Java 25 (versión con soporte a largo plazo, LTS).
- PostgreSQL 18.
- Apache Maven para compilar desde el código fuente (IntelliJ IDEA y Eclipse lo incluyen).

## Puesta en marcha

1. Crear la base de datos desde psql o pgAdmin:

   ```sql
   CREATE DATABASE expedientes;
   ```

   Las tablas se crean automáticamente al iniciar la aplicación (`src/main/resources/schema.sql`).

2. Si el usuario o la contraseña de PostgreSQL no son `postgres`/`postgres`, definir las variables de
   entorno `DB_USER` y `DB_PASSWORD` (también se pueden cambiar `DB_HOST`, `DB_PORT` y `DB_NAME`).

3. Ejecutar la aplicación:

   ```bash
   java -jar gestion-expedientes-1.0.0.jar
   ```

   o, desde esta carpeta, `mvn spring-boot:run`. Para generar el `.jar`: `mvn package` (queda en `target/`).

4. Ingresar a <http://localhost:8080>.

## Cuentas de demostración

Todas usan la contraseña `Prototipo-Expedientes-2026`.

| Rol | Usuario | Correo electrónico |
|---|---|---|
| Administrador | Administrador del estudio | admin@estudio.test |
| Administrativo | Gómez, Luis | lgomez@estudio.test |
| Gestor | López, Marta | mlopez@estudio.test |
| Gestor | Fernández, Pablo | pfernandez@estudio.test |
| Cliente | Pérez, Ana | ana.perez@correo.test |

## Recorrido sugerido

1. Gestor: abrir el expediente de ejemplo, entrar a **Revisar documentación** y cambiar el estado a **En revisión**.
2. Cliente: cargar documentos y ver el resultado de la validación preliminar (por ejemplo, un acta sin la
   traducción requerida o con un apellido distinto del registrado se informa como inconsistente).
3. Gestor: registrar una observación, aprobar los documentos y pasar el expediente a **Completo**,
   **Presentado** (con la fecha del turno) y **Resuelto** (con la resolución).
4. Cliente: consultar el expediente y su historial. Gestor: consultar los **Indicadores**.
5. Administrativo: registrar un expediente para un cliente nuevo. El enlace de activación de su cuenta
   aparece en la consola, junto con los demás correos simulados.

## Pruebas automatizadas

`mvn test` ejecuta las pruebas sobre una base H2 en memoria (no requiere PostgreSQL): transiciones de
estado, política de contraseñas, bloqueo por intentos fallidos, permisos por rol y el flujo completo de un
expediente desde el alta hasta la resolución.

## Configuración de producción

| Variable | Uso |
|---|---|
| `COOKIE_SECURE=true` | Agrega el atributo Secure a la cookie de sesión (requiere HTTPS) |
| `TOKEN_SECRETO` | Clave para firmar los enlaces de activación (al menos 32 caracteres aleatorios) |
| `URL_BASE` | Dirección pública usada en los enlaces de activación |
| `ALMACENAMIENTO` | Carpeta donde se guardan los archivos de los documentos |
| `DATOS_EJEMPLO=false` | Desactiva la carga de los datos de ejemplo |

La lista de contraseñas de uso frecuente (`contrasenas-comunes.txt`, 10.000 entradas) proviene del proyecto
SecLists (`Passwords/Common-Credentials/10k-most-common.txt`, licencia MIT).
