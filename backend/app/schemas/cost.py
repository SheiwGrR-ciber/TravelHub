from pydantic import BaseModel
from typing import List

class CostCalculateRequest(BaseModel):
    booking_ids: List[int]
