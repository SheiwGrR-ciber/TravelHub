from fastapi import APIRouter, HTTPException, Depends
from sqlalchemy.orm import Session
from app.db.database import SessionLocal
from app.models.booking import Booking
from app.models.service import Service
from app.schemas.booking import BookingCreate
from app.routes.auth import get_current_user

router = APIRouter(prefix="/bookings", tags=["bookings"])

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

@router.post("/")
def create_booking(booking: BookingCreate, current_user = Depends(get_current_user)):
    db = next(get_db())
    
    if current_user["role"] != "turista":
        raise HTTPException(status_code=403, detail="Solo turistas pueden reservar")
    
    service = db.query(Service).filter(Service.id == booking.service_id).first()
    if not service:
        raise HTTPException(status_code=404, detail="Servicio no encontrado")
    
    new_booking = Booking(
        tourist_id=current_user["id"],
        service_id=booking.service_id,
        date=booking.date,
        total=service.price
    )
    db.add(new_booking)
    db.commit()
    db.refresh(new_booking)
    return new_booking

@router.get("/")
def get_bookings(current_user = Depends(get_current_user)):
    db = next(get_db())
    return db.query(Booking).filter(Booking.tourist_id == current_user["id"]).all()