import 'dart:async';
import 'dart:convert';
import 'package:http/http.dart' as http;
import '../models/notification_model.dart';
import 'token_service.dart';

class NotificationService {
  static const String baseUrl = 'http://localhost:8080/api/notifications';
  final TokenService _tokenService = TokenService();

  StreamController<NotificationModel>? _notificationStreamController;
  StreamController<int>? _unreadCountController;
  http.Client? _sseClient;

  Stream<NotificationModel> get notificationStream {
    _notificationStreamController ??=
        StreamController<NotificationModel>.broadcast();
    return _notificationStreamController!.stream;
  }

  Stream<int> get unreadCountStream {
    _unreadCountController ??= StreamController<int>.broadcast();
    return _unreadCountController!.stream;
  }

  /// Connect to SSE stream for real-time notifications
  Future<void> connectToNotificationStream() async {
    final accessToken = await _tokenService.getAccessToken();
    if (accessToken == null) {
      print('No access token available');
      return;
    }

    try {
      _sseClient = http.Client();
      final request = http.Request('GET', Uri.parse('$baseUrl/stream'));
      request.headers['Authorization'] = 'Bearer $accessToken';
      request.headers['Accept'] = 'text/event-stream';
      request.headers['Cache-Control'] = 'no-cache';

      final response = await _sseClient!.send(request);

      if (response.statusCode == 200) {
        print('Connected to notification stream');

        response.stream
            .transform(utf8.decoder)
            .transform(const LineSplitter())
            .listen(
          (line) {
            if (line.startsWith('data:')) {
              final data = line.substring(5).trim();
              if (data == 'Connected to notification stream') {
                print('SSE connection confirmed');
                return;
              }

              try {
                final json = jsonDecode(data);
                final notification = NotificationModel.fromJson(json);
                _notificationStreamController?.add(notification);

                // Update unread count
                _fetchUnreadCount();
              } catch (e) {
                print('Error parsing notification: $e');
              }
            }
          },
          onError: (error) {
            print('SSE stream error: $error');
            _reconnect();
          },
          onDone: () {
            print('SSE stream closed');
            _reconnect();
          },
          cancelOnError: false,
        );
      } else {
        print('Failed to connect to SSE: ${response.statusCode}');
      }
    } catch (e) {
      print('Error connecting to SSE: $e');
      _reconnect();
    }
  }

  void _reconnect() {
    // Reconnect after 5 seconds
    Future.delayed(const Duration(seconds: 5), () {
      connectToNotificationStream();
    });
  }

  /// Get all notifications (paginated)
  Future<List<NotificationModel>> getNotifications({
    int page = 0,
    int size = 20,
  }) async {
    final accessToken = await _tokenService.getAccessToken();
    if (accessToken == null) {
      throw Exception('No access token');
    }

    final response = await http.get(
      Uri.parse('$baseUrl?page=$page&size=$size'),
      headers: {
        'Authorization': 'Bearer $accessToken',
        'Content-Type': 'application/json',
      },
    );

    if (response.statusCode == 200) {
      final jsonData = json.decode(response.body);
      final List content = jsonData['content'];
      return content.map((json) => NotificationModel.fromJson(json)).toList();
    } else {
      throw Exception('Failed to load notifications: ${response.statusCode}');
    }
  }

  /// Get unread notifications
  Future<List<NotificationModel>> getUnreadNotifications({
    int page = 0,
    int size = 20,
  }) async {
    final accessToken = await _tokenService.getAccessToken();
    if (accessToken == null) {
      throw Exception('No access token');
    }

    final response = await http.get(
      Uri.parse('$baseUrl/unread?page=$page&size=$size'),
      headers: {
        'Authorization': 'Bearer $accessToken',
        'Content-Type': 'application/json',
      },
    );

    if (response.statusCode == 200) {
      final jsonData = json.decode(response.body);
      final List content = jsonData['content'];
      return content.map((json) => NotificationModel.fromJson(json)).toList();
    } else {
      throw Exception(
          'Failed to load unread notifications: ${response.statusCode}');
    }
  }

  /// Get unread notification count
  Future<int> getUnreadCount() async {
    final accessToken = await _tokenService.getAccessToken();
    if (accessToken == null) {
      return 0;
    }

    try {
      final response = await http.get(
        Uri.parse('$baseUrl/unread/count'),
        headers: {
          'Authorization': 'Bearer $accessToken',
          'Content-Type': 'application/json',
        },
      );

      if (response.statusCode == 200) {
        final jsonData = json.decode(response.body);
        final count = jsonData['count'] as int;
        _unreadCountController?.add(count);
        return count;
      }
    } catch (e) {
      print('Error fetching unread count: $e');
    }
    return 0;
  }

  void _fetchUnreadCount() {
    getUnreadCount();
  }

  /// Mark notification as read
  Future<bool> markAsRead(int notificationId) async {
    final accessToken = await _tokenService.getAccessToken();
    if (accessToken == null) {
      throw Exception('No access token');
    }

    final response = await http.put(
      Uri.parse('$baseUrl/$notificationId/read'),
      headers: {
        'Authorization': 'Bearer $accessToken',
        'Content-Type': 'application/json',
      },
    );

    if (response.statusCode == 200) {
      _fetchUnreadCount();
      return true;
    }
    return false;
  }

  /// Mark all notifications as read
  Future<bool> markAllAsRead() async {
    final accessToken = await _tokenService.getAccessToken();
    if (accessToken == null) {
      throw Exception('No access token');
    }

    final response = await http.put(
      Uri.parse('$baseUrl/read-all'),
      headers: {
        'Authorization': 'Bearer $accessToken',
        'Content-Type': 'application/json',
      },
    );

    if (response.statusCode == 200) {
      _fetchUnreadCount();
      return true;
    }
    return false;
  }

  void dispose() {
    _sseClient?.close();
    _notificationStreamController?.close();
    _unreadCountController?.close();
  }
}
