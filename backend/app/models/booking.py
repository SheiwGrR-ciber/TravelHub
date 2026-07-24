from sqlalchemy import Column, Integer, String, Float, DateTime, ForeignKey
from sqlalchemy.sql import func
from app.db.database import Base

class Booking(Base):
    __tablename__ = "bookings"

    id = Column(Integer, primary_key=True, index=True)
    tourist_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    service_id = Column(Integer, ForeignKey("services.id"), nullable=False)
    date = Column(DateTime, nullable=False)
    status = Column(String, default="pendiente")  # pendiente, confirmada, cancelada
    total = Column(Float, nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())