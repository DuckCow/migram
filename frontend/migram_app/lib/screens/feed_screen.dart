import 'package:flutter/material.dart';
import 'package:flutter_gen/gen_l10n/app_localizations.dart';
import '../models/post_model.dart';
import '../services/post_service.dart';
import '../services/token_service.dart';
import '../utils/error_handler.dart';
import '../widgets/post_card.dart';
import '../widgets/board_filter_bottom_sheet.dart';
import '../widgets/search_bar.dart';
import '../widgets/notification_icon.dart';
import 'post_detail_screen.dart';
import 'create_post_screen.dart';
import 'login_screen.dart';
import 'profile_screen.dart';

class FeedScreen extends StatefulWidget {
  const FeedScreen({super.key});

  @override
  State<FeedScreen> createState() => _FeedScreenState();
}

class _FeedScreenState extends State<FeedScreen> {
  final PostService _postService = PostService();
  final ScrollController _scrollController = ScrollController();

  List<Post> _posts = [];
  List<int>? _selectedBoardIds;
  bool _isLoading = false;
  bool _isLoadingMore = false;
  int _currentPage = 0;
  final int _pageSize = 20;
  bool _hasMore = true;

  @override
  void initState() {
    super.initState();
    _loadFeed();
    _scrollController.addListener(_onScroll);
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scrollController.position.pixels >=
        _scrollController.position.maxScrollExtent - 200) {
      if (!_isLoadingMore && _hasMore) {
        _loadMore();
      }
    }
  }

  Future<void> _loadFeed() async {
    setState(() {
      _isLoading = true;
      _currentPage = 0;
      _hasMore = true;
    });

    try {
      final posts = await _postService.getFeed(
        boardIds: _selectedBoardIds,
        page: 0,
        size: _pageSize,
      );

      setState(() {
        _posts = posts;
        _isLoading = false;
        _hasMore = posts.length >= _pageSize;
      });
    } catch (e) {
      setState(() {
        _isLoading = false;
      });

      // Redirect to login on authentication failure
      if (e.toString().contains('302') || e.toString().contains('401') || e.toString().contains('403')) {
        debugPrint('Authentication failed, redirecting to login: $e');
        await TokenService().clearTokens();
        if (mounted) {
          Navigator.of(context).pushAndRemoveUntil(
            MaterialPageRoute(builder: (context) => const LoginScreen()),
            (route) => false,
          );
        }
      } else if (mounted) {
        ErrorHandler.showError(
          context,
          e,
          userMessage: ErrorMessages.loadPostsFailed(context),
        );
      }
    }
  }

  Future<void> _loadMore() async {
    if (_isLoadingMore) return;

    setState(() {
      _isLoadingMore = true;
    });

    try {
      final posts = await _postService.getFeed(
        boardIds: _selectedBoardIds,
        page: _currentPage + 1,
        size: _pageSize,
      );

      setState(() {
        _currentPage++;
        _posts.addAll(posts);
        _isLoadingMore = false;
        _hasMore = posts.length >= _pageSize;
      });
    } catch (e) {
      debugPrint('Error loading more posts: $e');
      setState(() {
        _isLoadingMore = false;
      });
      // Silently fail for pagination - user can retry by scrolling
    }
  }

  Future<void> _handleVote(int postId, String voteType) async {
    try {
      await _postService.votePost(postId, voteType);

      // Refresh the specific post
      await _loadFeed();
    } catch (e) {
      if (mounted) {
        ErrorHandler.showError(
          context,
          e,
          userMessage: ErrorMessages.voteFailed(context),
        );
      }
    }
  }

  void _showBoardFilter() async {
    final result = await showModalBottomSheet<List<int>?>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => BoardFilterBottomSheet(
        initialSelectedBoardIds: _selectedBoardIds,
      ),
    );

    if (result != null || result == null && _selectedBoardIds != null) {
      setState(() {
        _selectedBoardIds = result;
      });
      _loadFeed();
    }
  }

  Future<void> _handleLogout() async {
    final l10n = AppLocalizations.of(context)!;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(l10n.logout),
        content: Text(l10n.confirmLogout),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: Text(l10n.cancel),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: Text(l10n.logout),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      await TokenService().clearTokens();
      if (mounted) {
        Navigator.of(context).pushAndRemoveUntil(
          MaterialPageRoute(builder: (context) => const LoginScreen()),
          (route) => false,
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(AppLocalizations.of(context)!.appName),
        actions: [
          // Board filter chip
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 8),
            child: GestureDetector(
              onTap: _showBoardFilter,
              child: Chip(
                avatar: const Icon(Icons.dashboard, size: 18),
                label: Text(
                  _selectedBoardIds == null || _selectedBoardIds!.isEmpty
                      ? AppLocalizations.of(context)!.all
                      : AppLocalizations.of(context)!.applyBoards(_selectedBoardIds!.length),
                  style: const TextStyle(fontSize: 14),
                ),
                deleteIcon: _selectedBoardIds != null && _selectedBoardIds!.isNotEmpty
                    ? const Icon(Icons.close, size: 18)
                    : null,
                onDeleted: _selectedBoardIds != null && _selectedBoardIds!.isNotEmpty
                    ? () {
                        setState(() {
                          _selectedBoardIds = null;
                        });
                        _loadFeed();
                      }
                    : null,
              ),
            ),
          ),
          // Notification icon
          const NotificationIcon(),
          // Profile button
          IconButton(
            icon: const Icon(Icons.account_circle),
            onPressed: () {
              Navigator.of(context).push(
                MaterialPageRoute(builder: (context) => const ProfileScreen()),
              );
            },
            tooltip: AppLocalizations.of(context)!.profile,
          ),
        ],
      ),
      body: Column(
        children: [
          // Search bar
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: SearchBarWidget(
              initialBoardIds: _selectedBoardIds,
            ),
          ),
          // Feed content
          Expanded(
            child: RefreshIndicator(
              onRefresh: _loadFeed,
              child: _isLoading
                  ? const Center(child: CircularProgressIndicator())
                  : _posts.isEmpty
                      ? Center(
                          child: Text(
                            AppLocalizations.of(context)!.noPostsYet,
                            textAlign: TextAlign.center,
                            style: const TextStyle(color: Colors.grey),
                          ),
                        )
                      : ListView.builder(
                          controller: _scrollController,
                          itemCount: _posts.length + (_hasMore ? 1 : 0),
                          itemBuilder: (context, index) {
                            if (index == _posts.length) {
                              return const Center(
                                child: Padding(
                                  padding: EdgeInsets.all(16),
                                  child: CircularProgressIndicator(),
                                ),
                              );
                            }

                            final post = _posts[index];
                            return PostCard(
                              post: post,
                              onTap: () {
                                Navigator.of(context).push(
                                  MaterialPageRoute(
                                    builder: (context) => PostDetailScreen(
                                      postId: post.id,
                                    ),
                                  ),
                                );
                              },
                              onVote: (voteType) => _handleVote(post.id, voteType),
                            );
                          },
                        ),
            ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () async {
          final result = await Navigator.of(context).push(
            MaterialPageRoute(
              builder: (context) => const CreatePostScreen(),
            ),
          );

          // Refresh feed if post was created successfully
          if (result == true) {
            _loadFeed();
          }
        },
        child: const Icon(Icons.add),
      ),
    );
  }
}
