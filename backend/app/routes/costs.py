from fastapi import APIRouter, HTTPException, Depends
from sqlalchemy.orm import Session
from app.db.database import get_db
from app.models.booking import Booking
from app.models.service import Service
from app.routes.auth import get_current_user
from typing import List

router = APIRouter(prefix="/costs", tags=["costs"])

@router.post("/calculate")
def calculate_costs(booking_ids: List[int], current_user = Depends(get_current_user)):
    db = next(get_db())
    
    total = 0.0
    breakdown = {"guia": 0.0, "hotel": 0.0}
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
