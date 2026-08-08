from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from app.db.database import engine, Base
from app.models import User, Service, Booking, Itinerary, Message, Review, File
from app.routes import auth, services, bookings, itineraries, messages, reviews, costs, ws, admin, files
import os

Base.metadata.create_all(bind=engine)

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
    return {"status": "ok"}
