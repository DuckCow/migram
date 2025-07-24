import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'package:migram_app/screens/login_screen.dart';
import 'package:migram_app/services/dio_service.dart';
import 'package:migram_app/services/token_service.dart';
import 'package:webview_flutter/webview_flutter.dart';
import 'package:webview_flutter_android/webview_flutter_android.dart';
import 'package:webview_flutter_wkwebview/webview_flutter_wkwebview.dart';
import 'package:app_links/app_links.dart';
import 'dart:async';
// 다른 필요한 imports...

Future<void> main() async {
  print('=== App starting ===');
  WidgetsFlutterBinding.ensureInitialized();

  // 플랫폼별 WebView 설정
  late final WebViewController controller;

  print('About to initialize AppLinks...');
  // DeepLink 초기 설정
  await initAppLinks();
  print('AppLinks initialized');

  DioService().initialize();
  runApp(const MyApp());
}

// AppLinks 인스턴스와 구독을 전역으로 관리
late AppLinks _appLinks;
StreamSubscription<Uri>? _linkSubscription;

// DeepLink 처리를 위한 함수 (app_links로 변경)
Future<void> initAppLinks() async {
  _appLinks = AppLinks();

  // 앱이 시작될 때의 딥링크 확인
  try {
    print('Checking for initial link...');
    final initialUri = await _appLinks.getInitialLink();
    if (initialUri != null) {
      handleDeepLink(initialUri);
    }
  } catch (e) {
    print('Error getting initial URI: $e');
  }

  // 앱이 실행 중일 때의 딥링크 처리
  _linkSubscription = _appLinks.uriLinkStream.listen((Uri uri) {
    handleDeepLink(uri);
  }, onError: (error) {
    print('Error handling deeplink: $error');
  });
}

// DeepLink 처리 함수 (기존과 동일)
void handleDeepLink(Uri uri) async {
  print('=== handleDeepLink called ===');
  print('Full URI: $uri');
  print('URI path: ${uri.path}');
  print('URI query parameters: ${uri.queryParameters}');

  if (uri.path == '/callback') {
    // 이 부분 수정!
    print('OAuth callback path matched');

    // 에러 체크
    final error = uri.queryParameters['error'];
    if (error != null) {
      print('OAuth error received: $error');
      return;
    }

    // 토큰 체크
    final accessToken = uri.queryParameters['access_token'];
    final refreshToken = uri.queryParameters['refresh_token'];

    print('Access token received: ${accessToken != null}');
    print('Refresh token received: ${refreshToken != null}');

    if (accessToken != null && refreshToken != null) {
      print('Both tokens received, saving...');

      try {
        await TokenService().saveTokens(
          accessToken: accessToken,
          refreshToken: refreshToken,
        );

        print('Tokens saved successfully');

        // 저장 확인
        final savedAccessToken = await TokenService().getAccessToken();
        print('Token saved verification: ${savedAccessToken != null}');

        print('Navigating to LoginScreen for status check');
        navigatorKey.currentState?.pushAndRemoveUntil(
          MaterialPageRoute(builder: (context) => const LoginScreen()),
          (route) => false,
        );
      } catch (e) {
        print('Error saving tokens: $e');
      }
    } else {
      print('Tokens missing in callback');
      print('Access token: $accessToken');
      print('Refresh token: $refreshToken');
    }
  } else {
    print('Path does not match /oauth/callback: ${uri.path}');
  }
}

// 전역 네비게이터 키 (DeepLink로 돌아왔을 때 화면 전환을 위함)
final GlobalKey<NavigatorState> navigatorKey = GlobalKey<NavigatorState>();

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  void dispose() {
    // 앱 종료 시 구독 해제
    _linkSubscription?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Migram',
      navigatorKey: navigatorKey, // 이 줄 추가!
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: const LoginScreen(),
    );
  }
}
