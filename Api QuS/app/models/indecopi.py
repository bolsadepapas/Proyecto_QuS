"""
Modelos de datos para la información proveniente de INDECOPI (Mira a quién le compras).
Refleja la cabecera de la empresa, estadísticas de sanciones y la tabla de sanciones impuestas.
"""
from pydantic import BaseModel, Field
from typing import List, Optional


class SancionItem(BaseModel):
    anio: int = Field(..., description="Año de la sanción / multa")
    materia: str = Field(..., description="Materia o sector (ej. ELABORACION DE BEBIDAS, SERVICIOS BANCARIOS)")
    hecho_infractor: str = Field(..., description="Descripción del hecho infractor (ej. INFORMACION, IDONEIDAD)")
    numero_resolucion: str = Field(..., description="Número oficial de resolución (ej. 2482-2024/CC2)")
    fecha_acto_firme: Optional[str] = Field(None, description="Fecha de acto firme (YYYY-MM-DD)")
    sancion_impuesta_uit: float = Field(0.0, description="Monto de la multa expresado en UIT")
    tipo_sancion: str = Field("MULTA", description="MULTA (si > 0 UIT) o AMONESTACION (si == 0 UIT)")


class EvolucionSancion(BaseModel):
    anio: str = Field(..., description="Año analizado")
    total_sanciones: int = Field(..., description="Número de sanciones en dicho año")


class IndecopiResponse(BaseModel):
    ruc: str = Field(..., description="Número de documento de identidad consultado")
    razon_social: str = Field(..., description="Razón Social devuelta por Indecopi")
    nombre_comercial: Optional[str] = Field("", description="Nombre comercial si aplica")
    total_sanciones: int = Field(0, description="Conteo total de sanciones registradas")
    total_multas_uit: float = Field(0.0, description="Suma total de multas impuestas en UIT")
    total_reclamos: int = Field(0, description="Total de reclamos reportados")
    total_denuncias: int = Field(0, description="Total de denuncias reportadas")
    subtitulo_periodo: Optional[str] = Field(None, description="Rango de fechas evaluado por Indecopi")
    evolucion_anual: List[EvolucionSancion] = Field(default_factory=list, description="Histórico de sanciones por año")
    sanciones: List[SancionItem] = Field(default_factory=list, description="Lista detallada de sanciones")
    tiene_sanciones: bool = Field(False, description="Flag de riesgo: true si total_sanciones > 0")

