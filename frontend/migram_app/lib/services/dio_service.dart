import 'dart:io';
import 'package:dio/dio.dart';

class DioService {
  static final DioService _instance = DioService._internal();
  factory DioService() => _instance;

  final Dio _dio = Dio();
  bool _isInitialized = false;

  final String baseUrl =
      Platform.isAndroid ? 'http://10.0.2.2:8080' : 'http://localhost:8080';

  DioService._internal();

  void initialize() {
    if (_isInitialized) return;

    print('Initializing DioService with baseUrl: $baseUrl');
    _dio.options.baseUrl = baseUrl;
    _dio.options.connectTimeout = const Duration(milliseconds: 5000);
    _dio.options.receiveTimeout = const Duration(milliseconds: 5000);

    _dio.options.headers = {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    };

    _dio.interceptors.add(LogInterceptor(responseBody: true));
    _isInitialized = true;
  }

  Future<Response> get(String path, {Options? options}) async {
    try {
      final response = await _dio.get(
        path,
        options: options,
      );
      return response;
    } catch (e) {
      print('GET request error: $e');
      rethrow;
    }
  }

  Future<Response> post(String path, {dynamic data}) async {
    if (!_isInitialized) initialize();

    try {
      final response = await _dio.post(path, data: data);
      return response;
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<Response> put(String path, {dynamic data}) async {
    if (!_isInitialized) initialize();

    try {
      final response = await _dio.put(path, data: data);
      return response;
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Exception _handleError(DioException e) {
    switch (e.type) {
      case DioExceptionType.connectionTimeout:
        return Exception('연결 시간 초과');
      case DioExceptionType.receiveTimeout:
        return Exception('응답 시간 초과');
      case DioExceptionType.badResponse:
        return Exception('서버 에러: ${e.response?.statusCode}');
      default:
        return Exception('네트워크 에러 발생');
    }
  }

  String getLoginUrl() {
    if (!_isInitialized) initialize();

    final baseUri = Uri.parse(baseUrl);
    final loginUri = baseUri.resolve('/oauth2/authorization/google');
    return loginUri.toString();
  }
}
