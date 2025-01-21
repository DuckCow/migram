import 'package:dio/dio.dart';

class DioService {
  final Dio _dio = Dio();
  final String baseUrl = 'http:localhost:8080';

  void initialize() {
    _dio.options.baseUrl = baseUrl;
    _dio.options.connectTimeout = const Duration(milliseconds: 5000);
    _dio.options.receiveTimeout = const Duration(milliseconds: 5000);

    // 기본 헤더 설정
    _dio.options.headers = {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    };

    // 인터셉터 설정 (선택사항)
    _dio.interceptors.add(LogInterceptor(responseBody: true));
  }

  Future<Response> get(String path,
      {Map<String, dynamic>? queryParameters}) async {
    try {
      final response = await _dio.get(path, queryParameters: queryParameters);
      return response;
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<Response> post(String path, {dynamic data}) async {
    try {
      final response = await _dio.post(path, data: data);
      return response;
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<Response> put(String path, {dynamic data}) async {
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
    return '${_dio.options.baseUrl}/oauth2/authorization/google';
  }
}
