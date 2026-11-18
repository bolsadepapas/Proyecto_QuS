"""
Pruebas de Integración para los Endpoints REST de Api QuS
"""
import pytest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)


def test_health_endpoint():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "UP"
    assert "service" in data


def test_root_endpoint():
    response = client.get("/")
    assert response.status_code == 200
    data = response.json()
    assert "endpoints" in data


def test_consulta_sunat_gloria_valido():
    response = client.get("/api/v1/sunat/20100190797")
    assert response.status_code == 200
    data = response.json()
    assert data["ruc"] == "20100190797"
    assert "GLORIA" in data["razon_social"]
    assert data["estado"] == "ACTIVO"
    assert data["condicion"] == "HABIDO"


def test_consulta_indecopi_gloria_valido():
    response = client.get("/api/v1/indecopi/20100190797")
    assert response.status_code == 200
    data = response.json()
    assert data["ruc"] == "20100190797"
    assert data["total_sanciones"] >= 1
    assert data["tiene_sanciones"] is True
    assert len(data["sanciones"]) >= 1


def test_consulta_integrada_score_peru():
    response = client.get("/api/v1/consulta/20100190797")
    assert response.status_code == 200
    data = response.json()
    assert data["ruc"] == "20100190797"
    assert data["es_valido"] is True

    # Verificar contrato directo para Score Perú
    contribuyente = data["contribuyente"]
    assert contribuyente["ruc"] == "20100190797"
    assert contribuyente["estado"] == "ACTIVO"
    assert contribuyente["condicion"] == "HABIDO"

    sanciones = data["sanciones"]
    assert isinstance(sanciones, list)
    assert len(sanciones) >= 1
    assert sanciones[0]["entidad"] == "INDECOPI"
    assert "montoMultaUit" in sanciones[0]


def test_consulta_ruc_invalido_retorna_400():
    response = client.get("/api/v1/consulta/12345")
    assert response.status_code == 400
    data = response.json()
    assert data["detail"]["error"] == "RUC_INVALIDO"

