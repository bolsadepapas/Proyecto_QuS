"""
Pruebas Unitarias para el Validador de RUC (Módulo 11 de SUNAT)
Calidad de Software: Aseguramiento de Reglas de Entrada (GIGO prevention)
"""
import pytest
from app.core.validator import (
    validar_ruc,
    es_formato_ruc_valido,
    verificar_digito_control_modulo11,
    clasificar_tipo_contribuyente
)


def test_ruc_valido_leche_gloria():
    # RUC 20100190797 (Leche Gloria S.A.)
    ruc = "20100190797"
    valido, mensaje = validar_ruc(ruc)
    assert valido is True
    assert "PERSONA JURIDICA" in mensaje


def test_ruc_invalido_longitud_incorrecta():
    valido, mensaje = validar_ruc("2010019079")
    assert valido is False
    assert "11 dígitos" in mensaje

    valido, mensaje = validar_ruc("2010019079799")
    assert valido is False
    assert "11 dígitos" in mensaje


def test_ruc_invalido_caracteres_no_numericos():
    valido, mensaje = validar_ruc("2010019079A")
    assert valido is False
    assert "no numéricos" in mensaje


def test_ruc_invalido_prefijo_desconocido():
    valido, mensaje = validar_ruc("30100190797")
    assert valido is False
    assert "Prefijo" in mensaje


def test_ruc_invalido_digito_verificador_modulo11():
    # Cambiamos el último dígito de Gloria de 7 a 5
    ruc_alterado = "20100190795"
    assert verificar_digito_control_modulo11(ruc_alterado) is False
    valido, mensaje = validar_ruc(ruc_alterado)
    assert valido is False
    assert "dígito verificador" in mensaje


def test_clasificacion_persona_natural_con_negocio():
    tipo = clasificar_tipo_contribuyente("10123456789")
    assert tipo == "PERSONA NATURAL CON NEGOCIO"


def test_clasificacion_persona_juridica():
    tipo = clasificar_tipo_contribuyente("20100190797")
    assert tipo == "PERSONA JURIDICA (EMPRESA / SOCIEDAD)"

