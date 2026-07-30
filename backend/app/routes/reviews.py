from fastapi import APIRouter, HTTPException, Depends
from sqlalchemy.orm import Session
from app.db.database import get_db
from app.models.review import Review
from app.models.booking import Booking
from app.models.service import Service
from app.schemas.review import ReviewCreate
from app.routes.auth import get_current_user
from sqlalchemy import func

router = APIRouter(prefix="/reviews", tags=["reviews"])

@router.post("/")
def create_review(review: ReviewCreate, db: Session = Depends(get_db), current_user = Depends(get_current_user)):

    if current_user["role"] != "turista":
        raise HTTPException(status_code=403, detail="Solo turistas pueden calificar")

    booking = db.query(Booking).filter(Booking.id == review.booking_id).first()
    if not booking:
        raise HTTPException(status_code=404, detail="Reserva no encontrada")
    if booking.tourist_id != current_user["id"]:
        raise HTTPException(status_code=403, detail="No autorizado")
    if booking.status != "confirmada":
        raise HTTPException(status_code=400, detail="Solo se puede calificar una reserva confirmada")

    existing = db.query(Review).filter(Review.booking_id == review.booking_id).first()
    if existing:
        raise HTTPException(status_code=400, detail="Ya calificaste esta reserva")

    if review.rating < 1 or review.rating > 5:
        raise HTTPException(status_code=400, detail="La calificación debe ser entre 1 y 5")

    new_review = Review(
        booking_id=review.booking_id,
        tourist_id=current_user["id"],
        service_id=booking.service_id,
        rating=review.rating,
        comment=review.comment
    )
    db.add(new_review)
    db.commit()
    db.refresh(new_review)

    avg_rating = db.query(func.avg(Review.rating)).filter(Review.service_id == booking.service_id).scalar()
    service = db.query(Service).filter(Service.id == booking.service_id).first()
    service.rating = round(avg_rating, 2)
    db.commit()

    return new_review

@router.get("/service/{service_id}")
def get_service_reviews(service_id: int, db: Session = Depends(get_db)):
    reviews = db.query(Review).filter(Review.service_id == service_id).order_by(Review.created_at.desc()).all()
    return reviews

@router.get("/my")
def get_my_reviews(db: Session = Depends(get_db), current_user = Depends(get_current_user)):
    return db.query(Review).filter(Review.tourist_id == current_user["id"]).all()
