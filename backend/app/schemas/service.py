from pydantic import BaseModel, field_validator
from typing import Optional

SERVICE_TYPES = ["guia", "hotel", "restaurante", "traductor", "transportista"]

class ServiceCreate(BaseModel):
    type: str
    name: str
    description: str
    price: float
    location: str

    @field_validator("type")
    @classmethod
    def validate_type(cls, v: str) -> str:
        if v not in SERVICE_TYPES:
            raise ValueError(f"Tipo de servicio inválido. Permitidos: {SERVICE_TYPES}")
        return v

    @field_validator("price")
    @classmethod
    def validate_price(cls, v: float) -> float:
        if v <= 0:
            raise ValueError("El precio debe ser mayor a 0")
        return v

class ServiceUpdate(BaseModel):
    type: Optional[str] = None
    name: Optional[str] = None
    description: Optional[str] = None
    price: Optional[float] = None
    location: Optional[str] = None
    available: Optional[bool] = None

    @field_validator("type")
    @classmethod
    def validate_type(cls, v: Optional[str]) -> Optional[str]:
        if v is not None and v not in SERVICE_TYPES:
            raise ValueError(f"Tipo de servicio inválido. Permitidos: {SERVICE_TYPES}")
        return v

    @field_validator("price")
    @classmethod
    def validate_price(cls, v: Optional[float]) -> Optional[float]:
        if v is not None and v <= 0:
            raise ValueError("El precio debe ser mayor a 0")
        return v