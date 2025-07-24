// services/user_service.dart
import 'package:dio/dio.dart';
import 'package:migram_app/services/token_service.dart';

import 'dio_service.dart';

class UserService {
  Future<ProfileStatus> checkUserProfile() async {
    try {
      final accessToken = await TokenService().getAccessToken();
      if (accessToken == null) {
        throw Exception('No access token found');
      }

      final response = await DioService().get(
        '/auth/profile',
        options: Options(
          headers: {
            'Authorization': 'Bearer $accessToken',
          },
        ),
      );

      return ProfileStatus(
        isProfileComplete: response.data['isProfileComplete'] ?? false,
      );
    } catch (e) {
      print('Error checking user profile: $e');
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
