from fastapi import FastAPI

app = FastAPI(title="Cloud File Sorter API")

# existing routers
from app.routes.google_drive_mobile import router as mobile_google_router

# new web google router
from app.routes.google_drive_routes import router as google_router
from app.routes.upload_routes import router as upload_router

app.include_router(mobile_google_router, prefix="/api")
app.include_router(upload_router, prefix="/api")
app.include_router(google_router, prefix="/api")


@app.get("/")
def root():
    return {"message": "🚀 Cloud File Sorter API is running!"}
