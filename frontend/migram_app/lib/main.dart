import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:migram_app/screens/country_selection_screen.dart';
import 'package:migram_app/screens/main_screen.dart';
import 'package:migram_app/services/deep_link_service.dart';
import 'package:migram_app/services/dio_service.dart';
import 'package:migram_app/services/token_service.dart';
import 'package:migram_app/services/user_service.dart';
import 'package:uni_links/uni_links.dart';
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

  // DeepLink 초기 설정
  initUniLinks();

  DioService().initialize();
  runApp(const MyApp());
}

// DeepLink 처리를 위한 함수
Future<void> initUniLinks() async {
  // 앱이 시작될 때의 딥링크 확인
  try {
    final initialUri = await getInitialUri();
    if (initialUri != null) {
      handleDeepLink(initialUri);
    }
  } catch (e) {
    print('Error getting initial URI: $e');
  }

  // 앱이 실행 중일 때의 딥링크 처리
  uriLinkStream.listen((Uri? uri) {
    if (uri != null) {
      handleDeepLink(uri);
    }
  }, onError: (error) {
    print('Error handling deeplink: $error');
  });
}

// DeepLink 처리 함수
// 딥링크 처리 함수를 다음과 같이 수정
void handleDeepLink(Uri uri) async {
  print('Handling deeplink: $uri');
  if (uri.path == '/oauth/callback') {
    final code = uri.queryParameters['code'];
    print('OAuth code: $code');

    if (code != null) {
      try {
        print('Attempting to exchange code for token');
        // 백엔드 URL 로그 출력
        final url = '${DioService().baseUrl}/auth/token';
        print('Token exchange URL: $url');

        final response = await DioService().post('/auth/token', data: {
          'code': code,
        });

        print('Token response: ${response.data}');

        // 토큰 저장
        await TokenService().saveTokens(
          accessToken: response.data['accessToken'],
          refreshToken: response.data['refreshToken'],
        );

        // 저장된 토큰 확인
        final savedToken = await TokenService().getAccessToken();
        print('Saved token: $savedToken');

        // 앱이 이미 시작된 상태라면 LoginScreen으로 돌아가게 하여
        // 사용자 프로필 체크 후 적절한 화면으로 이동하도록 함
        print('Navigating back to LoginScreen');
        navigatorKey.currentState?.pushReplacement(
          MaterialPageRoute(builder: (context) => const LoginScreen()),
        );
      } catch (e) {
        print('Token exchange error: $e');
        // 에러 상세 정보도 출력
        if (e is DioException) {
          print('Dio error response: ${e.response?.data}');
          print('Dio error statusCode: ${e.response?.statusCode}');
        }
      }
    }
  }
}

// 전역 네비게이터 키 (DeepLink로 돌아왔을 때 화면 전환을 위함)
final GlobalKey<NavigatorState> navigatorKey = GlobalKey<NavigatorState>();

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
