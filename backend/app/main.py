from fastapi import FastAPI
from app.db.database import engine, Base
from app.models import User, Service, Booking, Itinerary, Message, Review
from app.routes import auth, services
from app.routes import auth, services, bookings  # Agrega bookings


Base.metadata.create_all(bind=engine)

app = FastAPI(title="TravelHub API")

app.include_router(auth.router)
app.include_router(services.router)
app.include_router(bookings.router)  # Agrega esta línea

@app.get("/")
def root():
    return {"message": "TravelHub API running"}