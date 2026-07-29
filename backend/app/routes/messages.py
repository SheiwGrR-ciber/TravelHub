from fastapi import APIRouter, HTTPException, Depends
from app.db.database import get_db
from app.models.message import Message
from app.models.booking import Booking
from app.models.service import Service
from app.schemas.message import MessageCreate
from app.routes.auth import get_current_user

router = APIRouter(prefix="/messages", tags=["messages"])

@router.post("/")
def send_message(message: MessageCreate, current_user = Depends(get_current_user)):
    db = next(get_db())
    
    booking = db.query(Booking).filter(Booking.id == message.booking_id).first()
    if not booking:
        raise HTTPException(status_code=404, detail="Reserva no encontrada")
    
    service = db.query(Service).filter(Service.id == booking.service_id).first()
    if current_user["id"] != booking.tourist_id and current_user["id"] != service.provider_id:
        raise HTTPException(status_code=403, detail="No autorizado")
    
    new_message = Message(
        sender_id=current_user["id"],
        receiver_id=message.receiver_id,
        booking_id=message.booking_id,
        content=message.content
    )
    db.add(new_message)
    db.commit()
    db.refresh(new_message)
    return new_message

@router.get("/booking/{booking_id}")
def get_messages(booking_id: int, current_user = Depends(get_current_user)):
    db = next(get_db())
    
    booking = db.query(Booking).filter(Booking.id == booking_id).first()
    if not booking:
        raise HTTPException(status_code=404, detail="Reserva no encontrada")
    
    # Verificar que el usuario es parte de la conversación
    service = db.query(Service).filter(Service.id == booking.service_id).first()
    if current_user["id"] not in [booking.tourist_id, service.provider_id]:
        raise HTTPException(status_code=403, detail="No autorizado")
    
    messages = db.query(Message).filter(Message.booking_id == booking_id).order_by(Message.timestamp).all()
    return messages