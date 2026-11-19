"""
Servicio Agregador y Orquestador de Consultas.
Coordina la validación del RUC, la llamada a SUNAT y a INDECOPI,
y consolida los resultados en el formato que requiere Score Perú.
"""
import time
import logging
from concurrent.futures import ThreadPoolExecutor
from typing import Dict, Any

from ..core.validator import validar_ruc
from ..core.exceptions import RucInvalidoException
from ..models.unified import ConsultaIntegradaResponse
from .sunat_client import sunat_client
from .indecopi_client import indecopi_client

logger = logging.getLogger(__name__)


class AggregatorService:
    def __init__(self):
        self.executor = ThreadPoolExecutor(max_workers=4)

    def consultar_ruc_completo(self, ruc: str) -> ConsultaIntegradaResponse:
        """
        Valida el RUC y consulta en paralelo SUNAT e INDECOPI.
        """
        inicio = time.time()
        es_valido, motivo_o_tipo = validar_ruc(ruc)
        if not es_valido:
            raise RucInvalidoException(motivo_o_tipo)

        ruc_limpio = ruc.strip()
        logger.info("Iniciando consulta agregada para RUC %s (%s)", ruc_limpio, motivo_o_tipo)

        # Consultas en paralelo para optimizar tiempo de respuesta (RNF de Calidad)
        futuro_sunat = self.executor.submit(sunat_client.consultar_ruc, ruc_limpio)
        futuro_indecopi = self.executor.submit(indecopi_client.consultar_sanciones_por_ruc, ruc_limpio)

        sunat_res = futuro_sunat.result()
        indecopi_res = futuro_indecopi.result()

        # Si SUNAT devolvió un nombre genérico de contingencia (por bloqueo Cloudflare 403),
        # enriquecemos la razón social con el nombre oficial registrado en INDECOPI
        if (
            indecopi_res.razon_social
            and indecopi_res.razon_social.strip()
            and not indecopi_res.razon_social.strip().startswith("CONTRIBUYENTE ")
            and (
                sunat_res.fuente_datos == "PADRON_ESTANDAR_SUNAT"
                or sunat_res.razon_social.startswith("EMPRESA COMERCIAL RUC ")
                or sunat_res.razon_social.startswith("CONTRIBUYENTE ")
            )
        ):
            sunat_res.razon_social = indecopi_res.razon_social.strip()
            sunat_res.nombre_comercial = indecopi_res.razon_social.strip()

        # Adaptación para contratos de Score Perú
        contribuyente_sp = sunat_client.adaptar_para_score_peru(sunat_res)
        sanciones_sp = indecopi_client.adaptar_para_score_peru(indecopi_res)

        duracion_ms = round((time.time() - inicio) * 1000, 2)
        logger.info("Consulta agregada para RUC %s completada en %s ms", ruc_limpio, duracion_ms)

        metadatos: Dict[str, Any] = {
            "latencia_ms": duracion_ms,
            "fuente_sunat": sunat_res.fuente_datos,
            "sanciones_encontradas": len(sanciones_sp),
            "multas_totales_uit": indecopi_res.total_multas_uit,
            "version_api": "1.0.0"
        }

        return ConsultaIntegradaResponse(
            ruc=ruc_limpio,
            es_valido=True,
            tipo_contribuyente=motivo_o_tipo,
            contribuyente=contribuyente_sp,
            sanciones=sanciones_sp,
            detalle_sunat=sunat_res,
            detalle_indecopi=indecopi_res,
            metadatos=metadatos
        )


aggregator_service = AggregatorService()

