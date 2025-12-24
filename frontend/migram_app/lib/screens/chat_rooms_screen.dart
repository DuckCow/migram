import 'package:flutter/material.dart';
import 'package:timeago/timeago.dart' as timeago;
import '../models/chat_model.dart';
import '../services/chat_service.dart';
import 'chat_screen.dart';

class ChatRoomsScreen extends StatefulWidget {
  const ChatRoomsScreen({super.key});

  @override
  State<ChatRoomsScreen> createState() => _ChatRoomsScreenState();
}

class _ChatRoomsScreenState extends State<ChatRoomsScreen> {
  final ChatService _chatService = ChatService();
  final ScrollController _scrollController = ScrollController();

  List<ChatRoom> _chatRooms = [];
  bool _isLoading = true;
  bool _isLoadingMore = false;
  int _currentPage = 0;
  bool _hasMore = true;

  int _totalUnreadCount = 0;

  @override
  void initState() {
    super.initState();
    _loadChatRooms();
    _scrollController.addListener(_onScroll);
    _listenToUnreadCount();
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  void _listenToUnreadCount() {
    _chatService.unreadCountStream?.listen((count) {
      if (mounted) {
        setState(() {
          _totalUnreadCount = count;
        });
      }
    });
  }

  Future<void> _loadChatRooms({bool refresh = false}) async {
    if (refresh) {
      setState(() {
        _currentPage = 0;
        _hasMore = true;
        _isLoading = true;
      });
    }

    try {
      final chatRooms = await _chatService.getChatRooms(
        page: _currentPage,
        size: 20,
      );

      setState(() {
        if (refresh) {
          _chatRooms = chatRooms;
        } else {
          _chatRooms.addAll(chatRooms);
        }
        _hasMore = chatRooms.length >= 20;
        _isLoading = false;
        _isLoadingMore = false;
      });

      // Also get total unread count
      final unreadCount = await _chatService.getTotalUnreadCount();
      setState(() {
        _totalUnreadCount = unreadCount;
      });
    } catch (e) {
      print('Error loading chat rooms: $e');
      setState(() {
        _isLoading = false;
        _isLoadingMore = false;
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('채팅방 목록을 불러오는데 실패했습니다: $e')),
        );
      }
    }
  }

  void _onScroll() {
    if (_scrollController.position.pixels >= _scrollController.position.maxScrollExtent - 200) {
      if (!_isLoadingMore && _hasMore) {
        setState(() {
          _isLoadingMore = true;
          _currentPage++;
        });
        _loadChatRooms();
      }
    }
  }

  Future<void> _onRefresh() async {
    await _loadChatRooms(refresh: true);
  }

  void _navigateToChatRoom(ChatRoom chatRoom) async {
    await Navigator.of(context).push(
      MaterialPageRoute(
        builder: (context) => ChatScreen(chatRoom: chatRoom),
      ),
    );

    // Reload chat rooms when returning (to update unread count and last message)
    _loadChatRooms(refresh: true);
  }

  String _formatTime(String? dateTimeStr) {
    if (dateTimeStr == null) return '';
    try {
      final dateTime = DateTime.parse(dateTimeStr);
      return timeago.format(dateTime, locale: 'ko');
    } catch (e) {
      return '';
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('채팅'),
        actions: [
          if (_totalUnreadCount > 0)
            Center(
              child: Container(
                margin: const EdgeInsets.only(right: 16),
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: Colors.red,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Text(
                  '$_totalUnreadCount',
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 12,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
            ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _chatRooms.isEmpty
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(
                        Icons.chat_bubble_outline,
                        size: 64,
                        color: Colors.grey[400],
                      ),
                      const SizedBox(height: 16),
                      Text(
                        '채팅방이 없습니다',
                        style: TextStyle(
                          fontSize: 16,
                          color: Colors.grey[600],
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        '서로 팔로우한 사용자와\nDM을 시작할 수 있습니다',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          fontSize: 14,
                          color: Colors.grey[500],
                        ),
                      ),
                    ],
                  ),
                )
              : RefreshIndicator(
                  onRefresh: _onRefresh,
                  child: ListView.builder(
                    controller: _scrollController,
                    itemCount: _chatRooms.length + (_isLoadingMore ? 1 : 0),
                    itemBuilder: (context, index) {
                      if (index == _chatRooms.length) {
                        return const Center(
                          child: Padding(
                            padding: EdgeInsets.all(16),
                            child: CircularProgressIndicator(),
                          ),
                        );
                      }

                      final chatRoom = _chatRooms[index];
                      return _buildChatRoomItem(chatRoom);
                    },
                  ),
                ),
    );
  }

  Widget _buildChatRoomItem(ChatRoom chatRoom) {
    return ListTile(
      leading: CircleAvatar(
        backgroundImage: chatRoom.otherUser.profilePicture != null
            ? NetworkImage(chatRoom.otherUser.profilePicture!)
            : null,
        child: chatRoom.otherUser.profilePicture == null
            ? Text(
                chatRoom.otherUser.displayName[0].toUpperCase(),
                style: const TextStyle(fontSize: 20),
              )
            : null,
      ),
      title: Row(
        children: [
          Expanded(
            child: Text(
              chatRoom.otherUser.displayName,
              style: TextStyle(
                fontWeight: chatRoom.unreadCount > 0 ? FontWeight.bold : FontWeight.normal,
              ),
            ),
          ),
          if (chatRoom.unreadCount > 0)
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
              decoration: BoxDecoration(
                color: Colors.red,
                borderRadius: BorderRadius.circular(10),
              ),
              child: Text(
                '${chatRoom.unreadCount}',
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 12,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
        ],
      ),
      subtitle: Row(
        children: [
          Expanded(
            child: Text(
              chatRoom.lastMessagePreview ?? '메시지를 시작하세요',
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                color: chatRoom.unreadCount > 0 ? Colors.black87 : Colors.grey[600],
                fontWeight: chatRoom.unreadCount > 0 ? FontWeight.w500 : FontWeight.normal,
              ),
            ),
          ),
          const SizedBox(width: 8),
          Text(
            _formatTime(chatRoom.lastMessageAt),
            style: TextStyle(
              fontSize: 12,
              color: Colors.grey[500],
            ),
          ),
        ],
      ),
      onTap: () => _navigateToChatRoom(chatRoom),
    );
  }
}
