import '../models/comment_model.dart';
import 'dio_service.dart';

class CommentService {
  final DioService _dioService = DioService();

  /// Get comments for a post
  Future<List<Comment>> getCommentsByPost({
    required int postId,
    int page = 0,
    int size = 50,
  }) async {
    try {
      final response = await _dioService.get(
        '/comments/post/$postId',
        queryParameters: {
          'page': page,
          'size': size,
          'sort': 'createdAt,desc',
        },
      );

      if (response.statusCode == 200) {
        final data = response.data;
        final List<dynamic> content = data['content'] as List<dynamic>;
        return content.map((json) => Comment.fromJson(json as Map<String, dynamic>)).toList();
      }

      throw Exception('Failed to load comments: ${response.statusCode}');
    } catch (e) {
      print('Error loading comments: $e');
      rethrow;
    }
  }

  /// Get a single comment by ID
  Future<Comment> getCommentById(int commentId) async {
    try {
      final response = await _dioService.get('/comments/$commentId');

      if (response.statusCode == 200) {
        return Comment.fromJson(response.data as Map<String, dynamic>);
      }

      throw Exception('Failed to load comment: ${response.statusCode}');
    } catch (e) {
      print('Error loading comment: $e');
      rethrow;
    }
  }

  /// Create a new comment or reply
  Future<Comment> createComment({
    required int postId,
    required String content,
    int? parentCommentId, // If provided, creates a reply
  }) async {
    try {
      final data = {
        'postId': postId,
        'content': content,
      };

      if (parentCommentId != null) {
        data['parentCommentId'] = parentCommentId;
      }

      final response = await _dioService.post('/comments', data: data);

      if (response.statusCode == 200) {
        return Comment.fromJson(response.data as Map<String, dynamic>);
      }

      throw Exception('Failed to create comment: ${response.statusCode}');
    } catch (e) {
      print('Error creating comment: $e');
      rethrow;
    }
  }

  /// Create a reply to a comment (convenience method)
  Future<Comment> createReply({
    required int postId,
    required int parentCommentId,
    required String content,
  }) async {
    return createComment(
      postId: postId,
      content: content,
      parentCommentId: parentCommentId,
    );
  }

  /// Vote on a comment
  /// voteType: 'UP' or 'DOWN'
  Future<void> voteComment(int commentId, String voteType) async {
    try {
      final response = await _dioService.post(
        '/comments/$commentId/vote',
        queryParameters: {'type': voteType},
      );

      if (response.statusCode != 200) {
        throw Exception('Failed to vote: ${response.statusCode}');
      }
    } catch (e) {
      print('Error voting on comment: $e');
      rethrow;
    }
  }

  /// Update a comment
  Future<Comment> updateComment({
    required int commentId,
    required int postId,
    required String content,
  }) async {
    try {
      final response = await _dioService.put(
        '/comments/$commentId',
        data: {
          'postId': postId,
          'content': content,
        },
      );

      if (response.statusCode == 200) {
        return Comment.fromJson(response.data as Map<String, dynamic>);
      }

      throw Exception('Failed to update comment: ${response.statusCode}');
    } catch (e) {
      print('Error updating comment: $e');
      rethrow;
    }
  }

  /// Delete a comment
  Future<void> deleteComment(int commentId) async {
    try {
      final response = await _dioService.delete('/comments/$commentId');

      if (response.statusCode != 200) {
        throw Exception('Failed to delete comment: ${response.statusCode}');
      }
    } catch (e) {
      print('Error deleting comment: $e');
      rethrow;
    }
  }
}
