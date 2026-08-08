from fastapi import APIRouter, HTTPException, Depends, Query
from sqlalchemy.orm import Session
from app.db.database import get_db
from app.models.itinerary import Itinerary
from app.models.booking import Booking
from app.models.service import Service
from app.schemas.itinerary import ItineraryCreate, ItineraryUpdate, DirectionsRequest
from app.routes.auth import get_current_user
from app.maps_utils import get_directions

router = APIRouter(prefix="/itineraries", tags=["itineraries"])

@router.post("")
def create_itinerary(itinerary: ItineraryCreate, db: Session = Depends(get_db), current_user = Depends(get_current_user)):
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
def get_itineraries(
    db: Session = Depends(get_db),
    current_user = Depends(get_current_user),
    skip: int = Query(0, ge=0),
    limit: int = Query(20, ge=1, le=100)
):
    return db.query(Itinerary).filter(Itinerary.tourist_id == current_user["id"]).order_by(Itinerary.day).offset(skip).limit(limit).all()

@router.get("/{itinerary_id}")
def get_itinerary(itinerary_id: int, db: Session = Depends(get_db), current_user = Depends(get_current_user)):
    itinerary = db.query(Itinerary).filter(Itinerary.id == itinerary_id).first()
    if not itinerary:
        raise HTTPException(status_code=404, detail="Itinerario no encontrado")
    if itinerary.tourist_id != current_user["id"]:
        raise HTTPException(status_code=403, detail="No autorizado")
    return itinerary

@router.put("/{itinerary_id}")
def update_itinerary(itinerary_id: int, data: ItineraryUpdate, db: Session = Depends(get_db), current_user = Depends(get_current_user)):
    itinerary = db.query(Itinerary).filter(Itinerary.id == itinerary_id).first()
    if not itinerary:
        raise HTTPException(status_code=404, detail="Itinerario no encontrado")
    if itinerary.tourist_id != current_user["id"]:
        raise HTTPException(status_code=403, detail="No autorizado")
    itinerary.route_data = data.route_data
    db.commit()
    return itinerary

@router.delete("/{itinerary_id}")
def delete_itinerary(itinerary_id: int, db: Session = Depends(get_db), current_user = Depends(get_current_user)):
    itinerary = db.query(Itinerary).filter(Itinerary.id == itinerary_id).first()
    if not itinerary:
        raise HTTPException(status_code=404, detail="Itinerario no encontrado")
    if itinerary.tourist_id != current_user["id"]:
        raise HTTPException(status_code=403, detail="No autorizado")
    db.delete(itinerary)
    db.commit()
    return {"message": "Itinerario eliminado"}

@router.post("/{itinerary_id}/add-booking/{booking_id}")
def add_booking_to_itinerary(itinerary_id: int, booking_id: int, db: Session = Depends(get_db), current_user = Depends(get_current_user)):
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
    route_data = dict(itinerary.route_data or {})
    booking_ids = list(route_data.get("booking_ids", []))
    if booking_id not in booking_ids:
        booking_ids.append(booking_id)
    route_data["booking_ids"] = booking_ids
    itinerary.route_data = route_data
    db.commit()
    return {"message": "Reserva agregada al itinerario"}

@router.post("/{itinerary_id}/remove-booking/{booking_id}")
def remove_booking_from_itinerary(itinerary_id: int, booking_id: int, db: Session = Depends(get_db), current_user = Depends(get_current_user)):
    itinerary = db.query(Itinerary).filter(Itinerary.id == itinerary_id).first()
    if not itinerary:
        raise HTTPException(status_code=404, detail="Itinerario no encontrado")
    if itinerary.tourist_id != current_user["id"]:
        raise HTTPException(status_code=403, detail="No autorizado")
    route_data = dict(itinerary.route_data or {})
    booking_ids = list(route_data.get("booking_ids", []))
    if booking_id in booking_ids:
        booking_ids.remove(booking_id)
        route_data["booking_ids"] = booking_ids
        itinerary.route_data = route_data
        db.commit()
    return {"message": "Reserva eliminada del itinerario"}

@router.get("/{itinerary_id}/bookings")
def get_itinerary_bookings(itinerary_id: int, db: Session = Depends(get_db), current_user = Depends(get_current_user)):
    itinerary = db.query(Itinerary).filter(Itinerary.id == itinerary_id).first()
    if not itinerary:
        raise HTTPException(status_code=404, detail="Itinerario no encontrado")
    if itinerary.tourist_id != current_user["id"]:
        raise HTTPException(status_code=403, detail="No autorizado")
    booking_ids = (itinerary.route_data or {}).get("booking_ids", [])
    if not booking_ids:
        return []
    bookings = db.query(Booking).filter(Booking.id.in_(booking_ids)).all()
    result = []
    for b in bookings:
        service = db.query(Service).filter(Service.id == b.service_id).first()
        result.append({
            "id": b.id,
            "service_id": b.service_id,
            "service_name": service.name if service else "Desconocido",
            "service_type": service.type if service else "",
            "service_location": service.location if service else "",
            "date": str(b.date),
            "status": b.status,
            "total": b.total
        })
    return result

@router.post("/{itinerary_id}/directions")
def calculate_directions(itinerary_id: int, req: DirectionsRequest, db: Session = Depends(get_db), current_user = Depends(get_current_user)):
    itinerary = db.query(Itinerary).filter(Itinerary.id == itinerary_id).first()
    if not itinerary:
        raise HTTPException(status_code=404, detail="Itinerario no encontrado")
    if itinerary.tourist_id != current_user["id"]:
        raise HTTPException(status_code=403, detail="No autorizado")
    result = get_directions(
        origin_lat=req.origin.lat,
        origin_lng=req.origin.lng,
        dest_lat=req.destination.lat,
        dest_lng=req.destination.lng,
        waypoints=[{"lat": w.lat, "lng": w.lng, "name": w.name} for w in req.waypoints]
    )
    return result
