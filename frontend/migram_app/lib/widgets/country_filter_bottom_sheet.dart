import 'package:flutter/material.dart';

class CountryFilterBottomSheet extends StatefulWidget {
  final List<String>? initialSelectedCountries;

  const CountryFilterBottomSheet({
    super.key,
    this.initialSelectedCountries,
  });

  @override
  State<CountryFilterBottomSheet> createState() => _CountryFilterBottomSheetState();
}

class _CountryFilterBottomSheetState extends State<CountryFilterBottomSheet> {
  late Set<String> _selectedCountries;

  // 주요 국가 목록 (실제로는 백엔드 API에서 가져올 수도 있음)
  final Map<String, String> _countries = {
    'JP': '🇯🇵 일본',
    'KR': '🇰🇷 한국',
    'CA': '🇨🇦 캐나다',
    'US': '🇺🇸 미국',
    'GB': '🇬🇧 영국',
    'FR': '🇫🇷 프랑스',
    'DE': '🇩🇪 독일',
    'IT': '🇮🇹 이탈리아',
    'ES': '🇪🇸 스페인',
    'AU': '🇦🇺 호주',
    'CN': '🇨🇳 중국',
    'TW': '🇹🇼 대만',
    'HK': '🇭🇰 홍콩',
    'SG': '🇸🇬 싱가포르',
    'TH': '🇹🇭 태국',
  };

  @override
  void initState() {
    super.initState();
    _selectedCountries = widget.initialSelectedCountries?.toSet() ?? {};
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

  void _clearAll() {
    setState(() {
      _selectedCountries.clear();
    });
  }

  void _apply() {
    Navigator.of(context).pop(_selectedCountries.isEmpty ? null : _selectedCountries.toList());
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
      ),
      child: SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Header
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  '국가 선택',
                  style: TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                TextButton(
                  onPressed: _clearAll,
                  child: const Text('전체 해제'),
                ),
              ],
            ),
            const SizedBox(height: 16),

            // Country chips
            Flexible(
              child: SingleChildScrollView(
                child: Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: _countries.entries.map((entry) {
                    final isSelected = _selectedCountries.contains(entry.key);
                    return FilterChip(
                      label: Text(entry.value),
                      selected: isSelected,
                      onSelected: (_) => _toggleCountry(entry.key),
                      selectedColor: Colors.blue.withOpacity(0.2),
                      checkmarkColor: Colors.blue,
                    );
                  }).toList(),
                ),
              ),
            ),

            const SizedBox(height: 16),

            // Apply button
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: _apply,
                style: ElevatedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 16),
                  backgroundColor: Colors.blue,
                  foregroundColor: Colors.white,
                ),
                child: Text(
                  _selectedCountries.isEmpty
                      ? '전체 보기'
                      : '${_selectedCountries.length}개 국가 적용',
                  style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
