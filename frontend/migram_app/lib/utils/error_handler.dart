import 'package:flutter/material.dart';
import 'package:flutter_gen/gen_l10n/app_localizations.dart';

/// Centralized error handling utility with localization support
class ErrorHandler {
  /// Show user-friendly error message
  /// Logs detailed error to debug console only
  static void showError(
    BuildContext context,
    dynamic error, {
    String? userMessage,
  }) {
    debugPrint('Error occurred: $error');

    if (context.mounted) {
      final l10n = AppLocalizations.of(context);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(userMessage ?? l10n!.errorGeneric),
          backgroundColor: Colors.red[700],
        ),
      );
    }
  }

  /// Show success message
  static void showSuccess(BuildContext context, String message) {
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(message),
          backgroundColor: Colors.green[700],
        ),
      );
    }
  }

  /// Show info message
  static void showInfo(BuildContext context, String message) {
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(message)),
      );
    }
  }
}

/// Common error messages using localization
class ErrorMessages {
  static String loadBoardsFailed(BuildContext context) => AppLocalizations.of(context)!.errorLoadBoards;
  static String loadPostsFailed(BuildContext context) => AppLocalizations.of(context)!.errorLoadPosts;
  static String createPostFailed(BuildContext context) => AppLocalizations.of(context)!.errorCreatePost;
  static String updatePostFailed(BuildContext context) => AppLocalizations.of(context)!.errorUpdatePost;
  static String deletePostFailed(BuildContext context) => AppLocalizations.of(context)!.errorDeletePost;
  static String voteFailed(BuildContext context) => AppLocalizations.of(context)!.errorVote;
  static String loadCommentsFailed(BuildContext context) => AppLocalizations.of(context)!.errorLoadComments;
  static String createCommentFailed(BuildContext context) => AppLocalizations.of(context)!.errorCreateComment;
  static String updateCommentFailed(BuildContext context) => AppLocalizations.of(context)!.errorUpdateComment;
  static String deleteCommentFailed(BuildContext context) => AppLocalizations.of(context)!.errorDeleteComment;
  static String networkError(BuildContext context) => AppLocalizations.of(context)!.errorNetwork;
  static String unknownError(BuildContext context) => AppLocalizations.of(context)!.errorGeneric;
}

/// Common success messages using localization
class SuccessMessages {
  static String postCreated(BuildContext context) => AppLocalizations.of(context)!.successPostCreated;
  static String postUpdated(BuildContext context) => AppLocalizations.of(context)!.successPostUpdated;
  static String postDeleted(BuildContext context) => AppLocalizations.of(context)!.successPostDeleted;
  static String commentCreated(BuildContext context) => AppLocalizations.of(context)!.successCommentCreated;
  static String commentUpdated(BuildContext context) => AppLocalizations.of(context)!.successCommentUpdated;
  static String commentDeleted(BuildContext context) => AppLocalizations.of(context)!.successCommentDeleted;
}
