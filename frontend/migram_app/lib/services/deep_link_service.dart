// lib/services/deep_link_service.dart
import 'package:uni_links/uni_links.dart';
import 'dart:async';

class DeepLinkService {
  static final DeepLinkService _instance = DeepLinkService._internal();
  factory DeepLinkService() => _instance;
  DeepLinkService._internal();

  StreamSubscription? _subscription;

  Future<void> initialize(Function(Uri) onLink) async {
    // 앱이 실행중일 때의 딥링크 처리
    _subscription = uriLinkStream.listen((Uri? uri) {
      if (uri != null) {
        onLink(uri);
      }
    }, onError: (err) {
      print('Deep link error: $err');
    });

    // 앱이 종료된 상태에서의 딥링크 처리
    try {
      final initialUri = await getInitialUri();
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
