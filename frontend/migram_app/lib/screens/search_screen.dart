import 'package:flutter/material.dart';
import 'package:migram_app/l10n/app_localizations.dart';
import '../models/post_model.dart';
import '../models/search_result_model.dart';
import '../services/search_service.dart';
import '../services/post_service.dart';
import '../widgets/post_card.dart';
import '../widgets/search_filter_bottom_sheet.dart';
import '../utils/error_handler.dart';
import 'post_detail_screen.dart';

class SearchScreen extends StatefulWidget {
  final String? initialQuery;
  final List<int>? initialBoardIds;

  const SearchScreen({
    super.key,
    this.initialQuery,
    this.initialBoardIds,
  });

  @override
  State<SearchScreen> createState() => _SearchScreenState();
}

class _SearchScreenState extends State<SearchScreen> {
  final SearchService _searchService = SearchService();
  final PostService _postService = PostService();
  final TextEditingController _searchController = TextEditingController();
  final ScrollController _scrollController = ScrollController();

  List<Post> _posts = [];
  bool _isLoading = false;
  bool _hasMore = true;
  int _currentPage = 0;
  String? _errorMessage;

  // Filters
  List<int>? _selectedBoardIds;
  SortType _sortBy = SortType.hot;
  String? _dateFrom;
  String? _dateTo;
  int? _minVotes;
  bool? _hasImage;

  @override
  void initState() {
    super.initState();
    _searchController.text = widget.initialQuery ?? '';
    _selectedBoardIds = widget.initialBoardIds;
    _scrollController.addListener(_onScroll);

    // Perform initial search
    if (widget.initialQuery != null || widget.initialBoardIds != null) {
      _search(isInitialLoad: true);
    }
  }

  @override
  void dispose() {
    _searchController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scrollController.position.pixels >=
        _scrollController.position.maxScrollExtent * 0.9) {
      if (!_isLoading && _hasMore) {
        _loadMore();
      }
    }
  }

  Future<void> _search({bool isInitialLoad = false}) async {
    if (_isLoading) return;

    setState(() {
      _isLoading = true;
      _errorMessage = null;
      if (isInitialLoad) {
        _posts.clear();
        _currentPage = 0;
        _hasMore = true;
      }
    });

    try {
      final result = await _searchService.search(
        keyword: _searchController.text.isEmpty ? null : _searchController.text,
        boardIds: _selectedBoardIds,
        sortBy: _sortBy,
        dateFrom: _dateFrom,
        dateTo: _dateTo,
        minVotes: _minVotes,
        hasImage: _hasImage,
        page: _currentPage,
        size: 20,
      );

      setState(() {
        _posts.addAll(result.posts);
        _hasMore = result.hasMore;
        _isLoading = false;
      });
    } catch (e) {
      debugPrint('Error searching posts: $e');
      setState(() {
        _errorMessage = 'Failed to search posts';
        _isLoading = false;
      });
    }
  }

  Future<void> _loadMore() async {
    _currentPage++;
    await _search();
  }

  void _openFilterSheet() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => SearchFilterBottomSheet(
        selectedBoardIds: _selectedBoardIds,
        sortBy: _sortBy,
        dateFrom: _dateFrom,
        dateTo: _dateTo,
        minVotes: _minVotes,
        hasImage: _hasImage,
        onApply: (boardIds, sortBy, dateFrom, dateTo, minVotes, hasImage) {
          setState(() {
            _selectedBoardIds = boardIds;
            _sortBy = sortBy;
            _dateFrom = dateFrom;
            _dateTo = dateTo;
            _minVotes = minVotes;
            _hasImage = hasImage;
          });
          _search(isInitialLoad: true);
        },
      ),
    );
  }

  int _getActiveFilterCount() {
    int count = 0;
    if (_selectedBoardIds != null && _selectedBoardIds!.isNotEmpty) count++;
    if (_sortBy != SortType.hot) count++;
    if (_dateFrom != null || _dateTo != null) count++;
    if (_minVotes != null) count++;
    if (_hasImage != null) count++;
    return count;
  }

  Future<void> _handleVote(int postId, String voteType) async {
    try {
      await _postService.votePost(postId, voteType);
      // Refresh the search results
      await _search();
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

  @override
  Widget build(BuildContext context) {
    final filterCount = _getActiveFilterCount();

    return Scaffold(
      appBar: AppBar(
        title: TextField(
          controller: _searchController,
          autofocus: widget.initialQuery == null,
          decoration: InputDecoration(
            hintText: 'Search posts...',
            border: InputBorder.none,
            hintStyle: TextStyle(color: Colors.grey[400]),
          ),
          onSubmitted: (value) {
            _search(isInitialLoad: true);
          },
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.search),
            onPressed: () => _search(isInitialLoad: true),
          ),
          Stack(
            children: [
              IconButton(
                icon: const Icon(Icons.filter_list),
                onPressed: _openFilterSheet,
              ),
              if (filterCount > 0)
                Positioned(
                  right: 8,
                  top: 8,
                  child: Container(
                    padding: const EdgeInsets.all(4),
                    decoration: const BoxDecoration(
                      color: Colors.red,
                      shape: BoxShape.circle,
                    ),
                    constraints: const BoxConstraints(
                      minWidth: 16,
                      minHeight: 16,
                    ),
                    child: Text(
                      '$filterCount',
                      style: const TextStyle(
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
        ],
      ),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_errorMessage != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.error_outline, size: 64, color: Colors.grey),
            const SizedBox(height: 16),
            Text(_errorMessage!, style: const TextStyle(color: Colors.grey)),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: () => _search(isInitialLoad: true),
              child: const Text('Retry'),
            ),
          ],
        ),
      );
    }

    if (_posts.isEmpty && !_isLoading) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.search_off, size: 64, color: Colors.grey),
            const SizedBox(height: 16),
            Text(
              _searchController.text.isEmpty
                  ? 'Enter a search term'
                  : 'No results found',
              style: const TextStyle(color: Colors.grey, fontSize: 16),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: () => _search(isInitialLoad: true),
      child: ListView.builder(
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
                  builder: (context) => PostDetailScreen(postId: post.id),
                ),
              );
            },
            onVote: (voteType) => _handleVote(post.id, voteType),
          );
        },
      ),
    );
  }
}
