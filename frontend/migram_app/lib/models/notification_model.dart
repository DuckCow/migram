enum NotificationType {
  followRequest,
  followAccepted,
  commentOnPost,
  replyOnComment,
}

class NotificationModel {
  final int id;
  final int actorId;
  final String? actorNickname;
  final String? actorProfilePicture;
  final NotificationType type;
  final int? referenceId;
  final String message;
  final bool isRead;
  final DateTime createdAt;

  NotificationModel({
    required this.id,
    required this.actorId,
    this.actorNickname,
    this.actorProfilePicture,
    required this.type,
    this.referenceId,
    required this.message,
    required this.isRead,
    required this.createdAt,
  });

  factory NotificationModel.fromJson(Map<String, dynamic> json) {
    return NotificationModel(
      id: json['id'],
      actorId: json['actorId'],
      actorNickname: json['actorNickname'],
      actorProfilePicture: json['actorProfilePicture'],
      type: _parseNotificationType(json['type']),
      referenceId: json['referenceId'],
      message: json['message'],
      isRead: json['isRead'],
      createdAt: DateTime.parse(json['createdAt']),
    );
  }

  static NotificationType _parseNotificationType(String type) {
    switch (type) {
      case 'FOLLOW_REQUEST':
        return NotificationType.followRequest;
      case 'FOLLOW_ACCEPTED':
        return NotificationType.followAccepted;
      case 'COMMENT_ON_POST':
        return NotificationType.commentOnPost;
      case 'REPLY_ON_COMMENT':
        return NotificationType.replyOnComment;
      default:
        throw Exception('Unknown notification type: $type');
    }
  }

  String getTypeIcon() {
    switch (type) {
      case NotificationType.followRequest:
        return '👥';
      case NotificationType.followAccepted:
        return '✅';
      case NotificationType.commentOnPost:
        return '💬';
      case NotificationType.replyOnComment:
        return '↩️';
    }
  }
}
