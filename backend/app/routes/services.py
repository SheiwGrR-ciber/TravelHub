from fastapi import APIRouter, HTTPException, Depends, Query
from app.db.database import get_db
from app.models.service import Service
from app.schemas.service import ServiceCreate, ServiceUpdate
from app.routes.auth import get_current_user

router = APIRouter(prefix="/services", tags=["services"])

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
def get_services(
    type: str = Query(None, description="Filtrar por tipo: guia, hotel"),
    location: str = Query(None, description="Filtrar por ubicación"),
    min_price: float = Query(None, description="Precio mínimo"),
    max_price: float = Query(None, description="Precio máximo"),
    min_rating: float = Query(None, description="Calificación mínima"),
    available: bool = Query(None, description="Disponible")
):
    db = next(get_db())
    query = db.query(Service)
    
    if type:
        query = query.filter(Service.type == type)
    if location:
        query = query.filter(Service.location.ilike(f"%{location}%"))
    if min_price is not None:
        query = query.filter(Service.price >= min_price)
    if max_price is not None:
        query = query.filter(Service.price <= max_price)
    if min_rating is not None:
        query = query.filter(Service.rating >= min_rating)
    if available is not None:
        query = query.filter(Service.available == available)
    
    return query.all()

@router.get("/{service_id}")
def get_service(service_id: int):
    db = next(get_db())
    service = db.query(Service).filter(Service.id == service_id).first()
    if not service:
        raise HTTPException(status_code=404, detail="Servicio no encontrado")
    return service

@router.put("/{service_id}")
def update_service(service_id: int, service_data: ServiceUpdate, current_user = Depends(get_current_user)):
    db = next(get_db())
    
    service = db.query(Service).filter(Service.id == service_id).first()
    if not service:
        raise HTTPException(status_code=404, detail="Servicio no encontrado")
    if service.provider_id != current_user["id"]:
        raise HTTPException(status_code=403, detail="Solo el prestador puede modificar su servicio")
    
    update_data = service_data.dict(exclude_unset=True)
    for key, value in update_data.items():
        setattr(service, key, value)
    
    db.commit()
    db.refresh(service)
    return service

@router.delete("/{service_id}")
def delete_service(service_id: int, current_user = Depends(get_current_user)):
    db = next(get_db())
    
    service = db.query(Service).filter(Service.id == service_id).first()
    if not service:
        raise HTTPException(status_code=404, detail="Servicio no encontrado")
    if service.provider_id != current_user["id"]:
        raise HTTPException(status_code=403, detail="Solo el prestador puede eliminar su servicio")
    
    db.delete(service)
    db.commit()
    return {"message": "Servicio eliminado"}