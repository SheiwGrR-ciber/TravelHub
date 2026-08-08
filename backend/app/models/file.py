from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, Enum
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from app.db.database import Base

FILE_TYPES = ["service_photo", "provider_certificate", "profile_avatar"]

class File(Base):
    __tablename__ = "files"

    id = Column(Integer, primary_key=True, index=True)
    entity_type = Column(Enum(*FILE_TYPES, name="file_type"), nullable=False)
    entity_id = Column(Integer, nullable=False)  # service_id, user_id, etc.
    url = Column(String, nullable=False)  # Cloud storage URL (S3, Firebase, Azure Blob)
    filename = Column(String)
    content_type = Column(String)
    size_bytes = Column(Integer)
    uploaded_by = Column(Integer, ForeignKey("users.id"), nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    uploader = relationship("User")