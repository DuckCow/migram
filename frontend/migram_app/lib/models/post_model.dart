import 'user_model.dart';

class Post {
  final int id;
  final String title;
  final String content;
  final String? imageUrl;
  final String country;
  final UserModel user;
  final DateTime createdAt;
  final DateTime modifiedAt;
  final int upvotes;
  final int downvotes;
  final int netVotes;
  final String? currentUserVote; // 'UP', 'DOWN', or null
  final int commentCount;
  final double? hotScore;

  Post({
    required this.id,
    required this.title,
    required this.content,
    this.imageUrl,
    required this.country,
    required this.user,
    required this.createdAt,
    required this.modifiedAt,
    required this.upvotes,
    required this.downvotes,
    required this.netVotes,
    this.currentUserVote,
    required this.commentCount,
    this.hotScore,
  });

  factory Post.fromJson(Map<String, dynamic> json) {
    return Post(
      id: json['id'] as int,
      title: json['title'] as String,
      content: json['content'] as String,
      imageUrl: json['mediaUrl'] as String?,
      country: json['country'] as String,
      user: UserModel.fromJson(json['userDTO'] as Map<String, dynamic>),
      createdAt: DateTime.parse(json['createdAt'] as String),
      modifiedAt: DateTime.parse(json['modifiedAt'] as String),
      upvotes: json['upvotes'] as int,
      downvotes: json['downvotes'] as int,
      netVotes: json['netVotes'] as int,
      currentUserVote: json['currentUserVote'] as String?,
      commentCount: json['commentCount'] as int,
      hotScore: json['hotScore'] as double?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'title': title,
      'content': content,
      'mediaUrl': imageUrl,
      'country': country,
      'user': user.toJson(),
      'createdAt': createdAt.toIso8601String(),
      'modifiedAt': modifiedAt.toIso8601String(),
      'upvotes': upvotes,
      'downvotes': downvotes,
      'netVotes': netVotes,
      'currentUserVote': currentUserVote,
      'commentCount': commentCount,
      'hotScore': hotScore,
    };
  }

  // Get country flag emoji from country code
  String getCountryFlag() {
    const Map<String, String> countryFlags = {
      'JP': '🇯🇵',
      'CA': '🇨🇦',
      'KR': '🇰🇷',
      'US': '🇺🇸',
      'GB': '🇬🇧',
      'FR': '🇫🇷',
      'DE': '🇩🇪',
      'IT': '🇮🇹',
      'ES': '🇪🇸',
      'AU': '🇦🇺',
      'NZ': '🇳🇿',
      'CN': '🇨🇳',
      'TW': '🇹🇼',
      'HK': '🇭🇰',
      'SG': '🇸🇬',
      'IN': '🇮🇳',
      'TH': '🇹🇭',
      'VN': '🇻🇳',
      'PH': '🇵🇭',
      'MY': '🇲🇾',
      'ID': '🇮🇩',
    };
    return countryFlags[country] ?? '🌍';
  }
}
