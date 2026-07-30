import datetime
from typing import Optional
from pydantic import BaseModel

class ReviewCreate(BaseModel):
    booking_id: int
    rating: int  # 1-5
    comment: str = ""

class ReviewResponse(BaseModel):
    id: int
    booking_id: int
    tourist_id: int
    service_id: int
    rating: int
    comment: str
    created_at: Optional[datetime.datetime] = None

    class Config:
        from_attributes = True
