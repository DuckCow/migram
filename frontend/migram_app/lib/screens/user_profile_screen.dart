import 'package:flutter/material.dart';
import '../models/user_model.dart';
import '../models/post_model.dart';
import '../models/follow_model.dart';
import '../services/user_service.dart';
import '../services/post_service.dart';
import '../services/follow_service.dart';
import '../services/block_service.dart';
import '../services/chat_service.dart';
import '../widgets/post_card.dart';
import 'follow_list_screen.dart';
import 'chat_screen.dart';

class UserProfileScreen extends StatefulWidget {
  final int userId;

  const UserProfileScreen({
    super.key,
    required this.userId,
  });

  @override
  State<UserProfileScreen> createState() => _UserProfileScreenState();
}

class _UserProfileScreenState extends State<UserProfileScreen> {
  final UserService _userService = UserService();
  final PostService _postService = PostService();
  final FollowService _followService = FollowService();
  final BlockService _blockService = BlockService();
  final ChatService _chatService = ChatService();

  UserModel? _user;
  List<Post> _userPosts = [];
  bool _isLoading = true;
  bool _isLoadingPosts = false;
  bool _isCurrentUser = false;

  // Follow stats
  int _followerCount = 0;
  int _followingCount = 0;
  FollowStatusResponse? _followStatus;
  bool _isBlocked = false;

  @override
  void initState() {
    super.initState();
    _checkIfCurrentUser();
    _loadUser();
  }

  Future<void> _checkIfCurrentUser() async {
    final currentUser = await _userService.getCurrentUser();
    setState(() {
      _isCurrentUser = currentUser?.id == widget.userId;
    });
  }

  Future<void> _loadUser() async {
    setState(() {
      _isLoading = true;
    });

    try {
      final user = await _userService.getUserById(widget.userId);
      setState(() {
        _user = user;
        _isLoading = false;
      });

      // Load user's posts and follow stats
      _loadUserPosts();
      if (!_isCurrentUser) {
        _loadFollowStatus();
      }
      _loadFollowStats();
    } catch (e) {
      print('Error loading user: $e');
      setState(() {
        _isLoading = false;
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('프로필을 불러오는데 실패했습니다: $e')),
        );
      }
    }
  }

  Future<void> _loadFollowStats() async {
    if (_user == null) return;

    try {
      final followerCount = await _followService.getFollowerCount(_user!.id);
      final followingCount = await _followService.getFollowingCount(_user!.id);

      setState(() {
        _followerCount = followerCount;
        _followingCount = followingCount;
      });
    } catch (e) {
      print('Error loading follow stats: $e');
    }
  }

  Future<void> _loadFollowStatus() async {
    if (_user == null) return;

    try {
      final status = await _followService.getFollowStatus(widget.userId);
      setState(() {
        _followStatus = status;
      });
    } catch (e) {
      print('Error loading follow status: $e');
    }
  }

  Future<void> _loadUserPosts() async {
    if (_user == null) return;

    setState(() {
      _isLoadingPosts = true;
    });

    try {
      final posts = await _postService.getFeed(page: 0, size: 50);
      // Filter posts by this user (excluding anonymous posts)
      final userPosts = posts
          .where((post) => !post.isAnonymous && post.user?.id == _user!.id)
          .toList();

      setState(() {
        _userPosts = userPosts;
        _isLoadingPosts = false;
      });
    } catch (e) {
      print('Error loading user posts: $e');
      setState(() {
        _isLoadingPosts = false;
      });
    }
  }

  Future<void> _handleFollow() async {
    if (_user == null) return;

    try {
      if (_followStatus?.status == FollowStatus.accepted) {
        // Unfollow
        await _followService.unfollow(_user!.id);
        setState(() {
          _followStatus = null;
        });
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('팔로우를 취소했습니다')),
        );
      } else if (_followStatus?.status == FollowStatus.requested) {
        // Cancel request
        await _followService.cancelFollowRequest(_user!.id);
        setState(() {
          _followStatus = null;
        });
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('팔로우 요청을 취소했습니다')),
        );
      } else {
        // Send follow request
        final result = await _followService.sendFollowRequest(_user!.id);
        if (result.success) {
          // Reload follow status
          await _loadFollowStatus();
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(result.message)),
          );
        }
      }
      _loadFollowStats();
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('팔로우 처리 실패: $e')),
      );
    }
  }

  Future<void> _handleBlock() async {
    if (_user == null) return;

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('사용자 차단'),
        content: Text('${_user!.displayName}님을 차단하시겠습니까?\n차단하면 서로 팔로우가 해제되고, 게시글과 댓글을 볼 수 없습니다.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('취소'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            style: TextButton.styleFrom(foregroundColor: Colors.red),
            child: const Text('차단'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      try {
        final result = await _blockService.blockUser(_user!.id);
        if (result.success) {
          if (mounted) {
            Navigator.of(context).pop();
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text(result.message)),
            );
          }
        } else {
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text(result.message)),
            );
          }
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('차단 실패: $e')),
          );
        }
      }
    }
  }

  Future<void> _handleMessage() async {
    if (_user == null) return;

    try {
      // Show loading indicator
      showDialog(
        context: context,
        barrierDismissible: false,
        builder: (context) => const Center(
          child: CircularProgressIndicator(),
        ),
      );

      // Try to create or get chat room
      final result = await _chatService.createOrGetChatRoom(_user!.id);

      // Close loading indicator
      if (mounted) {
        Navigator.of(context).pop();
      }

      if (result.success && result.chatRoom != null) {
        // Navigate to chat screen
        if (mounted) {
          Navigator.of(context).push(
            MaterialPageRoute(
              builder: (context) => ChatScreen(chatRoom: result.chatRoom!),
            ),
          );
        }
      } else {
        // Show error message
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(result.message)),
          );
        }
      }
    } catch (e) {
      // Close loading indicator if still open
      if (mounted && Navigator.of(context).canPop()) {
        Navigator.of(context).pop();
      }

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('메시지 전송 실패: $e')),
        );
      }
    }
  }

  String _getMessageButtonText() {
    // Always show "메시지" - backend will handle if it's a direct message or message request
    return '메시지';
  }

  bool _canSendMessage() {
    // Always allow message attempts
    // The backend will enforce actual permission rules based on:
    // - DM privacy settings (followers_only, everyone)
    // - Block status
    // - Follow relationships
    return true;
  }

  String _getCountryFlag(String? countryCode) {
    if (countryCode == null) return '';
    final countryFlags = {
      'JP': '🇯🇵',
      'KR': '🇰🇷',
      'CA': '🇨🇦',
      'US': '🇺🇸',
      'GB': '🇬🇧',
      'FR': '🇫🇷',
      'DE': '🇩🇪',
      'IT': '🇮🇹',
      'ES': '🇪🇸',
      'AU': '🇦🇺',
      'CN': '🇨🇳',
      'TW': '🇹🇼',
      'HK': '🇭🇰',
      'SG': '🇸🇬',
      'TH': '🇹🇭',
    };
    return countryFlags[countryCode] ?? '';
  }

  String _getFollowButtonText() {
    if (_followStatus?.status == FollowStatus.accepted) {
      return '팔로잉';
    } else if (_followStatus?.status == FollowStatus.requested) {
      return '요청됨';
    } else {
      return '팔로우';
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('프로필'),
        actions: !_isCurrentUser
            ? [
                IconButton(
                  icon: const Icon(Icons.block),
                  onPressed: _handleBlock,
                  tooltip: '차단',
                ),
              ]
            : null,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _user == null
              ? const Center(child: Text('사용자를 찾을 수 없습니다'))
              : RefreshIndicator(
                  onRefresh: _loadUser,
                  child: ListView(
                    children: [
                      // Profile header
                      _buildProfileHeader(),
                      const Divider(height: 1, thickness: 1),

                      // Stats
                      _buildStats(),
                      const Divider(height: 1, thickness: 1),

                      // User's posts
                      Padding(
                        padding: const EdgeInsets.all(16),
                        child: Text(
                          '게시글 (${_userPosts.length})',
                          style: const TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),

                      if (_isLoadingPosts)
                        const Center(
                          child: Padding(
                            padding: EdgeInsets.all(20),
                            child: CircularProgressIndicator(),
                          ),
                        )
                      else if (_userPosts.isEmpty)
                        const Padding(
                          padding: EdgeInsets.all(20),
                          child: Center(
                            child: Text('작성한 게시글이 없습니다'),
                          ),
                        )
                      else
                        ..._userPosts.map((post) => PostCard(
                              post: post,
                              onTap: () {
                                // Navigate to post detail
                              },
                              onVote: (voteType) async {
                                // Handle voting
                              },
                            )),
                    ],
                  ),
                ),
    );
  }

  Widget _buildProfileHeader() {
    return Padding(
      padding: const EdgeInsets.all(20),
      child: Column(
        children: [
          Row(
            children: [
              // Avatar
              CircleAvatar(
                radius: 40,
                backgroundImage: _user!.profilePicture != null
                    ? NetworkImage(_user!.profilePicture!)
                    : null,
                child: _user!.profilePicture == null
                    ? Text(
                        _user!.displayName[0].toUpperCase(),
                        style: const TextStyle(fontSize: 32),
                      )
                    : null,
              ),
              const SizedBox(width: 20),

              // User info
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      _user!.displayName,
                      style: const TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      _user!.email,
                      style: const TextStyle(
                        fontSize: 14,
                        color: Colors.grey,
                      ),
                    ),
                    if (_user!.originCountry != null ||
                        _user!.currentCountry != null) ...[
                      const SizedBox(height: 8),
                      Row(
                        children: [
                          if (_user!.originCountry != null) ...[
                            Text(
                              _getCountryFlag(_user!.originCountry),
                              style: const TextStyle(fontSize: 20),
                            ),
                            const SizedBox(width: 4),
                            const Text('출신'),
                            const SizedBox(width: 12),
                          ],
                          if (_user!.currentCountry != null) ...[
                            Text(
                              _getCountryFlag(_user!.currentCountry),
                              style: const TextStyle(fontSize: 20),
                            ),
                            const SizedBox(width: 4),
                            const Text('거주'),
                          ],
                        ],
                      ),
                    ],
                  ],
                ),
              ),
            ],
          ),

          // Action buttons (only for other users)
          if (!_isCurrentUser) ...[
            const SizedBox(height: 16),
            Row(
              children: [
                // Follow button
                Expanded(
                  child: ElevatedButton(
                    onPressed: _handleFollow,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: _followStatus?.status == FollowStatus.accepted
                          ? Colors.grey[300]
                          : Theme.of(context).primaryColor,
                      foregroundColor:
                          _followStatus?.status == FollowStatus.accepted ? Colors.black : Colors.white,
                    ),
                    child: Text(_getFollowButtonText()),
                  ),
                ),
                const SizedBox(width: 12),
                // Message button
                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: _canSendMessage() ? _handleMessage : null,
                    icon: const Icon(Icons.message),
                    label: Text(_getMessageButtonText()),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.white,
                      foregroundColor: Theme.of(context).primaryColor,
                      side: BorderSide(color: Theme.of(context).primaryColor),
                    ),
                  ),
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildStats() {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 20),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: [
          _buildStatItem('게시글', _userPosts.length.toString(), null),
          Container(
            height: 40,
            width: 1,
            color: Colors.grey[300],
          ),
          _buildStatItem('팔로워', _followerCount.toString(), () {
            Navigator.of(context).push(
              MaterialPageRoute(
                builder: (context) => FollowListScreen(
                  userId: _user!.id,
                  type: FollowListType.followers,
                ),
              ),
            );
          }),
          Container(
            height: 40,
            width: 1,
            color: Colors.grey[300],
          ),
          _buildStatItem('팔로잉', _followingCount.toString(), () {
            Navigator.of(context).push(
              MaterialPageRoute(
                builder: (context) => FollowListScreen(
                  userId: _user!.id,
                  type: FollowListType.followings,
                ),
              ),
            );
          }),
        ],
      ),
    );
  }

  Widget _buildStatItem(String label, String count, VoidCallback? onTap) {
    final child = Column(
      children: [
        Text(
          count,
          style: const TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 4),
        Text(
          label,
          style: const TextStyle(
            fontSize: 14,
            color: Colors.grey,
          ),
        ),
      ],
    );

    if (onTap != null) {
      return InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 8),
          child: child,
        ),
      );
    }

    return child;
  }
}
