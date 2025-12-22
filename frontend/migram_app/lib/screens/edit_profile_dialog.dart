import 'package:flutter/material.dart';
import '../models/user_model.dart';
import '../models/user_profile_model.dart';
import '../services/user_service.dart';

class EditProfileDialog extends StatefulWidget {
  final UserModel user;

  const EditProfileDialog({
    super.key,
    required this.user,
  });

  @override
  State<EditProfileDialog> createState() => _EditProfileDialogState();
}

class _EditProfileDialogState extends State<EditProfileDialog> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _nicknameController;
  final UserService _userService = UserService();

  String? _originCountry;
  String? _currentCountry;
  bool _isSubmitting = false;

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
    _nicknameController = TextEditingController(text: widget.user.nickname);
    _originCountry = widget.user.originCountry;
    _currentCountry = widget.user.currentCountry;
  }

  @override
  void dispose() {
    _nicknameController.dispose();
    super.dispose();
  }

  Future<void> _submitUpdate() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }

    setState(() {
      _isSubmitting = true;
    });

    try {
      final request = ProfileUpdateRequest(
        nickname: _nicknameController.text.trim().isNotEmpty
            ? _nicknameController.text.trim()
            : null,
        originCountry: _originCountry,
        currentCountry: _currentCountry,
      );

      await _userService.updateProfile(request);

      if (mounted) {
        Navigator.of(context).pop(true);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('프로필이 수정되었습니다')),
        );
      }
    } catch (e) {
      print('Error updating profile: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('프로필 수정 실패: $e')),
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
      title: const Text('프로필 수정'),
      content: SingleChildScrollView(
        child: Form(
          key: _formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              // Nickname
              TextFormField(
                controller: _nicknameController,
                decoration: const InputDecoration(
                  labelText: '닉네임',
                  hintText: '한글 2-10자',
                  border: OutlineInputBorder(),
                ),
                validator: (value) {
                  if (value == null || value.trim().isEmpty) {
                    return null; // Nickname is optional in update
                  }
                  if (!RegExp(r'^[가-힣]{2,10}$').hasMatch(value.trim())) {
                    return '닉네임은 한글 2-10자여야 합니다';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 16),

              // Origin Country
              DropdownButtonFormField<String>(
                value: _originCountry,
                decoration: const InputDecoration(
                  labelText: '출신 국가',
                  border: OutlineInputBorder(),
                ),
                items: [
                  const DropdownMenuItem(value: null, child: Text('선택 안함')),
                  ..._countries.entries.map((entry) {
                    return DropdownMenuItem(
                      value: entry.key,
                      child: Text(entry.value),
                    );
                  }).toList(),
                ],
                onChanged: (value) {
                  setState(() {
                    _originCountry = value;
                  });
                },
              ),
              const SizedBox(height: 16),

              // Current Country
              DropdownButtonFormField<String>(
                value: _currentCountry,
                decoration: const InputDecoration(
                  labelText: '거주 국가',
                  border: OutlineInputBorder(),
                ),
                items: [
                  const DropdownMenuItem(value: null, child: Text('선택 안함')),
                  ..._countries.entries.map((entry) {
                    return DropdownMenuItem(
                      value: entry.key,
                      child: Text(entry.value),
                    );
                  }).toList(),
                ],
                onChanged: (value) {
                  setState(() {
                    _currentCountry = value;
                  });
                },
              ),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: _isSubmitting ? null : () => Navigator.of(context).pop(),
          child: const Text('취소'),
        ),
        TextButton(
          onPressed: _isSubmitting ? null : _submitUpdate,
          child: _isSubmitting
              ? const SizedBox(
                  width: 16,
                  height: 16,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : const Text('저장'),
        ),
      ],
    );
  }
}
