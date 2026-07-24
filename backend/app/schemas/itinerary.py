from pydantic import BaseModel
from typing import Optional, List, Dict, Any

class ItineraryCreate(BaseModel):
    day: int
    route_data: Dict[str, Any]  # Guarda puntos del mapa