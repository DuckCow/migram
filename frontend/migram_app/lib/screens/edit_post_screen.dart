import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_gen/gen_l10n/app_localizations.dart';
import 'package:image_picker/image_picker.dart';
import '../models/post_model.dart';
import '../models/board_model.dart';
import '../services/post_service.dart';
import '../services/board_service.dart';
import '../utils/error_handler.dart';

class EditPostScreen extends StatefulWidget {
  final Post post;

  const EditPostScreen({
    super.key,
    required this.post,
  });

  @override
  State<EditPostScreen> createState() => _EditPostScreenState();
}

class _EditPostScreenState extends State<EditPostScreen> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _titleController;
  late final TextEditingController _contentController;
  final PostService _postService = PostService();
  final BoardService _boardService = BoardService();
  final ImagePicker _imagePicker = ImagePicker();

  late int _selectedBoardId;
  List<Board> _boards = [];
  File? _newImage; // New image selected by user
  bool _keepExistingImage = true; // Keep existing image or remove it
  bool _isSubmitting = false;
  bool _isLoadingBoards = true;

  @override
  void initState() {
    super.initState();
    // Pre-fill with existing post data
    _titleController = TextEditingController(text: widget.post.title);
    _contentController = TextEditingController(text: widget.post.content);
    _selectedBoardId = widget.post.boardId;
    _loadBoards();
  }

  Future<void> _loadBoards() async {
    setState(() {
      _isLoadingBoards = true;
    });

    try {
      final boards = await _boardService.getAllBoards();
      setState(() {
        _boards = boards;
        _isLoadingBoards = false;
      });
    } catch (e) {
      debugPrint('Error loading boards: $e');
      setState(() {
        _isLoadingBoards = false;
      });
      if (mounted) {
        ErrorHandler.showError(context, e, userMessage: ErrorMessages.loadBoardsFailed(context));
      }
    }
  }

  @override
  void dispose() {
    _titleController.dispose();
    _contentController.dispose();
    super.dispose();
  }

  Future<void> _pickImage(ImageSource source) async {
    try {
      final XFile? pickedFile = await _imagePicker.pickImage(
        source: source,
        maxWidth: 1920,
        maxHeight: 1920,
        imageQuality: 85,
      );

      if (pickedFile != null) {
        setState(() {
          _newImage = File(pickedFile.path);
          _keepExistingImage = false; // New image replaces old one
        });
      }
    } catch (e) {
      debugPrint('Error picking image: $e');
      if (mounted) {
        ErrorHandler.showError(context, e, userMessage: AppLocalizations.of(context)!.errorPickImage);
      }
    }
  }

  void _showImageSourceDialog() {
    final l10n = AppLocalizations.of(context)!;
    showModalBottomSheet(
      context: context,
      builder: (context) => SafeArea(
        child: Wrap(
          children: [
            ListTile(
              leading: const Icon(Icons.photo_library),
              title: Text(l10n.chooseFromGallery),
              onTap: () {
                Navigator.of(context).pop();
                _pickImage(ImageSource.gallery);
              },
            ),
            ListTile(
              leading: const Icon(Icons.camera_alt),
              title: Text(l10n.takePhoto),
              onTap: () {
                Navigator.of(context).pop();
                _pickImage(ImageSource.camera);
              },
            ),
          ],
        ),
      ),
    );
  }

  void _removeImage() {
    setState(() {
      _newImage = null;
      _keepExistingImage = false; // Remove existing image
    });
  }

  void _restoreExistingImage() {
    setState(() {
      _newImage = null;
      _keepExistingImage = true; // Keep existing image
    });
  }

  Future<void> _submitPost() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }

    setState(() {
      _isSubmitting = true;
    });

    try {
      // Use multipart endpoint if there's a new image or image state changed
      if (_newImage != null || !_keepExistingImage) {
        await _postService.updatePostWithImage(
          postId: widget.post.id,
          title: _titleController.text.trim(),
          content: _contentController.text.trim(),
          boardId: _selectedBoardId,
          imageFile: _newImage,
          keepExistingImage: _keepExistingImage,
        );
      } else {
        // No image changes, use regular update
        await _postService.updatePost(
          postId: widget.post.id,
          title: _titleController.text.trim(),
          content: _contentController.text.trim(),
          boardId: _selectedBoardId,
          imageUrl: widget.post.imageUrl,
        );
      }

      if (mounted) {
        Navigator.of(context).pop(true); // Return true to indicate success
        ErrorHandler.showSuccess(context, SuccessMessages.postUpdated(context));
      }
    } catch (e) {
      debugPrint('Error updating post: $e');
      if (mounted) {
        ErrorHandler.showError(context, e, userMessage: ErrorMessages.updatePostFailed(context));
      }
    } finally {
      if (mounted) {
        setState(() {
          _isSubmitting = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(AppLocalizations.of(context)!.editPost),
        actions: [
          if (_isSubmitting)
            const Center(
              child: Padding(
                padding: EdgeInsets.all(16),
                child: SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(strokeWidth: 2),
                ),
              ),
            )
          else
            TextButton(
              onPressed: _submitPost,
              child: Text(
                AppLocalizations.of(context)!.save,
                style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
              ),
            ),
        ],
      ),
      body: Form(
        key: _formKey,
        child: _isLoadingBoards
            ? const Center(child: CircularProgressIndicator())
            : ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  // Board dropdown
                  DropdownButtonFormField<int>(
                    value: _selectedBoardId,
                    decoration: InputDecoration(
                      labelText: AppLocalizations.of(context)!.boardRequired,
                      border: const OutlineInputBorder(),
                      hintText: AppLocalizations.of(context)!.selectBoard,
                    ),
                    items: _boards.map((board) {
                      return DropdownMenuItem(
                        value: board.id,
                        child: Text(board.name),
                      );
                    }).toList(),
                    onChanged: (value) {
                      if (value != null) {
                        setState(() {
                          _selectedBoardId = value;
                        });
                      }
                    },
                    validator: (value) {
                      if (value == null) {
                        return AppLocalizations.of(context)!.pleaseSelectBoard;
                      }
                      return null;
                    },
                  ),
                  const SizedBox(height: 16),

            // Title input
            TextFormField(
              controller: _titleController,
              decoration: InputDecoration(
                labelText: AppLocalizations.of(context)!.titleLabel,
                border: const OutlineInputBorder(),
                hintText: AppLocalizations.of(context)!.titleHint,
              ),
              maxLength: 100,
              validator: (value) {
                if (value == null || value.trim().isEmpty) {
                  return AppLocalizations.of(context)!.titleValidation;
                }
                if (value.trim().length < 2) {
                  return AppLocalizations.of(context)!.titleMinLength;
                }
                return null;
              },
            ),
            const SizedBox(height: 16),

            // Content input
            TextFormField(
              controller: _contentController,
              decoration: InputDecoration(
                labelText: AppLocalizations.of(context)!.contentLabel,
                border: const OutlineInputBorder(),
                hintText: AppLocalizations.of(context)!.contentHint,
                alignLabelWithHint: true,
              ),
              maxLines: 10,
              maxLength: 5000,
              validator: (value) {
                if (value == null || value.trim().isEmpty) {
                  return AppLocalizations.of(context)!.contentValidation;
                }
                if (value.trim().length < 5) {
                  return AppLocalizations.of(context)!.contentMinLength;
                }
                return null;
              },
            ),
            const SizedBox(height: 16),

            // Image management section
            if (_newImage != null) ...[
              // New image preview
              Container(
                height: 300,
                decoration: BoxDecoration(
                  border: Border.all(color: Colors.grey[300]!),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Stack(
                  children: [
                    // New image
                    ClipRRect(
                      borderRadius: BorderRadius.circular(8),
                      child: Image.file(
                        _newImage!,
                        width: double.infinity,
                        height: 300,
                        fit: BoxFit.cover,
                      ),
                    ),
                    // Remove button
                    Positioned(
                      top: 8,
                      right: 8,
                      child: IconButton(
                        icon: const Icon(Icons.close, color: Colors.white),
                        onPressed: widget.post.imageUrl != null
                            ? _restoreExistingImage
                            : _removeImage,
                        style: IconButton.styleFrom(
                          backgroundColor: Colors.black54,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 8),
              Text(
                AppLocalizations.of(context)!.newImageSelected,
                style: const TextStyle(fontSize: 12, color: Colors.blue),
              ),
              const SizedBox(height: 16),
            ] else if (widget.post.imageUrl != null && _keepExistingImage) ...[
              // Existing image preview
              Container(
                height: 300,
                decoration: BoxDecoration(
                  border: Border.all(color: Colors.grey[300]!),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Stack(
                  children: [
                    // Existing image
                    ClipRRect(
                      borderRadius: BorderRadius.circular(8),
                      child: Image.network(
                        widget.post.imageUrl!,
                        width: double.infinity,
                        height: 300,
                        fit: BoxFit.cover,
                        errorBuilder: (context, error, stackTrace) {
                          return Container(
                            color: Colors.grey[200],
                            child: const Center(
                              child: Icon(Icons.broken_image, size: 50),
                            ),
                          );
                        },
                      ),
                    ),
                    // Remove button
                    Positioned(
                      top: 8,
                      right: 8,
                      child: IconButton(
                        icon: const Icon(Icons.close, color: Colors.white),
                        onPressed: _removeImage,
                        style: IconButton.styleFrom(
                          backgroundColor: Colors.black54,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 8),
              OutlinedButton.icon(
                onPressed: _showImageSourceDialog,
                icon: const Icon(Icons.edit),
                label: Text(AppLocalizations.of(context)!.changeImage),
                style: OutlinedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 12),
                ),
              ),
              const SizedBox(height: 16),
            ] else ...[
              // No image - show add button
              OutlinedButton.icon(
                onPressed: _showImageSourceDialog,
                icon: const Icon(Icons.add_photo_alternate),
                label: Text(
                  widget.post.imageUrl != null
                      ? AppLocalizations.of(context)!.addImageExistingRemoved
                      : AppLocalizations.of(context)!.addImageOptional,
                ),
                style: OutlinedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 16),
                ),
              ),
              if (widget.post.imageUrl != null && !_keepExistingImage) ...[
                const SizedBox(height: 8),
                TextButton.icon(
                  onPressed: _restoreExistingImage,
                  icon: const Icon(Icons.undo),
                  label: Text(AppLocalizations.of(context)!.restoreExistingImage),
                ),
              ],
              const SizedBox(height: 16),
            ],

                  // Helper text
                  Text(
                    AppLocalizations.of(context)!.requiredFields,
                    style: const TextStyle(
                      fontSize: 12,
                      color: Colors.grey,
                    ),
                  ),
                ],
              ),
      ),
    );
  }
}
