import os

SECRET_KEY = os.getenv("TRAVELHUB_SECRET_KEY")
if not SECRET_KEY:
    raise ValueError("TRAVELHUB_SECRET_KEY environment variable is required")

ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = int(os.getenv("TRAVELHUB_TOKEN_EXPIRE_MINUTES", "30"))

DATABASE_URL = os.getenv("TRAVELHUB_DATABASE_URL")
if not DATABASE_URL:
    raise ValueError("TRAVELHUB_DATABASE_URL environment variable is required")

GOOGLE_MAPS_API_KEY = os.getenv("GOOGLE_MAPS_API_KEY", "")
GOOGLE_CLIENT_ID = os.getenv("GOOGLE_CLIENT_ID", "")
