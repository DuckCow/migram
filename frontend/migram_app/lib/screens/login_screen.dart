import 'package:flutter/material.dart';
import 'package:migram_app/screens/google_signin_webview.dart';
import 'package:migram_app/services/auth_service.dart';
import 'package:migram_app/services/dio_service.dart';

class LoginScreen extends StatelessWidget {
  const LoginScreen({super.key});

  Future<void> _handleGoogleSignIn(BuildContext context) async {
    try {
      final String loginUrl = DioService().getLoginUrl();

      final result = await Navigator.push<bool>(
        context,
        MaterialPageRoute(
          builder: (context) => GoogleSignInWebView(loginUrl: loginUrl),
        ),
      );

      if (result == true && context.mounted) {
        handleLoginSuccess(context);
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('로그인 중 오류가 발생했습니다')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
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
                  onTap: () => _handleGoogleSignIn(context),
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
