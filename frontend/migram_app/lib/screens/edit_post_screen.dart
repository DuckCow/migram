import 'dart:io';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import '../models/post_model.dart';
import '../services/post_service.dart';

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
  final ImagePicker _imagePicker = ImagePicker();

  late String _selectedCountry;
  File? _newImage; // New image selected by user
  bool _keepExistingImage = true; // Keep existing image or remove it
  bool _isSubmitting = false;

  // 주요 국가 목록 (백엔드 Country enum의 code 값 사용)
  final Map<String, String> _countries = {
    'JP': '🇯🇵 일본',
    'KR': '🇰🇷 한국',
    'CA': '🇨🇦 캐나다',
    'US': '🇺🇸 미국',
    'GB': '🇬🇧 영국',
    'FR': '🇫🇷 프랑스',
    'DE': '🇩🇪 독일',
    'IT': '🇮🇹 이탈리아',
    'ES': '🇪🇸 스페인',
    'AU': '🇦🇺 호주',
    'CN': '🇨🇳 중국',
    'TW': '🇹🇼 대만',
    'HK': '🇭🇰 홍콩',
    'SG': '🇸🇬 싱가포르',
    'TH': '🇹🇭 태국',
  };

  @override
  void initState() {
    super.initState();
    // Pre-fill with existing post data
    _titleController = TextEditingController(text: widget.post.title);
    _contentController = TextEditingController(text: widget.post.content);
    _selectedCountry = widget.post.country;
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
      print('Error picking image: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('이미지 선택 실패: $e')),
        );
      }
    }
  }

  void _showImageSourceDialog() {
    showModalBottomSheet(
      context: context,
      builder: (context) => SafeArea(
        child: Wrap(
          children: [
            ListTile(
              leading: const Icon(Icons.photo_library),
              title: const Text('갤러리에서 선택'),
              onTap: () {
                Navigator.of(context).pop();
                _pickImage(ImageSource.gallery);
              },
            ),
            ListTile(
              leading: const Icon(Icons.camera_alt),
              title: const Text('카메라로 촬영'),
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
          country: _selectedCountry,
          imageFile: _newImage,
          keepExistingImage: _keepExistingImage,
        );
      } else {
        // No image changes, use regular update
        await _postService.updatePost(
          postId: widget.post.id,
          title: _titleController.text.trim(),
          content: _contentController.text.trim(),
          country: _selectedCountry,
          imageUrl: widget.post.imageUrl,
        );
      }

      if (mounted) {
        Navigator.of(context).pop(true); // Return true to indicate success
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('게시글이 수정되었습니다')),
        );
      }
    } catch (e) {
      print('Error updating post: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('게시글 수정 실패: $e')),
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
        title: const Text('게시글 수정'),
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
              child: const Text(
                '완료',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
              ),
            ),
        ],
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            // Country dropdown
            DropdownButtonFormField<String>(
              value: _selectedCountry,
              decoration: const InputDecoration(
                labelText: '국가 *',
                border: OutlineInputBorder(),
                hintText: '국가를 선택하세요',
              ),
              items: _countries.entries.map((entry) {
                return DropdownMenuItem(
                  value: entry.key,
                  child: Text(entry.value),
                );
              }).toList(),
              onChanged: (value) {
                if (value != null) {
                  setState(() {
                    _selectedCountry = value;
                  });
                }
              },
              validator: (value) {
                if (value == null || value.isEmpty) {
                  return '국가를 선택해주세요';
                }
                return null;
              },
            ),
            const SizedBox(height: 16),

            // Title input
            TextFormField(
              controller: _titleController,
              decoration: const InputDecoration(
                labelText: '제목 *',
                border: OutlineInputBorder(),
                hintText: '게시글 제목을 입력하세요',
              ),
              maxLength: 100,
              validator: (value) {
                if (value == null || value.trim().isEmpty) {
                  return '제목을 입력해주세요';
                }
                if (value.trim().length < 2) {
                  return '제목은 최소 2자 이상이어야 합니다';
                }
                return null;
              },
            ),
            const SizedBox(height: 16),

            // Content input
            TextFormField(
              controller: _contentController,
              decoration: const InputDecoration(
                labelText: '내용 *',
                border: OutlineInputBorder(),
                hintText: '게시글 내용을 입력하세요',
                alignLabelWithHint: true,
              ),
              maxLines: 10,
              maxLength: 5000,
              validator: (value) {
                if (value == null || value.trim().isEmpty) {
                  return '내용을 입력해주세요';
                }
                if (value.trim().length < 5) {
                  return '내용은 최소 5자 이상이어야 합니다';
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
              const Text(
                '새 이미지가 선택되었습니다',
                style: TextStyle(fontSize: 12, color: Colors.blue),
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
                label: const Text('이미지 변경'),
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
                      ? '이미지 추가 (기존 이미지 제거됨)'
                      : '이미지 추가 (선택)',
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
                  label: const Text('기존 이미지 복원'),
                ),
              ],
              const SizedBox(height: 16),
            ],

            // Helper text
            const Text(
              '* 필수 항목입니다',
              style: TextStyle(
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
