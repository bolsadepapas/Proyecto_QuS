"""
Modelos de datos para la información proveniente de SUNAT (Consulta RUC).
Refleja fielmente la pantalla 'Resultado de la Búsqueda' del portal e-consultaruc.
"""
from pydantic import BaseModel, Field
from typing import List, Optional


class SunatContribuyenteResponse(BaseModel):
    ruc: str = Field(..., description="Número de RUC de 11 dígitos")
    razon_social: str = Field(..., description="Razón Social o Apellidos y Nombres")
    tipo_contribuyente: Optional[str] = Field(None, description="Ej: SOCIEDAD ANONIMA, PERSONA NATURAL")
    nombre_comercial: Optional[str] = Field(None, description="Nombre comercial registrado")
    fecha_inscripcion: Optional[str] = Field(None, description="Fecha de inscripción en formato DD/MM/AAAA")
    fecha_inicio_actividades: Optional[str] = Field(None, description="Fecha inicio actividades DD/MM/AAAA")
    estado: str = Field("ACTIVO", description="Estado del contribuyente: ACTIVO, SUSPENSION TEMPORAL, BAJA DEFINITIVA")
    condicion: str = Field("HABIDO", description="Condición: HABIDO, NO HABIDO, NO HALLADO")
    domicilio_fiscal: Optional[str] = Field(None, description="Dirección legal completa")
    sistema_emision_comprobante: Optional[str] = Field(None, description="Ej: MANUAL/COMPUTARIZADO")
    actividad_comercio_exterior: Optional[str] = Field(None, description="Ej: IMPORTADOR/EXPORTADOR, SIN ACTIVIDAD")
    sistema_contabilidad: Optional[str] = Field(None, description="Ej: COMPUTARIZADO, MANUAL")
    actividades_economicas: List[str] = Field(default_factory=list, description="Lista de actividades principal y secundarias")
    comprobantes_pago_autorizados: List[str] = Field(default_factory=list, description="Comprobantes autorizados: FACTURA, BOLETA, etc.")
    sistema_emision_electronica: List[str] = Field(default_factory=list, description="Sistemas de emisión electrónica autorizados")
    es_activo: bool = Field(True, description="Flag booleano que indica si estado == ACTIVO")
    es_habido: bool = Field(True, description="Flag booleano que indica si condicion == HABIDO")
    fuente_datos: str = Field("SUNAT_PORTAL", description="Identificador de la fuente: SUNAT_PORTAL o PADRON_REDUCIDO")

