import 'dart:async';

import 'package:flutter/material.dart';
import 'package:migram_app/screens/country_selection_screen.dart';
import 'package:migram_app/screens/main_screen.dart';
import 'package:migram_app/services/dio_service.dart';
import 'package:app_links/app_links.dart';

class AuthService {
  final _dioService = DioService();
  late AppLinks _appLinks;
  StreamSubscription<Uri>? _linkSubscription;

  // BuildContext를 저장하기 위한 변수 (주의: 메모리 누수 방지를 위해 사용 후 null로 설정)
  BuildContext? _currentContext;

  Future<bool> checkProfileStatus() async {
    try {
      print('Checking profile status...'); // 디버깅 로그
      final response = await _dioService.get('/api/user/check-profile');
      print('Profile response: ${response.data}'); // 응답 확인
      final data = response.data;
      return data['isProfileComplete'] ?? false;
    } catch (e) {
      print('Profile check error: $e'); // 에러 로그
      rethrow; // 에러를 상위로 전파
    }
  }

  Future<void> updateUserCountries(
      String originCountry, String currentCountry) async {
    try {
      await _dioService.post('/api/user/countries', data: {
        'originCountry': originCountry,
        'currentCountry': currentCountry,
      });
    } catch (e) {
      throw Exception('Failed to update user countries: ${e.toString()}');
    }
  }

  // Deep link 초기화
  void initDeepLinkListener(BuildContext context) {
    _currentContext = context;
    _appLinks = AppLinks(); // 여기가 수정된 부분!

    _linkSubscription = _appLinks.uriLinkStream.listen((Uri uri) {
      print('Received deep link: $uri');
      if (uri.scheme == 'migram' && uri.host == 'oauth') {
        _handleLoginSuccess();
      }
    });
  }

  // 앱이 종료된 상태에서 딥링크로 실행될 때 처리
  Future<void> checkInitialLink(BuildContext context) async {
    _currentContext = context;
    try {
      final Uri? initialLink = await _appLinks.getInitialLink();
      if (initialLink != null) {
        print('App launched with link: $initialLink');
        if (initialLink.scheme == 'migram' && initialLink.host == 'oauth') {
          _handleLoginSuccess();
        }
      }
    } catch (e) {
      print('Failed to get initial link: $e');
    }
  }

  // 내부 메서드로 변경 (BuildContext 사용)
  void _handleLoginSuccess() async {
    if (_currentContext == null) {
      print('Context is null, cannot navigate');
      return;
    }

    try {
      final isProfileComplete = await checkProfileStatus();

      if (_currentContext!.mounted) {
        // Context가 여전히 유효한지 확인
        if (isProfileComplete) {
          Navigator.pushReplacement(
            _currentContext!,
            MaterialPageRoute(builder: (context) => const MainScreen()),
          );
        } else {
          Navigator.pushReplacement(
            _currentContext!,
            MaterialPageRoute(
                builder: (context) => const CountrySelectionScreen()),
          );
        }
      }
    } catch (e) {
      if (_currentContext!.mounted) {
        ScaffoldMessenger.of(_currentContext!).showSnackBar(
          const SnackBar(content: Text('프로필 확인 중 오류가 발생했습니다')),
        );
      }
    }

    // Context 사용 후 null로 설정 (메모리 누수 방지)
    _currentContext = null;
  }

  // 리소스 정리
  void dispose() {
    _linkSubscription?.cancel();
    _currentContext = null;
  }
}
