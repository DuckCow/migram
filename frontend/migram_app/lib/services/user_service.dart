// services/user_service.dart
import 'dio_service.dart';

class UserService {
  Future<ProfileStatus> checkUserProfile() async {
    try {
      final response = await DioService().get('/api/user/check-profile');
      return ProfileStatus.fromJson(response.data);
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
