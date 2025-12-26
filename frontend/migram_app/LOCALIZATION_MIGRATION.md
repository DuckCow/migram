# Localization Migration Guide

## Problem
The Flutter app currently has **238 hardcoded Korean strings** across **24 files** and exposes raw exceptions to users in error messages.

## Solution Pattern

### 1. Error Handling (CRITICAL - Security/UX Issue)

**❌ WRONG:**
```dart
} catch (e) {
  print('Error: $e');
  ScaffoldMessenger.of(context).showSnackBar(
    SnackBar(content: Text('오류 발생: $e')),  // Exposes exception to user!
  );
}
```

**✅ CORRECT:**
```dart
} catch (e) {
  // Already done: error_handler.dart created
  ErrorHandler.showError(
    context,
    e,  // Logged to debugPrint automatically
    userMessage: ErrorMessages.loadPostsFailed,  // User-friendly message
  );
}
```

### 2. User-Facing Strings

**Current State:** Hardcoded Korean everywhere
**Temporary Fix:** Use English (already done in BoardFilterBottomSheet)
**Future:** Implement proper l10n/intl

**Example Migration:**
```dart
// Before
Text('게시판 필터')

// After (temporary)
Text('Board Filter')

// Future (with l10n)
Text(AppLocalizations.of(context).boardFilter)
```

---

## Files Fixed ✅

### `/lib/utils/error_handler.dart` - **NEW**
- Centralized error handling utility
- `ErrorHandler.showError()` - Logs to debugPrint, shows user-friendly message
- `ErrorMessages` class - Common error messages
- `SuccessMessages` class - Common success messages

### `/lib/widgets/board_filter_bottom_sheet.dart` - **UPDATED**
- ✅ Uses ErrorHandler
- ✅ All Korean strings → English
- ✅ No raw exceptions exposed

---

## Files Requiring Migration (Priority Order)

### HIGH PRIORITY (Board Migration Critical)

1. **`/lib/screens/feed_screen.dart`** (9 Korean strings)
   - Country filter → Board filter
   - Error handling for feed load failures

2. **`/lib/screens/create_post_screen.dart`** (23 Korean strings)
   - Country dropdown → Board dropdown
   - Form validation messages
   - Success/error messages

3. **`/lib/screens/edit_post_screen.dart`** (24 Korean strings)
   - Country dropdown → Board dropdown
   - Update success/error messages

4. **`/lib/screens/post_detail_screen.dart`** (18 Korean strings)
   - Comment section messages
   - Vote error handling
   - Delete confirmations

### MEDIUM PRIORITY

5. **`/lib/screens/profile_screen.dart`** (19 Korean strings)
6. **`/lib/screens/search_screen.dart`** (4 Korean strings)
7. **`/lib/widgets/comment_card.dart`** (11 Korean strings)
8. **`/lib/widgets/follow_button.dart`** (12 Korean strings)
9. **`/lib/widgets/edit_comment_dialog.dart`** (6 Korean strings)

### LOW PRIORITY (Not Board-Related)

10-24. Remaining screens/widgets (chat, profile, follow, etc.)

---

## Migration Checklist

For each file:

- [ ] Replace all Korean strings with English equivalents
- [ ] Replace all `print()` with `debugPrint()`
- [ ] Replace all raw exception exposure with `ErrorHandler.showError()`
- [ ] Use `ErrorMessages.*` constants where applicable
- [ ] Use `SuccessMessages.*` constants where applicable
- [ ] Test error scenarios to ensure no exceptions visible to users

---

## Common Patterns

### Snackbar Replacements

```dart
// Korean error message
ScaffoldMessenger.of(context).showSnackBar(
  SnackBar(content: Text('게시글을 불러오는데 실패했습니다')),
);

// English with ErrorHandler
ErrorHandler.showError(context, e, userMessage: ErrorMessages.loadPostsFailed);
```

```dart
// Korean success message
ScaffoldMessenger.of(context).showSnackBar(
  SnackBar(content: Text('게시글이 생성되었습니다')),
);

// English with ErrorHandler
ErrorHandler.showSuccess(context, SuccessMessages.postCreated);
```

### UI Text Replacements

| Korean | English |
|--------|---------|
| 게시판 필터 | Board Filter |
| 전체 해제 | Clear All |
| 전체 보기 | View All |
| 적용 | Apply |
| 취소 | Cancel |
| 저장 | Save |
| 삭제 | Delete |
| 수정 | Edit |
| 댓글 | Comments |
| 답글 | Reply |
| 좋아요 | Like |
| 공유 | Share |

---

## Future: Proper Localization

When ready to implement proper l10n:

1. Add dependencies to `pubspec.yaml`:
```yaml
dependencies:
  flutter_localizations:
    sdk: flutter
  intl: any
```

2. Generate ARB files for translations
3. Replace all hardcoded strings with `AppLocalizations.of(context).key`
4. Support Korean, English, and other languages

---

## Testing Checklist

- [ ] No Korean strings visible in production builds
- [ ] Error messages never show raw exceptions (e.g., `$e`, stack traces)
- [ ] All errors logged to console with `debugPrint()`
- [ ] User-facing error messages are clear and actionable
- [ ] Success messages confirm user actions

---

## Notes

- **DO NOT** expose implementation details (class names, stack traces, etc.) to users
- **DO** log detailed errors for debugging
- **DO** provide actionable error messages ("Please try again", "Check your connection")
- **DO NOT** use technical jargon in user messages ("Failed to deserialize JSON", etc.)
