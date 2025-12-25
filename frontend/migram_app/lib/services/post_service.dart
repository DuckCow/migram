import 'dart:io';
import 'package:dio/dio.dart';
import 'package:http_parser/http_parser.dart';
import '../models/post_model.dart';
import 'dio_service.dart';

class PostService {
  final DioService _dioService = DioService();

  /// Get feed with Hot algorithm sorting
  /// Optionally filter by multiple countries (comma-separated)
  Future<List<Post>> getFeed({
    List<String>? countries,
    int page = 0,
    int size = 20,
  }) async {
    try {
      final queryParams = <String, dynamic>{
        'page': page,
        'size': size,
      };

      if (countries != null && countries.isNotEmpty) {
        queryParams['countries'] = countries.join(',');
      }

      final response = await _dioService.get(
        '/posts/feed',
        queryParameters: queryParams,
      );

      if (response.statusCode == 200) {
        final data = response.data;
        final List<dynamic> content = data['content'] as List<dynamic>;
        return content.map((json) => Post.fromJson(json as Map<String, dynamic>)).toList();
      }

      throw Exception('Failed to load feed: ${response.statusCode}');
    } catch (e) {
      print('Error loading feed: $e');
      rethrow;
    }
  }

  /// Get a single post by ID
  Future<Post> getPostById(int postId) async {
    try {
      final response = await _dioService.get('/posts/$postId');

      if (response.statusCode == 200) {
        return Post.fromJson(response.data as Map<String, dynamic>);
      }

      throw Exception('Failed to load post: ${response.statusCode}');
    } catch (e) {
      print('Error loading post: $e');
      rethrow;
    }
  }

  /// Vote on a post
  /// voteType: 'UP' or 'DOWN'
  Future<void> votePost(int postId, String voteType) async {
    try {
      final response = await _dioService.post(
        '/posts/$postId/vote',
        queryParameters: {'type': voteType},
      );

      if (response.statusCode != 200) {
        throw Exception('Failed to vote: ${response.statusCode}');
      }
    } catch (e) {
      print('Error voting on post: $e');
      rethrow;
    }
  }

  /// Create a new post
  Future<Post> createPost({
    required String title,
    required String content,
    required String country,
    String? imageUrl,
    bool isAnonymous = false,
  }) async {
    try {
      final requestData = {
        'title': title,
        'content': content,
        'country': country,
        'imageUrl': imageUrl,
        'isAnonymous': isAnonymous,
      };

      final response = await _dioService.post(
        '/posts',
        data: requestData,
      );

      if (response.statusCode == 201 || response.statusCode == 200) {
        return Post.fromJson(response.data as Map<String, dynamic>);
      }

      throw Exception('Failed to create post: ${response.statusCode}');
    } catch (e) {
      print('Error creating post: $e');
      rethrow;
    }
  }

  /// Update an existing post
  Future<Post> updatePost({
    required int postId,
    required String title,
    required String content,
    required String country,
    String? imageUrl,
  }) async {
    try {
      final requestData = {
        'title': title,
        'content': content,
        'country': country,
        'imageUrl': imageUrl,
      };

      final response = await _dioService.put('/posts/$postId', data: requestData);

      if (response.statusCode == 200) {
        return Post.fromJson(response.data as Map<String, dynamic>);
      }

      throw Exception('Failed to update post: ${response.statusCode}');
    } catch (e) {
      print('Error updating post: $e');
      rethrow;
    }
  }

  /// Delete a post
  Future<void> deletePost(int postId) async {
    try {
      final response = await _dioService.delete('/posts/$postId');

      if (response.statusCode != 200) {
        throw Exception('Failed to delete post: ${response.statusCode}');
      }
    } catch (e) {
      print('Error deleting post: $e');
      rethrow;
    }
  }

  /// Create a post with image upload
  Future<Post> createPostWithImage({
    required String title,
    required String content,
    required String country,
    File? imageFile,
    bool isAnonymous = false,
  }) async {
    try {
      final formData = FormData.fromMap({
        'title': title,
        'content': content,
        'country': country,
        'isAnonymous': isAnonymous,
      });

      if (imageFile != null) {
        final fileName = imageFile.path.split('/').last;
        formData.files.add(
          MapEntry(
            'image',
            await MultipartFile.fromFile(
              imageFile.path,
              filename: fileName,
              contentType: MediaType('image', 'jpeg'),
            ),
          ),
        );
      }

      final response = await _dioService.post(
        '/posts/with-image',
        data: formData,
        options: Options(
          contentType: 'multipart/form-data',
        ),
      );

      if (response.statusCode == 201 || response.statusCode == 200) {
        return Post.fromJson(response.data as Map<String, dynamic>);
      }

      throw Exception('Failed to create post: ${response.statusCode}');
    } catch (e) {
      print('Error creating post with image: $e');
      rethrow;
    }
  }

  /// Update a post with optional image upload
  Future<Post> updatePostWithImage({
    required int postId,
    required String title,
    required String content,
    required String country,
    File? imageFile,
    bool keepExistingImage = true,
  }) async {
    try {
      final formData = FormData.fromMap({
        'title': title,
        'content': content,
        'country': country,
        'keepExistingImage': keepExistingImage.toString(),
      });

      if (imageFile != null) {
        final fileName = imageFile.path.split('/').last;
        formData.files.add(
          MapEntry(
            'image',
            await MultipartFile.fromFile(
              imageFile.path,
              filename: fileName,
              contentType: MediaType('image', 'jpeg'),
            ),
          ),
        );
      }

      final response = await _dioService.put(
        '/posts/$postId/with-image',
        data: formData,
        options: Options(
          contentType: 'multipart/form-data',
        ),
      );

      if (response.statusCode == 200) {
        return Post.fromJson(response.data as Map<String, dynamic>);
      }

      throw Exception('Failed to update post: ${response.statusCode}');
    } catch (e) {
      print('Error updating post with image: $e');
      rethrow;
    }
  }
}
