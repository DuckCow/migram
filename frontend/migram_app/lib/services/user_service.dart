// services/user_service.dart
import 'package:dio/dio.dart';
import 'package:migram_app/services/token_service.dart';

import 'dio_service.dart';

class UserService {
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
