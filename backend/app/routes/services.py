from fastapi import APIRouter, HTTPException, Depends
from sqlalchemy.orm import Session
from app.db.database import SessionLocal
from app.models.service import Service
from app.models.user import User
from app.schemas.service import ServiceCreate
from app.routes.auth import get_current_user

router = APIRouter(prefix="/services", tags=["services"])

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

@router.post("/")
def create_service(service: ServiceCreate, current_user = Depends(get_current_user)):
    db = next(get_db())
    
    if current_user["role"] != "prestador":
        raise HTTPException(status_code=403, detail="Solo prestadores pueden crear servicios")
    
    new_service = Service(
        provider_id=current_user["id"],
        type=service.type,
        name=service.name,
        description=service.description,
        price=service.price,
        location=service.location
    )
    db.add(new_service)
    db.commit()
    db.refresh(new_service)
    return new_service

@router.get("/")
def get_services():
    db = next(get_db())
    return db.query(Service).all()