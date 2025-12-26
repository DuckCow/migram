import 'package:flutter/material.dart';
import 'package:flutter/gestures.dart';
import 'package:timeago/timeago.dart' as timeago;
import '../models/comment_model.dart';
import '../services/user_service.dart';
import '../services/comment_service.dart';
import '../screens/user_profile_screen.dart';
import 'vote_button.dart';
import 'edit_comment_dialog.dart';

class CommentCard extends StatefulWidget {
  final Comment comment;
  final Function(int commentId, String voteType) onVote;
  final Function(Comment)? onReply;
  final Function()? onUpdate; // Callback when comment is updated/deleted
  final bool isParentInGroup; // True if this is a parent comment in a group

  const CommentCard({
    super.key,
    required this.comment,
    required this.onVote,
    this.onReply,
    this.onUpdate,
    this.isParentInGroup = false, // Default to false for backward compatibility
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
    // Determine if this is a reply for visual styling
    final isReply = !widget.isParentInGroup;

    // Smaller avatar for replies, normal for parents
    final avatarRadius = isReply ? 12.0 : 14.0;
    final avatarFontSize = isReply ? 10.0 : 12.0;

    return Container(
      padding: EdgeInsets.fromLTRB(
        isReply ? 12 : 16, // Slightly less padding for replies
        isReply ? 8 : 12,
        16,
        isReply ? 8 : 12,
      ),
      decoration: BoxDecoration(
        border: isReply
            ? null // No border for replies (grouped container has border)
            : Border(
                bottom: BorderSide(color: Colors.grey[200]!),
              ),
        color: isReply ? Colors.grey[50] : null, // Light background for replies
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header: Avatar, nickname, timestamp
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Avatar (tappable, smaller for replies)
              InkWell(
                onTap: () {
                  Navigator.of(context).push(
                    MaterialPageRoute(
                      builder: (context) =>
                          UserProfileScreen(userId: widget.comment.user.id),
                    ),
                  );
                },
                child: CircleAvatar(
                  radius: avatarRadius,
                  backgroundImage: widget.comment.user.profilePicture != null
                      ? NetworkImage(widget.comment.user.profilePicture!)
                      : null,
                  child: widget.comment.user.profilePicture == null
                      ? Text(
                          widget.comment.user.displayName[0].toUpperCase(),
                          style: TextStyle(fontSize: avatarFontSize),
                        )
                      : null,
                ),
              ),
              const SizedBox(width: 8),
              // Content area with Expanded to prevent overflow
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Nickname and timestamp
                    Row(
                      children: [
                        InkWell(
                          onTap: () {
                            Navigator.of(context).push(
                              MaterialPageRoute(
                                builder: (context) =>
                                    UserProfileScreen(userId: widget.comment.user.id),
                              ),
                            );
                          },
                          child: Text(
                            widget.comment.user.displayName,
                            style: const TextStyle(
                              fontWeight: FontWeight.bold,
                              fontSize: 13,
                            ),
                          ),
                        ),
                        const SizedBox(width: 6),
                        Text(
                          timeago.format(widget.comment.createdAt, locale: 'ko'),
                          style: const TextStyle(
                            color: Colors.grey,
                            fontSize: 12,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 4),
                    // Comment content (grayed out if deleted)
                    Text(
                      widget.comment.content,
                      style: TextStyle(
                        fontSize: isReply ? 13 : 14, // Slightly smaller for replies
                        color: widget.comment.isDeleted ? Colors.grey[600] : Colors.black87,
                        fontStyle: widget.comment.isDeleted ? FontStyle.italic : FontStyle.normal,
                      ),
                    ),
                    SizedBox(height: isReply ? 6 : 8), // Less spacing for replies
                    // Action buttons row (lighter for replies)
                    Row(
                      children: [
                        // Vote button (compact mode)
                        VoteButton(
                          upvotes: widget.comment.upvotes,
                          downvotes: widget.comment.downvotes,
                          currentUserVote: widget.comment.currentUserVote,
                          onVote: (voteType) => widget.onVote(widget.comment.id, voteType),
                          isCompact: true,
                        ),
                        const SizedBox(width: 8),
                        // Reply button (only show if onReply callback is provided)
                        if (widget.onReply != null)
                          TextButton.icon(
                            onPressed: () => widget.onReply!(widget.comment),
                            icon: const Icon(Icons.reply, size: 14),
                            label: const Text(
                              '답글',
                              style: TextStyle(fontSize: 12),
                            ),
                            style: TextButton.styleFrom(
                              padding: EdgeInsets.zero,
                              minimumSize: const Size(0, 24),
                              tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                            ),
                          ),
                        // Edit/Delete buttons (show inline for parents, popup for replies)
                        if (_isCommentOwner() && !widget.comment.isDeleted) ...[
                          if (widget.isParentInGroup) ...[
                            // Parent in group: show Edit/Delete inline
                            const SizedBox(width: 8),
                            TextButton.icon(
                              onPressed: _editComment,
                              icon: const Icon(Icons.edit, size: 14),
                              label: const Text(
                                '수정',
                                style: TextStyle(fontSize: 12),
                              ),
                              style: TextButton.styleFrom(
                                padding: EdgeInsets.zero,
                                minimumSize: const Size(0, 24),
                                tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                              ),
                            ),
                            const SizedBox(width: 8),
                            TextButton.icon(
                              onPressed: _deleteComment,
                              icon: const Icon(Icons.delete, size: 14, color: Colors.red),
                              label: const Text(
                                '삭제',
                                style: TextStyle(fontSize: 12, color: Colors.red),
                              ),
                              style: TextButton.styleFrom(
                                padding: EdgeInsets.zero,
                                minimumSize: const Size(0, 24),
                                tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                              ),
                            ),
                          ] else ...[
                            // Reply: show "..." menu
                            const SizedBox(width: 8),
                            PopupMenuButton<String>(
                              padding: EdgeInsets.zero,
                              icon: Icon(Icons.more_horiz, size: 16, color: Colors.grey[600]),
                              iconSize: 16,
                              onSelected: (value) {
                                if (value == 'edit') {
                                  _editComment();
                                } else if (value == 'delete') {
                                  _deleteComment();
                                }
                              },
                              itemBuilder: (context) => [
                                const PopupMenuItem(
                                  value: 'edit',
                                  child: Row(
                                    children: [
                                      Icon(Icons.edit, size: 16),
                                      SizedBox(width: 8),
                                      Text('수정'),
                                    ],
                                  ),
                                ),
                                const PopupMenuItem(
                                  value: 'delete',
                                  child: Row(
                                    children: [
                                      Icon(Icons.delete, size: 16, color: Colors.red),
                                      SizedBox(width: 8),
                                      Text('삭제', style: TextStyle(color: Colors.red)),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                          ],
                        ],
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
