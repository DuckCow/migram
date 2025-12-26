# Dummy Translation Service

Simple FastAPI service for testing the translation feature end-to-end.

## Setup

```bash
# Create virtual environment
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt
```

## Run

```bash
python main.py
```

Service will start on `http://localhost:8001`

## Endpoints

### POST /translate
Translate text (dummy implementation)

**Request:**
```json
{
  "text": "Hello world",
  "sourceLang": "en",
  "targetLang": "ko",
  "quality": "best"
}
```

**Response:**
```json
{
  "translatedText": "[KO] Hello world",
  "model": "dummy-v1.0"
}
```

### GET /health
Health check

**Response:**
```json
{
  "status": "healthy",
  "service": "dummy-translation-service"
}
```

## Upgrade to Real Model (Future)

To replace with NLLB or other translation model:

1. Install model dependencies (transformers, torch, etc.)
2. Update `/translate` endpoint to call actual model
3. Return real translation instead of dummy `[LANG] {text}` format

