from fastapi import FastAPI
from app.db.database import engine, Base
from app.models import User, Service, Booking, Itinerary, Message, Review
from app.routes import auth  # <-- Agrega esta línea

Base.metadata.create_all(bind=engine)

app = FastAPI(title="TravelHub API")
app.include_router(auth.router)  # <-- Agrega esta línea

@app.get("/")
def root():
    return {"message": "TravelHub API running"}