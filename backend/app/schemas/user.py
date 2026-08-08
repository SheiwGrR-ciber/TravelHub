from pydantic import BaseModel, EmailStr, field_validator

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
