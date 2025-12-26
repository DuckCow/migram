class Board {
  final int id;
  final String name;
  final String? description;
  final String boardType;
  final String? context;
  final String status;
  final int postCount;

  Board({
    required this.id,
    required this.name,
    this.description,
    required this.boardType,
    this.context,
    required this.status,
    this.postCount = 0,
  });

  factory Board.fromJson(Map<String, dynamic> json) {
    return Board(
      id: json['id'],
      name: json['name'],
      description: json['description'],
      boardType: json['boardType'],
      context: json['context'],
      status: json['status'],
      postCount: json['postCount'] ?? 0,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'description': description,
      'boardType': boardType,
      'context': context,
      'status': status,
      'postCount': postCount,
    };
  }

  // Get country flag emoji from context (for COUNTRY type boards)
  String? getCountryFlag() {
    if (boardType != 'COUNTRY' || context == null) return null;

    final Map<String, String> countryFlags = {
      'JAPAN': '🇯🇵',
      'KOREA_SOUTH': '🇰🇷',
      'CANADA': '🇨🇦',
      'UNITED_STATES': '🇺🇸',
      'UNITED_KINGDOM': '🇬🇧',
      'FRANCE': '🇫🇷',
      'GERMANY': '🇩🇪',
      'ITALY': '🇮🇹',
      'SPAIN': '🇪🇸',
      'AUSTRALIA': '🇦🇺',
      'CHINA': '🇨🇳',
      'TAIWAN': '🇹🇼',
      'HONG_KONG': '🇭🇰',
      'SINGAPORE': '🇸🇬',
      'THAILAND': '🇹🇭',
    };

    return countryFlags[context] ?? '🌍';
  }

  @override
  String toString() => 'Board(id: $id, name: $name, type: $boardType)';
}
