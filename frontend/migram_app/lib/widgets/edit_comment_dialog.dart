import 'package:flutter/material.dart';
import '../models/comment_model.dart';
import '../services/comment_service.dart';

class EditCommentDialog extends StatefulWidget {
  final Comment comment;

  const EditCommentDialog({
    super.key,
    required this.comment,
  });

  @override
  State<EditCommentDialog> createState() => _EditCommentDialogState();
}

class _EditCommentDialogState extends State<EditCommentDialog> {
  late final TextEditingController _contentController;
  final CommentService _commentService = CommentService();
  bool _isSubmitting = false;

  @override
  void initState() {
    super.initState();
    _contentController = TextEditingController(text: widget.comment.content);
  }

  @override
  void dispose() {
    _contentController.dispose();
    super.dispose();
  }

  Future<void> _submitEdit() async {
    final content = _contentController.text.trim();
    if (content.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('댓글 내용을 입력해주세요')),
      );
      return;
    }

    setState(() {
      _isSubmitting = true;
    });

    try {
      await _commentService.updateComment(
        commentId: widget.comment.id,
        postId: widget.comment.postId,
        content: content,
      );

      if (mounted) {
        Navigator.of(context).pop(true); // Return true to indicate success
      }
    } catch (e) {
      print('Error updating comment: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('댓글 수정 실패: $e')),
        );
        setState(() {
          _isSubmitting = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('댓글 수정'),
      content: TextField(
        controller: _contentController,
        decoration: const InputDecoration(
          hintText: '댓글 내용을 입력하세요',
          border: OutlineInputBorder(),
        ),
        maxLines: 5,
        maxLength: 500,
        autofocus: true,
      ),
      actions: [
        TextButton(
          onPressed: _isSubmitting ? null : () => Navigator.of(context).pop(),
          child: const Text('취소'),
        ),
        TextButton(
          onPressed: _isSubmitting ? null : _submitEdit,
          child: _isSubmitting
              ? const SizedBox(
                  width: 16,
                  height: 16,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : const Text('수정'),
        ),
      ],
    );
  }
}
