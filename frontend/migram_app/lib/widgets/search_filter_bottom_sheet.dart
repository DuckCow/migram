import 'package:flutter/material.dart';
import '../models/search_result_model.dart';

class SearchFilterBottomSheet extends StatefulWidget {
  final List<String>? selectedCountries;
  final SortType sortBy;
  final String? dateFrom;
  final String? dateTo;
  final int? minVotes;
  final bool? hasImage;
  final Function(
    List<String>? countries,
    SortType sortBy,
    String? dateFrom,
    String? dateTo,
    int? minVotes,
    bool? hasImage,
  ) onApply;

  const SearchFilterBottomSheet({
    super.key,
    this.selectedCountries,
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
  late List<String> _selectedCountries;
  late SortType _sortBy;
  late TextEditingController _minVotesController;
  bool? _hasImage;

  // Available countries
  static const List<Map<String, String>> _availableCountries = [
    {'code': 'JP', 'name': '일본', 'flag': '🇯🇵'},
    {'code': 'CA', 'name': '캐나다', 'flag': '🇨🇦'},
    {'code': 'KR', 'name': '한국', 'flag': '🇰🇷'},
    {'code': 'US', 'name': '미국', 'flag': '🇺🇸'},
    {'code': 'GB', 'name': '영국', 'flag': '🇬🇧'},
    {'code': 'AU', 'name': '호주', 'flag': '🇦🇺'},
  ];

  @override
  void initState() {
    super.initState();
    _selectedCountries = List.from(widget.selectedCountries ?? []);
    _sortBy = widget.sortBy;
    _minVotesController =
        TextEditingController(text: widget.minVotes?.toString() ?? '');
    _hasImage = widget.hasImage;
  }

  @override
  void dispose() {
    _minVotesController.dispose();
    super.dispose();
  }

  void _toggleCountry(String countryCode) {
    setState(() {
      if (_selectedCountries.contains(countryCode)) {
        _selectedCountries.remove(countryCode);
      } else {
        _selectedCountries.add(countryCode);
      }
    });
  }

  void _clearAllCountries() {
    setState(() {
      _selectedCountries.clear();
    });
  }

  void _resetFilters() {
    setState(() {
      _selectedCountries.clear();
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
      _selectedCountries.isEmpty ? null : _selectedCountries,
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
    return Container(
      decoration: BoxDecoration(
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
                    '필터',
                    style: TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  Spacer(),
                  TextButton(
                    onPressed: _resetFilters,
                    child: Text('초기화'),
                  ),
                ],
              ),
            ),
            Divider(height: 1),

            // Scrollable content
            Flexible(
              child: SingleChildScrollView(
                padding: EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Sort type section
                    _buildSectionTitle('정렬'),
                    SizedBox(height: 8),
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

                    SizedBox(height: 24),

                    // Countries section
                    Row(
                      children: [
                        _buildSectionTitle('국가'),
                        Spacer(),
                        if (_selectedCountries.isNotEmpty)
                          TextButton(
                            onPressed: _clearAllCountries,
                            child: Text('전체 해제'),
                          ),
                      ],
                    ),
                    SizedBox(height: 8),
                    Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: _availableCountries.map((country) {
                        final code = country['code']!;
                        final isSelected = _selectedCountries.contains(code);
                        return FilterChip(
                          label: Text('${country['flag']} ${country['name']}'),
                          selected: isSelected,
                          onSelected: (selected) => _toggleCountry(code),
                        );
                      }).toList(),
                    ),

                    SizedBox(height: 24),

                    // Min votes section
                    _buildSectionTitle('최소 추천 수'),
                    SizedBox(height: 8),
                    TextField(
                      controller: _minVotesController,
                      keyboardType: TextInputType.number,
                      decoration: InputDecoration(
                        hintText: '예: 10',
                        border: OutlineInputBorder(),
                        contentPadding: EdgeInsets.symmetric(
                          horizontal: 12,
                          vertical: 8,
                        ),
                      ),
                    ),

                    SizedBox(height: 24),

                    // Has image section
                    _buildSectionTitle('이미지 필터'),
                    SizedBox(height: 8),
                    Wrap(
                      spacing: 8,
                      children: [
                        ChoiceChip(
                          label: Text('전체'),
                          selected: _hasImage == null,
                          onSelected: (selected) {
                            if (selected) {
                              setState(() => _hasImage = null);
                            }
                          },
                        ),
                        ChoiceChip(
                          label: Text('이미지 있음'),
                          selected: _hasImage == true,
                          onSelected: (selected) {
                            if (selected) {
                              setState(() => _hasImage = true);
                            }
                          },
                        ),
                        ChoiceChip(
                          label: Text('이미지 없음'),
                          selected: _hasImage == false,
                          onSelected: (selected) {
                            if (selected) {
                              setState(() => _hasImage = false);
                            }
                          },
                        ),
                      ],
                    ),

                    SizedBox(height: 32),
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
                    padding: EdgeInsets.symmetric(vertical: 16),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(8),
                    ),
                  ),
                  child: Text(
                    '적용하기',
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
      style: TextStyle(
        fontSize: 16,
        fontWeight: FontWeight.bold,
      ),
    );
  }
}
