from fastapi import APIRouter, HTTPException, Depends, UploadFile, File as FastAPIFile, Form
from sqlalchemy.orm import Session
from app.db.database import get_db
from app.models.file import File, FILE_TYPES
from app.models.user import User
from app.models.service import Service
from app.routes.auth import get_current_user
import os
import uuid
from datetime import datetime

router = APIRouter(prefix="/files", tags=["files"])

# Configuración de almacenamiento (ejemplo para S3/local)
STORAGE_PROVIDER = os.getenv("STORAGE_PROVIDER", "local")  # local, s3, firebase, azure
LOCAL_STORAGE_PATH = os.getenv("LOCAL_STORAGE_PATH", "./uploads")
MAX_FILE_SIZE = int(os.getenv("MAX_UPLOAD_BYTES", str(10 * 1024 * 1024)))
ALLOWED_CONTENT_TYPES = {"image/jpeg", "image/png", "image/webp", "application/pdf"}

@router.post("/upload")
async def upload_file(
    file: UploadFile = FastAPIFile(...),
    entity_type: str = Form(...),
    entity_id: int = Form(...),
    db: Session = Depends(get_db),
    current_user = Depends(get_current_user)
):
    if entity_type not in FILE_TYPES:
        raise HTTPException(status_code=400, detail=f"Tipo de entidad inválido. Permitidos: {FILE_TYPES}")

    # Validar que la entidad existe y el usuario tiene permiso
    if entity_type == "service_photo":
        service = db.query(Service).filter(Service.id == entity_id).first()
        if not service:
            raise HTTPException(status_code=404, detail="Servicio no encontrado")
        if service.provider_id != current_user["id"] and current_user["role"] != "admin":
            raise HTTPException(status_code=403, detail="No autorizado para subir fotos a este servicio")
    elif entity_type == "provider_certificate":
        if current_user["id"] != entity_id and current_user["role"] != "admin":
            raise HTTPException(status_code=403, detail="No autorizado")
    elif entity_type == "profile_avatar":
        if current_user["id"] != entity_id and current_user["role"] != "admin":
            raise HTTPException(status_code=403, detail="No autorizado")

    # Generar nombre único
    ext = os.path.splitext(file.filename)[1] if file.filename else ""
    unique_name = f"{entity_type}/{entity_id}/{uuid.uuid4().hex}{ext}"

    # Guardar archivo (implementación local de ejemplo)
    if STORAGE_PROVIDER == "local":
        os.makedirs(LOCAL_STORAGE_PATH, exist_ok=True)
        file_path = os.path.join(LOCAL_STORAGE_PATH, unique_name)
        os.makedirs(os.path.dirname(file_path), exist_ok=True)

        if file.content_type not in ALLOWED_CONTENT_TYPES:
            raise HTTPException(status_code=400, detail="Tipo de archivo no permitido")
        content = await file.read(MAX_FILE_SIZE + 1)
        if len(content) > MAX_FILE_SIZE:
            raise HTTPException(status_code=413, detail="El archivo supera el limite permitido")
        with open(file_path, "wb") as f:
            f.write(content)

        file_url = f"/static/{unique_name}"
        file_size = len(content)
    else:
        # TODO: Implementar S3, Firebase Storage, Azure Blob
        raise HTTPException(status_code=501, detail=f"Proveedor de almacenamiento {STORAGE_PROVIDER} no implementado")

    # Registrar en BD
    new_file = File(
        entity_type=entity_type,
        entity_id=entity_id,
        url=file_url,
        filename=file.filename,
        content_type=file.content_type,
        size_bytes=file_size,
        uploaded_by=current_user["id"]
    )
    db.add(new_file)
    db.commit()
    db.refresh(new_file)

    return {
        "id": new_file.id,
        "url": file_url,
        "filename": new_file.filename,
        "content_type": new_file.content_type,
        "size_bytes": new_file.size_bytes
    }

@router.get("/entity/{entity_type}/{entity_id}")
def get_entity_files(
    entity_type: str,
    entity_id: int,
    db: Session = Depends(get_db)
):
    if entity_type not in FILE_TYPES:
        raise HTTPException(status_code=400, detail=f"Tipo de entidad inválido")

    files = db.query(File).filter(
        File.entity_type == entity_type,
        File.entity_id == entity_id
    ).all()

    return files

@router.delete("/{file_id}")
def delete_file(
    file_id: int,
    db: Session = Depends(get_db),
    current_user = Depends(get_current_user)
):
    file_record = db.query(File).filter(File.id == file_id).first()
    if not file_record:
        raise HTTPException(status_code=404, detail="Archivo no encontrado")

    if file_record.uploaded_by != current_user["id"] and current_user["role"] != "admin":
        raise HTTPException(status_code=403, detail="No autorizado")

    # Eliminar archivo físico (local)
    if STORAGE_PROVIDER == "local":
        file_path = os.path.join(LOCAL_STORAGE_PATH, file_record.url.replace("/static/", ""))
        if os.path.exists(file_path):
            os.remove(file_path)

    db.delete(file_record)
    db.commit()

    return {"message": "Archivo eliminado"}
