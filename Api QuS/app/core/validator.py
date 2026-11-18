"""
Validador de RUC (Registro Único de Contribuyentes - Perú)
Aplica el algoritmo oficial de SUNAT (Módulo 11 ponderado).
"""
import re
from typing import Tuple


PREFIJOS_VALIDOS = ("10", "15", "17", "20")
PONDERADORES = [5, 4, 3, 2, 7, 6, 5, 4, 3, 2]


def es_formato_ruc_valido(ruc: str) -> bool:
    """Verifica si el string tiene exactamente 11 dígitos numéricos."""
    if not ruc or not isinstance(ruc, str):
        return False
    return bool(re.match(r"^\d{11}$", ruc.strip()))


def verificar_digito_control_modulo11(ruc: str) -> bool:
    """
    Calcula el dígito verificador usando el algoritmo de Módulo 11 de SUNAT.
    Multiplica los primeros 10 dígitos por los factores [5, 4, 3, 2, 7, 6, 5, 4, 3, 2],
    obtiene el residuo contra 11 y el complemento (11 - residuo).
    """
    ruc = ruc.strip()
    if not es_formato_ruc_valido(ruc):
        return False

    digitos = [int(c) for c in ruc]
    suma = sum(d * p for d, p in zip(digitos[:10], PONDERADORES))
    resto = suma % 11
    complemento = 11 - resto

    if complemento == 10:
        digito_esperado = 0
    elif complemento == 11:
        digito_esperado = 1
    else:
        digito_esperado = complemento

    return digitos[10] == digito_esperado


def validar_ruc(ruc: str) -> Tuple[bool, str]:
    """
    Realiza una validación completa de un número de RUC.
    Retorna (es_valido, motivo_error_o_tipo_persona).
    """
    if not ruc or not isinstance(ruc, str):
        return False, "El RUC no puede ser nulo ni vacío"

    ruc_limpio = ruc.strip()

    if not re.match(r"^\d+$", ruc_limpio):
        return False, f"El RUC '{ruc}' contiene caracteres no numéricos"

    if len(ruc_limpio) != 11:
        return False, f"El RUC debe tener exactamente 11 dígitos. Se recibieron {len(ruc_limpio)}"

    prefijo = ruc_limpio[:2]
    if prefijo not in PREFIJOS_VALIDOS:
        return False, f"Prefijo '{prefijo}' no reconocido para RUC en Perú (debe iniciar con 10, 15, 17 o 20)"

    if not verificar_digito_control_modulo11(ruc_limpio):
        return False, f"El dígito verificador del RUC '{ruc_limpio}' es inválido según Módulo 11"

    tipo = clasificar_tipo_contribuyente(ruc_limpio)
    return True, tipo


def clasificar_tipo_contribuyente(ruc: str) -> str:
    """Clasifica el tipo de persona en base al prefijo del RUC."""
    prefijo = ruc[:2]
    if prefijo == "10":
        return "PERSONA NATURAL CON NEGOCIO"
    elif prefijo in ("15", "17"):
        return "PERSONA NATURAL SIN NEGOCIO"
    elif prefijo == "20":
        return "PERSONA JURIDICA (EMPRESA / SOCIEDAD)"
    return "OTRO TIPO DE CONTRIBUYENTE"

