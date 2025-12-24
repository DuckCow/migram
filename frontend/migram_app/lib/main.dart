import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:migram_app/screens/login_screen.dart';
import 'package:migram_app/screens/main_screen.dart';
import 'package:migram_app/services/dio_service.dart';
import 'package:migram_app/services/token_service.dart';
import 'package:migram_app/services/notification_service.dart';
import 'package:migram_app/services/chat_service.dart';
import 'package:app_links/app_links.dart';
import 'package:timeago/timeago.dart' as timeago;
import 'dart:async';

Future<void> main() async {
  print('=== App starting ===');
  WidgetsFlutterBinding.ensureInitialized();

  // Initialize timeago with Korean locale
  timeago.setLocaleMessages('ko', timeago.KoMessages());

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
      print('Initial link found: $initialUri');
      handleDeepLink(initialUri);
    } else {
      print('No initial link found');
    }
  } catch (e) {
    print('Error getting initial URI: $e');
  }

  // 앱이 실행 중일 때의 딥링크 처리
  _linkSubscription = _appLinks.uriLinkStream.listen((Uri uri) {
    print('Deep link received while app running: $uri');
    handleDeepLink(uri);
  }, onError: (error) {
    print('Error handling deeplink: $error');
  });
}

void handleDeepLink(Uri uri) async {
  print('=== handleDeepLink called ===');
  print('Full URI: $uri');
  print('URI path: ${uri.path}');
  print('URI query parameters: ${uri.queryParameters}');

  // '/callback' 또는 '/oauth/callback' 둘 다 처리
  if (uri.path == '/callback' || uri.path == '/oauth/callback') {
    print('OAuth callback path matched');
    final code = uri.queryParameters['code'];

    print('Authorization code received: ${code != null}');

    if (code != null) {
      try {
        print('Authorization code received, exchanging for tokens...');

        // Authorization code를 토큰으로 교환
        final dioService = DioService();
        final response = await dioService.post(
          '/auth/token',
          data: {'code': code},
        );

        print('Token exchange response: ${response.statusCode}');

        if (response.statusCode == 200) {
          final data = response.data;
          final accessToken = data['accessToken'];
          final refreshToken = data['refreshToken'];

          if (accessToken != null && refreshToken != null) {
            print('Tokens received, saving...');

            // 토큰 저장
            await TokenService().saveTokens(
              accessToken: accessToken,
              refreshToken: refreshToken,
            );
            print('Tokens saved successfully');

            // 토큰 저장 확인
            final savedToken = await TokenService().getAccessToken();
            print('Token saved verification: ${savedToken != null}');

            // 네비게이터가 준비될 때까지 대기
            await Future.delayed(const Duration(milliseconds: 100));

            // Direct navigation to MainScreen (no profile check)
            print('Navigating to MainScreen');
            navigatorKey.currentState?.pushAndRemoveUntil(
              MaterialPageRoute(builder: (context) => const MainScreen()),
              (route) => false,
            );
          } else {
            throw Exception('Tokens not found in response');
          }
        } else {
          throw Exception('Token exchange failed: ${response.statusCode}');
        }
      } catch (e) {
        print('Error in handleDeepLink: $e');
        if (e is DioException) {
          print('Dio error response: ${e.response?.data}');
          print('Dio error statusCode: ${e.response?.statusCode}');
        }
        // 에러 발생시 LoginScreen으로
        navigatorKey.currentState?.pushAndRemoveUntil(
          MaterialPageRoute(builder: (context) => const LoginScreen()),
          (route) => false,
        );
      }
    } else {
      print('Authorization code missing, redirecting to LoginScreen');
      navigatorKey.currentState?.pushAndRemoveUntil(
        MaterialPageRoute(builder: (context) => const LoginScreen()),
        (route) => false,
      );
    }
  } else {
    print('Not an OAuth callback (path: ${uri.path}), ignoring');
  }
}

// 전역 네비게이터 키
final GlobalKey<NavigatorState> navigatorKey = GlobalKey<NavigatorState>();

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final NotificationService _notificationService = NotificationService();
  final ChatService _chatService = ChatService();

  @override
  void initState() {
    super.initState();
    _initializeServices();
  }

  Future<void> _initializeServices() async {
    // Check if user is logged in
    final accessToken = await TokenService().getAccessToken();
    if (accessToken != null) {
      // Connect to SSE streams for real-time notifications and chat
      await _notificationService.connectToNotificationStream();
      await _chatService.connectToChatStream();
      // Fetch initial unread counts
      await _notificationService.getUnreadCount();
      await _chatService.getTotalUnreadCount();
    }
  }

  @override
  void dispose() {
    // 앱 종료 시 구독 해제
    _linkSubscription?.cancel();
    _notificationService.dispose();
    _chatService.disconnect();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Migram',
      navigatorKey: navigatorKey,
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: const LoginScreen(),
    );
  }
}
