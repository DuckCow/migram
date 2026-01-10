import 'dart:io';

/// App configuration
///
/// To use ngrok for iOS physical device testing:
/// 1. Run: ngrok http 8080
/// 2. Change [useNgrok] to true
/// 3. Update [ngrokDomain] with your ngrok URL
/// 4. Add ngrok URL to Google OAuth redirect URIs
class AppConfig {
  AppConfig._();

  // ============================================================
  // CHANGE THIS for iOS physical device testing with ngrok
  // ============================================================
  static const bool useNgrok = true;  // Set to false for iOS Simulator
  static const String ngrokDomain = 'https://cletus-nonbending-inurbanely.ngrok-free.dev';  // ← Update this!
  // ============================================================

  // Local development URLs
  static const String _androidEmulatorUrl = 'http://10.0.2.2:8080';
  static const String _iosLocalUrl = 'http://192.168.219.118:8080';

  /// Returns the appropriate base URL for the current platform/config
  static String get baseUrl {
    if (useNgrok && Platform.isIOS) {
      return ngrokDomain;
    }
    return Platform.isAndroid ? _androidEmulatorUrl : _iosLocalUrl;
  }
}
