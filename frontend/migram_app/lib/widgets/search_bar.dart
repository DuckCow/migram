import 'package:flutter/material.dart';
import '../screens/search_screen.dart';

/// SearchBar widget that navigates to search screen when tapped
/// This is a fake search bar that doesn't accept input directly
class SearchBarWidget extends StatelessWidget {
  final String? initialQuery;
  final List<String>? initialCountries;

  const SearchBarWidget({
    super.key,
    this.initialQuery,
    this.initialCountries,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () {
        // Navigate to search screen
        Navigator.of(context).push(
          MaterialPageRoute(
            builder: (context) => SearchScreen(
              initialQuery: initialQuery,
              initialCountries: initialCountries,
            ),
          ),
        );
      },
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        decoration: BoxDecoration(
          color: Colors.grey[200],
          borderRadius: BorderRadius.circular(24),
          border: Border.all(color: Colors.grey[300]!),
        ),
        child: Row(
          children: [
            Icon(Icons.search, color: Colors.grey[600], size: 20),
            const SizedBox(width: 8),
            Expanded(
              child: Text(
                initialQuery ?? '게시물 검색...',
                style: TextStyle(
                  color: Colors.grey[600],
                  fontSize: 14,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
