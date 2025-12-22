import 'package:flutter/material.dart';
import '../models/post_model.dart';
import '../services/post_service.dart';
import '../services/token_service.dart';
import '../widgets/post_card.dart';
import '../widgets/country_filter_bottom_sheet.dart';
import '../widgets/search_bar.dart';
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
  List<String>? _selectedCountries;
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
        countries: _selectedCountries,
        page: 0,
        size: _pageSize,
      );

      setState(() {
        _posts = posts;
        _isLoading = false;
        _hasMore = posts.length >= _pageSize;
      });
    } catch (e) {
      print('Error loading feed: $e');
      setState(() {
        _isLoading = false;
      });

      // 인증 실패 시 로그인 화면으로 이동
      if (e.toString().contains('302') || e.toString().contains('401') || e.toString().contains('403')) {
        await TokenService().clearTokens();
        if (mounted) {
          Navigator.of(context).pushAndRemoveUntil(
            MaterialPageRoute(builder: (context) => const LoginScreen()),
            (route) => false,
          );
        }
      } else if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('피드를 불러오는데 실패했습니다: $e')),
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
        countries: _selectedCountries,
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
      print('Error loading more: $e');
      setState(() {
        _isLoadingMore = false;
      });
    }
  }

  Future<void> _handleVote(int postId, String voteType) async {
    try {
      await _postService.votePost(postId, voteType);

      // Refresh the specific post
      await _loadFeed();
    } catch (e) {
      print('Error voting: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('투표에 실패했습니다')),
        );
      }
    }
  }

  void _showCountryFilter() async {
    final result = await showModalBottomSheet<List<String>?>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => CountryFilterBottomSheet(
        initialSelectedCountries: _selectedCountries,
      ),
    );

    if (result != null || result == null && _selectedCountries != null) {
      setState(() {
        _selectedCountries = result;
      });
      _loadFeed();
    }
  }

  Future<void> _handleLogout() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('로그아웃'),
        content: const Text('로그아웃 하시겠습니까?'),
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
        title: const Text('Migram'),
        actions: [
          // Country filter chip
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 8),
            child: GestureDetector(
              onTap: _showCountryFilter,
              child: Chip(
                avatar: const Text('🌍'),
                label: Text(
                  _selectedCountries == null || _selectedCountries!.isEmpty
                      ? '전체'
                      : '${_selectedCountries!.length}개 국가',
                  style: const TextStyle(fontSize: 14),
                ),
                deleteIcon: _selectedCountries != null && _selectedCountries!.isNotEmpty
                    ? const Icon(Icons.close, size: 18)
                    : null,
                onDeleted: _selectedCountries != null && _selectedCountries!.isNotEmpty
                    ? () {
                        setState(() {
                          _selectedCountries = null;
                        });
                        _loadFeed();
                      }
                    : null,
              ),
            ),
          ),
          // Profile button
          IconButton(
            icon: const Icon(Icons.account_circle),
            onPressed: () {
              Navigator.of(context).push(
                MaterialPageRoute(builder: (context) => const ProfileScreen()),
              );
            },
            tooltip: '프로필',
          ),
        ],
      ),
      body: Column(
        children: [
          // Search bar
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: SearchBarWidget(
              initialCountries: _selectedCountries,
            ),
          ),
          // Feed content
          Expanded(
            child: RefreshIndicator(
              onRefresh: _loadFeed,
              child: _isLoading
                  ? const Center(child: CircularProgressIndicator())
                  : _posts.isEmpty
                      ? const Center(
                          child: Text(
                            '게시글이 없습니다\n새로운 게시글을 작성해보세요!',
                            textAlign: TextAlign.center,
                            style: TextStyle(color: Colors.grey),
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
