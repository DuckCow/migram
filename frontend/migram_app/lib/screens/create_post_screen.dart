import 'dart:io';
import 'package:flutter/material.dart';
import 'package:migram_app/l10n/app_localizations.dart';
import 'package:image_picker/image_picker.dart';
import '../models/board_model.dart';
import '../services/post_service.dart';
import '../services/board_service.dart';
import '../utils/error_handler.dart';

class CreatePostScreen extends StatefulWidget {
  const CreatePostScreen({super.key});

  @override
  State<CreatePostScreen> createState() => _CreatePostScreenState();
}

class _CreatePostScreenState extends State<CreatePostScreen> {
  final _formKey = GlobalKey<FormState>();
  final _titleController = TextEditingController();
  final _contentController = TextEditingController();
  final PostService _postService = PostService();
  final BoardService _boardService = BoardService();
  final ImagePicker _imagePicker = ImagePicker();

  int? _selectedBoardId;
  List<Board> _boards = [];
  File? _selectedImage;
  bool _isSubmitting = false;
  bool _isAnonymous = false;
  bool _isLoadingBoards = true;

  @override
  void initState() {
    super.initState();
    _loadBoards();
  }

  @override
  void dispose() {
    _titleController.dispose();
    _contentController.dispose();
    super.dispose();
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
      setState(() {
        _isLoadingBoards = false;
      });
      if (mounted) {
        ErrorHandler.showError(
          context,
          e,
          userMessage: ErrorMessages.loadBoardsFailed(context),
        );
      }
    }
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
          _selectedImage = File(pickedFile.path);
        });
      }
    } catch (e) {
      if (mounted) {
        ErrorHandler.showError(
          context,
          e,
          userMessage: AppLocalizations.of(context)!.errorPickImage,
        );
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
      _selectedImage = null;
    });
  }

  Future<void> _submitPost() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }

    if (_selectedBoardId == null) {
      ErrorHandler.showInfo(context, AppLocalizations.of(context)!.pleaseSelectBoard);
      return;
    }

    setState(() {
      _isSubmitting = true;
    });

    try {
      // Use different method depending on whether image is selected
      if (_selectedImage != null) {
        await _postService.createPostWithImage(
          title: _titleController.text.trim(),
          content: _contentController.text.trim(),
          boardId: _selectedBoardId!,
          imageFile: _selectedImage,
          isAnonymous: _isAnonymous,
        );
      } else {
        await _postService.createPost(
          title: _titleController.text.trim(),
          content: _contentController.text.trim(),
          boardId: _selectedBoardId!,
          isAnonymous: _isAnonymous,
        );
      }

      if (mounted) {
        Navigator.of(context).pop(true); // Return true to indicate success
        ErrorHandler.showSuccess(context, SuccessMessages.postCreated(context));
      }
    } catch (e) {
      if (mounted) {
        ErrorHandler.showError(
          context,
          e,
          userMessage: ErrorMessages.createPostFailed(context),
        );
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
        title: Text(AppLocalizations.of(context)!.createPost),
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
                AppLocalizations.of(context)!.post,
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
                      final flag = board.getCountryFlag();
                      return DropdownMenuItem(
                        value: board.id,
                        child: Row(
                          children: [
                            if (flag != null) ...[
                              Text(flag),
                              const SizedBox(width: 8),
                            ],
                            Text(board.name),
                          ],
                        ),
                      );
                    }).toList(),
                    onChanged: (value) {
                      setState(() {
                        _selectedBoardId = value;
                      });
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

            // Image picker section
            if (_selectedImage != null) ...[
              // Image preview
              Container(
                height: 300,
                decoration: BoxDecoration(
                  border: Border.all(color: Colors.grey[300]!),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Stack(
                  children: [
                    // Image
                    ClipRRect(
                      borderRadius: BorderRadius.circular(8),
                      child: Image.file(
                        _selectedImage!,
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
                        onPressed: _removeImage,
                        style: IconButton.styleFrom(
                          backgroundColor: Colors.black54,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
            ] else ...[
              // Image picker button
              OutlinedButton.icon(
                onPressed: _showImageSourceDialog,
                icon: const Icon(Icons.add_photo_alternate),
                label: Text(AppLocalizations.of(context)!.addImageOptional),
                style: OutlinedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 16),
                ),
              ),
              const SizedBox(height: 16),
            ],

            // Anonymous checkbox
            CheckboxListTile(
              value: _isAnonymous,
              onChanged: (value) {
                setState(() {
                  _isAnonymous = value ?? false;
                });
              },
              title: Text(AppLocalizations.of(context)!.postAnonymously),
              subtitle: Text(AppLocalizations.of(context)!.anonymousPostInfo),
              controlAffinity: ListTileControlAffinity.leading,
              contentPadding: EdgeInsets.zero,
            ),
            const SizedBox(height: 8),

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
