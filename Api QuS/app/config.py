"""
Configuración general de la API de Calidad de Software (Api QuS).
Las credenciales OAuth 2.0 se cargan exclusivamente desde variables de entorno (.env).
"""
from pathlib import Path
from pydantic_settings import BaseSettings, SettingsConfigDict
from typing import Optional

BASE_DIR = Path(__file__).resolve().parent.parent


class Settings(BaseSettings):
    # Entorno
    APP_NAME: str = "Api QuS - Servicio Integrador SUNAT & INDECOPI"
    APP_VERSION: str = "1.0.0"
    DEBUG: bool = True
    PORT: int = 8000
    HOST: str = "0.0.0.0"

    # Indecopi OAuth & REST API (Cargadas desde variables de entorno / .env)
    INDECOPI_TOKEN_URL: str = "https://servicios.indecopi.gob.pe/appTodosServicioAutorizacion/oauth/token"
    INDECOPI_BASE_API: str = "https://servicios.indecopi.gob.pe/appDPCMAQLCApi"
    INDECOPI_USERNAME: str = ""
    INDECOPI_PASSWORD: str = ""
    INDECOPI_CLIENT_ID: str = ""
    INDECOPI_CLIENT_SECRET: str = ""

    # SUNAT Config
    SUNAT_CONSULTA_URL: str = "https://e-consultaruc.sunat.gob.pe/cl-ti-itmrconsruc/FrameCriterioBusquedaWeb.jsp"
    SUNAT_ALIAS_URL: str = "https://e-consultaruc.sunat.gob.pe/cl-ti-itmrconsruc/jcrS00Alias"
    SUNAT_COOKIE: Optional[str] = None

    # Timeouts (segundos)
    REQUEST_TIMEOUT: int = 15

    model_config = SettingsConfigDict(
        env_file=str(BASE_DIR / ".env"),
        case_sensitive=True,
        extra="ignore"
    )


settings = Settings()
