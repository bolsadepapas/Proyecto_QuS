"""
Endpoints para consultar información tributaria en SUNAT.
"""
from fastapi import APIRouter
from ..models.sunat import SunatContribuyenteResponse
from ..core.validator import validar_ruc
from ..core.exceptions import RucInvalidoException
from ..services.sunat_client import sunat_client

router = APIRouter(prefix="/api/v1/sunat", tags=["SUNAT"])


@router.get(
    "/{ruc}",
    response_model=SunatContribuyenteResponse,
    summary="Consulta de RUC en SUNAT",
    description="Devuelve el estado, condición, domicilio fiscal, actividades económicas y comprobantes autorizados."
)
def consultar_sunat(ruc: str):
    es_valido, motivo = validar_ruc(ruc)
    if not es_valido:
        raise RucInvalidoException(motivo)

    return sunat_client.consultar_ruc(ruc)

