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

    class Config:
        from_attributes = True
