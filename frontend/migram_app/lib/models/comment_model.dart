import 'user_model.dart';

class Comment {
  final int id;
  final String content;
  final int postId; // Post ID for the comment
  final UserModel user;
  final DateTime createdAt;
  final DateTime modifiedAt;
  final int upvotes;
  final int downvotes;
  final int netVotes;
  final String? currentUserVote; // 'UP', 'DOWN', or null
  final int? parentCommentId; // For nested replies
  final String? replyToUsername; // Username being replied to (preserved on delete)
  final int? replyToUserId; // User ID being replied to (for clickable @mentions)
  final List<Comment> replies; // Child comments
  final int replyCount; // Number of replies
  final bool isDeleted; // Soft delete flag

  Comment({
    required this.id,
    required this.content,
    required this.postId,
    required this.user,
    required this.createdAt,
    required this.modifiedAt,
    required this.upvotes,
    required this.downvotes,
    required this.netVotes,
    this.currentUserVote,
    this.parentCommentId,
    this.replyToUsername,
    this.replyToUserId,
    this.replies = const [],
    this.replyCount = 0,
    this.isDeleted = false,
  });

  factory Comment.fromJson(Map<String, dynamic> json) {
    return Comment(
      id: json['id'] as int,
      content: json['content'] as String,
      postId: json['postId'] as int,
      user: UserModel.fromJson(json['userDTO'] as Map<String, dynamic>),
      createdAt: DateTime.parse(json['createdAt'] as String),
      modifiedAt: DateTime.parse(json['modifiedAt'] as String),
      upvotes: json['upvotes'] as int,
      downvotes: json['downvotes'] as int,
      netVotes: json['netVotes'] as int,
      currentUserVote: json['currentUserVote'] as String?,
      parentCommentId: json['parentCommentId'] as int?,
      replyToUsername: json['replyToUsername'] as String?,
      replyToUserId: json['replyToUserId'] as int?,
      replies: (json['replies'] as List<dynamic>?)
              ?.map((reply) => Comment.fromJson(reply as Map<String, dynamic>))
              .toList() ??
          [],
      replyCount: json['replyCount'] as int? ?? 0,
      isDeleted: json['isDeleted'] as bool? ?? false,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'content': content,
      'postId': postId,
      'user': user.toJson(),
      'createdAt': createdAt.toIso8601String(),
      'modifiedAt': modifiedAt.toIso8601String(),
      'upvotes': upvotes,
      'downvotes': downvotes,
      'netVotes': netVotes,
      'currentUserVote': currentUserVote,
      'parentCommentId': parentCommentId,
      'replyToUsername': replyToUsername,
      'replyToUserId': replyToUserId,
      'replies': replies.map((reply) => reply.toJson()).toList(),
      'replyCount': replyCount,
      'isDeleted': isDeleted,
    };
  }
}
