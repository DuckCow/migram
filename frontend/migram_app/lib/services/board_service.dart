import '../models/board_model.dart';
import 'dio_service.dart';

class BoardService {
  final DioService _dioService = DioService();

  // Get all active boards (paginated)
  Future<List<Board>> getAllBoards({int page = 0, int size = 200}) async {
    final response = await _dioService.get(
      '/boards',
      queryParameters: {
        'page': page,
        'size': size,
      },
    );

    if (response.statusCode == 200) {
      final Map<String, dynamic> data = response.data;
      final List<dynamic> content = data['content'];
      return content.map((json) => Board.fromJson(json)).toList();
    } else {
      throw Exception('Failed to load boards: ${response.statusCode}');
    }
  }

  // Get boards by type (COUNTRY, CITY, TOPIC)
  Future<List<Board>> getBoardsByType(String boardType, {int page = 0, int size = 200}) async {
    final response = await _dioService.get(
      '/boards/type/$boardType',
      queryParameters: {
        'page': page,
        'size': size,
      },
    );

    if (response.statusCode == 200) {
      final Map<String, dynamic> data = response.data;
      final List<dynamic> content = data['content'];
      return content.map((json) => Board.fromJson(json)).toList();
    } else {
      throw Exception('Failed to load boards by type: ${response.statusCode}');
    }
  }

  // Get board by ID
  Future<Board> getBoardById(int boardId) async {
    final response = await _dioService.get('/boards/$boardId');

    if (response.statusCode == 200) {
      final Map<String, dynamic> data = response.data;
      return Board.fromJson(data);
    } else {
      throw Exception('Failed to load board: ${response.statusCode}');
    }
  }

  // Get board by context (e.g., "JAPAN", "KOREA_SOUTH")
  Future<Board?> getBoardByContext(String context) async {
    final response = await _dioService.get('/boards/context/$context');

    if (response.statusCode == 200) {
      final Map<String, dynamic> data = response.data;
      return Board.fromJson(data);
    } else if (response.statusCode == 404) {
      return null;
    } else {
      throw Exception('Failed to load board by context: ${response.statusCode}');
    }
  }
}
