// screens/login_screen.dart
import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart' as url_launcher;
import '../services/dio_service.dart';
import '../services/token_service.dart';
import '../services/user_service.dart';
import 'country_selection_screen.dart';
import 'main_screen.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _checkLoginStatus();
  }

  Future<void> _checkLoginStatus() async {
    try {
      final accessToken = await TokenService().getAccessToken();

      if (accessToken == null) {
        // 로그인되지 않은 경우
        setState(() {
          _isLoading = false;
        });
        print("is not login");
        return;
      }
      print("is login");

      // 로그인된 경우, 사용자 프로필 정보 확인
      final userService = UserService();
      final profileStatus = await userService.checkUserProfile();

      if (context.mounted) {
        if (profileStatus.isProfileComplete) {
          // 국가 설정이 완료된 경우
          Navigator.of(context).pushReplacement(
            MaterialPageRoute(builder: (context) => const MainScreen()),
          );
        } else {
          // 국가 설정이 필요한 경우
          Navigator.of(context).pushReplacement(
            MaterialPageRoute(
                builder: (context) => const CountrySelectionScreen()),
          );
        }
      }
    } catch (e) {
      print('Error checking login status: $e');
      setState(() {
        _isLoading = false;
      });
    }
  }

  Future<void> _handleGoogleSignIn() async {
    try {
      final String loginUrl = DioService().getLoginUrl();
      print('Login URL: $loginUrl');

      final Uri url = Uri.parse(loginUrl);
      if (await url_launcher.canLaunchUrl(url)) {
        await url_launcher.launchUrl(
          url,
          mode: url_launcher.LaunchMode.externalApplication,
        );
      }
    } catch (e) {
      print('Login error: $e');
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('로그인 중 오류가 발생했습니다')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return const Scaffold(
        body: Center(
          child: CircularProgressIndicator(),
        ),
      );
    }

    return Scaffold(
      body: SafeArea(
        child: Center(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 24.0),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                // Migram 로고나 텍스트
                const Text(
                  'Migram',
                  style: TextStyle(
                    fontSize: 40,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 50),

                // 구글 로그인 버튼
                InkWell(
                  onTap: _handleGoogleSignIn,
                  child: Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 16,
                      vertical: 12,
                    ),
                    decoration: BoxDecoration(
                      border: Border.all(color: Colors.grey),
                      borderRadius: BorderRadius.circular(4),
                    ),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Image.asset(
                          'assets/images/google_logo.png', // 구글 로고 이미지 필요
                          height: 24,
                        ),
                        const SizedBox(width: 12),
                        const Text(
                          'Google 계정으로 로그인',
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
