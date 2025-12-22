import 'dart:io';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import '../services/post_service.dart';

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
  final ImagePicker _imagePicker = ImagePicker();

  String? _selectedCountry;
  File? _selectedImage;
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
          _selectedImage = File(pickedFile.path);
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
      _selectedImage = null;
    });
  }

  Future<void> _submitPost() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }

    if (_selectedCountry == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('국가를 선택해주세요')),
      );
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
          country: _selectedCountry!,
          imageFile: _selectedImage,
        );
      } else {
        await _postService.createPost(
          title: _titleController.text.trim(),
          content: _contentController.text.trim(),
          country: _selectedCountry!,
        );
      }

      if (mounted) {
        Navigator.of(context).pop(true); // Return true to indicate success
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('게시글이 작성되었습니다')),
        );
      }
    } catch (e) {
      print('Error creating post: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('게시글 작성 실패: $e')),
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
        title: const Text('새 게시글'),
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
                setState(() {
                  _selectedCountry = value;
                });
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
                label: const Text('이미지 추가 (선택)'),
                style: OutlinedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 16),
                ),
              ),
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
