import 'package:flutter/material.dart';
import '../models/user_model.dart';
import '../models/post_model.dart';
import '../services/user_service.dart';
import '../services/post_service.dart';
import '../services/follow_service.dart';
import '../services/block_service.dart';
import '../services/chat_service.dart';
import '../widgets/post_card.dart';
import '../widgets/follow_button.dart';
import 'chat_screen.dart';

class OtherUserProfileScreen extends StatefulWidget {
  final int userId;

  const OtherUserProfileScreen({
    super.key,
    required this.userId,
  });

  @override
  State<OtherUserProfileScreen> createState() => _OtherUserProfileScreenState();
}

class _OtherUserProfileScreenState extends State<OtherUserProfileScreen> {
  final UserService _userService = UserService();
  final PostService _postService = PostService();
  final FollowService _followService = FollowService();
  final BlockService _blockService = BlockService();
  final ChatService _chatService = ChatService();

  UserModel? _user;
  List<Post> _userPosts = [];
  bool _isLoading = true;
  bool _isLoadingPosts = false;

  // Follow stats
  int _followerCount = 0;
  int _followingCount = 0;

  // Block status
  bool _hasBlocked = false;
  bool _isBlockedBy = false;

  @override
  void initState() {
    super.initState();
    _loadUserProfile();
  }

  Future<void> _loadUserProfile() async {
    setState(() {
      _isLoading = true;
    });

    try {
      final user = await _userService.getUserById(widget.userId);
      setState(() {
        _user = user;
        _isLoading = false;
      });

      // Load user's posts, follow stats, and block status
      _loadUserPosts();
      _loadFollowStats();
      _loadBlockStatus();
    } catch (e) {
      print('Error loading user profile: $e');
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

  Future<void> _loadBlockStatus() async {
    if (_user == null) return;

    try {
      final blockStatus = await _blockService.getBlockStatus(_user!.id);
      if (blockStatus != null) {
        setState(() {
          _hasBlocked = blockStatus.hasBlocked;
          _isBlockedBy = blockStatus.isBlockedBy;
        });
      }
    } catch (e) {
      print('Error loading block status: $e');
    }
  }

  Future<void> _loadUserPosts() async {
    if (_user == null) return;

    setState(() {
      _isLoadingPosts = true;
    });

    try {
      final posts = await _postService.getFeed(page: 0, size: 50);
      // Filter posts by this user
      final userPosts = posts.where((post) => post.user.id == _user!.id).toList();

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

  Future<void> _showBlockDialog() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(_hasBlocked ? '차단 해제' : '사용자 차단'),
        content: Text(
          _hasBlocked
              ? '${_user!.displayName}님을 차단 해제하시겠습니까?'
              : '${_user!.displayName}님을 차단하시겠습니까?\n\n차단하면:\n• 서로 팔로우가 해제됩니다\n• DM을 주고받을 수 없습니다\n• 팔로우 요청을 할 수 없습니다',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('취소'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            style: TextButton.styleFrom(
              foregroundColor: Colors.red,
            ),
            child: Text(_hasBlocked ? '차단 해제' : '차단'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      await _toggleBlock();
    }
  }

  Future<void> _toggleBlock() async {
    try {
      final result = _hasBlocked
          ? await _blockService.unblockUser(_user!.id)
          : await _blockService.blockUser(_user!.id);

      if (result.success) {
        setState(() {
          _hasBlocked = !_hasBlocked;
        });

        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(result.message)),
          );
        }

        // Reload follow stats as they may have changed
        _loadFollowStats();
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
          SnackBar(content: Text('작업 실패: $e')),
        );
      }
    }
  }

  Future<void> _startDM() async {
    try {
      final result = await _chatService.createOrGetChatRoom(_user!.id);

      if (result.success && result.chatRoom != null) {
        if (mounted) {
          Navigator.of(context).push(
            MaterialPageRoute(
              builder: (context) => ChatScreen(chatRoom: result.chatRoom!),
            ),
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
          SnackBar(content: Text('DM 시작 실패: $e')),
        );
      }
    }
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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_user?.displayName ?? '프로필'),
        actions: [
          // Block button in app bar (three dots menu)
          if (_user != null)
            PopupMenuButton<String>(
              onSelected: (value) {
                if (value == 'block') {
                  _showBlockDialog();
                }
              },
              itemBuilder: (context) => [
                PopupMenuItem(
                  value: 'block',
                  child: Row(
                    children: [
                      Icon(
                        _hasBlocked ? Icons.block : Icons.block_outlined,
                        color: Colors.red,
                      ),
                      const SizedBox(width: 8),
                      Text(
                        _hasBlocked ? '차단 해제' : '사용자 차단',
                        style: const TextStyle(color: Colors.red),
                      ),
                    ],
                  ),
                ),
              ],
            ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _user == null
              ? const Center(child: Text('프로필을 불러올 수 없습니다'))
              : RefreshIndicator(
                  onRefresh: _loadUserProfile,
                  child: ListView(
                    children: [
                      // Show blocked message if blocked
                      if (_hasBlocked || _isBlockedBy)
                        Container(
                          padding: const EdgeInsets.all(16),
                          color: Colors.red[50],
                          child: Row(
                            children: [
                              Icon(Icons.block, color: Colors.red[700]),
                              const SizedBox(width: 12),
                              Expanded(
                                child: Text(
                                  _hasBlocked
                                      ? '차단한 사용자입니다'
                                      : '이 사용자가 당신을 차단했습니다',
                                  style: TextStyle(
                                    color: Colors.red[700],
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),

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
                            // Navigate to post detail (optional)
                          },
                          onVote: (voteType) async {
                            // Handle voting (optional)
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

          // Follow button and DM button (only show if not blocked)
          if (!_hasBlocked && !_isBlockedBy) ...[
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: FollowButton(
                    userId: _user!.id,
                    onFollowStatusChanged: () {
                      _loadFollowStats();
                    },
                  ),
                ),
                const SizedBox(width: 8),
                OutlinedButton.icon(
                  onPressed: _startDM,
                  icon: const Icon(Icons.message_outlined),
                  label: const Text('DM'),
                  style: OutlinedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
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
          _buildStatItem('게시글', _userPosts.length.toString()),
          Container(
            height: 40,
            width: 1,
            color: Colors.grey[300],
          ),
          _buildStatItem('팔로워', _followerCount.toString()),
          Container(
            height: 40,
            width: 1,
            color: Colors.grey[300],
          ),
          _buildStatItem('팔로잉', _followingCount.toString()),
        ],
      ),
    );
  }

  Widget _buildStatItem(String label, String count) {
    return Column(
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
  }
}
