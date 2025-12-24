import 'package:flutter/material.dart';
import 'package:timeago/timeago.dart' as timeago;
import '../models/notification_model.dart';
import '../services/notification_service.dart';

class NotificationsScreen extends StatefulWidget {
  const NotificationsScreen({super.key});

  @override
  State<NotificationsScreen> createState() => _NotificationsScreenState();
}

class _NotificationsScreenState extends State<NotificationsScreen> {
  final NotificationService _notificationService = NotificationService();
  final List<NotificationModel> _notifications = [];
  bool _isLoading = false;
  int _currentPage = 0;
  final int _pageSize = 20;
  final ScrollController _scrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    _loadNotifications();
    _listenToNewNotifications();
    _scrollController.addListener(_onScroll);
  }

  Future<void> _loadNotifications() async {
    if (_isLoading) return;

    setState(() {
      _isLoading = true;
    });

    try {
      final notifications = await _notificationService.getNotifications(
        page: _currentPage,
        size: _pageSize,
      );

      if (mounted) {
        setState(() {
          _notifications.addAll(notifications);
          _currentPage++;
          _isLoading = false;
        });
      }
    } catch (e) {
      print('Error loading notifications: $e');
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  void _listenToNewNotifications() {
    _notificationService.notificationStream.listen((notification) {
      if (mounted) {
        setState(() {
          _notifications.insert(0, notification);
        });
      }
    });
  }

  void _onScroll() {
    if (_scrollController.position.pixels ==
        _scrollController.position.maxScrollExtent) {
      _loadNotifications();
    }
  }

  void _onNotificationTap(NotificationModel notification) async {
    // Mark as read
    if (!notification.isRead) {
      await _notificationService.markAsRead(notification.id);
      setState(() {
        final index = _notifications.indexOf(notification);
        if (index != -1) {
          _notifications[index] = NotificationModel(
            id: notification.id,
            actorId: notification.actorId,
            actorNickname: notification.actorNickname,
            actorProfilePicture: notification.actorProfilePicture,
            type: notification.type,
            referenceId: notification.referenceId,
            message: notification.message,
            isRead: true,
            createdAt: notification.createdAt,
          );
        }
      });
    }

    // Navigate based on notification type
    if (notification.referenceId != null) {
      switch (notification.type) {
        case NotificationType.followRequest:
        case NotificationType.followAccepted:
          // TODO: Navigate to user profile or follow requests screen
          break;
        case NotificationType.commentOnPost:
        case NotificationType.replyOnComment:
          // TODO: Navigate to post detail screen
          break;
      }
    }
  }

  Future<void> _markAllAsRead() async {
    await _notificationService.markAllAsRead();
    setState(() {
      for (int i = 0; i < _notifications.length; i++) {
        if (!_notifications[i].isRead) {
          _notifications[i] = NotificationModel(
            id: _notifications[i].id,
            actorId: _notifications[i].actorId,
            actorNickname: _notifications[i].actorNickname,
            actorProfilePicture: _notifications[i].actorProfilePicture,
            type: _notifications[i].type,
            referenceId: _notifications[i].referenceId,
            message: _notifications[i].message,
            isRead: true,
            createdAt: _notifications[i].createdAt,
          );
        }
      }
    });
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Notifications'),
        actions: [
          TextButton(
            onPressed: _markAllAsRead,
            child: const Text('Mark all as read'),
          ),
        ],
      ),
      body: _notifications.isEmpty && !_isLoading
          ? const Center(
              child: Text('No notifications yet'),
            )
          : RefreshIndicator(
              onRefresh: () async {
                setState(() {
                  _notifications.clear();
                  _currentPage = 0;
                });
                await _loadNotifications();
              },
              child: ListView.builder(
                controller: _scrollController,
                itemCount: _notifications.length + (_isLoading ? 1 : 0),
                itemBuilder: (context, index) {
                  if (index == _notifications.length) {
                    return const Center(
                      child: Padding(
                        padding: EdgeInsets.all(16.0),
                        child: CircularProgressIndicator(),
                      ),
                    );
                  }

                  final notification = _notifications[index];
                  return _buildNotificationItem(notification);
                },
              ),
            ),
    );
  }

  Widget _buildNotificationItem(NotificationModel notification) {
    return Container(
      color: notification.isRead ? null : Colors.blue.withOpacity(0.1),
      child: ListTile(
        leading: CircleAvatar(
          backgroundImage: notification.actorProfilePicture != null
              ? NetworkImage(notification.actorProfilePicture!)
              : null,
          child: notification.actorProfilePicture == null
              ? Text(notification.getTypeIcon())
              : null,
        ),
        title: Text(
          notification.message,
          style: TextStyle(
            fontWeight: notification.isRead ? FontWeight.normal : FontWeight.bold,
          ),
        ),
        subtitle: Text(
          timeago.format(notification.createdAt, locale: 'en'),
          style: const TextStyle(fontSize: 12),
        ),
        trailing: notification.isRead
            ? null
            : Container(
                width: 8,
                height: 8,
                decoration: const BoxDecoration(
                  color: Colors.blue,
                  shape: BoxShape.circle,
                ),
              ),
        onTap: () => _onNotificationTap(notification),
      ),
    );
  }
}
