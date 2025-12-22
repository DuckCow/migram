import '../models/follow_model.dart';
import '../models/user_model.dart';
import 'dio_service.dart';

class FollowService {
  final DioService _dioService = DioService();

  /// Send follow request
  /// POST /api/follows/request/{targetId}
  Future<FollowRequestResult> sendFollowRequest(int targetId) async {
    try {
      final response = await _dioService.post('/api/follows/request/$targetId');

      if (response.statusCode == 200 || response.statusCode == 400) {
        return FollowRequestResult.fromJson(response.data as Map<String, dynamic>);
      }

      throw Exception('Failed to send follow request: ${response.statusCode}');
    } catch (e) {
      print('Error sending follow request: $e');
      rethrow;
    }
  }

  /// Accept follow request
  /// POST /api/follows/accept/{requestId}
  Future<FollowRequestResult> acceptFollowRequest(int requestId) async {
    try {
      final response = await _dioService.post('/api/follows/accept/$requestId');

      if (response.statusCode == 200 || response.statusCode == 400 || response.statusCode == 403) {
        return FollowRequestResult.fromJson(response.data as Map<String, dynamic>);
      }

      throw Exception('Failed to accept follow request: ${response.statusCode}');
    } catch (e) {
      print('Error accepting follow request: $e');
      rethrow;
    }
  }

  /// Reject follow request
  /// POST /api/follows/reject/{requestId}
  Future<FollowRequestResult> rejectFollowRequest(int requestId) async {
    try {
      final response = await _dioService.post('/api/follows/reject/$requestId');

      if (response.statusCode == 200 || response.statusCode == 400 || response.statusCode == 403) {
        return FollowRequestResult.fromJson(response.data as Map<String, dynamic>);
      }

      throw Exception('Failed to reject follow request: ${response.statusCode}');
    } catch (e) {
      print('Error rejecting follow request: $e');
      rethrow;
    }
  }

  /// Cancel sent follow request
  /// DELETE /api/follows/cancel/{targetId}
  Future<bool> cancelFollowRequest(int targetId) async {
    try {
      final response = await _dioService.delete('/api/follows/cancel/$targetId');

      if (response.statusCode == 200) {
        final data = response.data as Map<String, dynamic>;
        return data['success'] as bool;
      }

      throw Exception('Failed to cancel follow request: ${response.statusCode}');
    } catch (e) {
      print('Error canceling follow request: $e');
      rethrow;
    }
  }

  /// Unfollow user
  /// DELETE /api/follows/unfollow/{targetId}
  Future<bool> unfollow(int targetId) async {
    try {
      final response = await _dioService.delete('/api/follows/unfollow/$targetId');

      if (response.statusCode == 200) {
        final data = response.data as Map<String, dynamic>;
        return data['success'] as bool;
      }

      throw Exception('Failed to unfollow: ${response.statusCode}');
    } catch (e) {
      print('Error unfollowing: $e');
      rethrow;
    }
  }

  /// Get pending follow requests (받은 요청)
  /// GET /api/follows/requests/pending
  Future<List<FollowRequest>> getPendingRequests() async {
    try {
      final response = await _dioService.get('/api/follows/requests/pending');

      if (response.statusCode == 200) {
        final List<dynamic> data = response.data as List<dynamic>;
        return data
            .map((json) => FollowRequest.fromJson(json as Map<String, dynamic>))
            .toList();
      }

      throw Exception('Failed to get pending requests: ${response.statusCode}');
    } catch (e) {
      print('Error getting pending requests: $e');
      rethrow;
    }
  }

  /// Get sent follow requests (보낸 요청)
  /// GET /api/follows/requests/sent
  Future<List<FollowRequest>> getSentRequests() async {
    try {
      final response = await _dioService.get('/api/follows/requests/sent');

      if (response.statusCode == 200) {
        final List<dynamic> data = response.data as List<dynamic>;
        return data
            .map((json) => FollowRequest.fromJson(json as Map<String, dynamic>))
            .toList();
      }

      throw Exception('Failed to get sent requests: ${response.statusCode}');
    } catch (e) {
      print('Error getting sent requests: $e');
      rethrow;
    }
  }

  /// Get followers (ACCEPTED only)
  /// GET /api/follows/{userId}/followers
  Future<List<UserModel>> getFollowers(int userId, {int page = 0, int size = 20}) async {
    try {
      final response = await _dioService.get(
        '/api/follows/$userId/followers',
        queryParameters: {'page': page, 'size': size},
      );

      if (response.statusCode == 200) {
        final data = response.data as Map<String, dynamic>;
        final List<dynamic> content = data['content'] as List<dynamic>;
        return content
            .map((json) => UserModel.fromJson(json as Map<String, dynamic>))
            .toList();
      }

      throw Exception('Failed to get followers: ${response.statusCode}');
    } catch (e) {
      print('Error getting followers: $e');
      rethrow;
    }
  }

  /// Get followings (ACCEPTED only)
  /// GET /api/follows/{userId}/followings
  Future<List<UserModel>> getFollowings(int userId, {int page = 0, int size = 20}) async {
    try {
      final response = await _dioService.get(
        '/api/follows/$userId/followings',
        queryParameters: {'page': page, 'size': size},
      );

      if (response.statusCode == 200) {
        final data = response.data as Map<String, dynamic>;
        final List<dynamic> content = data['content'] as List<dynamic>;
        return content
            .map((json) => UserModel.fromJson(json as Map<String, dynamic>))
            .toList();
      }

      throw Exception('Failed to get followings: ${response.statusCode}');
    } catch (e) {
      print('Error getting followings: $e');
      rethrow;
    }
  }

  /// Get follower count
  /// GET /api/follows/{userId}/followers/count
  Future<int> getFollowerCount(int userId) async {
    try {
      final response = await _dioService.get('/api/follows/$userId/followers/count');

      if (response.statusCode == 200) {
        return response.data as int;
      }

      throw Exception('Failed to get follower count: ${response.statusCode}');
    } catch (e) {
      print('Error getting follower count: $e');
      rethrow;
    }
  }

  /// Get following count
  /// GET /api/follows/{userId}/followings/count
  Future<int> getFollowingCount(int userId) async {
    try {
      final response = await _dioService.get('/api/follows/$userId/followings/count');

      if (response.statusCode == 200) {
        return response.data as int;
      }

      throw Exception('Failed to get following count: ${response.statusCode}');
    } catch (e) {
      print('Error getting following count: $e');
      rethrow;
    }
  }

  /// Get follow status for target user
  /// GET /api/follows/status/{targetId}
  Future<FollowStatusResponse> getFollowStatus(int targetId) async {
    try {
      final response = await _dioService.get('/api/follows/status/$targetId');

      if (response.statusCode == 200) {
        return FollowStatusResponse.fromJson(response.data as Map<String, dynamic>);
      }

      throw Exception('Failed to get follow status: ${response.statusCode}');
    } catch (e) {
      print('Error getting follow status: $e');
      rethrow;
    }
  }
}
