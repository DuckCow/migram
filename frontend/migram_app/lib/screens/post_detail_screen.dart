import 'package:flutter/material.dart';
import 'package:timeago/timeago.dart' as timeago;
import '../models/post_model.dart';
import '../models/comment_model.dart';
import '../services/post_service.dart';
import '../services/comment_service.dart';
import '../services/user_service.dart';
import '../widgets/vote_button.dart';
import '../widgets/comment_card.dart';
import 'edit_post_screen.dart';

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
  final TextEditingController _commentController = TextEditingController();
  final FocusNode _commentFocusNode = FocusNode();

  Post? _post;
  List<Comment> _comments = [];
  bool _isLoading = true;
  bool _isSubmitting = false;
  Comment? _replyingTo; // Track which comment is being replied to
  int? _currentUserId; // Current logged-in user ID

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
        _comments = comments;
        _isLoading = false;
      });
    } catch (e) {
      print('Error loading post details: $e');
      setState(() {
        _isLoading = false;
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('게시글을 불러오는데 실패했습니다: $e')),
        );
      }
    }
  }

  Future<void> _handlePostVote(String voteType) async {
    if (_post == null) return;

    try {
      await _postService.votePost(_post!.id, voteType);
      await _loadPostAndComments();
    } catch (e) {
      print('Error voting on post: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('투표에 실패했습니다')),
        );
      }
    }
  }

  Future<void> _handleCommentVote(int commentId, String voteType) async {
    try {
      await _commentService.voteComment(commentId, voteType);
      await _loadPostAndComments();
    } catch (e) {
      print('Error voting on comment: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('투표에 실패했습니다')),
        );
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
        parentCommentId: _replyingTo?.id, // Add parentCommentId if replying
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
      print('Error submitting comment: $e');
      setState(() {
        _isSubmitting = false;
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('댓글 작성에 실패했습니다: $e')),
        );
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

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('게시글 삭제'),
        content: const Text('정말로 이 게시글을 삭제하시겠습니까?'),
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
        await _postService.deletePost(_post!.id);
        if (mounted) {
          Navigator.of(context).pop(true); // Go back to feed
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('게시글이 삭제되었습니다')),
          );
        }
      } catch (e) {
        print('Error deleting post: $e');
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('게시글 삭제 실패: $e')),
          );
        }
      }
    }
  }

  bool _isPostOwner() {
    return _currentUserId != null && _post != null && _currentUserId == _post!.user.id;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('게시글'),
        actions: _isPostOwner()
            ? [
                IconButton(
                  icon: const Icon(Icons.edit),
                  onPressed: _editPost,
                  tooltip: '수정',
                ),
                IconButton(
                  icon: const Icon(Icons.delete),
                  onPressed: _deletePost,
                  tooltip: '삭제',
                ),
              ]
            : null,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _post == null
              ? const Center(child: Text('게시글을 찾을 수 없습니다'))
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
                              '댓글 ${_comments.length}개',
                              style: const TextStyle(
                                fontWeight: FontWeight.bold,
                                fontSize: 16,
                              ),
                            ),
                          ),
                          // Comments list
                          ..._comments.map((comment) => CommentCard(
                                comment: comment,
                                onVote: _handleCommentVote,
                                onReply: _startReplyingTo,
                                onUpdate: _loadPostAndComments,
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
              CircleAvatar(
                radius: 18,
                backgroundImage: _post!.user.profilePicture != null
                    ? NetworkImage(_post!.user.profilePicture!)
                    : null,
                child: _post!.user.profilePicture == null
                    ? Text(
                        _post!.user.displayName[0].toUpperCase(),
                        style: const TextStyle(fontSize: 16),
                      )
                    : null,
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      _post!.user.displayName,
                      style: const TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 15,
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
              Text(
                _post!.getCountryFlag(),
                style: const TextStyle(fontSize: 24),
              ),
            ],
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
          // Content
          Text(
            _post!.content,
            style: const TextStyle(fontSize: 16),
          ),
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
                        '${_replyingTo!.user.displayName}님에게 답글',
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
                          ? '답글을 입력하세요...'
                          : '댓글을 입력하세요...',
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
