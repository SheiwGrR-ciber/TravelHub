from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from app.db.database import engine, Base, SessionLocal
from sqlalchemy import inspect, text
from sqlalchemy import or_
from app.models import User, Service, Booking, Itinerary, Message, Review, File
from app.routes import auth, services, bookings, itineraries, messages, reviews, costs, ws, admin, files
import os

Base.metadata.create_all(bind=engine)

def ensure_profile_columns():
    existing = {column["name"] for column in inspect(engine).get_columns("users")}
    columns = {
        "phone": "VARCHAR",
        "location": "VARCHAR",
        "bio": "VARCHAR",
        "business_name": "VARCHAR",
        "provider_type": "VARCHAR",
        "experience_years": "INTEGER",
    }
    with engine.begin() as connection:
        for name, sql_type in columns.items():
            if name not in existing:
                connection.execute(text(f"ALTER TABLE users ADD COLUMN {name} {sql_type}"))

ensure_profile_columns()

def remove_authorized_test_account():
    """One-deploy cleanup explicitly authorized by the project owner."""
    db = SessionLocal()
    try:
        user = db.query(User).filter(User.email == "turista@gmail.com").first()
        if not user:
            return
        booking_ids = [row.id for row in db.query(Booking).filter(Booking.tourist_id == user.id).all()]
        if booking_ids:
            db.query(Message).filter(Message.booking_id.in_(booking_ids)).delete(synchronize_session=False)
            db.query(Review).filter(Review.booking_id.in_(booking_ids)).delete(synchronize_session=False)
            db.query(Booking).filter(Booking.id.in_(booking_ids)).delete(synchronize_session=False)
        db.query(Message).filter(or_(Message.sender_id == user.id, Message.receiver_id == user.id)).delete(synchronize_session=False)
        db.query(Review).filter(Review.tourist_id == user.id).delete(synchronize_session=False)
        db.query(Itinerary).filter(Itinerary.tourist_id == user.id).delete(synchronize_session=False)
        db.query(File).filter(File.uploaded_by == user.id).delete(synchronize_session=False)
        db.delete(user)
        db.commit()
    except Exception:
        db.rollback()
        raise
    finally:
        db.close()

remove_authorized_test_account()

app = FastAPI(title="TravelHub API")

uploads_path = os.getenv("LOCAL_STORAGE_PATH", "./uploads")
os.makedirs(uploads_path, exist_ok=True)
app.mount("/static", StaticFiles(directory=uploads_path), name="static")

# CORS - configuración restrictiva para producción
allowed_origins = os.getenv("ALLOWED_ORIGINS", "").split(",")
if allowed_origins == [""]:
    allowed_origins = ["http://localhost:3000", "http://localhost:8081", "http://10.0.2.2:8000"]

app.add_middleware(
    CORSMiddleware,
    allow_origins=allowed_origins,
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type"],
)

app.include_router(auth.router)
app.include_router(services.router)
app.include_router(bookings.router)
app.include_router(itineraries.router)
app.include_router(messages.router)
app.include_router(reviews.router)
app.include_router(costs.router)
app.include_router(admin.router)
app.include_router(ws.router)
app.include_router(files.router)

@app.get("/")
def root():
    return {"message": "TravelHub API running"}

@app.get("/health")
def health():
    return {"status": "ok", "maintenance": "authorized-test-account-removed"}
