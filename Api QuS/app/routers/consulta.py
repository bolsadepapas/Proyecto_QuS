"""
Endpoint Maestro Integrador para Score Perú (Proyecto Calidad de Software).
"""
from fastapi import APIRouter
from ..models.unified import ConsultaIntegradaResponse
from ..services.aggregator_service import aggregator_service

router = APIRouter(prefix="/api/v1/consulta", tags=["Consulta Integrada"])


@router.get(
    "/{ruc}",
    response_model=ConsultaIntegradaResponse,
    summary="Consulta unificada SUNAT + INDECOPI",
    description=(
        "Endpoint principal diseñado para integrarse con Score Perú. "
        "Ejecuta en paralelo la consulta de SUNAT e INDECOPI, valida el dígito verificador Módulo 11 "
        "y retorna tanto las estructuras directas (Contribuyente y Sancion) como el detalle enriquecido."
    )
)
def consultar_ruc_integrado(ruc: str):
    return aggregator_service.consultar_ruc_completo(ruc)

