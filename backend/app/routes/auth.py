import jwt
import os
import bcrypt
from datetime import datetime, timedelta, timezone
from fastapi import Depends
from fastapi.security import OAuth2PasswordBearer
from fastapi import APIRouter, HTTPException
from sqlalchemy.orm import Session
from pydantic import BaseModel

from app.db.database import get_db
from app.models.user import User
from app.schemas.token import Token, LoginRequest
from app.schemas.user import UserCreate, UserProfileUpdate
from app.config import SECRET_KEY, ALGORITHM, ACCESS_TOKEN_EXPIRE_MINUTES, GOOGLE_CLIENT_ID

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="auth/login")
router = APIRouter(prefix="/auth", tags=["auth"])

def profile_response(user: User) -> dict:
    return {
        "id": user.id, "name": user.name, "email": user.email, "role": user.role,
        "phone": user.phone, "location": user.location, "bio": user.bio,
        "business_name": user.business_name, "provider_type": user.provider_type,
        "experience_years": user.experience_years, "verified": user.verified,
        "approved": user.approved,
    }

class VerifyRequest(BaseModel):
    email: str
    code: str

class ResendRequest(BaseModel):
    email: str

class GoogleAuthRequest(BaseModel):
    id_token: str

def hash_password(password: str) -> str:
    return bcrypt.hashpw(password.encode("utf-8"), bcrypt.gensalt()).decode("utf-8")

def verify_password(password: str, hashed: str) -> bool:
    try:
        return bcrypt.checkpw(password.encode("utf-8"), hashed.encode("utf-8"))
    except (TypeError, ValueError, AttributeError):
        return False

def create_access_token(data: dict):
    to_encode = data.copy()
    expire = datetime.now(timezone.utc) + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)

@router.post("/register")
def register(user: UserCreate, db: Session = Depends(get_db)):
    existing = db.query(User).filter(User.email == user.email).first()
    if existing:
        raise HTTPException(status_code=400, detail="Email ya registrado")

    hashed = hash_password(user.password)
    new_user = User(
        name=user.name,
        email=user.email,
        password_hash=hashed,
        role=user.role,
        verified=True,
        approved=False if user.role == "prestador" else True,
        verification_code=None,
        verification_code_expires=None
    )
    db.add(new_user)
    db.commit()


    return {
        "message": "Usuario creado. Revisa tu correo para el código de verificación.",
        "id": new_user.id,
        "email": new_user.email
    }

@router.post("/verify")
def verify_email(body: VerifyRequest, db: Session = Depends(get_db)):
    raise HTTPException(status_code=410, detail="La verificacion por correo ya no es necesaria")
    db_user = db.query(User).filter(User.email == body.email).first()
    if not db_user:
        raise HTTPException(status_code=404, detail="Usuario no encontrado")

    if db_user.verified:
        raise HTTPException(status_code=400, detail="El email ya está verificado")

    if db_user.verification_code != body.code:
        raise HTTPException(status_code=400, detail="Código incorrecto")

    expires_at = db_user.verification_code_expires
    if expires_at and expires_at.tzinfo is None:
        expires_at = expires_at.replace(tzinfo=timezone.utc)
    if expires_at and expires_at < datetime.now(timezone.utc):
        raise HTTPException(status_code=400, detail="Código expirado. Solicita uno nuevo.")

    db_user.verified = True
    db_user.verification_code = None
    db_user.verification_code_expires = None
    db.commit()

    token = create_access_token({"sub": db_user.email, "role": db_user.role})
    return {"message": "Email verificado exitosamente", "access_token": token, "token_type": "bearer"}

@router.post("/resend-code")
def resend_verification_code(body: ResendRequest, db: Session = Depends(get_db)):
    raise HTTPException(status_code=410, detail="La verificacion por correo ya no es necesaria")
    db_user = db.query(User).filter(User.email == body.email).first()
    if not db_user:
        raise HTTPException(status_code=404, detail="Usuario no encontrado")

    if db_user.verified:
        raise HTTPException(status_code=400, detail="El email ya está verificado")

    code = generate_verification_code()
    db_user.verification_code = code
    db_user.verification_code_expires = datetime.now(timezone.utc) + timedelta(minutes=10)
    db.commit()

    send_verification_email(body.email, code)
    return {"message": "Código reenviado"}

@router.post("/login", response_model=Token)
def login(user: LoginRequest, db: Session = Depends(get_db)):
    db_user = db.query(User).filter(User.email == user.email).first()
    if not db_user:
        raise HTTPException(status_code=401, detail="Credenciales incorrectas")

    if not verify_password(user.password, db_user.password_hash):
        raise HTTPException(status_code=401, detail="Credenciales incorrectas")

    token = create_access_token({"sub": db_user.email, "role": db_user.role})
    return {"access_token": token, "token_type": "bearer"}

@router.post("/google")
def google_auth(body: GoogleAuthRequest, db: Session = Depends(get_db)):
    try:
        import requests
        resp = requests.get(
            "https://oauth2.googleapis.com/tokeninfo",
            params={"id_token": body.id_token},
            timeout=10,
        )
        if resp.status_code != 200:
            raise HTTPException(status_code=401, detail="Token de Google inválido")
        google_data = resp.json()
        if not GOOGLE_CLIENT_ID:
            raise HTTPException(status_code=503, detail="Google Sign-In no esta configurado")
        if google_data.get("aud") != GOOGLE_CLIENT_ID:
            raise HTTPException(status_code=401, detail="Token de Google no pertenece a esta aplicacion")
        email = google_data.get("email")
        name = google_data.get("name", email.split("@")[0])

        if not email:
            raise HTTPException(status_code=400, detail="Email no proporcionado por Google")

        db_user = db.query(User).filter(User.email == email).first()
        if not db_user:
            db_user = User(
                name=name,
                email=email,
                password_hash=hash_password(os.urandom(32).hex()),
                role="turista",
                verified=True
            )
            db.add(db_user)
            db.commit()
            db.refresh(db_user)

        token = create_access_token({"sub": db_user.email, "role": db_user.role})
        return {"access_token": token, "token_type": "bearer", "user": {"id": db_user.id, "email": db_user.email, "role": db_user.role}}
    except requests.RequestException:
        raise HTTPException(status_code=502, detail="Error al verificar token con Google")

def get_current_user(token: str = Depends(oauth2_scheme), db: Session = Depends(get_db)):
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        email = payload.get("sub")
        if email is None:
            raise HTTPException(status_code=401, detail="Token inválido")

        user = db.query(User).filter(User.email == email).first()
        if not user:
            raise HTTPException(status_code=401, detail="Usuario no encontrado")

        return profile_response(user)
    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=401, detail="Token expirado")
    except jwt.InvalidTokenError:
        raise HTTPException(status_code=401, detail="Token inválido")

@router.get("/me")
def get_me(current_user = Depends(get_current_user)):
    return current_user

@router.put("/me")
def update_me(profile: UserProfileUpdate, db: Session = Depends(get_db), current_user = Depends(get_current_user)):
    user = db.query(User).filter(User.id == current_user["id"]).first()
    if not user:
        raise HTTPException(status_code=404, detail="Usuario no encontrado")
    user.name = profile.name
    user.phone = profile.phone
    user.location = profile.location
    user.bio = profile.bio
    if user.role == "prestador":
        user.business_name = profile.business_name
        user.provider_type = profile.provider_type
        user.experience_years = profile.experience_years
    else:
        user.business_name = None
        user.provider_type = None
        user.experience_years = None
    db.commit()
    db.refresh(user)
    return profile_response(user)
