import 'package:flutter/material.dart';
import '../models/user_profile_model.dart';
import '../models/post_model.dart';
import '../services/user_service.dart';
import '../services/post_service.dart';
import '../services/follow_service.dart';
import '../services/token_service.dart';
import '../widgets/post_card.dart';
import 'edit_profile_dialog.dart';
import 'login_screen.dart';
import 'follow_requests_screen.dart';
import 'follow_list_screen.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({super.key});

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  final UserService _userService = UserService();
  final PostService _postService = PostService();
  final FollowService _followService = FollowService();

  UserProfile? _profile;
  List<Post> _userPosts = [];
  bool _isLoading = true;
  bool _isLoadingPosts = false;

  // Follow stats
  int _followerCount = 0;
  int _followingCount = 0;
  int _pendingRequestsCount = 0;

  @override
  void initState() {
    super.initState();
    _loadProfile();
  }

  Future<void> _loadProfile() async {
    setState(() {
      _isLoading = true;
    });

    try {
      final profile = await _userService.getCurrentUserProfile();
      setState(() {
        _profile = profile;
        _isLoading = false;
      });

      // Load user's posts and follow stats
      _loadUserPosts();
      _loadFollowStats();
    } catch (e) {
      print('Error loading profile: $e');
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
    if (_profile == null) return;

    try {
      final followerCount = await _followService.getFollowerCount(_profile!.user.id);
      final followingCount = await _followService.getFollowingCount(_profile!.user.id);
      final pendingRequests = await _followService.getPendingRequests();

      setState(() {
        _followerCount = followerCount;
        _followingCount = followingCount;
        _pendingRequestsCount = pendingRequests.length;
      });
    } catch (e) {
      print('Error loading follow stats: $e');
    }
  }

  Future<void> _loadUserPosts() async {
    if (_profile == null) return;

    setState(() {
      _isLoadingPosts = true;
    });

    try {
      final posts = await _postService.getFeed(page: 0, size: 20);
      // Filter posts by current user
      final userPosts = posts.where((post) => post.user.id == _profile!.user.id).toList();

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

  Future<void> _editProfile() async {
    if (_profile == null) return;

    final result = await showDialog<bool>(
      context: context,
      builder: (context) => EditProfileDialog(user: _profile!.user),
    );

    if (result == true) {
      _loadProfile();
    }
  }

  Future<void> _logout() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('로그아웃'),
        content: const Text('정말로 로그아웃 하시겠습니까?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('취소'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('로그아웃'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      await TokenService().clearTokens();
      _userService.clearCurrentUser();
      if (mounted) {
        Navigator.of(context).pushAndRemoveUntil(
          MaterialPageRoute(builder: (context) => const LoginScreen()),
          (route) => false,
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
        title: const Text('프로필'),
        actions: [
          // Follow requests notification
          Stack(
            children: [
              IconButton(
                icon: const Icon(Icons.person_add),
                onPressed: () async {
                  await Navigator.of(context).push(
                    MaterialPageRoute(
                      builder: (context) => const FollowRequestsScreen(),
                    ),
                  );
                  // Reload stats after returning
                  _loadFollowStats();
                },
                tooltip: '팔로우 요청',
              ),
              if (_pendingRequestsCount > 0)
                Positioned(
                  right: 8,
                  top: 8,
                  child: Container(
                    padding: EdgeInsets.all(4),
                    decoration: BoxDecoration(
                      color: Colors.red,
                      shape: BoxShape.circle,
                    ),
                    constraints: BoxConstraints(
                      minWidth: 16,
                      minHeight: 16,
                    ),
                    child: Text(
                      '$_pendingRequestsCount',
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: 10,
                        fontWeight: FontWeight.bold,
                      ),
                      textAlign: TextAlign.center,
                    ),
                  ),
                ),
            ],
          ),
          IconButton(
            icon: const Icon(Icons.logout),
            onPressed: _logout,
            tooltip: '로그아웃',
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _profile == null
              ? const Center(child: Text('프로필을 불러올 수 없습니다'))
              : RefreshIndicator(
                  onRefresh: _loadProfile,
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
                          '내 게시글 (${_userPosts.length})',
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
                            // Navigate to post detail (optional - could be implemented)
                          },
                          onVote: (voteType) async {
                            // Handle voting (optional - could be implemented)
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
      child: Row(
        children: [
          // Avatar
          CircleAvatar(
            radius: 40,
            backgroundImage: _profile!.user.profilePicture != null
                ? NetworkImage(_profile!.user.profilePicture!)
                : null,
            child: _profile!.user.profilePicture == null
                ? Text(
                    _profile!.user.displayName[0].toUpperCase(),
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
                  _profile!.user.displayName,
                  style: const TextStyle(
                    fontSize: 24,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  _profile!.user.email,
                  style: const TextStyle(
                    fontSize: 14,
                    color: Colors.grey,
                  ),
                ),
                if (_profile!.user.originCountry != null ||
                    _profile!.user.currentCountry != null) ...[
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      if (_profile!.user.originCountry != null) ...[
                        Text(
                          _getCountryFlag(_profile!.user.originCountry),
                          style: const TextStyle(fontSize: 20),
                        ),
                        const SizedBox(width: 4),
                        const Text('출신'),
                        const SizedBox(width: 12),
                      ],
                      if (_profile!.user.currentCountry != null) ...[
                        Text(
                          _getCountryFlag(_profile!.user.currentCountry),
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

          // Edit button
          IconButton(
            icon: const Icon(Icons.edit),
            onPressed: _editProfile,
            tooltip: '프로필 수정',
          ),
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
          _buildStatItem('게시글', _profile!.postCount.toString(), null),
          Container(
            height: 40,
            width: 1,
            color: Colors.grey[300],
          ),
          _buildStatItem('팔로워', _followerCount.toString(), () {
            Navigator.of(context).push(
              MaterialPageRoute(
                builder: (context) => FollowListScreen(
                  userId: _profile!.user.id,
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
                  userId: _profile!.user.id,
                  type: FollowListType.followings,
                ),
              ),
            );
          }),
          Container(
            height: 40,
            width: 1,
            color: Colors.grey[300],
          ),
          _buildStatItem('댓글', _profile!.commentCount.toString(), null),
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
