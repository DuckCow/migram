// services/token_service.dart
import 'package:shared_preferences/shared_preferences.dart';

class TokenService {
  static const String ACCESS_TOKEN_KEY = 'access_token';
  static const String REFRESH_TOKEN_KEY = 'refresh_token';

  Future<void> saveTokens({
    required String accessToken,
    required String refreshToken,
  }) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(ACCESS_TOKEN_KEY, accessToken);
    await prefs.setString(REFRESH_TOKEN_KEY, refreshToken);
  }

  Future<String?> getAccessToken() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(ACCESS_TOKEN_KEY);
  }

  Future<String?> getRefreshToken() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(REFRESH_TOKEN_KEY);
  }

  Future<void> deleteTokens() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(ACCESS_TOKEN_KEY);
    await prefs.remove(REFRESH_TOKEN_KEY);
  }

  Future<void> clearTokens() async {
    await deleteTokens();
  }
}
