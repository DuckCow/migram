import 'dart:convert';
import 'package:http/http.dart' as http;
import 'token_service.dart';

class BlockService {
  static const String baseUrl = 'http://localhost:8080/api/blocks';
  final TokenService _tokenService = TokenService();

  /// Block a user
  Future<BlockResult> blockUser(int userId) async {
    try {
      final accessToken = await _tokenService.getAccessToken();
      if (accessToken == null) {
        return BlockResult(success: false, message: 'Not authenticated');
      }

      final response = await http.post(
        Uri.parse('$baseUrl/$userId'),
        headers: {
          'Authorization': 'Bearer $accessToken',
          'Content-Type': 'application/json',
        },
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        return BlockResult.fromJson(data);
      } else {
        final data = jsonDecode(response.body);
        return BlockResult.fromJson(data);
      }
    } catch (e) {
      return BlockResult(success: false, message: 'Failed to block user: $e');
    }
  }

  /// Unblock a user
  Future<BlockResult> unblockUser(int userId) async {
    try {
      final accessToken = await _tokenService.getAccessToken();
      if (accessToken == null) {
        return BlockResult(success: false, message: 'Not authenticated');
      }

      final response = await http.delete(
        Uri.parse('$baseUrl/$userId'),
        headers: {
          'Authorization': 'Bearer $accessToken',
          'Content-Type': 'application/json',
        },
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        return BlockResult.fromJson(data);
      } else {
        final data = jsonDecode(response.body);
        return BlockResult.fromJson(data);
      }
    } catch (e) {
      return BlockResult(success: false, message: 'Failed to unblock user: $e');
    }
  }

  /// Get block status with a user
  Future<BlockStatus?> getBlockStatus(int userId) async {
    try {
      final accessToken = await _tokenService.getAccessToken();
      if (accessToken == null) return null;

      final response = await http.get(
        Uri.parse('$baseUrl/$userId/status'),
        headers: {
          'Authorization': 'Bearer $accessToken',
          'Content-Type': 'application/json',
        },
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        return BlockStatus.fromJson(data);
      }
      return null;
    } catch (e) {
      return null;
    }
  }

  /// Get list of blocked user IDs
  Future<List<int>> getBlockedUsers() async {
    try {
      final accessToken = await _tokenService.getAccessToken();
      if (accessToken == null) return [];

      final response = await http.get(
        Uri.parse(baseUrl),
        headers: {
          'Authorization': 'Bearer $accessToken',
          'Content-Type': 'application/json',
        },
      );

      if (response.statusCode == 200) {
        final List<dynamic> data = jsonDecode(response.body);
        return data.cast<int>();
      }
      return [];
    } catch (e) {
      return [];
    }
  }
}

class BlockResult {
  final bool success;
  final String message;

  BlockResult({
    required this.success,
    required this.message,
  });

  factory BlockResult.fromJson(Map<String, dynamic> json) {
    return BlockResult(
      success: json['success'] ?? false,
      message: json['message'] ?? '',
    );
  }
}

class BlockStatus {
  final bool hasBlocked;
  final bool isBlockedBy;

  BlockStatus({
    required this.hasBlocked,
    required this.isBlockedBy,
  });

  factory BlockStatus.fromJson(Map<String, dynamic> json) {
    return BlockStatus(
      hasBlocked: json['hasBlocked'] ?? false,
      isBlockedBy: json['isBlockedBy'] ?? false,
    );
  }
}
