from fastapi import FastAPI

from src.api.resume import router as resume_router


app = FastAPI(title="Resume Analysis Agent", version="1.0.0")
app.include_router(resume_router)


@app.get("/")
async def root():
    return {
        "message": "Resume Analysis Agent API",
        "docs": "/docs",
        "endpoints": {
            "parse": "/api/resume/parse",
            "analyze": "/api/resume/analyze",
        },
    }


@app.get("/health")
async def health():
    return {"status": "ok"}


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("src.main:app", host="0.0.0.0", port=8000, reload=True)
