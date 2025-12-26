import '../models/search_result_model.dart';
import 'dio_service.dart';

class SearchService {
  final DioService _dioService = DioService();

  /// Search posts with filters
  ///
  /// Parameters:
  /// - keyword: Search term (searches title and content)
  /// - boardIds: List of board IDs to filter by
  /// - sortBy: Sort type (HOT, RECENT, POPULAR, COMMENTED)
  /// - dateFrom: Start date (ISO format: 2025-01-01)
  /// - dateTo: End date (ISO format: 2025-12-31)
  /// - minVotes: Minimum net votes
  /// - hasImage: Filter posts with/without images
  /// - page, size: Pagination
  Future<SearchResult> search({
    String? keyword,
    List<int>? boardIds,
    SortType sortBy = SortType.hot,
    String? dateFrom,
    String? dateTo,
    int? minVotes,
    bool? hasImage,
    int page = 0,
    int size = 20,
  }) async {
    try {
      final queryParams = <String, dynamic>{
        'page': page,
        'size': size,
        'sortBy': sortBy.value,
      };

      // Add optional parameters
      if (keyword != null && keyword.isNotEmpty) {
        queryParams['keyword'] = keyword;
      }

      if (boardIds != null && boardIds.isNotEmpty) {
        queryParams['boardIds'] = boardIds.join(',');
      }

      if (dateFrom != null && dateFrom.isNotEmpty) {
        queryParams['dateFrom'] = dateFrom;
      }

      if (dateTo != null && dateTo.isNotEmpty) {
        queryParams['dateTo'] = dateTo;
      }

      if (minVotes != null) {
        queryParams['minVotes'] = minVotes;
      }

      if (hasImage != null) {
        queryParams['hasImage'] = hasImage;
      }

      final response = await _dioService.get(
        '/posts/search',
        queryParameters: queryParams,
      );

      if (response.statusCode == 200) {
        return SearchResult.fromJson(response.data as Map<String, dynamic>);
      }

      throw Exception('Failed to search posts: ${response.statusCode}');
    } catch (e) {
      print('Error searching posts: $e');
      rethrow;
    }
  }

  /// Quick search by keyword only
  Future<SearchResult> quickSearch(String keyword, {int page = 0}) async {
    return search(keyword: keyword, page: page);
  }

  /// Search by boards only (for board filter)
  Future<SearchResult> searchByBoards(
    List<int> boardIds, {
    SortType sortBy = SortType.hot,
    int page = 0,
  }) async {
    return search(boardIds: boardIds, sortBy: sortBy, page: page);
  }
}
