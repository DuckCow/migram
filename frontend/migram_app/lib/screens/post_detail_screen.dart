import 'package:flutter/material.dart';
import 'package:flutter_gen/gen_l10n/app_localizations.dart';
import 'package:timeago/timeago.dart' as timeago;
import '../models/post_model.dart';
import '../models/comment_model.dart';
import '../services/post_service.dart';
import '../services/comment_service.dart';
import '../services/user_service.dart';
import '../services/translation_service.dart';
import '../widgets/vote_button.dart';
import '../widgets/comment_card.dart';
import '../utils/error_handler.dart';
import 'edit_post_screen.dart';
import 'user_profile_screen.dart';

class PostDetailScreen extends StatefulWidget {
  final int postId;

  const PostDetailScreen({
    super.key,
    required this.postId,
  });

  @override
  State<PostDetailScreen> createState() => _PostDetailScreenState();
}

class _PostDetailScreenState extends State<PostDetailScreen> {
  final PostService _postService = PostService();
  final CommentService _commentService = CommentService();
  final UserService _userService = UserService();
  final TranslationService _translationService = TranslationService();
  final TextEditingController _commentController = TextEditingController();
  final FocusNode _commentFocusNode = FocusNode();

  Post? _post;
  List<Comment> _comments = []; // Top-level comments with nested replies[]
  bool _isLoading = true;
  bool _isSubmitting = false;
  Comment? _replyingTo; // Track which comment is being replied to
  int? _currentUserId; // Current logged-in user ID

  // Translation state (REPLACE mode)
  bool _showPostTranslation = false;
  bool _isTranslatingPost = false;
  String? _translatedPostText;

  @override
  void initState() {
    super.initState();
    _loadCurrentUser();
    _loadPostAndComments();
  }

  Future<void> _loadCurrentUser() async {
    final user = await _userService.getCurrentUser();
    setState(() {
      _currentUserId = user?.id;
    });
  }

  @override
  void dispose() {
    _commentController.dispose();
    _commentFocusNode.dispose();
    super.dispose();
  }

  Future<void> _loadPostAndComments() async {
    setState(() {
      _isLoading = true;
    });

    try {
      final post = await _postService.getPostById(widget.postId);
      final comments = await _commentService.getCommentsByPost(
        postId: widget.postId,
      );

      setState(() {
        _post = post;
        _comments = comments; // Top-level comments with nested replies[]
        _isLoading = false;
      });
    } catch (e) {
      debugPrint('Error loading post details: $e');
      setState(() {
        _isLoading = false;
      });
      if (mounted) {
        ErrorHandler.showError(context, e, userMessage: ErrorMessages.loadPostsFailed(context));
      }
    }
  }

  Future<void> _handlePostVote(String voteType) async {
    if (_post == null) return;

    try {
      await _postService.votePost(_post!.id, voteType);
      await _loadPostAndComments();
    } catch (e) {
      debugPrint('Error voting on post: $e');
      if (mounted) {
        ErrorHandler.showError(context, e, userMessage: ErrorMessages.voteFailed(context));
      }
    }
  }

  Future<void> _handleCommentVote(int commentId, String voteType) async {
    try {
      await _commentService.voteComment(commentId, voteType);
      await _loadPostAndComments();
    } catch (e) {
      debugPrint('Error voting on comment: $e');
      if (mounted) {
        ErrorHandler.showError(context, e, userMessage: ErrorMessages.voteFailed(context));
      }
    }
  }

  void _startReplyingTo(Comment comment) {
    setState(() {
      _replyingTo = comment;
    });
    _commentFocusNode.requestFocus();
  }

  void _cancelReply() {
    setState(() {
      _replyingTo = null;
    });
  }

  Future<void> _submitComment() async {
    if (_commentController.text.trim().isEmpty) {
      return;
    }

    setState(() {
      _isSubmitting = true;
    });

    try {
      await _commentService.createComment(
        postId: widget.postId,
        content: _commentController.text.trim(),
        parentCommentId: _replyingTo?.id,
      );

      _commentController.clear();
      _commentFocusNode.unfocus();
      setState(() {
        _replyingTo = null;
      });

      await _loadPostAndComments();

      setState(() {
        _isSubmitting = false;
      });
    } catch (e) {
      debugPrint('Error submitting comment: $e');
      setState(() {
        _isSubmitting = false;
      });
      if (mounted) {
        // Show user-friendly error for depth violation
        String errorMessage = ErrorMessages.createCommentFailed(context);
        if (e.toString().contains('Depth limit') || e.toString().contains('reply to a reply')) {
          errorMessage = AppLocalizations.of(context)!.cannotReplyToReply;
        }
        ErrorHandler.showError(context, e, userMessage: errorMessage);
      }
    }
  }

  Future<void> _editPost() async {
    if (_post == null) return;

    final result = await Navigator.of(context).push<bool>(
      MaterialPageRoute(
        builder: (context) => EditPostScreen(post: _post!),
      ),
    );

    if (result == true) {
      _loadPostAndComments();
    }
  }

  Future<void> _deletePost() async {
    if (_post == null) return;

    final l10n = AppLocalizations.of(context)!;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(l10n.deletePost),
        content: Text(l10n.confirmDeletePost),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: Text(l10n.cancel),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            style: TextButton.styleFrom(foregroundColor: Colors.red),
            child: Text(l10n.delete),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      try {
        await _postService.deletePost(_post!.id);
        if (mounted) {
          Navigator.of(context).pop(true); // Go back to feed
          ErrorHandler.showSuccess(context, SuccessMessages.postDeleted(context));
        }
      } catch (e) {
        debugPrint('Error deleting post: $e');
        if (mounted) {
          ErrorHandler.showError(context, e, userMessage: ErrorMessages.deletePostFailed(context));
        }
      }
    }
  }

  bool _isPostOwner() {
    return _currentUserId != null && _post != null && !_post!.isAnonymous && _currentUserId == _post!.user?.id;
  }

  Future<void> _togglePostTranslation() async {
    if (_showPostTranslation) {
      // Hide translation - switch back to original
      setState(() {
        _showPostTranslation = false;
      });
      return;
    }

    // Show translation
    if (_translatedPostText != null) {
      // Already have cached translation, just show it
      setState(() {
        _showPostTranslation = true;
      });
      return;
    }

    // Need to fetch translation
    setState(() {
      _isTranslatingPost = true;
    });

    try {
      final locale = Localizations.localeOf(context).languageCode;

      final response = await _translationService.getTranslation(
        contentType: 'POST',
        contentId: widget.postId,
        targetLang: locale,
      );

      if (mounted) {
        setState(() {
          _translatedPostText = response.translatedText;
          _showPostTranslation = true;
          _isTranslatingPost = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isTranslatingPost = false;
        });
        ErrorHandler.showError(
          context,
          e,
          userMessage: AppLocalizations.of(context)!.errorTranslate,
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(AppLocalizations.of(context)!.post),
        actions: _isPostOwner()
            ? [
                IconButton(
                  icon: const Icon(Icons.edit),
                  onPressed: _editPost,
                  tooltip: AppLocalizations.of(context)!.edit,
                ),
                IconButton(
                  icon: const Icon(Icons.delete),
                  onPressed: _deletePost,
                  tooltip: AppLocalizations.of(context)!.delete,
                ),
              ]
            : null,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _post == null
              ? Center(child: Text(AppLocalizations.of(context)!.postNotFound))
              : Column(
                  children: [
                    // Post content and comments list
                    Expanded(
                      child: ListView(
                        children: [
                          // Post content
                          _buildPostContent(),
                          const Divider(height: 1, thickness: 1),
                          // Comments header
                          Padding(
                            padding: const EdgeInsets.all(16),
                            child: Text(
                              AppLocalizations.of(context)!.commentsCount(_comments.length),
                              style: const TextStyle(
                                fontWeight: FontWeight.bold,
                                fontSize: 16,
                              ),
                            ),
                          ),
                          // Comments list (Instagram-style: top-level with nested replies)
                          ..._comments.map((comment) => _CommentGroupWidget(
                                key: ValueKey(comment.id),
                                comment: comment,
                                onVote: _handleCommentVote,
                                onReply: _startReplyingTo,
                                onUpdate: _loadPostAndComments,
                                currentUserId: _currentUserId,
                              )),
                        ],
                      ),
                    ),
                    // Comment input at bottom
                    _buildCommentInput(),
                  ],
                ),
    );
  }

  Widget _buildPostContent() {
    if (_post == null) return const SizedBox.shrink();

    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header: Avatar, nickname, timestamp, country flag
          Row(
            children: [
              // Avatar and user info (tappable if not anonymous)
              Expanded(
                child: InkWell(
                  onTap: _post!.isAnonymous || _post!.user == null
                      ? null
                      : () {
                          Navigator.of(context).push(
                            MaterialPageRoute(
                              builder: (context) =>
                                  UserProfileScreen(userId: _post!.user!.id),
                            ),
                          );
                        },
                  child: Row(
                    children: [
                      CircleAvatar(
                        radius: 18,
                        backgroundColor:
                            _post!.isAnonymous ? Colors.grey[400] : null,
                        backgroundImage: !_post!.isAnonymous &&
                                _post!.user?.profilePicture != null
                            ? NetworkImage(_post!.user!.profilePicture!)
                            : null,
                        child: _post!.isAnonymous
                            ? const Icon(
                                Icons.person,
                                size: 20,
                                color: Colors.white,
                              )
                            : (_post!.user?.profilePicture == null
                                ? Text(
                                    _post!.user!.displayName[0].toUpperCase(),
                                    style: const TextStyle(fontSize: 16),
                                  )
                                : null),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              _post!.isAnonymous
                                  ? AppLocalizations.of(context)!.anonymous
                                  : _post!.user!.displayName,
                              style: TextStyle(
                                fontWeight: FontWeight.bold,
                                fontSize: 15,
                                color: _post!.isAnonymous
                                    ? Colors.grey[600]
                                    : null,
                              ),
                            ),
                            Text(
                              timeago.format(_post!.createdAt, locale: 'ko'),
                              style: const TextStyle(
                                color: Colors.grey,
                                fontSize: 13,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
          // Board name badge
          Container(
            margin: const EdgeInsets.only(top: 8),
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
            decoration: BoxDecoration(
              color: Colors.grey[200],
              borderRadius: BorderRadius.circular(12),
            ),
            child: Text(
              _post!.boardName,
              style: const TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w500,
                color: Colors.black87,
              ),
            ),
          ),
          const SizedBox(height: 16),
          // Title
          Text(
            _post!.title,
            style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 12),
          // Content - REPLACE mode (show original OR translated)
          Text(
            _showPostTranslation && _translatedPostText != null
                ? _translatedPostText!
                : _post!.content,
            style: const TextStyle(fontSize: 16),
          ),
          // Translation toggle button (only if language differs)
          if (_post!.originalLang != Localizations.localeOf(context).languageCode) ...[
            const SizedBox(height: 12),
            TextButton.icon(
              onPressed: _isTranslatingPost ? null : _togglePostTranslation,
              icon: _isTranslatingPost
                  ? const SizedBox(
                      width: 16,
                      height: 16,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : Icon(
                      _showPostTranslation ? Icons.translate_outlined : Icons.translate,
                      size: 18,
                    ),
              label: Text(
                _isTranslatingPost
                    ? AppLocalizations.of(context)!.translating
                    : (_showPostTranslation
                        ? AppLocalizations.of(context)!.hideTranslation
                        : AppLocalizations.of(context)!.seeTranslation),
                style: const TextStyle(fontSize: 14),
              ),
              style: TextButton.styleFrom(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                minimumSize: const Size(0, 32),
              ),
            ),
          ],
          // Image (if exists)
          if (_post!.imageUrl != null) ...[
            const SizedBox(height: 16),
            ClipRRect(
              borderRadius: BorderRadius.circular(8),
              child: Image.network(
                _post!.imageUrl!,
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
          const SizedBox(height: 20),
          // Vote buttons
          VoteButton(
            upvotes: _post!.upvotes,
            downvotes: _post!.downvotes,
            currentUserVote: _post!.currentUserVote,
            onVote: _handlePostVote,
            isCompact: false,
          ),
        ],
      ),
    );
  }

  Widget _buildCommentInput() {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        border: Border(
          top: BorderSide(color: Colors.grey[300]!),
        ),
      ),
      padding: EdgeInsets.only(
        left: 16,
        right: 16,
        top: 8,
        bottom: MediaQuery.of(context).viewInsets.bottom + 8,
      ),
      child: SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Reply indicator
            if (_replyingTo != null) ...[
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: Colors.blue[50],
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.reply, size: 16, color: Colors.blue),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        '${AppLocalizations.of(context)!.replyingTo} ${_replyingTo!.user.displayName}',
                        style: const TextStyle(
                          fontSize: 13,
                          color: Colors.blue,
                        ),
                      ),
                    ),
                    IconButton(
                      icon: const Icon(Icons.close, size: 18),
                      onPressed: _cancelReply,
                      padding: EdgeInsets.zero,
                      constraints: const BoxConstraints(),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 8),
            ],
            // Comment input
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _commentController,
                    focusNode: _commentFocusNode,
                    decoration: InputDecoration(
                      hintText: _replyingTo != null
                          ? AppLocalizations.of(context)!.writeReply
                          : AppLocalizations.of(context)!.writeComment,
                      border: const OutlineInputBorder(),
                      contentPadding: const EdgeInsets.symmetric(
                        horizontal: 12,
                        vertical: 10,
                      ),
                    ),
                    maxLines: null,
                    textInputAction: TextInputAction.send,
                    onSubmitted: (_) => _submitComment(),
                  ),
                ),
                const SizedBox(width: 8),
                _isSubmitting
                    ? const SizedBox(
                        width: 40,
                        height: 40,
                        child: Center(
                          child: CircularProgressIndicator(strokeWidth: 2),
                        ),
                      )
                    : IconButton(
                        icon: const Icon(Icons.send),
                        onPressed: _submitComment,
                        color: Theme.of(context).primaryColor,
                      ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

// Instagram-style comment group widget: renders a parent comment + its replies
class _CommentGroupWidget extends StatefulWidget {
  final Comment comment; // Top-level comment with nested replies[]
  final Function(int commentId, String voteType) onVote;
  final Function(Comment)? onReply;
  final Function()? onUpdate;
  final int? currentUserId;

  const _CommentGroupWidget({
    super.key,
    required this.comment,
    required this.onVote,
    this.onReply,
    this.onUpdate,
    this.currentUserId,
  });

  @override
  State<_CommentGroupWidget> createState() => _CommentGroupWidgetState();
}

class _CommentGroupWidgetState extends State<_CommentGroupWidget> {
  bool _repliesExpanded = true; // Show replies by default for better UX

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Parent comment (top-level)
        CommentCard(
          key: ValueKey('parent-${widget.comment.id}'),
          comment: widget.comment,
          onVote: widget.onVote,
          onReply: widget.onReply,
          onUpdate: widget.onUpdate,
          isParentInGroup: true,
        ),

        // Instagram-style "View replies (N)" toggle row
        if (widget.comment.replies.isNotEmpty)
          InkWell(
            onTap: () {
              setState(() {
                _repliesExpanded = !_repliesExpanded;
              });
            },
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 48, vertical: 8),
              child: Row(
                children: [
                  Container(
                    width: 24,
                    height: 1,
                    color: Colors.grey[400],
                  ),
                  const SizedBox(width: 8),
                  Text(
                    _repliesExpanded
                        ? AppLocalizations.of(context)!.hideReplies
                        : AppLocalizations.of(context)!.viewReplies(widget.comment.replies.length),
                    style: TextStyle(
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                      color: Colors.grey[700],
                    ),
                  ),
                ],
              ),
            ),
          ),

        // Replies list (indented, Instagram-style) with clear visual boundary
        if (_repliesExpanded && widget.comment.replies.isNotEmpty) ...[
          Container(
            margin: const EdgeInsets.only(left: 28), // Indent for visual distinction
            decoration: BoxDecoration(
              border: Border(
                left: BorderSide(
                  color: Colors.grey[300]!,
                  width: 2,
                ),
              ),
            ),
            child: Column(
              children: [
                ...widget.comment.replies.map((reply) => CommentCard(
                      key: ValueKey('reply-${reply.id}'),
                      comment: reply,
                      onVote: widget.onVote,
                      onReply: null, // No reply button on replies (depth=1)
                      onUpdate: widget.onUpdate,
                      isParentInGroup: false, // Signal this is a reply
                    )),
              ],
            ),
          ),
          // Divider AFTER the reply group to separate from next parent comment
          Divider(
            height: 1,
            thickness: 1,
            color: Colors.grey[400],
            indent: 16,
            endIndent: 16,
          ),
        ],
      ],
    );
  }
}
