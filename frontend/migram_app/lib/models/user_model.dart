class UserModel {
  final int id;
  final String name;
  final String email;
  final String? profilePicture;
  final String? nickname;
  final String? nicknameKey;
  final String? originCountry;
  final String? currentCountry;

  UserModel({
    required this.id,
    required this.name,
    required this.email,
    this.profilePicture,
    this.nickname,
    this.nicknameKey,
    this.originCountry,
    this.currentCountry,
  });

  factory UserModel.fromJson(Map<String, dynamic> json) {
    return UserModel(
      id: json['id'] as int,
      name: json['name'] as String,
      email: json['email'] as String,
      profilePicture: json['profilePicture'] as String?,
      nickname: json['nickname'] as String?,
      nicknameKey: json['nicknameKey'] as String?,
      originCountry: json['originCountry'] as String?,
      currentCountry: json['currentCountry'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'email': email,
      'profilePicture': profilePicture,
      'nickname': nickname,
      'nicknameKey': nicknameKey,
      'originCountry': originCountry,
      'currentCountry': currentCountry,
    };
  }

  // Get display name (nickname or name)
  String get displayName => nickname ?? name;
}
