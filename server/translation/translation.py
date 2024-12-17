from flask import Flask, request, jsonify
from transformers import MarianMTModel, MarianTokenizer
import torch

app = Flask(__name__)

# 모델과 토크나이저를 저장할 딕셔너리
models = {}
tokenizers = {}

def load_model(src_lang, tgt_lang):
   model_name = f'Helsinki-NLP/opus-mt-{src_lang}-{tgt_lang}'
   key = f"{src_lang}-{tgt_lang}"
   
   if key not in models:
       try:
           tokenizer = MarianTokenizer.from_pretrained(model_name)
           model = MarianMTModel.from_pretrained(model_name)
           
           if torch.cuda.is_available():
               model = model.to('cuda')
               
           models[key] = model
           tokenizers[key] = tokenizer
           
       except Exception as e:
           raise Exception(f"Failed to load model {model_name}: {str(e)}")
   
   return models[key], tokenizers[key]

@app.route('/translate', methods=['POST'])
def translate():
   try:
       data = request.json
       text = data.get('text')
       source_lang = data.get('sourceLang')
       target_lang = data.get('targetLang')
       
       if not all([text, source_lang, target_lang]):
           return jsonify({
               'error': 'Missing required parameters'
           }), 400
           
       model, tokenizer = load_model(source_lang, target_lang)
       
       # 번역 수행
       inputs = tokenizer(text, return_tensors="pt", padding=True)
       
       if torch.cuda.is_available():
           inputs = {k: v.to('cuda') for k, v in inputs.items()}
           
       translated = model.generate(**inputs)
       translated_text = tokenizer.batch_decode(translated, skip_special_tokens=True)[0]
       
       return jsonify({
           'translatedText': translated_text
       })
       
   except Exception as e:
       return jsonify({
           'error': str(e)
       }), 500

# 상태 체크 엔드포인트
@app.route('/health', methods=['GET'])
def health_check():
   return jsonify({
       'status': 'healthy',
       'loaded_models': list(models.keys())
   })

if __name__ == '__main__':
   # 개발 환경에서는 debug=True로 설정 가능
   app.run(host='localhost', port=5001, debug=False)