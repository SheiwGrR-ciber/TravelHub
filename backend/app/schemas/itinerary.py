from pydantic import BaseModel
from typing import Optional, List, Dict, Any

class ItineraryCreate(BaseModel):
    day: int
    route_data: Dict[str, Any]

class ItineraryUpdate(BaseModel):
    route_data: Dict[str, Any]

class RoutePoint(BaseModel):
    lat: float
    lng: float
    name: str = ""
    order: int = 0

class DirectionsRequest(BaseModel):
    origin: RoutePoint
    destination: RoutePoint
    waypoints: List[RoutePoint] = []

class DirectionsResponse(BaseModel):
    total_distance_m: float
    total_distance_km: float
    total_duration_s: float
    total_duration_min: float
    total_duration_text: str
    legs: List[Dict[str, Any]]
    polyline: str
    source: str