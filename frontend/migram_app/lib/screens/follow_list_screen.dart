import 'package:flutter/material.dart';
import '../models/user_model.dart';
import '../services/follow_service.dart';
import '../widgets/follow_button.dart';
import 'user_profile_screen.dart';

enum FollowListType {
  followers,
  followings,
}

/// Follow List Screen
/// Shows followers or followings list
class FollowListScreen extends StatefulWidget {
  final int userId;
  final FollowListType type;

  const FollowListScreen({
    super.key,
    required this.userId,
    required this.type,
  });

  @override
  State<FollowListScreen> createState() => _FollowListScreenState();
}

class _FollowListScreenState extends State<FollowListScreen> {
  final FollowService _followService = FollowService();
  final ScrollController _scrollController = ScrollController();

  List<UserModel> _users = [];
  bool _isLoading = false;
  bool _isLoadingMore = false;
  int _currentPage = 0;
  bool _hasMore = true;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _loadUsers();
    _scrollController.addListener(_onScroll);
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scrollController.position.pixels >=
        _scrollController.position.maxScrollExtent * 0.9) {
      if (!_isLoadingMore && _hasMore) {
        _loadMore();
      }
    }
  }

  Future<void> _loadUsers() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
      _currentPage = 0;
      _hasMore = true;
    });

    try {
      final List<UserModel> users;
      if (widget.type == FollowListType.followers) {
        users = await _followService.getFollowers(widget.userId, page: 0);
      } else {
        users = await _followService.getFollowings(widget.userId, page: 0);
      }

      setState(() {
        _users = users;
        _isLoading = false;
        _hasMore = users.length >= 20;
      });
    } catch (e) {
      setState(() {
        _errorMessage = '목록을 불러오는데 실패했습니다: $e';
        _isLoading = false;
      });
    }
  }

  Future<void> _loadMore() async {
    if (_isLoadingMore) return;

    setState(() {
      _isLoadingMore = true;
    });

    try {
      final List<UserModel> users;
      if (widget.type == FollowListType.followers) {
        users = await _followService.getFollowers(
          widget.userId,
          page: _currentPage + 1,
        );
      } else {
        users = await _followService.getFollowings(
          widget.userId,
          page: _currentPage + 1,
        );
      }

      setState(() {
        _currentPage++;
        _users.addAll(users);
        _isLoadingMore = false;
        _hasMore = users.length >= 20;
      });
    } catch (e) {
      setState(() {
        _isLoadingMore = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final title = widget.type == FollowListType.followers ? '팔로워' : '팔로잉';

    return Scaffold(
      appBar: AppBar(
        title: Text(title),
      ),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_isLoading) {
      return Center(child: CircularProgressIndicator());
    }

    if (_errorMessage != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.error_outline, size: 64, color: Colors.grey),
            SizedBox(height: 16),
            Text(_errorMessage!, style: TextStyle(color: Colors.grey)),
            SizedBox(height: 16),
            ElevatedButton(
              onPressed: _loadUsers,
              child: Text('다시 시도'),
            ),
          ],
        ),
      );
    }

    if (_users.isEmpty) {
      final emptyMessage = widget.type == FollowListType.followers
          ? '팔로워가 없습니다'
          : '팔로잉한 사용자가 없습니다';

      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.people_outline, size: 64, color: Colors.grey),
            SizedBox(height: 16),
            Text(
              emptyMessage,
              style: TextStyle(color: Colors.grey, fontSize: 16),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _loadUsers,
      child: ListView.builder(
        controller: _scrollController,
        itemCount: _users.length + (_hasMore ? 1 : 0),
        itemBuilder: (context, index) {
          if (index == _users.length) {
            return Center(
              child: Padding(
                padding: EdgeInsets.all(16),
                child: CircularProgressIndicator(),
              ),
            );
          }

          final user = _users[index];
          return _buildUserCard(user);
        },
      ),
    );
  }

  Widget _buildUserCard(UserModel user) {
    return Card(
      margin: EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: InkWell(
        onTap: () {
          // Navigate to user profile
          Navigator.of(context).push(
            MaterialPageRoute(
              builder: (context) => UserProfileScreen(userId: user.id),
            ),
          );
        },
        child: Padding(
          padding: EdgeInsets.all(16),
          child: Row(
            children: [
              // Avatar
              CircleAvatar(
                radius: 24,
                backgroundImage: user.profilePicture != null
                    ? NetworkImage(user.profilePicture!)
                    : null,
                child: user.profilePicture == null
                    ? Text(
                        user.displayName[0].toUpperCase(),
                        style: TextStyle(fontSize: 20),
                      )
                    : null,
              ),
              SizedBox(width: 12),

              // User info
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      user.displayName,
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 16,
                      ),
                    ),
                    if (user.currentCountry != null) ...[
                      SizedBox(height: 4),
                      Text(
                        user.currentCountry!,
                        style: TextStyle(
                          color: Colors.grey,
                          fontSize: 14,
                        ),
                      ),
                    ],
                  ],
                ),
              ),

              // Follow button
              FollowButton(
                targetUserId: user.id,
                onStatusChanged: () {
                  // Optional: Refresh list when follow status changes
                },
              ),
            ],
          ),
        ),
      ),
    );
  }
}
