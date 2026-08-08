from fastapi import APIRouter, HTTPException, Depends, Query
from app.db.database import get_db
from app.models.message import Message
from app.models.booking import Booking
from app.models.service import Service
from app.schemas.message import MessageCreate
from app.routes.auth import get_current_user
from sqlalchemy.orm import Session

router = APIRouter(prefix="/messages", tags=["messages"])

@router.post("/")
def send_message(message: MessageCreate, db: Session = Depends(get_db), current_user = Depends(get_current_user)):

    booking = db.query(Booking).filter(Booking.id == message.booking_id).first()
    if not booking:
        raise HTTPException(status_code=404, detail="Reserva no encontrada")

    service = db.query(Service).filter(Service.id == booking.service_id).first()
    if not service:
        raise HTTPException(status_code=404, detail="Servicio no encontrado")
    if current_user["id"] != booking.tourist_id and current_user["id"] != service.provider_id:
        raise HTTPException(status_code=403, detail="No autorizado")
    allowed_receivers = {booking.tourist_id, service.provider_id} - {current_user["id"]}
    if message.receiver_id not in allowed_receivers:
        raise HTTPException(status_code=400, detail="El receptor no pertenece a esta reserva")

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
def get_messages(
    booking_id: int,
    db: Session = Depends(get_db),
    current_user = Depends(get_current_user),
    skip: int = Query(0, ge=0),
    limit: int = Query(50, ge=1, le=200)
):

    booking = db.query(Booking).filter(Booking.id == booking_id).first()
    if not booking:
        raise HTTPException(status_code=404, detail="Reserva no encontrada")

    # Verificar que el usuario es parte de la conversación
    service = db.query(Service).filter(Service.id == booking.service_id).first()
    if not service:
        raise HTTPException(status_code=404, detail="Servicio no encontrado")
    if current_user["id"] not in [booking.tourist_id, service.provider_id]:
        raise HTTPException(status_code=403, detail="No autorizado")

    messages = db.query(Message).filter(Message.booking_id == booking_id).order_by(Message.timestamp.desc()).offset(skip).limit(limit).all()
    return list(reversed(messages))  # Devolver en orden cronológico
