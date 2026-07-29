from pydantic import BaseModel
from typing import Optional

class ServiceCreate(BaseModel):
    type: str  # "guia" o "hotel"
    name: str
    description: str
    price: float
    location: str

class ServiceUpdate(BaseModel):
    type: Optional[str] = None
    name: Optional[str] = None
    description: Optional[str] = None
    price: Optional[float] = None
    location: Optional[str] = None
    available: Optional[bool] = None