from fastapi import APIRouter, HTTPException, Depends
from sqlalchemy.orm import Session
from app.db.database import get_db
from app.models.user import User
from app.routes.auth import get_current_user

router = APIRouter(prefix="/admin", tags=["admin"])

def require_admin(current_user: dict = Depends(get_current_user)):
    if current_user.get("role") != "admin":
        raise HTTPException(status_code=403, detail="Se requiere rol de administrador")
    return current_user

@router.get("/prestadores-pendientes")
def get_pending_prestadores(admin=Depends(require_admin), db: Session = Depends(get_db)):
    pending = db.query(User).filter(User.role == "prestador", User.approved == False).all()
    return [{"id": u.id, "name": u.name, "email": u.email, "created_at": str(u.created_at)} for u in pending]

@router.post("/aprobar/{user_id}")
def approve_prestador(user_id: int, admin=Depends(require_admin), db: Session = Depends(get_db)):
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="Usuario no encontrado")
    if user.role != "prestador":
        raise HTTPException(status_code=400, detail="El usuario no es prestador")
    user.approved = True
    db.commit()
    return {"message": f"Prestador {user.name} aprobado"}

@router.post("/rechazar/{user_id}")
def reject_prestador(user_id: int, admin=Depends(require_admin), db: Session = Depends(get_db)):
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="Usuario no encontrado")
    if user.role != "prestador":
        raise HTTPException(status_code=400, detail="El usuario no es prestador")
    db.delete(user)
    db.commit()
    return {"message": f"Prestador {user.name} rechazado y eliminado"}
