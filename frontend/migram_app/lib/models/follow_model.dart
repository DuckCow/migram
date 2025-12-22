import 'user_model.dart';

/// Follow status enum
enum FollowStatus {
  requested,
  accepted,
  rejected;

  static FollowStatus fromString(String status) {
    switch (status.toUpperCase()) {
      case 'REQUESTED':
        return FollowStatus.requested;
      case 'ACCEPTED':
        return FollowStatus.accepted;
      case 'REJECTED':
        return FollowStatus.rejected;
      default:
        throw ArgumentError('Unknown follow status: $status');
    }
  }

  String toServerString() {
    return name.toUpperCase();
  }
}

/// Follow request model
class FollowRequest {
  final int id;
  final UserModel requester;
  final UserModel target;
  final FollowStatus status;
  final DateTime createdAt;
  final DateTime? respondedAt;

  FollowRequest({
    required this.id,
    required this.requester,
    required this.target,
    required this.status,
    required this.createdAt,
    this.respondedAt,
  });

  factory FollowRequest.fromJson(Map<String, dynamic> json) {
    return FollowRequest(
      id: json['id'] as int,
      requester: UserModel.fromJson(json['requester'] as Map<String, dynamic>),
      target: UserModel.fromJson(json['target'] as Map<String, dynamic>),
      status: FollowStatus.fromString(json['status'] as String),
      createdAt: DateTime.parse(json['createdAt'] as String),
      respondedAt: json['respondedAt'] != null
          ? DateTime.parse(json['respondedAt'] as String)
          : null,
    );
  }
}

/// Follow request result from API
class FollowRequestResult {
  final bool success;
  final String message;
  final FollowStatus? status;

  FollowRequestResult({
    required this.success,
    required this.message,
    this.status,
  });

  factory FollowRequestResult.fromJson(Map<String, dynamic> json) {
    return FollowRequestResult(
      success: json['success'] as bool,
      message: json['message'] as String,
      status: json['status'] != null
          ? FollowStatus.fromString(json['status'] as String)
          : null,
    );
  }
}

/// Follow status response for a specific user
class FollowStatusResponse {
  final FollowStatus? status;
  final bool isFollowing;

  FollowStatusResponse({
    this.status,
    required this.isFollowing,
  });

  factory FollowStatusResponse.fromJson(Map<String, dynamic> json) {
    return FollowStatusResponse(
      status: json['status'] != null
          ? FollowStatus.fromString(json['status'] as String)
          : null,
      isFollowing: json['isFollowing'] as bool,
    );
  }
}
