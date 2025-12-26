import 'package:flutter/material.dart';
import 'package:flutter_gen/gen_l10n/app_localizations.dart';
import '../models/search_result_model.dart';
import '../models/board_model.dart';
import '../services/board_service.dart';
import '../utils/error_handler.dart';

class SearchFilterBottomSheet extends StatefulWidget {
  final List<int>? selectedBoardIds;
  final SortType sortBy;
  final String? dateFrom;
  final String? dateTo;
  final int? minVotes;
  final bool? hasImage;
  final Function(
    List<int>? boardIds,
    SortType sortBy,
    String? dateFrom,
    String? dateTo,
    int? minVotes,
    bool? hasImage,
  ) onApply;

  const SearchFilterBottomSheet({
    super.key,
    this.selectedBoardIds,
    required this.sortBy,
    this.dateFrom,
    this.dateTo,
    this.minVotes,
    this.hasImage,
    required this.onApply,
  });

  @override
  State<SearchFilterBottomSheet> createState() =>
      _SearchFilterBottomSheetState();
}

class _SearchFilterBottomSheetState extends State<SearchFilterBottomSheet> {
  final BoardService _boardService = BoardService();
  late Set<int> _selectedBoardIds;
  late SortType _sortBy;
  late TextEditingController _minVotesController;
  bool? _hasImage;
  List<Board> _boards = [];
  bool _isLoadingBoards = true;

  @override
  void initState() {
    super.initState();
    _selectedBoardIds = Set.from(widget.selectedBoardIds ?? []);
    _sortBy = widget.sortBy;
    _minVotesController =
        TextEditingController(text: widget.minVotes?.toString() ?? '');
    _hasImage = widget.hasImage;
    _loadBoards();
  }

  @override
  void dispose() {
    _minVotesController.dispose();
    super.dispose();
  }

  Future<void> _loadBoards() async {
    setState(() {
      _isLoadingBoards = true;
    });

    try {
      final boards = await _boardService.getAllBoards();
      setState(() {
        _boards = boards;
        _isLoadingBoards = false;
      });
    } catch (e) {
      setState(() {
        _isLoadingBoards = false;
      });
      if (mounted) {
        ErrorHandler.showError(
          context,
          e,
          userMessage: ErrorMessages.loadBoardsFailed(context),
        );
      }
    }
  }

  void _toggleBoard(int boardId) {
    setState(() {
      if (_selectedBoardIds.contains(boardId)) {
        _selectedBoardIds.remove(boardId);
      } else {
        _selectedBoardIds.add(boardId);
      }
    });
  }

  void _clearAllBoards() {
    setState(() {
      _selectedBoardIds.clear();
    });
  }

  void _resetFilters() {
    setState(() {
      _selectedBoardIds.clear();
      _sortBy = SortType.hot;
      _minVotesController.clear();
      _hasImage = null;
    });
  }

  void _applyFilters() {
    final minVotes = _minVotesController.text.isEmpty
        ? null
        : int.tryParse(_minVotesController.text);

    widget.onApply(
      _selectedBoardIds.isEmpty ? null : _selectedBoardIds.toList(),
      _sortBy,
      null, // dateFrom (not implemented in this version)
      null, // dateTo (not implemented in this version)
      minVotes,
      _hasImage,
    );

    Navigator.of(context).pop();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;

    return Container(
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.only(
          topLeft: Radius.circular(20),
          topRight: Radius.circular(20),
        ),
      ),
      child: SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Header
            Padding(
              padding: const EdgeInsets.all(16.0),
              child: Row(
                children: [
                  Text(
                    'Filter',
                    style: const TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const Spacer(),
                  TextButton(
                    onPressed: _resetFilters,
                    child: Text('Reset'),
                  ),
                ],
              ),
            ),
            const Divider(height: 1),

            // Scrollable content
            Flexible(
              child: _isLoadingBoards
                  ? const Center(child: CircularProgressIndicator())
                  : SingleChildScrollView(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          // Sort type section
                          _buildSectionTitle('Sort By'),
                          const SizedBox(height: 8),
                          Wrap(
                            spacing: 8,
                            children: SortType.values.map((sortType) {
                              final isSelected = _sortBy == sortType;
                              return ChoiceChip(
                                label: Text(sortType.displayName),
                                selected: isSelected,
                                onSelected: (selected) {
                                  if (selected) {
                                    setState(() {
                                      _sortBy = sortType;
                                    });
                                  }
                                },
                              );
                            }).toList(),
                          ),

                          const SizedBox(height: 24),

                          // Boards section
                          Row(
                            children: [
                              _buildSectionTitle(l10n.boards),
                              const Spacer(),
                              if (_selectedBoardIds.isNotEmpty)
                                TextButton(
                                  onPressed: _clearAllBoards,
                                  child: Text(l10n.clearAll),
                                ),
                            ],
                          ),
                          const SizedBox(height: 8),
                          Wrap(
                            spacing: 8,
                            runSpacing: 8,
                            children: _boards.map((board) {
                              final isSelected = _selectedBoardIds.contains(board.id);
                              final flag = board.getCountryFlag();
                              return FilterChip(
                                label: Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    if (flag != null) ...[
                                      Text(flag),
                                      const SizedBox(width: 4),
                                    ],
                                    Text(board.name),
                                  ],
                                ),
                                selected: isSelected,
                                onSelected: (selected) => _toggleBoard(board.id),
                              );
                            }).toList(),
                          ),

                          const SizedBox(height: 24),

                          // Min votes section
                          _buildSectionTitle('Minimum Votes'),
                          const SizedBox(height: 8),
                          TextField(
                            controller: _minVotesController,
                            keyboardType: TextInputType.number,
                            decoration: const InputDecoration(
                              hintText: 'e.g. 10',
                              border: OutlineInputBorder(),
                              contentPadding: EdgeInsets.symmetric(
                                horizontal: 12,
                                vertical: 8,
                              ),
                            ),
                          ),

                          const SizedBox(height: 24),

                          // Has image section
                          _buildSectionTitle('Image Filter'),
                          const SizedBox(height: 8),
                          Wrap(
                            spacing: 8,
                            children: [
                              ChoiceChip(
                                label: Text(l10n.all),
                                selected: _hasImage == null,
                                onSelected: (selected) {
                                  if (selected) {
                                    setState(() => _hasImage = null);
                                  }
                                },
                              ),
                              ChoiceChip(
                                label: const Text('With Image'),
                                selected: _hasImage == true,
                                onSelected: (selected) {
                                  if (selected) {
                                    setState(() => _hasImage = true);
                                  }
                                },
                              ),
                              ChoiceChip(
                                label: const Text('Without Image'),
                                selected: _hasImage == false,
                                onSelected: (selected) {
                                  if (selected) {
                                    setState(() => _hasImage = false);
                                  }
                                },
                              ),
                            ],
                          ),

                          const SizedBox(height: 32),
                        ],
                      ),
                    ),
            ),

            // Apply button
            Padding(
              padding: const EdgeInsets.all(16.0),
              child: SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: _applyFilters,
                  style: ElevatedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(vertical: 16),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(8),
                    ),
                  ),
                  child: const Text(
                    'Apply',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSectionTitle(String title) {
    return Text(
      title,
      style: const TextStyle(
        fontSize: 16,
        fontWeight: FontWeight.bold,
      ),
    );
  }
}
