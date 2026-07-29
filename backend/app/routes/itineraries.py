from fastapi import APIRouter, HTTPException, Depends
from sqlalchemy.orm import Session
from app.db.database import get_db
from app.models.itinerary import Itinerary
from app.models.booking import Booking
from app.schemas.itinerary import ItineraryCreate
from app.routes.auth import get_current_user

router = APIRouter(prefix="/itineraries", tags=["itineraries"])

@router.post("")
def create_itinerary(itinerary: ItineraryCreate, current_user = Depends(get_current_user)):
    db = next(get_db())
    
    if current_user["role"] != "turista":
        raise HTTPException(status_code=403, detail="Solo turistas pueden crear itinerarios")
    
    new_itinerary = Itinerary(
        tourist_id=current_user["id"],
        day=itinerary.day,
        route_data=itinerary.route_data
    )
    db.add(new_itinerary)
    db.commit()
    db.refresh(new_itinerary)
    return new_itinerary

@router.get("")
def get_itineraries(current_user = Depends(get_current_user)):
    db = next(get_db())
    return db.query(Itinerary).filter(Itinerary.tourist_id == current_user["id"]).all()

@router.get("/{itinerary_id}")
def get_itinerary(itinerary_id: int, current_user = Depends(get_current_user)):
    db = next(get_db())
    itinerary = db.query(Itinerary).filter(Itinerary.id == itinerary_id).first()
    if not itinerary:
        raise HTTPException(status_code=404, detail="Itinerario no encontrado")
    if itinerary.tourist_id != current_user["id"]:
        raise HTTPException(status_code=403, detail="No autorizado")
    return itinerary

@router.post("/{itinerary_id}/add-booking/{booking_id}")
def add_booking_to_itinerary(itinerary_id: int, booking_id: int, current_user = Depends(get_current_user)):
    db = next(get_db())
    
    itinerary = db.query(Itinerary).filter(Itinerary.id == itinerary_id).first()
    if not itinerary:
        raise HTTPException(status_code=404, detail="Itinerario no encontrado")
    if itinerary.tourist_id != current_user["id"]:
        raise HTTPException(status_code=403, detail="No autorizado")
    
    booking = db.query(Booking).filter(Booking.id == booking_id).first()
    if not booking:
        raise HTTPException(status_code=404, detail="Reserva no encontrada")
    if booking.tourist_id != current_user["id"]:
        raise HTTPException(status_code=403, detail="No autorizado")
    
    # Agregar la reserva al itinerario (guardamos IDs en route_data)
    if "booking_ids" not in itinerary.route_data:
        itinerary.route_data["booking_ids"] = []
    if booking_id not in itinerary.route_data["booking_ids"]:
        itinerary.route_data["booking_ids"].append(booking_id)
    
    db.commit()
    return {"message": "Reserva agregada al itinerario"}