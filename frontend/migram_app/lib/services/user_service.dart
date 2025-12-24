// services/user_service.dart
import 'package:dio/dio.dart';
import 'package:migram_app/services/token_service.dart';
import '../models/user_model.dart';
import '../models/user_profile_model.dart';

import 'dio_service.dart';

class UserService {
  static UserModel? _currentUser; // Cache current user
  final DioService _dioService = DioService();

  /// Get current user (cached)
  Future<UserModel?> getCurrentUser() async {
    if (_currentUser != null) return _currentUser;

    try {
      final response = await _dioService.get('/users/me');
      if (response.statusCode == 200) {
        _currentUser = UserModel.fromJson(response.data as Map<String, dynamic>);
        return _currentUser;
      }
    } catch (e) {
      print('Error getting current user: $e');
    }
    return null;
  }

  /// Get current user profile with stats
  Future<UserProfile> getCurrentUserProfile() async {
    try {
      final response = await _dioService.get('/users/me/profile');
      if (response.statusCode == 200) {
        final profile = UserProfile.fromJson(response.data as Map<String, dynamic>);
        // Update cache
        _currentUser = profile.user;
        return profile;
      }
      throw Exception('Failed to load profile: ${response.statusCode}');
    } catch (e) {
      print('Error getting user profile: $e');
      rethrow;
    }
  }

  /// Get user by ID
  Future<UserModel> getUserById(int userId) async {
    try {
      final response = await _dioService.get('/users/$userId');
      if (response.statusCode == 200) {
        return UserModel.fromJson(response.data as Map<String, dynamic>);
      }
      throw Exception('Failed to load user: ${response.statusCode}');
    } catch (e) {
      print('Error getting user by ID: $e');
      rethrow;
    }
  }

  /// Update current user profile
  Future<UserModel> updateProfile(ProfileUpdateRequest request) async {
    try {
      final response = await _dioService.put(
        '/users/me',
        data: request.toJson(),
      );

      if (response.statusCode == 200) {
        final updatedUser = UserModel.fromJson(response.data as Map<String, dynamic>);
        // Update cache
        _currentUser = updatedUser;
        return updatedUser;
      }

      throw Exception('Failed to update profile: ${response.statusCode}');
    } catch (e) {
      print('Error updating profile: $e');
      rethrow;
    }
  }

  /// Clear cached user (call on logout)
  void clearCurrentUser() {
    _currentUser = null;
  }

  Future<ProfileStatus> checkUserProfile() async {
    try {
      print('UserService: Getting access token...');
      final accessToken = await TokenService().getAccessToken();
      if (accessToken == null) {
        throw Exception('No access token found');
      }
      print('UserService: Access token obtained');

      print('UserService: Making API request...');
      final response = await DioService().get(
        '/auth/profile',
        options: Options(
          headers: {
            'Authorization': 'Bearer $accessToken',
          },
        ),
      );

      print('UserService: API response received');
      print('UserService: Response data type: ${response.data.runtimeType}');
      print('UserService: Response data: ${response.data}');

      // 안전한 파싱
      final data = response.data;
      if (data is Map<String, dynamic>) {
        final isComplete = data['isProfileComplete'];
        print(
            'UserService: isProfileComplete value: $isComplete (type: ${isComplete.runtimeType})');

        return ProfileStatus(
          isProfileComplete: isComplete == true,
        );
      } else {
        throw Exception('Unexpected response format: ${data.runtimeType}');
      }
    } catch (e) {
      print('Error checking user profile: $e');
      if (e is DioException) {
        print('UserService error response: ${e.response?.data}');
        print('UserService error statusCode: ${e.response?.statusCode}');
      }
      rethrow;
    }
  }
}

class ProfileStatus {
  final bool isProfileComplete;
  final String? originCountry;
  final String? currentCountry;

  ProfileStatus({
    required this.isProfileComplete,
    this.originCountry,
    this.currentCountry,
  });

  factory ProfileStatus.fromJson(Map<String, dynamic> json) {
    return ProfileStatus(
      isProfileComplete: json['isProfileComplete'] ?? false,
      originCountry: json['originCountry'],
      currentCountry: json['currentCountry'],
    );
  }
}
