"""
Dummy Translation Service for MVP testing.
Returns fake translation: [targetLang] original_text
Replace with real NLLB model later.
"""
from flask import Flask, request, jsonify

app = Flask(__name__)

@app.route('/translate', methods=['POST'])
def translate():
    try:
        data = request.json
        text = data.get('text', '')
        source_lang = data.get('sourceLang', 'en')
        target_lang = data.get('targetLang', 'en')

        if not text:
            return jsonify({'error': 'Missing text parameter'}), 400

        # Dummy translation: prefix BOTH title and body with target language
        # Backend sends "Title\n\nBody" format
        parts = text.split('\n\n', 1)  # Split into max 2 parts
        if len(parts) == 2:
            title, body = parts
            translated_text = f"[{target_lang.upper()}] {title}\n\n[{target_lang.upper()}] {body}"
        else:
            translated_text = f"[{target_lang.upper()}] {text}"

        return jsonify({
            'translatedText': translated_text,
            'model': 'dummy'
        })

    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/health', methods=['GET'])
def health_check():
    return jsonify({'status': 'healthy', 'model': 'dummy'})

if __name__ == '__main__':
    print("Starting dummy translation service on port 5001...")
    app.run(host='localhost', port=5001, debug=False)
