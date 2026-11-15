# Contrato de integración — validar tokens desde otro microservicio

> **Para el equipo que desarrolla el servicio de archivos (S3) o cualquier otro
> microservicio que necesite saber quién está haciendo una petición.**

Vuestro servicio **no guarda contraseñas ni usuarios**. Cuando os llega una petición con
un token, nos preguntáis a nosotros si ese token es válido y qué puede hacer su dueño.

---

## 1. Resumen en 30 segundos

```
Cliente ──[Authorization: Bearer <token>]──▶ VUESTRO servicio
                                                    │
                                                    │ POST /api/v1/auth/introspect
                                                    │ X-Service-Key: <clave>
                                                    │ { "token": "<token>" }
                                                    ▼
                                            Servicio de autenticación
                                                    │
                            { "active": true, "permissions": [...] }
                                                    │
                                                    ▼
                            ¿Tiene el permiso que necesito? → sí: 200 / no: 403
```

---

## 2. Datos que necesitáis de nosotros

| Dato | Valor |
|---|---|
| URL base | `http://<IP-DEL-EC2-DE-AUTENTICACION>/api/v1` |
| Endpoint de validación | `POST /api/v1/auth/introspect` |
| Cabecera de autorización entre servicios | `X-Service-Key: <clave compartida>` |
| Documentación interactiva | `http://<IP>/swagger-ui.html` |

La **clave compartida** os la pasamos por mensaje privado. No la escribáis en el código:
guardadla como variable de entorno en vuestro EC2.

---

## 3. El endpoint

### Petición

```http
POST /api/v1/auth/introspect HTTP/1.1
Host: 52.14.23.45
Content-Type: application/json
X-Service-Key: la-clave-que-os-pasamos

{
  "token": "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJqaG9qYW0i..."
}
```

El campo `token` es el JWT **tal cual**, sin el prefijo `Bearer `.

### Respuesta cuando el token es válido — `200 OK`

```json
{
  "active": true,
  "userId": 2,
  "username": "jhojam",
  "email": "jhojam@correo.com",
  "roles": ["ROLE_USER"],
  "permissions": ["FILE_UPLOAD", "FILE_READ", "USER_READ"],
  "issuedAt": "2026-11-14T15:56:55Z",
  "expiresAt": "2026-11-14T16:11:55Z",
  "issuer": "auth-service"
}
```

### Respuesta cuando el token NO es válido — `200 OK`

```json
{
  "active": false,
  "reason": "TOKEN_EXPIRED"
}
```

> ⚠️ **Importante:** un token inválido devuelve **`200 OK`** con `active: false`, **no**
> un error HTTP. Esto es a propósito (sigue el espíritu del estándar
> [RFC 7662](https://datatracker.ietf.org/doc/html/rfc7662)): la pregunta *"¿es válido
> este token?"* se respondió correctamente, y la respuesta fue "no".
>
> **Vuestra lógica debe mirar el campo `active`, no el código HTTP.**

### Motivos de rechazo posibles

| `reason` | Qué significa | Qué debéis responder a vuestro cliente |
|---|---|---|
| `TOKEN_EXPIRED` | El token caducó (duran 15 min) | `401` — que renueve el token |
| `TOKEN_INVALID` | Firma incorrecta, malformado o falsificado | `401` |
| `USER_NOT_FOUND` | El usuario fue eliminado | `401` |
| `USER_DISABLED` | Un administrador desactivó la cuenta | `403` |
| `USER_LOCKED` | Cuenta bloqueada por intentos fallidos | `403` |
| `INVALID_SERVICE_KEY` | **Vuestra** clave de servicio es incorrecta | `500` — es un error vuestro de configuración |

Si la clave de servicio es incorrecta, el código HTTP **sí** es `401`.

---

## 4. Los permisos que podéis exigir

Estos permisos ya existen en nuestro sistema y vienen dentro de `permissions`:

| Permiso | Para qué lo pensamos | Quién lo tiene |
|---|---|---|
| `FILE_UPLOAD` | Subir un archivo al bucket | `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_USER` |
| `FILE_READ` | Listar y descargar archivos | `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_USER` |
| `FILE_DELETE` | Borrar un archivo del bucket | solo `ROLE_ADMIN` |

Si necesitáis otros (por ejemplo `FILE_SHARE`), **decídnoslo**: los añadimos en nuestro
catálogo y aparecerán automáticamente en la respuesta. No os los inventéis por vuestro
lado, porque si no existen en nuestro sistema nunca llegarán en la lista.

### Qué permiso comprobar en cada operación

```
POST   /archivos        → exigir FILE_UPLOAD
GET    /archivos        → exigir FILE_READ
GET    /archivos/{id}   → exigir FILE_READ
DELETE /archivos/{id}   → exigir FILE_DELETE
```

---

## 5. Ejemplos de implementación

### 5.1 Spring Boot (si vuestro servicio también es Java)

```java
/**
 * Cliente que pregunta al servicio de autenticación si un token es válido.
 */
@Service
@RequiredArgsConstructor
public class AuthClient {

    private final RestClient restClient;

    @Value("${auth.service.url}")       // http://52.14.23.45/api/v1
    private String authServiceUrl;

    @Value("${auth.service.key}")       // la clave compartida
    private String serviceKey;

    /**
     * Valida un token contra el servicio de autenticación.
     *
     * @param token JWT recibido en la cabecera Authorization
     * @return el resultado de la introspección
     */
    public IntrospectionResult introspect(String token) {
        return restClient.post()
                .uri(authServiceUrl + "/auth/introspect")
                .header("X-Service-Key", serviceKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("token", token))
                .retrieve()
                .body(IntrospectionResult.class);
    }

    /** Estructura de la respuesta. Los campos nulos llegan ausentes del JSON. */
    public record IntrospectionResult(
            boolean active,
            String reason,
            Long userId,
            String username,
            String email,
            Set<String> roles,
            Set<String> permissions
    ) {
        /**
         * Comprueba si el portador del token tiene un permiso concreto.
         *
         * @param permission nombre del permiso, por ejemplo FILE_UPLOAD
         * @return true si el token es válido Y tiene ese permiso
         */
        public boolean can(String permission) {
            return active && permissions != null && permissions.contains(permission);
        }
    }
}
```

Y el uso en un controlador:

```java
@PostMapping("/archivos")
public ResponseEntity<?> subir(@RequestHeader("Authorization") String authHeader,
                               @RequestParam MultipartFile archivo) {

    String token = authHeader.replaceFirst("^Bearer ", "");
    var resultado = authClient.introspect(token);

    if (!resultado.active()) {
        return ResponseEntity.status(401).body(Map.of("error", resultado.reason()));
    }
    if (!resultado.can("FILE_UPLOAD")) {
        return ResponseEntity.status(403).body(Map.of("error", "Falta el permiso FILE_UPLOAD"));
    }

    // Aquí ya sabéis quién es: resultado.username() y resultado.userId()
    // Usadlo para organizar el bucket, por ejemplo: s3://bucket/usuarios/{userId}/...
    return ResponseEntity.ok(s3Service.subir(archivo, resultado.userId()));
}
```

### 5.2 Node.js / Express

```javascript
const axios = require('axios');

const AUTH_URL = process.env.AUTH_SERVICE_URL;   // http://52.14.23.45/api/v1
const SERVICE_KEY = process.env.AUTH_SERVICE_KEY;

/**
 * Middleware que valida el token y exige un permiso concreto.
 *
 * @param {string} permisoRequerido permiso que debe tener el usuario
 */
function requierePermiso(permisoRequerido) {
  return async (req, res, next) => {
    const header = req.headers.authorization || '';
    const token = header.startsWith('Bearer ') ? header.slice(7) : null;

    if (!token) {
      return res.status(401).json({ error: 'Falta la cabecera Authorization' });
    }

    try {
      const { data } = await axios.post(
        `${AUTH_URL}/auth/introspect`,
        { token },
        { headers: { 'X-Service-Key': SERVICE_KEY }, timeout: 5000 },
      );

      if (!data.active) {
        return res.status(401).json({ error: data.reason });
      }
      if (!data.permissions.includes(permisoRequerido)) {
        return res.status(403).json({ error: `Falta el permiso ${permisoRequerido}` });
      }

      req.usuario = data;   // queda disponible en el resto de la ruta
      next();
    } catch (error) {
      // Si el servicio de autenticación no responde, NO se deja pasar.
      console.error('Fallo al validar el token:', error.message);
      return res.status(503).json({ error: 'Servicio de autenticación no disponible' });
    }
  };
}

// Uso
app.post('/archivos', requierePermiso('FILE_UPLOAD'), subirArchivo);
app.get('/archivos', requierePermiso('FILE_READ'), listarArchivos);
app.delete('/archivos/:id', requierePermiso('FILE_DELETE'), borrarArchivo);
```

### 5.3 Python / FastAPI

```python
import os
import httpx
from fastapi import Depends, HTTPException, Header

AUTH_URL = os.environ["AUTH_SERVICE_URL"]
SERVICE_KEY = os.environ["AUTH_SERVICE_KEY"]


async def validar_token(authorization: str = Header(...)) -> dict:
    """Valida el token contra el servicio de autenticación y devuelve el usuario."""
    if not authorization.startswith("Bearer "):
        raise HTTPException(401, "Falta la cabecera Authorization")

    token = authorization.removeprefix("Bearer ")

    async with httpx.AsyncClient(timeout=5.0) as client:
        try:
            respuesta = await client.post(
                f"{AUTH_URL}/auth/introspect",
                json={"token": token},
                headers={"X-Service-Key": SERVICE_KEY},
            )
        except httpx.RequestError:
            raise HTTPException(503, "Servicio de autenticación no disponible")

    datos = respuesta.json()
    if not datos.get("active"):
        raise HTTPException(401, datos.get("reason", "TOKEN_INVALID"))
    return datos


def requiere_permiso(permiso: str):
    """Dependencia que además exige un permiso concreto."""
    async def comprobar(usuario: dict = Depends(validar_token)) -> dict:
        if permiso not in usuario.get("permissions", []):
            raise HTTPException(403, f"Falta el permiso {permiso}")
        return usuario
    return comprobar


# Uso
@app.post("/archivos")
async def subir(usuario: dict = Depends(requiere_permiso("FILE_UPLOAD"))):
    return {"subido_por": usuario["username"]}
```

---

## 6. Recomendaciones importantes

### 6.1 Cachead la respuesta unos segundos

Si llamáis a nuestra API en **cada** petición, nos convertís en un cuello de botella y
multiplicáis la latencia de vuestro servicio.

Guardad el resultado en memoria durante **30–60 segundos**, usando el token como clave.
Como nuestros tokens de acceso duran 15 minutos, una caché de un minuto es un compromiso
razonable: casi nunca llamáis, y si revocamos a alguien tarda como mucho un minuto en
notarlo.

```javascript
// Ejemplo mínimo en Node
const cache = new Map();
const TTL_MS = 45_000;

async function introspectarConCache(token) {
  const guardado = cache.get(token);
  if (guardado && Date.now() - guardado.momento < TTL_MS) {
    return guardado.datos;
  }
  const datos = await introspectar(token);
  cache.set(token, { datos, momento: Date.now() });
  return datos;
}
```

### 6.2 Si nuestro servicio no responde, NO dejéis pasar

Ante un fallo de red o un *timeout*, devolved `503` y rechazad la petición. Nunca
asumáis que el token es bueno porque no pudisteis comprobarlo: eso convierte una caída
nuestra en un agujero de seguridad vuestro.

### 6.3 Poned un *timeout* corto

5 segundos es suficiente. Sin *timeout*, una caída nuestra os deja las peticiones
colgadas y acabáis sin hilos disponibles.

### 6.4 No valides el JWT por tu cuenta

Técnicamente podríais verificar la firma vosotros mismos (os pasaríamos la clave), y
sería más rápido. **No lo recomendamos en este proyecto** porque:

- Tendríais que custodiar nuestra clave de firma: si se filtra por vuestro lado,
  cualquiera puede fabricar tokens válidos para todo el sistema.
- No detectaríais que un administrador desactivó una cuenta o le quitó un rol hace un
  minuto: el token seguiría pareciendo válido hasta caducar.

La introspección consulta la base de datos en cada llamada, así que los cambios de
permisos surten efecto de inmediato.

---

## 7. Probarlo sin escribir código

Pedid un token al servicio de autenticación y validadlo, todo con `curl`:

```bash
IP=52.14.23.45
CLAVE=la-clave-que-os-pasamos

# 1. Conseguir un token (como lo haría el frontend)
TOKEN=$(curl -s -X POST http://$IP/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identifier":"admin","password":"LA-CONTRASENA"}' \
  | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

echo "Token obtenido: ${TOKEN:0:40}..."

# 2. Validarlo (como lo haría vuestro servicio)
curl -s -X POST http://$IP/api/v1/auth/introspect \
  -H "Content-Type: application/json" \
  -H "X-Service-Key: $CLAVE" \
  -d "{\"token\":\"$TOKEN\"}"

# 3. Probar con un token inventado
curl -s -X POST http://$IP/api/v1/auth/introspect \
  -H "Content-Type: application/json" \
  -H "X-Service-Key: $CLAVE" \
  -d '{"token":"esto.no.es.valido"}'
# → {"active":false,"reason":"TOKEN_INVALID"}
```

También tenéis la interfaz gráfica en `http://<IP>/swagger-ui.html`, donde podéis probar
el endpoint rellenando un formulario.

---

## 8. Checklist de integración

- [ ] Tenemos la URL del servicio de autenticación
- [ ] Tenemos la clave `X-Service-Key` y está en una variable de entorno (no en el código)
- [ ] Nuestro grupo de seguridad permite salir hacia su EC2 (puerto 80)
- [ ] Les hemos pedido que su grupo de seguridad acepte entradas desde el nuestro
- [ ] Nuestro código mira `active`, no el código HTTP
- [ ] Comprobamos el permiso concreto de cada operación, no solo que el token sea válido
- [ ] Hay un *timeout* de 5 s y, si falla, rechazamos la petición
- [ ] Hay caché de ~45 s para no saturar su servicio
- [ ] Probado con token válido, token caducado y token inventado
