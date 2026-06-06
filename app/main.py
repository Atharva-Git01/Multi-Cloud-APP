"""BBG Cloud App — FastAPI application entry point."""
from __future__ import annotations

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.core.config import settings
from app.core.database import init_db

# ---------------------------------------------------------------------------
# Logging
# ---------------------------------------------------------------------------

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s  %(levelname)-8s  %(name)s — %(message)s",
)
logger = logging.getLogger("bbg.main")


# ---------------------------------------------------------------------------
# Lifespan (startup / shutdown)
# ---------------------------------------------------------------------------

@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting BBG Cloud App…")
    await init_db()
    logger.info("Database tables ensured.")
    yield
    logger.info("BBG Cloud App shutting down.")


# ---------------------------------------------------------------------------
# App factory
# ---------------------------------------------------------------------------

app = FastAPI(
    title="BBG Cloud App",
    description=(
        "Multi-cloud intelligent file routing platform. "
        "Files are automatically routed to the best cloud provider "
        "(Google Drive, OneDrive, MEGA, Box, pCloud, Dropbox) "
        "based on file type and available quota."
    ),
    version="1.0.0",
    lifespan=lifespan,
    docs_url="/api/docs",
    redoc_url="/api/redoc",
    openapi_url="/api/openapi.json",
)

# ---------------------------------------------------------------------------
# CORS
# ---------------------------------------------------------------------------

origins_raw = settings.CORS_ORIGINS
cors_origins = [o.strip() for o in origins_raw.split(",")] if origins_raw != "*" else ["*"]

app.add_middleware(
    CORSMiddleware,
    allow_origins=cors_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ---------------------------------------------------------------------------
# Routers
# ---------------------------------------------------------------------------

from app.routes.auth import router as auth_router
from app.routes.files import router as files_router
from app.routes.onboarding import router as onboarding_router
from app.routes.providers import router as providers_router
from app.routes.routing import router as routing_router
from app.routes.storage import router as storage_router

app.include_router(auth_router, prefix="/api")
app.include_router(onboarding_router, prefix="/api")
app.include_router(files_router, prefix="/api")
app.include_router(storage_router, prefix="/api")
app.include_router(providers_router, prefix="/api")
app.include_router(routing_router, prefix="/api")

# ---------------------------------------------------------------------------
# Legacy routes kept for backward compatibility
# ---------------------------------------------------------------------------

try:
    from app.routes.google_drive_mobile import router as mobile_google_router
    from app.routes.google_drive_routes import router as google_router
    from app.routes.upload_routes import router as upload_router

    app.include_router(mobile_google_router, prefix="/api/legacy")
    app.include_router(upload_router, prefix="/api/legacy")
    app.include_router(google_router, prefix="/api/legacy")
    logger.info("Legacy routers mounted under /api/legacy")
except ImportError:
    logger.info("Legacy routers not found — skipping.")

# ---------------------------------------------------------------------------
# Global exception handlers
# ---------------------------------------------------------------------------

@app.exception_handler(Exception)
async def unhandled_exception_handler(request: Request, exc: Exception):
    logger.exception("Unhandled exception: %s", exc)
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={"detail": "An internal server error occurred."},
    )


# ---------------------------------------------------------------------------
# Health check
# ---------------------------------------------------------------------------

@app.get("/", tags=["health"])
async def root():
    return {"status": "ok", "message": "BBG Cloud App is running."}


@app.get("/api/health", tags=["health"])
async def health():
    return {"status": "ok", "version": "1.0.0"}
