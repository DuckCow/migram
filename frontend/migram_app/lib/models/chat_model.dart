import 'user_model.dart';

class ChatRoom {
  final int id;
  final UserModel otherUser;
  final String? lastMessageAt;
  final String? lastMessagePreview;
  final int unreadCount;
  final String createdAt;
  final String status; // PENDING, ACTIVE, IGNORED
  final int initiatorId;
  final bool isPending;

  ChatRoom({
    required this.id,
    required this.otherUser,
    this.lastMessageAt,
    this.lastMessagePreview,
    required this.unreadCount,
    required this.createdAt,
    required this.status,
    required this.initiatorId,
    required this.isPending,
  });

  factory ChatRoom.fromJson(Map<String, dynamic> json) {
    return ChatRoom(
      id: json['id'],
      otherUser: UserModel.fromJson(json['otherUser']),
      lastMessageAt: json['lastMessageAt'],
      lastMessagePreview: json['lastMessagePreview'],
      unreadCount: json['unreadCount'] ?? 0,
      createdAt: json['createdAt'],
      status: json['status'] ?? 'ACTIVE',
      initiatorId: json['initiatorId'] ?? 0,
      isPending: json['isPending'] ?? false,
    );
  }
}

class Message {
  final int id;
  final int chatRoomId;
  final int senderId;
  final String? senderNickname;
  final String? senderProfilePicture;
  final String content;
  final bool isRead;
  final String? readAt;
  final String createdAt;

  Message({
    required this.id,
    required this.chatRoomId,
    required this.senderId,
    this.senderNickname,
    this.senderProfilePicture,
    required this.content,
    required this.isRead,
    this.readAt,
    required this.createdAt,
  });

  factory Message.fromJson(Map<String, dynamic> json) {
    return Message(
      id: json['id'],
      chatRoomId: json['chatRoomId'],
      senderId: json['senderId'],
      senderNickname: json['senderNickname'],
      senderProfilePicture: json['senderProfilePicture'],
      content: json['content'],
      isRead: json['isRead'] ?? false,
      readAt: json['readAt'],
      createdAt: json['createdAt'],
    );
  }

  String getDisplayName() {
    return senderNickname ?? 'User';
  }
}
