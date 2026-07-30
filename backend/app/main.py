from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import text
from app.db.database import engine, Base
from app.models import User, Service, Booking, Itinerary, Message, Review
from app.routes import auth, services, bookings, itineraries, messages, reviews, costs, ws, admin

Base.metadata.create_all(bind=engine)

with engine.connect() as conn:
    conn.execute(text("ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_code VARCHAR"))
    conn.execute(text("ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_code_expires TIMESTAMP"))
    conn.execute(text("ALTER TABLE users ADD COLUMN IF NOT EXISTS approved BOOLEAN DEFAULT FALSE"))
    conn.commit()

app = FastAPI(title="TravelHub API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth.router)
app.include_router(services.router)
app.include_router(bookings.router)
app.include_router(itineraries.router)
app.include_router(messages.router)
app.include_router(reviews.router)
app.include_router(costs.router)
app.include_router(admin.router)
app.mount("", ws.router)

@app.get("/")
def root():
    return {"message": "TravelHub API running"}