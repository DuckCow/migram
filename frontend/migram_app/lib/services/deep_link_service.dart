// lib/services/deep_link_service.dart
import 'package:app_links/app_links.dart';
import 'dart:async';

class DeepLinkService {
  static final DeepLinkService _instance = DeepLinkService._internal();
  factory DeepLinkService() => _instance;
  DeepLinkService._internal();

  StreamSubscription<Uri>? _subscription;
  late AppLinks _appLinks;

  Future<void> initialize(Function(Uri) onLink) async {
    _appLinks = AppLinks();

    // 앱이 실행중일 때의 딥링크 처리
    _subscription = _appLinks.uriLinkStream.listen((Uri uri) {
      onLink(uri);
    }, onError: (err) {
      print('Deep link error: $err');
    });

    // 앱이 종료된 상태에서의 딥링크 처리
    try {
      final initialUri = await _appLinks.getInitialLink();
      if (initialUri != null) {
        onLink(initialUri);
      }
    } catch (e) {
      print('Initial deep link error: $e');
    }
  }

  void dispose() {
    _subscription?.cancel();
  }
}
