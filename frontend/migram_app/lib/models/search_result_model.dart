import 'post_model.dart';

/// Search result wrapper from backend
class SearchResult {
  final List<Post> posts;
  final int totalCount;
  final bool hasMore;
  final String? query;
  final SearchFilters? filters;

  SearchResult({
    required this.posts,
    required this.totalCount,
    required this.hasMore,
    this.query,
    this.filters,
  });

  factory SearchResult.fromJson(Map<String, dynamic> json) {
    return SearchResult(
      posts: (json['posts'] as List<dynamic>)
          .map((post) => Post.fromJson(post as Map<String, dynamic>))
          .toList(),
      totalCount: json['totalCount'] as int,
      hasMore: json['hasMore'] as bool,
      query: json['query'] as String?,
      filters: json['filters'] != null
          ? SearchFilters.fromJson(json['filters'] as Map<String, dynamic>)
          : null,
    );
  }
}

/// Applied filters information
class SearchFilters {
  final List<String>? countries;
  final String sortBy;
  final String? dateRange;
  final int? minVotes;
  final bool? hasImage;

  SearchFilters({
    this.countries,
    required this.sortBy,
    this.dateRange,
    this.minVotes,
    this.hasImage,
  });

  factory SearchFilters.fromJson(Map<String, dynamic> json) {
    return SearchFilters(
      countries: json['countries'] != null
          ? List<String>.from(json['countries'] as List<dynamic>)
          : null,
      sortBy: json['sortBy'] as String,
      dateRange: json['dateRange'] as String?,
      minVotes: json['minVotes'] as int?,
      hasImage: json['hasImage'] as bool?,
    );
  }
}

/// Sort options enum
enum SortType {
  hot('HOT'),
  recent('RECENT'),
  popular('POPULAR'),
  commented('COMMENTED');

  final String value;
  const SortType(this.value);

  String get displayName {
    switch (this) {
      case SortType.hot:
        return '인기순 🔥';
      case SortType.recent:
        return '최신순';
      case SortType.popular:
        return '추천순 👍';
      case SortType.commented:
        return '댓글순 💬';
    }
  }
}
