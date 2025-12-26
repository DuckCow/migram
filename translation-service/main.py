from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import Optional
import uvicorn

app = FastAPI(title="Dummy Translation Service")

class TranslationRequest(BaseModel):
    text: str
    sourceLang: str
    targetLang: str
    quality: str = "best"

class TranslationResponse(BaseModel):
    translatedText: str
    model: str

@app.post("/translate", response_model=TranslationResponse)
async def translate(request: TranslationRequest):
    """
    Dummy translation endpoint
    Returns fake translation for testing the end-to-end flow
    """
    # Check if text contains title + content format (separated by \n\n)
    if '\n\n' in request.text:
        # Split into title and content
        parts = request.text.split('\n\n', 1)
        title = parts[0]
        content = parts[1] if len(parts) > 1 else ''

        # Translate both title and content
        translated_title = f"[{request.targetLang.upper()}] {title}"
        translated_content = f"[{request.targetLang.upper()}] {content}"

        # Recombine
        fake_translation = f"{translated_title}\n\n{translated_content}"
    else:
        # Simple text without title/content separation
        fake_translation = f"[{request.targetLang.upper()}] {request.text}"

    return TranslationResponse(
        translatedText=fake_translation,
        model="dummy-v1.0"
    )

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {"status": "healthy", "service": "dummy-translation-service"}

if __name__ == "__main__":
    print("🌐 Starting Dummy Translation Service on port 8001")
    print("📝 This is a dummy service for testing. Returns '[LANG] {text}' format.")
    print("   To replace with real model, update the /translate endpoint.")
    uvicorn.run(app, host="0.0.0.0", port=8001)
