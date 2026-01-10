import 'package:dio/dio.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:migram_app/config.dart';

class DioService {
  static final DioService _instance = DioService._internal();
  factory DioService() => _instance;

  final Dio _dio = Dio();
  bool _isInitialized = false;

  // Base URL configured in lib/config.dart
  final String baseUrl = AppConfig.baseUrl;

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

    // JWT 토큰 자동 추가 인터셉터
    _dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) async {
        // 토큰이 필요없는 요청들은 제외
        final publicPaths = ['/auth/token', '/api/countries'];
        if (!publicPaths.any((path) => options.path.contains(path))) {
          final prefs = await SharedPreferences.getInstance();
          final accessToken = prefs.getString('access_token');
          if (accessToken != null) {
            options.headers['Authorization'] = 'Bearer $accessToken';
            print('Added Authorization header to ${options.path}');
          }
        }
        return handler.next(options);
      },
      onError: (DioException e, handler) async {
        // 인증 실패 또는 토큰 만료 시 자동으로 토큰 삭제
        if (e.response?.statusCode == 401 ||
            e.response?.statusCode == 403 ||
            (e.response?.statusCode == 302 &&
             e.response?.headers.value('location')?.contains('oauth2') == true)) {
          print('Authentication failed, clearing tokens...');
          final prefs = await SharedPreferences.getInstance();
          await prefs.remove('access_token');
          await prefs.remove('refresh_token');
        }
        return handler.next(e);
      },
    ));

    _dio.interceptors.add(LogInterceptor(responseBody: true));
    _isInitialized = true;
  }

  Future<Response> get(String path, {Options? options, Map<String, dynamic>? queryParameters}) async {
    if (!_isInitialized) initialize();

    try {
      final response = await _dio.get(
        path,
        options: options,
        queryParameters: queryParameters,
      );
      return response;
    } catch (e) {
      print('GET request error: $e');
      rethrow;
    }
  }

  Future<Response> post(String path, {dynamic data, Map<String, dynamic>? queryParameters, Options? options}) async {
    if (!_isInitialized) initialize();

    try {
      final response = await _dio.post(
        path,
        data: data,
        queryParameters: queryParameters,
        options: options,
      );
      return response;
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<Response> put(String path, {dynamic data, Options? options}) async {
    if (!_isInitialized) initialize();

    try {
      final response = await _dio.put(
        path,
        data: data,
        options: options,
      );
      return response;
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<Response> delete(String path) async {
    if (!_isInitialized) initialize();

    try {
      final response = await _dio.delete(path);
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
