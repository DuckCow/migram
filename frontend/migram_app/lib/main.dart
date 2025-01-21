import 'package:flutter/material.dart';
import 'package:migram_app/screens/main_screen.dart';
import 'package:migram_app/services/dio_service.dart';
import 'screens/login_screen.dart'; // LoginScreen import

void main() {
  WidgetsFlutterBinding.ensureInitialized(); // Flutter 엔진 초기화
  DioService().initialize(); // DioService 초기화
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
        // 앱의 기본 테마 설정
      ),
      home: const MainScreen(), // 시작 화면을 LoginScreen으로 설정
    );
  }
}
