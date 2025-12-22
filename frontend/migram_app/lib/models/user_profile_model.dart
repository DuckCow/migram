import 'user_model.dart';

class UserProfile {
  final UserModel user;
  final int postCount;
  final int commentCount;

  UserProfile({
    required this.user,
    required this.postCount,
    required this.commentCount,
  });

  factory UserProfile.fromJson(Map<String, dynamic> json) {
    return UserProfile(
      user: UserModel.fromJson(json['user'] as Map<String, dynamic>),
      postCount: json['postCount'] as int,
      commentCount: json['commentCount'] as int,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'user': user.toJson(),
      'postCount': postCount,
      'commentCount': commentCount,
    };
  }
}

class ProfileUpdateRequest {
  final String? nickname;
  final String? originCountry;
  final String? currentCountry;

  ProfileUpdateRequest({
    this.nickname,
    this.originCountry,
    this.currentCountry,
  });

  Map<String, dynamic> toJson() {
    return {
      'nickname': nickname,
      'originCountry': originCountry,
      'currentCountry': currentCountry,
    };
  }
}
