class TranslationRequest {
  final String contentType;
  final int contentId;
  final String targetLang;
  final String? sourceText;  // Optional custom text (for preview)
  final String translationType;  // 'FULL' or 'PREVIEW'

  TranslationRequest({
    required this.contentType,
    required this.contentId,
    required this.targetLang,
    this.sourceText,
    this.translationType = 'FULL',
  });

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{
      'contentType': contentType,
      'contentId': contentId,
      'targetLang': targetLang,
      'translationType': translationType,
    };
    if (sourceText != null) {
      json['sourceText'] = sourceText;
    }
    return json;
  }
}

class TranslationResponse {
  final String contentType;
  final int contentId;
  final String sourceLang;
  final String targetLang;
  final String translatedText;
  final String model;
  final bool cached;
  final bool skipped;  // True if source == target language (no translation needed)

  TranslationResponse({
    required this.contentType,
    required this.contentId,
    required this.sourceLang,
    required this.targetLang,
    required this.translatedText,
    required this.model,
    required this.cached,
    this.skipped = false,
  });

  factory TranslationResponse.fromJson(Map<String, dynamic> json) {
    return TranslationResponse(
      contentType: json['contentType'],
      contentId: json['contentId'],
      sourceLang: json['sourceLang'],
      targetLang: json['targetLang'],
      translatedText: json['translatedText'],
      model: json['model'],
      cached: json['cached'],
      skipped: json['skipped'] ?? false,
    );
  }
}
