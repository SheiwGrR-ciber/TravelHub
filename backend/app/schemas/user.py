from typing import Optional
from pydantic import BaseModel, EmailStr, Field, field_validator

PROVIDER_TYPES = {"guia", "hotel", "restaurante", "traductor", "transportista"}

class UserCreate(BaseModel):
    name: str
    email: EmailStr
    password: str
    role: str  # turista, prestador, admin

    @field_validator("role")
    @classmethod
    def validate_public_role(cls, value: str) -> str:
        if value not in {"turista", "prestador"}:
            raise ValueError("El rol debe ser turista o prestador")
        return value

    @field_validator("password")
    @classmethod
    def validate_password(cls, value: str) -> str:
        if len(value) < 8:
            raise ValueError("La contrasena debe tener al menos 8 caracteres")
        if len(value.encode("utf-8")) > 72:
            raise ValueError("La contrasena no puede superar 72 bytes")
        return value

class UserProfileUpdate(BaseModel):
    name: str = Field(min_length=2, max_length=100)
    phone: Optional[str] = Field(default=None, max_length=30)
    location: Optional[str] = Field(default=None, max_length=150)
    bio: Optional[str] = Field(default=None, max_length=500)
    business_name: Optional[str] = Field(default=None, max_length=150)
    provider_type: Optional[str] = None
    experience_years: Optional[int] = Field(default=None, ge=0, le=80)

    @field_validator("name", "phone", "location", "bio", "business_name", mode="before")
    @classmethod
    def clean_text(cls, value):
        if value is None:
            return None
        cleaned = value.strip()
        return cleaned or None

    @field_validator("provider_type")
    @classmethod
    def validate_provider_type(cls, value: Optional[str]):
        if value is not None and value not in PROVIDER_TYPES:
            raise ValueError("Tipo de prestador no valido")
        return value
