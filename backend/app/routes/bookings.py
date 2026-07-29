from fastapi import APIRouter, HTTPException, Depends
from app.db.database import get_db
from app.models.booking import Booking
from app.models.service import Service
from app.schemas.booking import BookingCreate, BookingStatusUpdate
from app.routes.auth import get_current_user

router = APIRouter(prefix="/bookings", tags=["bookings"])

@router.post("")
def create_booking(booking: BookingCreate, current_user = Depends(get_current_user)):
    db = next(get_db())
    
    if current_user["role"] != "turista":
        raise HTTPException(status_code=403, detail="Solo turistas pueden reservar")
    
    service = db.query(Service).filter(Service.id == booking.service_id).first()
    if not service:
        raise HTTPException(status_code=404, detail="Servicio no encontrado")
    if not service.available:
        raise HTTPException(status_code=400, detail="El servicio no está disponible")
    
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

@router.get("")
def get_bookings(current_user = Depends(get_current_user)):
    db = next(get_db())
    if current_user["role"] == "turista":
        return db.query(Booking).filter(Booking.tourist_id == current_user["id"]).all()
    elif current_user["role"] == "prestador":
        services = db.query(Service).filter(Service.provider_id == current_user["id"]).all()
        service_ids = [s.id for s in services]
        return db.query(Booking).filter(Booking.service_id.in_(service_ids)).all()
    return []

@router.get("/{booking_id}")
def get_booking(booking_id: int, current_user = Depends(get_current_user)):
    db = next(get_db())
    booking = db.query(Booking).filter(Booking.id == booking_id).first()
    if not booking:
        raise HTTPException(status_code=404, detail="Reserva no encontrada")
    
    service = db.query(Service).filter(Service.id == booking.service_id).first()
    if current_user["id"] != booking.tourist_id and current_user["id"] != service.provider_id:
        raise HTTPException(status_code=403, detail="No autorizado")
    
    return booking

@router.put("/{booking_id}/status")
def update_booking_status(booking_id: int, status_data: BookingStatusUpdate, current_user = Depends(get_current_user)):
    db = next(get_db())
    
    booking = db.query(Booking).filter(Booking.id == booking_id).first()
    if not booking:
        raise HTTPException(status_code=404, detail="Reserva no encontrada")
    
    service = db.query(Service).filter(Service.id == booking.service_id).first()
    
    if current_user["role"] == "prestador" and current_user["id"] == service.provider_id:
        if status_data.status not in ["confirmada", "cancelada"]:
            raise HTTPException(status_code=400, detail="Estado inválido")
    elif current_user["role"] == "turista" and current_user["id"] == booking.tourist_id:
        if status_data.status != "cancelada":
            raise HTTPException(status_code=400, detail="El turista solo puede cancelar reservas")
    else:
        raise HTTPException(status_code=403, detail="No autorizado")
    
    booking.status = status_data.status
    db.commit()
    db.refresh(booking)
    return booking