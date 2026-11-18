"""
Excepciones de dominio de la API
"""
from fastapi import HTTPException, status


class RucInvalidoException(HTTPException):
    def __init__(self, detalle: str):
        super().__init__(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={
                "error": "RUC_INVALIDO",
                "mensaje": detalle,
                "codigo": 400
            }
        )


class ContribuyenteNoEncontradoException(HTTPException):
    def __init__(self, ruc: str):
        super().__init__(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={
                "error": "CONTRIBUYENTE_NO_ENCONTRADO",
                "mensaje": f"No se encontró información para el RUC {ruc}",
                "codigo": 404
            }
        )


class ServicioExternoException(HTTPException):
    def __init__(self, entidad: str, mensaje: str, status_code: int = status.HTTP_502_BAD_GATEWAY):
        super().__init__(
            status_code=status_code,
            detail={
                "error": f"FALLO_SERVICIO_{entidad.upper()}",
                "entidad": entidad,
                "mensaje": mensaje,
                "codigo": status_code
            }
        )

