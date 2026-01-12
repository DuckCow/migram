import 'package:flutter/material.dart';
import 'package:migram_app/l10n/app_localizations.dart';
import 'package:timeago/timeago.dart' as timeago;
import '../models/post_model.dart';
import '../screens/user_profile_screen.dart';
import '../services/translation_service.dart';
import '../utils/error_handler.dart';
import 'vote_button.dart';

class PostCard extends StatefulWidget {
  final Post post;
  final VoidCallback onTap;
  final Function(String) onVote;

  const PostCard({
    super.key,
    required this.post,
    required this.onTap,
    required this.onVote,
  });

  @override
  State<PostCard> createState() => _PostCardState();
}

class _PostCardState extends State<PostCard> {
  final TranslationService _translationService = TranslationService();

  // Translation state (REPLACE mode)
  bool _showTranslation = false;
  bool _isTranslating = false;
  String? _translatedText; // Combined title + content

  Future<void> _toggleTranslation() async {
    if (_showTranslation) {
      // Hide translation - switch back to original
      setState(() {
        _showTranslation = false;
      });
      return;
    }

    // Show translation
    if (_translatedText != null) {
      // Already have cached translation
      setState(() {
        _showTranslation = true;
      });
      return;
    }

    // Skip if same language
    final targetLang = Localizations.localeOf(context).languageCode;
    if (widget.post.originalLang == targetLang) {
      return;
    }

    // Need to fetch translation
    setState(() {
      _isTranslating = true;
    });

    try {
      final response = await _translationService.getTranslation(
        contentType: 'POST',
        contentId: widget.post.id,
        targetLang: targetLang,
      );

      if (mounted) {
        setState(() {
          _translatedText = response.translatedText;
          _showTranslation = true;
          _isTranslating = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isTranslating = false;
        });
        ErrorHandler.showError(
          context,
          e,
          userMessage: AppLocalizations.of(context)!.errorTranslate,
        );
      }
    }
  }

  // Parse translated text into title and content
  // Backend returns "[LANG] Title\n\nContent" format
  (String, String) _parseTranslation() {
    if (_translatedText == null || !_showTranslation) {
      return (widget.post.title, widget.post.content);
    }

    // Split by double newline
    final parts = _translatedText!.split('\n\n');

    if (parts.length < 2) {
      // No double newline found - treat entire text as title
      return (_translatedText!, widget.post.content);
    }

    final title = parts[0];
    final content = parts.sublist(1).join('\n\n');

    return (title, content);
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;

    return GestureDetector(
      onTap: widget.onTap,
      child: Card(
        margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Header: Avatar, nickname, timestamp, board name
              Row(
                children: [
                  // Avatar and Nickname (tappable if not anonymous)
                  Expanded(
                    child: InkWell(
                      onTap: widget.post.isAnonymous || widget.post.user == null
                          ? null
                          : () {
                              Navigator.of(context).push(
                                MaterialPageRoute(
                                  builder: (context) =>
                                      UserProfileScreen(userId: widget.post.user!.id),
                                ),
                              );
                            },
                      child: Row(
                        children: [
                          // Avatar
                          CircleAvatar(
                            radius: 16,
                            backgroundColor:
                                widget.post.isAnonymous ? Colors.grey[400] : null,
                            backgroundImage: !widget.post.isAnonymous &&
                                    widget.post.user?.profilePicture != null
                                ? NetworkImage(widget.post.user!.profilePicture!)
                                : null,
                            child: widget.post.isAnonymous
                                ? const Icon(
                                    Icons.person,
                                    size: 18,
                                    color: Colors.white,
                                  )
                                : (widget.post.user?.profilePicture == null
                                    ? Text(
                                        widget.post.user!.displayName[0].toUpperCase(),
                                        style: const TextStyle(fontSize: 14),
                                      )
                                    : null),
                          ),
                          const SizedBox(width: 8),
                          // Nickname or "Anonymous"
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  widget.post.isAnonymous
                                      ? 'Anonymous'
                                      : widget.post.user!.displayName,
                                  style: TextStyle(
                                    fontWeight: FontWeight.bold,
                                    fontSize: 14,
                                    color: widget.post.isAnonymous
                                        ? Colors.grey[600]
                                        : null,
                                  ),
                                ),
                                Text(
                                  timeago.format(widget.post.createdAt, locale: 'ko'),
                                  style: const TextStyle(
                                    color: Colors.grey,
                                    fontSize: 12,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                  // Board name
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                    decoration: BoxDecoration(
                      color: Colors.grey[200],
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(
                      widget.post.boardName,
                      style: const TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.w500,
                        color: Colors.black87,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              // Title row with translate button
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Title (REPLACE mode - show original OR translated)
                  Expanded(
                    child: Text(
                      _showTranslation && _translatedText != null
                          ? _parseTranslation().$1 // Translated title
                          : widget.post.title, // Original title
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                      ),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  // Translate button (only if language differs)
                  if (widget.post.originalLang != Localizations.localeOf(context).languageCode) ...[
                    const SizedBox(width: 8),
                    _isTranslating
                        ? const SizedBox(
                            width: 20,
                            height: 20,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : InkWell(
                            onTap: _toggleTranslation,
                            child: Icon(
                              _showTranslation ? Icons.translate_outlined : Icons.translate,
                              size: 20,
                              color: _showTranslation ? Colors.blue : Colors.grey[600],
                            ),
                          ),
                  ],
                ],
              ),
              const SizedBox(height: 6),
              // Content preview (REPLACE mode - show original OR translated)
              // Always limited to 3 lines to save translation costs
              Text(
                _showTranslation && _translatedText != null
                    ? _parseTranslation().$2 // Translated content
                    : widget.post.content, // Original content
                style: const TextStyle(fontSize: 14),
                maxLines: 3,
                overflow: TextOverflow.ellipsis,
              ),
              // Image preview (if exists)
              if (widget.post.imageUrl != null) ...[
                const SizedBox(height: 8),
                ClipRRect(
                  borderRadius: BorderRadius.circular(8),
                  child: Image.network(
                    widget.post.imageUrl!,
                    height: 200,
                    width: double.infinity,
                    fit: BoxFit.cover,
                    errorBuilder: (context, error, stackTrace) {
                      return Container(
                        height: 200,
                        color: Colors.grey[300],
                        child: const Center(
                          child: Icon(Icons.error, color: Colors.grey),
                        ),
                      );
                    },
                  ),
                ),
              ],
              const SizedBox(height: 12),
              // Footer: Vote buttons and comment count
              Row(
                children: [
                  VoteButton(
                    upvotes: widget.post.upvotes,
                    downvotes: widget.post.downvotes,
                    currentUserVote: widget.post.currentUserVote,
                    onVote: widget.onVote,
                    isCompact: false,
                  ),
                  const Spacer(),
                  Row(
                    children: [
                      const Icon(
                        Icons.comment_outlined,
                        size: 18,
                        color: Colors.grey,
                      ),
                      const SizedBox(width: 4),
                      Text(
                        '${widget.post.commentCount}',
                        style: const TextStyle(
                          color: Colors.grey,
                          fontSize: 14,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
