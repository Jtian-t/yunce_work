from fastapi import FastAPI

from src.api.resume import router as resume_router

app = FastAPI(title="parse_pdf_langchain", version="1.0.0")
app.include_router(resume_router)


@app.get("/")
async def root():
    return {
        "service": "parse_pdf_langchain",
        "version": "1.0.0",
        "routes": {
            "parse": "/api/resume/parse",
            "parse_report": "/api/resume/parse-report",
            "decision_report": "/api/resume/decision-report",
        },
    }


@app.get("/health")
async def health():
    return {"status": "ok"}
