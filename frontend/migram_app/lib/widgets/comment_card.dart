import 'package:flutter/material.dart';
import 'package:timeago/timeago.dart' as timeago;
import '../models/comment_model.dart';
import '../services/user_service.dart';
import '../services/comment_service.dart';
import 'vote_button.dart';
import 'edit_comment_dialog.dart';

class CommentCard extends StatefulWidget {
  final Comment comment;
  final Function(int commentId, String voteType) onVote;
  final Function(Comment)? onReply;
  final Function()? onUpdate; // Callback when comment is updated/deleted
  final int depth;

  const CommentCard({
    super.key,
    required this.comment,
    required this.onVote,
    this.onReply,
    this.onUpdate,
    this.depth = 0,
  });

  @override
  State<CommentCard> createState() => _CommentCardState();
}

class _CommentCardState extends State<CommentCard> {
  final UserService _userService = UserService();
  final CommentService _commentService = CommentService();
  int? _currentUserId;

  @override
  void initState() {
    super.initState();
    _loadCurrentUser();
  }

  Future<void> _loadCurrentUser() async {
    final user = await _userService.getCurrentUser();
    if (mounted) {
      setState(() {
        _currentUserId = user?.id;
      });
    }
  }

  Future<void> _editComment() async {
    final result = await showDialog<bool>(
      context: context,
      builder: (context) => EditCommentDialog(comment: widget.comment),
    );

    if (result == true && widget.onUpdate != null) {
      widget.onUpdate!();
    }
  }

  Future<void> _deleteComment() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('댓글 삭제'),
        content: const Text('정말로 이 댓글을 삭제하시겠습니까?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('취소'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            style: TextButton.styleFrom(foregroundColor: Colors.red),
            child: const Text('삭제'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      try {
        await _commentService.deleteComment(widget.comment.id);
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('댓글이 삭제되었습니다')),
          );
          if (widget.onUpdate != null) {
            widget.onUpdate!();
          }
        }
      } catch (e) {
        print('Error deleting comment: $e');
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('댓글 삭제 실패: $e')),
          );
        }
      }
    }
  }

  bool _isCommentOwner() {
    return _currentUserId != null && _currentUserId == widget.comment.user.id;
  }

  @override
  Widget build(BuildContext context) {
    final leftPadding = 16.0 + (widget.depth * 24.0); // Indent nested replies

    return Container(
      padding: EdgeInsets.fromLTRB(leftPadding, 12, 16, 12),
      decoration: BoxDecoration(
        border: Border(
          bottom: BorderSide(color: Colors.grey[200]!),
          left: widget.depth > 0
              ? BorderSide(
                  color: Colors.grey[300]!,
                  width: 2,
                )
              : BorderSide.none,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header: Avatar, nickname, timestamp
          Row(
            children: [
              // Avatar
              CircleAvatar(
                radius: 14,
                backgroundImage: widget.comment.user.profilePicture != null
                    ? NetworkImage(widget.comment.user.profilePicture!)
                    : null,
                child: widget.comment.user.profilePicture == null
                    ? Text(
                        widget.comment.user.displayName[0].toUpperCase(),
                        style: const TextStyle(fontSize: 12),
                      )
                    : null,
              ),
              const SizedBox(width: 8),
              // Nickname
              Text(
                widget.comment.user.displayName,
                style: const TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 13,
                ),
              ),
              const SizedBox(width: 6),
              // Timestamp
              Text(
                timeago.format(widget.comment.createdAt, locale: 'ko'),
                style: const TextStyle(
                  color: Colors.grey,
                  fontSize: 12,
                ),
              ),
              const Spacer(),
              // Vote button (compact mode)
              VoteButton(
                upvotes: widget.comment.upvotes,
                downvotes: widget.comment.downvotes,
                currentUserVote: widget.comment.currentUserVote,
                onVote: (voteType) => widget.onVote(widget.comment.id, voteType),
                isCompact: true,
              ),
            ],
          ),
          const SizedBox(height: 8),
          // Comment content
          Text(
            widget.comment.content,
            style: const TextStyle(fontSize: 14),
          ),
          // Action buttons row
          const SizedBox(height: 4),
          Row(
            children: [
              // Reply button
              if (widget.onReply != null)
                TextButton.icon(
                  onPressed: () => widget.onReply!(widget.comment),
                  icon: const Icon(Icons.reply, size: 16),
                  label: Text(
                    widget.comment.replyCount > 0
                        ? '답글 ${widget.comment.replyCount}개'
                        : '답글',
                    style: const TextStyle(fontSize: 12),
                  ),
                  style: TextButton.styleFrom(
                    padding: EdgeInsets.zero,
                    minimumSize: const Size(0, 28),
                    tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                  ),
                ),
              // Edit button (only for owner)
              if (_isCommentOwner()) ...[
                const SizedBox(width: 8),
                TextButton.icon(
                  onPressed: _editComment,
                  icon: const Icon(Icons.edit, size: 16),
                  label: const Text(
                    '수정',
                    style: TextStyle(fontSize: 12),
                  ),
                  style: TextButton.styleFrom(
                    padding: EdgeInsets.zero,
                    minimumSize: const Size(0, 28),
                    tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                  ),
                ),
                const SizedBox(width: 8),
                TextButton.icon(
                  onPressed: _deleteComment,
                  icon: const Icon(Icons.delete, size: 16, color: Colors.red),
                  label: const Text(
                    '삭제',
                    style: TextStyle(fontSize: 12, color: Colors.red),
                  ),
                  style: TextButton.styleFrom(
                    padding: EdgeInsets.zero,
                    minimumSize: const Size(0, 28),
                    tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                  ),
                ),
              ],
            ],
          ),
          // Nested replies
          if (widget.comment.replies.isNotEmpty) ...[
            const SizedBox(height: 8),
            ...widget.comment.replies.map((reply) => CommentCard(
                  comment: reply,
                  onVote: widget.onVote,
                  onReply: widget.onReply,
                  onUpdate: widget.onUpdate,
                  depth: widget.depth + 1,
                )),
          ],
        ],
      ),
    );
  }
}
