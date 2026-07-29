import os

SECRET_KEY = os.getenv("TRAVELHUB_SECRET_KEY", "mi_clave_secreta_super_segura")
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = int(os.getenv("TRAVELHUB_TOKEN_EXPIRE_MINUTES", "30"))
DATABASE_URL = os.getenv("TRAVELHUB_DATABASE_URL", "sqlite:///./travelhub.db")
