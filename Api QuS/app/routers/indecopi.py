"""
Endpoints para consultar sanciones e historial de proveedores en INDECOPI.
"""
from fastapi import APIRouter
from ..models.indecopi import IndecopiResponse
from ..core.validator import validar_ruc
from ..core.exceptions import RucInvalidoException
from ..services.indecopi_client import indecopi_client

router = APIRouter(prefix="/api/v1/indecopi", tags=["INDECOPI"])


@router.get(
    "/{ruc}",
    response_model=IndecopiResponse,
    summary="Consulta de Sanciones en INDECOPI (Mira a quién le compras)",
    description="Devuelve el resumen de sanciones, multas acumuladas en UIT y el detalle histórico de resoluciones firmes."
)
def consultar_indecopi(ruc: str):
    es_valido, motivo = validar_ruc(ruc)
    if not es_valido:
        raise RucInvalidoException(motivo)

    return indecopi_client.consultar_sanciones_por_ruc(ruc)

