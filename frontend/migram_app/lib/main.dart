import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:migram_app/services/deep_link_service.dart';
import 'package:migram_app/services/dio_service.dart';
import 'package:webview_flutter/webview_flutter.dart';
import 'package:webview_flutter_android/webview_flutter_android.dart';
import 'package:webview_flutter_wkwebview/webview_flutter_wkwebview.dart';
import 'screens/login_screen.dart';
import 'package:migram_app/main.dart'
    show defaultTargetPlatform, TargetPlatform;

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // 플랫폼별 WebView 설정
  late final WebViewController controller;

  if (defaultTargetPlatform == TargetPlatform.android) {
    AndroidWebViewController.enableDebugging(true);
    final AndroidWebViewController controller = AndroidWebViewController(
      const PlatformWebViewControllerCreationParams(),
    );
  } else if (defaultTargetPlatform == TargetPlatform.iOS) {
    final WebKitWebViewController controller = WebKitWebViewController(
      WebKitWebViewControllerCreationParams(
        allowsInlineMediaPlayback: true,
        mediaTypesRequiringUserAction: const <PlaybackMediaTypes>{},
      ),
    );
  }

  //deepLink Service
  final deepLinkService = DeepLinkService();
  await deepLinkService.initialize((uri) {
    if (uri.path == '/oauth/callback') {
      final code = uri.queryParameters['code'];
      if (code != null) {
        // TODO: 인증 코드를 백엔드로 전송하여 토큰 교환
        print('Received auth code: $code');
      }
    }
  });

  DioService().initialize();
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Migram',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: const LoginScreen(),
    );
  }
}
