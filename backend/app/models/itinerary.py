from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, JSON
from sqlalchemy.sql import func
from app.db.database import Base

class Itinerary(Base):
    __tablename__ = "itineraries"

    id = Column(Integer, primary_key=True, index=True)
    tourist_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    day = Column(Integer, nullable=False)
    route_data = Column(JSON)  # Guarda puntos del mapa
    created_at = Column(DateTime(timezone=True), server_default=func.now())