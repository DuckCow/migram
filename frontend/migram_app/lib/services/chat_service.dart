import 'dart:convert';
import 'dart:async';
import 'dart:io';
import 'package:http/http.dart' as http;
import 'package:http_parser/http_parser.dart';
import '../models/chat_model.dart';
import 'token_service.dart';

class ChatService {
  static const String baseUrl = 'http://localhost:8080/api/chat';
  final TokenService _tokenService = TokenService();

  // SSE client for real-time messages
  http.Client? _sseClient;
  StreamController<Message>? _messageStreamController;
  StreamController<int>? _unreadCountStreamController;

  /// Get stream of new messages
  Stream<Message>? get messageStream => _messageStreamController?.stream;

  /// Get stream of total unread count updates
  Stream<int>? get unreadCountStream => _unreadCountStreamController?.stream;

  /// Connect to SSE stream for real-time chat messages
  Future<void> connectToChatStream() async {
    try {
      final accessToken = await _tokenService.getAccessToken();
      if (accessToken == null) {
        print('No access token, cannot connect to chat stream');
        return;
      }

      // Initialize stream controllers
      _messageStreamController = StreamController<Message>.broadcast();
      _unreadCountStreamController = StreamController<int>.broadcast();

      _sseClient = http.Client();

      final request = http.Request('GET', Uri.parse('$baseUrl/stream'));
      request.headers['Authorization'] = 'Bearer $accessToken';
      request.headers['Accept'] = 'text/event-stream';

      final response = await _sseClient!.send(request);

      response.stream.transform(utf8.decoder).transform(const LineSplitter()).listen(
        (line) {
          if (line.startsWith('data:')) {
            final data = line.substring(5).trim();
            if (data.isNotEmpty) {
              try {
                final json = jsonDecode(data);
                final message = Message.fromJson(json);
                _messageStreamController?.add(message);

                // Fetch updated unread count
                _fetchUnreadCount();
              } catch (e) {
                print('Error parsing message: $e');
              }
            }
          }
        },
        onError: (error) {
          print('Chat SSE error: $error');
          _reconnect();
        },
        onDone: () {
          print('Chat SSE connection closed');
          _reconnect();
        },
      );

      print('Connected to chat SSE stream');
    } catch (e) {
      print('Error connecting to chat stream: $e');
      _reconnect();
    }
  }

  /// Reconnect to SSE stream after delay
  Future<void> _reconnect() async {
    await Future.delayed(const Duration(seconds: 5));
    connectToChatStream();
  }

  /// Disconnect from SSE stream
  void disconnect() {
    _sseClient?.close();
    _sseClient = null;
    _messageStreamController?.close();
    _messageStreamController = null;
    _unreadCountStreamController?.close();
    _unreadCountStreamController = null;
  }

  /// Fetch total unread count and update stream
  Future<void> _fetchUnreadCount() async {
    try {
      final count = await getTotalUnreadCount();
      _unreadCountStreamController?.add(count);
    } catch (e) {
      print('Error fetching unread count: $e');
    }
  }

  /// Get chat rooms for current user
  Future<List<ChatRoom>> getChatRooms({int page = 0, int size = 20}) async {
    try {
      final accessToken = await _tokenService.getAccessToken();
      if (accessToken == null) return [];

      final response = await http.get(
        Uri.parse('$baseUrl/rooms?page=$page&size=$size'),
        headers: {
          'Authorization': 'Bearer $accessToken',
          'Content-Type': 'application/json',
        },
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        final List<dynamic> content = data['content'];
        return content.map((json) => ChatRoom.fromJson(json)).toList();
      }
      return [];
    } catch (e) {
      print('Error getting chat rooms: $e');
      return [];
    }
  }

  /// Create or get existing chat room with another user
  Future<ChatResult> createOrGetChatRoom(int userId) async {
    try {
      final accessToken = await _tokenService.getAccessToken();
      if (accessToken == null) {
        return ChatResult(success: false, message: 'Not authenticated');
      }

      final response = await http.post(
        Uri.parse('$baseUrl/rooms/$userId'),
        headers: {
          'Authorization': 'Bearer $accessToken',
          'Content-Type': 'application/json',
        },
      );

      final data = jsonDecode(response.body);

      if (response.statusCode == 200) {
        return ChatResult.fromJson(data);
      } else {
        return ChatResult.fromJson(data);
      }
    } catch (e) {
      return ChatResult(success: false, message: 'Failed to create chat room: $e');
    }
  }

  /// Get messages in a chat room
  Future<List<Message>> getMessages(int chatRoomId, {int page = 0, int size = 50}) async {
    try {
      final accessToken = await _tokenService.getAccessToken();
      if (accessToken == null) return [];

      final response = await http.get(
        Uri.parse('$baseUrl/rooms/$chatRoomId/messages?page=$page&size=$size'),
        headers: {
          'Authorization': 'Bearer $accessToken',
          'Content-Type': 'application/json',
        },
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        final List<dynamic> content = data['content'];
        return content.map((json) => Message.fromJson(json)).toList();
      }
      return [];
    } catch (e) {
      print('Error getting messages: $e');
      return [];
    }
  }

  /// Upload image for chat message
  Future<String?> uploadChatImage(File imageFile) async {
    try {
      final accessToken = await _tokenService.getAccessToken();
      if (accessToken == null) {
        print('No access token for image upload');
        return null;
      }

      final request = http.MultipartRequest(
        'POST',
        Uri.parse('$baseUrl/upload-image'),
      );

      request.headers['Authorization'] = 'Bearer $accessToken';

      // Add file to request
      final fileName = imageFile.path.split('/').last;
      final fileExtension = fileName.split('.').last.toLowerCase();

      // Determine content type
      MediaType contentType;
      if (fileExtension == 'jpg' || fileExtension == 'jpeg') {
        contentType = MediaType('image', 'jpeg');
      } else if (fileExtension == 'png') {
        contentType = MediaType('image', 'png');
      } else {
        contentType = MediaType('image', 'jpeg'); // Default
      }

      request.files.add(await http.MultipartFile.fromPath(
        'file',
        imageFile.path,
        contentType: contentType,
      ));

      final streamedResponse = await request.send();
      final response = await http.Response.fromStream(streamedResponse);

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data['success'] == true) {
          return data['imageUrl'];
        }
      }

      print('Failed to upload image: ${response.statusCode} ${response.body}');
      return null;
    } catch (e) {
      print('Error uploading image: $e');
      return null;
    }
  }

  /// Send a message in a chat room (with optional image)
  Future<MessageResult> sendMessage(
    int chatRoomId, {
    String? content,
    String? imageUrl,
  }) async {
    try {
      final accessToken = await _tokenService.getAccessToken();
      if (accessToken == null) {
        return MessageResult(success: false, message: 'Not authenticated');
      }

      final Map<String, dynamic> body = {};
      if (content != null && content.isNotEmpty) {
        body['content'] = content;
      }
      if (imageUrl != null && imageUrl.isNotEmpty) {
        body['imageUrl'] = imageUrl;
      }

      if (body.isEmpty) {
        return MessageResult(success: false, message: 'Message must have content or image');
      }

      final response = await http.post(
        Uri.parse('$baseUrl/rooms/$chatRoomId/messages'),
        headers: {
          'Authorization': 'Bearer $accessToken',
          'Content-Type': 'application/json',
        },
        body: jsonEncode(body),
      );

      final data = jsonDecode(response.body);

      if (response.statusCode == 200) {
        return MessageResult.fromJson(data);
      } else {
        return MessageResult.fromJson(data);
      }
    } catch (e) {
      return MessageResult(success: false, message: 'Failed to send message: $e');
    }
  }

  /// Mark all messages in a chat room as read
  Future<void> markMessagesAsRead(int chatRoomId) async {
    try {
      final accessToken = await _tokenService.getAccessToken();
      if (accessToken == null) return;

      await http.put(
        Uri.parse('$baseUrl/rooms/$chatRoomId/read'),
        headers: {
          'Authorization': 'Bearer $accessToken',
          'Content-Type': 'application/json',
        },
      );
    } catch (e) {
      print('Error marking messages as read: $e');
    }
  }

  /// Get unread message count for a specific chat room
  Future<int> getUnreadCount(int chatRoomId) async {
    try {
      final accessToken = await _tokenService.getAccessToken();
      if (accessToken == null) return 0;

      final response = await http.get(
        Uri.parse('$baseUrl/rooms/$chatRoomId/unread'),
        headers: {
          'Authorization': 'Bearer $accessToken',
          'Content-Type': 'application/json',
        },
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        return data['count'] ?? 0;
      }
      return 0;
    } catch (e) {
      print('Error getting unread count: $e');
      return 0;
    }
  }

  /// Get total unread message count across all chat rooms
  Future<int> getTotalUnreadCount() async {
    try {
      final accessToken = await _tokenService.getAccessToken();
      if (accessToken == null) return 0;

      final response = await http.get(
        Uri.parse('$baseUrl/unread'),
        headers: {
          'Authorization': 'Bearer $accessToken',
          'Content-Type': 'application/json',
        },
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        return (data['count'] ?? 0).toInt();
      }
      return 0;
    } catch (e) {
      print('Error getting total unread count: $e');
      return 0;
    }
  }
}

class ChatResult {
  final bool success;
  final String message;
  final ChatRoom? chatRoom;

  ChatResult({
    required this.success,
    required this.message,
    this.chatRoom,
  });

  factory ChatResult.fromJson(Map<String, dynamic> json) {
    return ChatResult(
      success: json['success'] ?? false,
      message: json['message'] ?? '',
      chatRoom: json['chatRoom'] != null ? ChatRoom.fromJson(json['chatRoom']) : null,
    );
  }
}

class MessageResult {
  final bool success;
  final String message;
  final Message? messageDTO;

  MessageResult({
    required this.success,
    required this.message,
    this.messageDTO,
  });

  factory MessageResult.fromJson(Map<String, dynamic> json) {
    return MessageResult(
      success: json['success'] ?? false,
      message: json['message'] ?? '',
      messageDTO: json['messageDTO'] != null ? Message.fromJson(json['messageDTO']) : null,
    );
  }
}
