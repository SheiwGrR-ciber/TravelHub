from sqlalchemy import Column, Integer, String, Float, Boolean, DateTime, ForeignKey, Enum
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from app.db.database import Base

SERVICE_TYPES = ["guia", "hotel", "restaurante", "traductor", "transportista"]

class Service(Base):
    __tablename__ = "services"

    id = Column(Integer, primary_key=True, index=True)
    provider_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    type = Column(Enum(*SERVICE_TYPES, name="service_type"), nullable=False)
    name = Column(String, nullable=False)
    description = Column(String)
    price = Column(Float, nullable=False)
    location = Column(String)
    rating = Column(Float, default=0.0)
    available = Column(Boolean, default=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    provider = relationship("User", back_populates="services")