from fastapi import APIRouter, HTTPException, Depends
from sqlalchemy.orm import Session
from app.db.database import get_db
from app.models.booking import Booking
from app.models.service import Service, SERVICE_TYPES
from app.routes.auth import get_current_user
from app.schemas.cost import CostCalculateRequest

router = APIRouter(prefix="/costs", tags=["costs"])

@router.post("/calculate")
def calculate_costs(body: CostCalculateRequest, db: Session = Depends(get_db), current_user = Depends(get_current_user)):
    booking_ids = body.booking_ids
    
    total = 0.0
    breakdown = {t: 0.0 for t in SERVICE_TYPES}
    details = []

    for booking_id in booking_ids:
        booking = db.query(Booking).filter(Booking.id == booking_id).first()
        if not booking:
            continue
        if booking.tourist_id != current_user["id"]:
            continue

        service = db.query(Service).filter(Service.id == booking.service_id).first()
        if not service:
            continue

        category = service.type
        if category not in breakdown:
            breakdown[category] = 0.0

        breakdown[category] += booking.total
        total += booking.total
        details.append({
            "booking_id": booking.id,
            "service_name": service.name,
            "category": category,
            "price": booking.total
        })

    return {
        "total": round(total, 2),
        "breakdown": {k: round(v, 2) for k, v in breakdown.items() if v > 0},
        "details": details
    }
