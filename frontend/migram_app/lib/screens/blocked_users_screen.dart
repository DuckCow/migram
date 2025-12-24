import 'package:flutter/material.dart';
import '../models/user_model.dart';
import '../services/block_service.dart';
import '../services/user_service.dart';

class BlockedUsersScreen extends StatefulWidget {
  const BlockedUsersScreen({super.key});

  @override
  State<BlockedUsersScreen> createState() => _BlockedUsersScreenState();
}

class _BlockedUsersScreenState extends State<BlockedUsersScreen> {
  final BlockService _blockService = BlockService();
  final UserService _userService = UserService();

  List<UserModel> _blockedUsers = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadBlockedUsers();
  }

  Future<void> _loadBlockedUsers() async {
    setState(() {
      _isLoading = true;
    });

    try {
      final blockedUserIds = await _blockService.getBlockedUsers();

      // Fetch user details for each blocked user ID
      final List<UserModel> users = [];
      for (final userId in blockedUserIds) {
        try {
          final user = await _userService.getUserById(userId);
          users.add(user);
        } catch (e) {
          print('Error fetching user $userId: $e');
        }
      }

      setState(() {
        _blockedUsers = users;
        _isLoading = false;
      });
    } catch (e) {
      print('Error loading blocked users: $e');
      setState(() {
        _isLoading = false;
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('차단 목록을 불러오는데 실패했습니다: $e')),
        );
      }
    }
  }

  Future<void> _unblockUser(UserModel user) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('차단 해제'),
        content: Text('${user.displayName}님의 차단을 해제하시겠습니까?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('취소'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('차단 해제'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      try {
        final result = await _blockService.unblockUser(user.id);

        if (result.success) {
          setState(() {
            _blockedUsers.removeWhere((u) => u.id == user.id);
          });

          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text(result.message)),
            );
          }
        } else {
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text(result.message)),
            );
          }
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('차단 해제 실패: $e')),
          );
        }
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('차단 목록'),
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _blockedUsers.isEmpty
              ? const Center(
                  child: Padding(
                    padding: EdgeInsets.all(20),
                    child: Text(
                      '차단한 사용자가 없습니다',
                      style: TextStyle(
                        fontSize: 16,
                        color: Colors.grey,
                      ),
                    ),
                  ),
                )
              : RefreshIndicator(
                  onRefresh: _loadBlockedUsers,
                  child: ListView.builder(
                    itemCount: _blockedUsers.length,
                    itemBuilder: (context, index) {
                      final user = _blockedUsers[index];
                      return ListTile(
                        leading: CircleAvatar(
                          backgroundImage: user.profilePicture != null
                              ? NetworkImage(user.profilePicture!)
                              : null,
                          child: user.profilePicture == null
                              ? Text(user.displayName[0].toUpperCase())
                              : null,
                        ),
                        title: Text(user.displayName),
                        subtitle: Text(user.email),
                        trailing: OutlinedButton(
                          onPressed: () => _unblockUser(user),
                          child: const Text('차단 해제'),
                        ),
                      );
                    },
                  ),
                ),
    );
  }
}
