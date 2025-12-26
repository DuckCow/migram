import '../models/translation_model.dart';
import 'dio_service.dart';

class TranslationService {
  final DioService _dioService = DioService();

  /// Get translation for post or comment
  Future<TranslationResponse> getTranslation({
    required String contentType,
    required int contentId,
    required String targetLang,
  }) async {
    final request = TranslationRequest(
      contentType: contentType,
      contentId: contentId,
      targetLang: targetLang,
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
