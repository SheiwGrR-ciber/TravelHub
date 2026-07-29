from pydantic import BaseModel
from datetime import datetime

class BookingCreate(BaseModel):
    service_id: int
    date: datetime

class BookingStatusUpdate(BaseModel):
    status: str  # confirmada, cancelada