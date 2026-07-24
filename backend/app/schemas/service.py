from pydantic import BaseModel

class ServiceCreate(BaseModel):
    type: str  # "guia" o "hotel"
    name: str
    description: str
    price: float
    location: str