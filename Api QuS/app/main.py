"""
Punto de Entrada Principal de la API (Api QuS)
Proyecto de Calidad de Software - Score Perú
"""
import logging
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from .config import settings
from .routers import health, sunat, indecopi, consulta

# Configuración de Logging
logging.basicConfig(
    level=logging.INFO if not settings.DEBUG else logging.DEBUG,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
logger = logging.getLogger("api-qus")

app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    description=(
        "API REST desarrollada para el proyecto de Calidad de Software (Score Perú). "
        "Permite consultar y agregar información de contribuyentes desde SUNAT "
        "(Consulta RUC) e INDECOPI (Mira a quién le compras) a partir del número de RUC de 11 dígitos."
    ),
    docs_url="/docs",
    redoc_url="/redoc"
)

# Configuración de CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Manejador global de excepciones para garantizar respuestas JSON uniformes
@app.exception_handler(Exception)
async def generic_exception_handler(request: Request, exc: Exception):
    logger.error("Error no controlado en %s: %s", request.url.path, str(exc), exc_info=True)
    return JSONResponse(
        status_code=500,
        content={
            "error": "INTERNAL_SERVER_ERROR",
            "mensaje": "Ocurrió un error interno en el servicio Api QuS.",
            "detalle": str(exc) if settings.DEBUG else None
        }
    )

# Inclusión de Routers
app.include_router(health.router)
app.include_router(consulta.router)
app.include_router(sunat.router)
app.include_router(indecopi.router)


@app.get("/", tags=["Root"])
def root():
    return {
        "mensaje": "Api QuS activa y lista para recibir consultas",
        "documentacion": "/docs",
        "health": "/health",
        "endpoints": {
            "consulta_unificada": "/api/v1/consulta/{ruc}",
            "consulta_sunat": "/api/v1/sunat/{ruc}",
            "consulta_indecopi": "/api/v1/indecopi/{ruc}"
        }
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host=settings.HOST, port=settings.PORT, reload=settings.DEBUG)

