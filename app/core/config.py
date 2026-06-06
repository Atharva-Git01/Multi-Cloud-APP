from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    # Database
    DATABASE_URL: str = "postgresql+asyncpg://bbg:bbg@localhost/bbgcloud"
    REDIS_URL: str = "redis://localhost:6379"

    # Security
    SECRET_KEY: str = "change-me-in-prod"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 30
    REFRESH_TOKEN_EXPIRE_DAYS: int = 30
    ENCRYPTION_KEY: str = "base64-32-byte-key-change-in-prod"

    # Google Drive / Google OAuth
    GOOGLE_CLIENT_ID: str = ""
    GOOGLE_CLIENT_SECRET: str = ""
    GOOGLE_REDIRECT_URI: str = ""

    # OneDrive / Microsoft
    ONEDRIVE_CLIENT_ID: str = ""
    ONEDRIVE_CLIENT_SECRET: str = ""
    ONEDRIVE_REDIRECT_URI: str = ""

    # Box
    BOX_CLIENT_ID: str = ""
    BOX_CLIENT_SECRET: str = ""

    # pCloud
    PCLOUD_CLIENT_ID: str = ""
    PCLOUD_CLIENT_SECRET: str = ""

    # Dropbox
    DROPBOX_APP_KEY: str = ""
    DROPBOX_APP_SECRET: str = ""

    # CORS
    CORS_ORIGINS: str = "*"

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")


settings = Settings()
