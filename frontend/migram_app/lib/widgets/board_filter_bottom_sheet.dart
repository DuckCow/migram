import 'package:flutter/material.dart';
import 'package:migram_app/l10n/app_localizations.dart';
import '../models/board_model.dart';
import '../services/board_service.dart';
import '../utils/error_handler.dart';

class BoardFilterBottomSheet extends StatefulWidget {
  final List<int>? initialSelectedBoardIds;

  const BoardFilterBottomSheet({
    super.key,
    this.initialSelectedBoardIds,
  });

  @override
  State<BoardFilterBottomSheet> createState() => _BoardFilterBottomSheetState();
}

class _BoardFilterBottomSheetState extends State<BoardFilterBottomSheet> {
  final BoardService _boardService = BoardService();
  List<Board> _boards = [];
  Set<int> _selectedBoardIds = {};
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _selectedBoardIds = Set.from(widget.initialSelectedBoardIds ?? []);
    _loadBoards();
  }

  Future<void> _loadBoards() async {
    setState(() {
      _isLoading = true;
    });

    try {
      final boards = await _boardService.getAllBoards();
      setState(() {
        _boards = boards;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _isLoading = false;
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

  void _clearAll() {
    setState(() {
      _selectedBoardIds.clear();
    });
  }

  void _apply() {
    Navigator.of(context).pop(_selectedBoardIds.isEmpty ? null : _selectedBoardIds.toList());
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // Handle bar
          Container(
            margin: const EdgeInsets.only(top: 12),
            width: 40,
            height: 4,
            decoration: BoxDecoration(
              color: Colors.grey[300],
              borderRadius: BorderRadius.circular(2),
            ),
          ),
          // Header
          Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  AppLocalizations.of(context)!.boardFilter,
                  style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                TextButton(
                  onPressed: _clearAll,
                  child: Text(AppLocalizations.of(context)!.clearAll),
                ),
              ],
            ),
          ),
          const Divider(height: 1),
          // Boards list
          Flexible(
            child: _isLoading
                ? const Center(
                    child: Padding(
                      padding: EdgeInsets.all(32),
                      child: CircularProgressIndicator(),
                    ),
                  )
                : _boards.isEmpty
                    ? Center(
                        child: Padding(
                          padding: const EdgeInsets.all(32),
                          child: Text(
                            AppLocalizations.of(context)!.noBoardsAvailable,
                            style: const TextStyle(color: Colors.grey),
                          ),
                        ),
                      )
                    : SingleChildScrollView(
                        child: Padding(
                          padding: const EdgeInsets.all(16),
                          child: Wrap(
                            spacing: 8,
                            runSpacing: 8,
                            children: _boards.map((board) {
                              final isSelected = _selectedBoardIds.contains(board.id);
                              final flag = board.getCountryFlag();

                              return FilterChip(
                                selected: isSelected,
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
                                onSelected: (selected) {
                                  _toggleBoard(board.id);
                                },
                                selectedColor: Theme.of(context).primaryColor.withOpacity(0.2),
                                checkmarkColor: Theme.of(context).primaryColor,
                              );
                            }).toList(),
                          ),
                        ),
                      ),
          ),
          // Action buttons
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              border: Border(
                top: BorderSide(color: Colors.grey[300]!),
              ),
            ),
            child: SafeArea(
              child: SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: _apply,
                  style: ElevatedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(vertical: 14),
                  ),
                  child: Text(
                    _selectedBoardIds.isEmpty
                        ? AppLocalizations.of(context)!.viewAll
                        : AppLocalizations.of(context)!.applyBoards(_selectedBoardIds.length),
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
