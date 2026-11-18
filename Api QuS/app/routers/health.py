"""
Endpoint de monitoreo y estado de salud (Health Check).
"""
from fastapi import APIRouter
from datetime import datetime

router = APIRouter(tags=["Monitoreo"])


@router.get("/health")
def health_check():
    return {
        "status": "UP",
        "timestamp": datetime.utcnow().isoformat() + "Z",
        "service": "Api QuS - Integrador SUNAT & INDECOPI",
        "version": "1.0.0"
    }

