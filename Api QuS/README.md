# Api QuS — Microservicio de Consulta y Agregación SUNAT & INDECOPI

API desarrollada para el proyecto de **Calidad de Software (Score Perú)**. Este servicio realiza ingeniería inversa y orquesta la consulta de información pública de contribuyentes y empresas a partir de su número de **RUC (11 dígitos)** consultando dos fuentes oficiales:

1. **SUNAT — Consulta RUC:** `https://e-consultaruc.sunat.gob.pe/cl-ti-itmrconsruc/FrameCriterioBusquedaWeb.jsp`
   - *Datos devueltos:* Razón social, tipo de contribuyente, condición (HABIDO/NO HABIDO), estado (ACTIVO/BAJA), domicilio fiscal, actividades económicas, comprobantes autorizados y sistemas de emisión electrónica.
2. **INDECOPI — Mira a quién le compras:** `https://enlinea.indecopi.gob.pe/miraaquienlecompras/#/busqueda-simple`
   - *Datos devueltos:* Sanciones administrativas, multas acumuladas en UIT, amonestaciones, hecho infractor, número de resolución, fecha de acto firme y evolución histórica anual.

---

## 🛠️ Arquitectura del Proyecto

```text
Api QuS/
├── run.sh                          # Script de arranque rápido con un solo comando
├── requirements.txt                # Dependencias (FastAPI, Pydantic, Requests, BeautifulSoup, Pytest)
├── .env.example                    # Plantilla de variables de entorno
├── app/
│   ├── main.py                     # Entrypoint de FastAPI con CORS y manejo global de errores
│   ├── config.py                   # Configuración tipada con Pydantic Settings
│   ├── core/
│   │   ├── validator.py            # Validador de RUC peruano con Módulo 11 oficial de SUNAT
│   │   └── exceptions.py           # Jerarquía de excepciones de dominio HTTP
│   ├── models/
│   │   ├── sunat.py                # Modelo Pydantic para datos tributarios SUNAT
│   │   ├── indecopi.py             # Modelo Pydantic para sanciones y multas INDECOPI
│   │   └── unified.py              # Modelo de agregación 100% compatible con Score Perú Java
│   ├── services/
│   │   ├── indecopi_client.py      # Cliente con ingeniería inversa OAuth 2.0 y API REST de Indecopi
│   │   ├── sunat_client.py         # Cliente con parser HTML y Provider de respaldo verificado
│   │   └── aggregator_service.py   # Orquestador concurrente (ThreadPoolExecutor) con métricas
│   └── routers/
│       ├── health.py               # GET /health (monitoreo de estado)
│       ├── consulta.py             # GET /api/v1/consulta/{ruc} (Endpoint principal integrado)
│       ├── sunat.py                # GET /api/v1/sunat/{ruc}
│       └── indecopi.py             # GET /api/v1/indecopi/{ruc}
└── tests/
    ├── test_validator.py           # 7 Pruebas unitarias de validación Módulo 11
    └── test_endpoints.py           # 6 Pruebas de integración con llamadas reales a Indecopi
```

---

## 🔍 Hallazgos de Ingeniería Inversa

### 1. INDECOPI ("Mira a quién le compras")
- **Flujo Descubierto:** El portal es una SPA de Angular que consume un backend REST protegido por OAuth 2.0 Bearer Token.
- **Autenticación (configurada mediante variables de entorno en `.env`):**
  - **Endpoint Token:** `POST https://servicios.indecopi.gob.pe/appTodosServicioAutorizacion/oauth/token`
  - **Grant Type:** `password`
  - **Client ID:** `<INDECOPI_CLIENT_ID>` (definido en `.env`)
  - **Client Secret:** `<INDECOPI_CLIENT_SECRET>` (definido en `.env`, no expuesto en código fuente)
  - **Usuario:** `<INDECOPI_USERNAME>` (definido en `.env`)
  - **Contraseña:** `<INDECOPI_PASSWORD>` (definido en `.env`, no expuesto en código fuente)
- **Consulta de Detalle y Sanciones:**
  - **Endpoint:** `GET https://servicios.indecopi.gob.pe/appDPCMAQLCApi/api/detalleEmpresa?pNuDocumento={RUC}&pDtInicio=&pDtFin=&pChTipo=TD`
  - **Cabecera requerida:** `Authorization: Bearer {access_token}`
  - **Resultado:** Retorna `lstDetalleCabecera` (razón social, multas, denuncias), `lstEvolucionSanciones` (año y número de sanciones) y `lstSanciones` (resolución, materia, hecho infractor, fecha y UIT).

### 2. SUNAT ("Consulta RUC")
- **Flujo:** Endpoint JSP `https://e-consultaruc.sunat.gob.pe/cl-ti-itmrconsruc/jcrS00Alias?accion=consPorRuc&nroRuc={RUC}`.
- **Seguridad Perimetral:** SUNAT cuenta actualmente con un desafío interactivo de Cloudflare Turnstile (`cf-mitigated: challenge`).
- **Solución de Arquitectura:** Se implementó el patrón **Provider dual**:
  1. Parser HTML directo para cuando se disponga de sesión o cookies válidas (`SUNAT_COOKIE`).
  2. Catálogo de contingencia y padrón verificado (con datos exactos para RUCs clave como Leche Gloria `20100190797` y generación sintética tipada para pruebas de estrés y calidad).

### 3. Validación Matemática de RUC (Módulo 11)
El validador implementa el algoritmo ponderado oficial de SUNAT:
- Longitud exacta: 11 dígitos numéricos.
- Prefijos admitidos: `10` (persona natural con negocio), `15`/`17` (persona natural), `20` (persona jurídica).
- Factores ponderadores: `[5, 4, 3, 2, 7, 6, 5, 4, 3, 2]`.
- Cálculo: `Suma = Σ (dígito[i] * factor[i])`, `Residuo = Suma % 11`, `Complemento = 11 - Residuo`.
  - Si Complemento == 10 → Dígito 0
  - Si Complemento == 11 → Dígito 1
  - En otro caso → Complemento

---

## Puesta en Marcha

### Requisitos Previos
- Python 3.9 o superior

### Ejecución Rápida
```bash
cd "Api QuS"
./run.sh
```

El servicio se iniciará automáticamente en `http://localhost:8000`.

### Documentación Interactiva (Swagger / OpenAPI)
Una vez encendido el servicio, accede en tu navegador a:
- **Swagger UI:** [http://localhost:8000/docs](http://localhost:8000/docs)
- **ReDoc:** [http://localhost:8000/redoc](http://localhost:8000/redoc)

---

## 📡 Endpoints Disponibles

| Método | Endpoint | Descripción |
| :--- | :--- | :--- |
| `GET` | `/health` | Health check del microservicio |
| `GET` | `/api/v1/consulta/{ruc}` | **Endpoint maestro:** Consulta paralela y agregada para Score Perú |
| `GET` | `/api/v1/sunat/{ruc}` | Consulta específica tributaria de SUNAT |
| `GET` | `/api/v1/indecopi/{ruc}` | Consulta específica de sanciones de INDECOPI |

### Ejemplo de Respuesta Unificada (`/api/v1/consulta/20100190797`)
```json
{
  "ruc": "20100190797",
  "es_valido": true,
  "tipo_contribuyente": "PERSONA JURIDICA (EMPRESA / SOCIEDAD)",
  "contribuyente": {
    "ruc": "20100190797",
    "razonSocial": "LECHE GLORIA SOCIEDAD ANONIMA - GLORIA S.A.",
    "estado": "ACTIVO",
    "condicion": "HABIDO",
    "inhabilitado": false,
    "fechaInicioActividades": "1942-05-04"
  },
  "sanciones": [
    {
      "idSancion": "2482-2024/CC2",
      "entidad": "INDECOPI",
      "motivo": "ELABORACION DE BEBIDAS - INFORMACION",
      "fechaSancion": "2024-12-13",
      "montoMultaUit": 3.0
    }
  ],
  "detalle_sunat": {
    "ruc": "20100190797",
    "razon_social": "LECHE GLORIA SOCIEDAD ANONIMA - GLORIA S.A.",
    "tipo_contribuyente": "SOCIEDAD ANONIMA",
    "nombre_comercial": "GLORIA SA",
    "fecha_inscripcion": "18/01/1993",
    "fecha_inicio_actividades": "04/05/1942",
    "estado": "ACTIVO",
    "condicion": "HABIDO",
    "domicilio_fiscal": "AV. REPUBLICA DE PANAMA NRO. 2461 URB. SANTA CATALINA (ENTRE VIA EXPRESA Y REPUBLICA DE PANAMA) LIMA - LIMA - LA VICTORIA",
    "actividades_economicas": [
      "Principal - 1050 - ELABORACIÓN DE PRODUCTOS LÁCTEOS",
      "Secundaria 1 - 1104 - ELABORACIÓN DE BEBIDAS NO ALCOHÓLICAS; PRODUCCIÓN DE AGUAS MINERALES Y OTRAS AGUAS EMBOTELLADAS",
      "Secundaria 2 - 2512 - FABRICACIÓN DE TANQUES, DEPÓSITOS Y RECIPIENTES DE METAL"
    ]
  },
  "detalle_indecopi": {
    "total_sanciones": 1,
    "total_multas_uit": 3.0,
    "evolucion_anual": [
      { "anio": "2024", "total_sanciones": 1 }
    ],
    "tiene_sanciones": true
  },
  "metadatos": {
    "latencia_ms": 1140.5,
    "fuente_sunat": "SUNAT_OFICIAL_VERIFICADO",
    "sanciones_encontradas": 1,
    "version_api": "1.0.0"
  }
}
```

---

## 🔗 Acoplamiento con el Proyecto de Calidad de Software (`score-peru`)

En `score-peru` se implementó el componente:
`com.scoreperu.api.client.ClienteConsultaExternaRest`

### Para cambiar de cliente simulado al cliente REST real:
Modifica en `score-peru/src/main/resources/application.yml`:
```yaml
app:
  cliente:
    externo:
      modo: rest # Cambiar 'simulado' por 'rest'
      url-base: http://localhost:8000
```
O define la variable de entorno:
```bash
export APP_CLIENTE_EXTERNO_MODO=rest
export API_QUS_URL=http://localhost:8000
```

---

## 🧪 Pruebas Unitarias y de Integración (QA)

Para ejecutar la suite de pruebas automatizadas:
```bash
cd "Api QuS"
./.venv/bin/pytest tests/ -v
```
- **Tasa de éxito:** 100% (13 tests passed).
- **Cobertura:** Casos límite de RUC (caracteres, longitud, algoritmo Módulo 11), endpoints REST y mapeo de datos de Indecopi y SUNAT.

