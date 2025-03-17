import 'package:flutter/material.dart';
import 'package:migram_app/screens/country_selection_screen.dart';
import 'package:migram_app/screens/main_screen.dart';
import 'package:migram_app/services/dio_service.dart';

class AuthService {
  final _dioService = DioService();

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
}

// 로그인 성공 후 네비게이션 처리는 동일
// context를 매개변수로 받도록 수정
void handleLoginSuccess(BuildContext context) async {
  final authService = AuthService();

  try {
    final isProfileComplete = await authService.checkProfileStatus();

    if (isProfileComplete) {
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(builder: (context) => const MainScreen()),
      );
    } else {
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(builder: (context) => const CountrySelectionScreen()),
      );
    }
  } catch (e) {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('프로필 확인 중 오류가 발생했습니다')),
    );
  }
}
