"""
Modelo Unificado para integración con Score Perú (Calidad de Software).
Adapta y unifica los datos de SUNAT e INDECOPI en un único contrato REST
directamente consumible por el Motor de Scoring y las interfaces Java.
"""
from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from .sunat import SunatContribuyenteResponse
from .indecopi import IndecopiResponse


class ContribuyenteScorePeru(BaseModel):
    """
    Estructura 100% compatible con el Record Java:
    com.scoreperu.api.model.Contribuyente
    """
    ruc: str
    razonSocial: str
    estado: str
    condicion: str
    inhabilitado: bool = False
    fechaInicioActividades: Optional[str] = None


class SancionScorePeru(BaseModel):
    """
    Estructura 100% compatible con el Record Java:
    com.scoreperu.api.model.Sancion
    """
    idSancion: str
    entidad: str = "INDECOPI"
    motivo: str
    fechaSancion: Optional[str] = None
    montoMultaUit: float = 0.0


class ConsultaIntegradaResponse(BaseModel):
    """
    Payload principal devuelto por GET /api/v1/consulta/{ruc}
    """
    ruc: str
    es_valido: bool = True
    tipo_contribuyente: str

    # Contratos directos para Score Perú
    contribuyente: ContribuyenteScorePeru
    sanciones: List[SancionScorePeru] = Field(default_factory=list)

    # Objetos enriquecidos con la totalidad de los datos extraídos
    detalle_sunat: SunatContribuyenteResponse
    detalle_indecopi: IndecopiResponse

    # Metadatos de auditoría y rendimiento (Calidad de Software)
    metadatos: Dict[str, Any] = Field(default_factory=dict)

