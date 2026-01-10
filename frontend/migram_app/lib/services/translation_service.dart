import '../models/translation_model.dart';
import 'dio_service.dart';

class TranslationService {
  final DioService _dioService = DioService();

  /// Get translation for post or comment
  ///
  /// [contentType] - 'POST' or 'COMMENT'
  /// [contentId] - ID of the post or comment
  /// [targetLang] - Target language code (e.g., 'en', 'ko')
  /// [sourceText] - Optional custom text for translation (for preview)
  /// [translationType] - 'FULL' for complete content, 'PREVIEW' for truncated
  Future<TranslationResponse> getTranslation({
    required String contentType,
    required int contentId,
    required String targetLang,
    String? sourceText,
    String translationType = 'FULL',
  }) async {
    final request = TranslationRequest(
      contentType: contentType,
      contentId: contentId,
      targetLang: targetLang,
      sourceText: sourceText,
      translationType: translationType,
    );

    final response = await _dioService.post(
      '/translations',
      data: request.toJson(),
    );

    if (response.statusCode == 200) {
      return TranslationResponse.fromJson(response.data);
    } else {
      throw Exception('Failed to get translation: ${response.statusCode}');
    }
  }
}
