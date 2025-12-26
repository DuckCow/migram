class TranslationRequest {
  final String contentType;
  final int contentId;
  final String targetLang;

  TranslationRequest({
    required this.contentType,
    required this.contentId,
    required this.targetLang,
  });

  Map<String, dynamic> toJson() => {
        'contentType': contentType,
        'contentId': contentId,
        'targetLang': targetLang,
      };
}

class TranslationResponse {
  final String contentType;
  final int contentId;
  final String sourceLang;
  final String targetLang;
  final String translatedText;
  final String model;
  final bool cached;

  TranslationResponse({
    required this.contentType,
    required this.contentId,
    required this.sourceLang,
    required this.targetLang,
    required this.translatedText,
    required this.model,
    required this.cached,
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
    );
  }
}
