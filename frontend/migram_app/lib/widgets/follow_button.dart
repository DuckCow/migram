import 'package:flutter/material.dart';
import '../models/follow_model.dart';
import '../services/follow_service.dart';

/// Follow button widget that changes based on follow status
/// - null: Show "팔로우" button (primary)
/// - REQUESTED: Show "요청중" button (can cancel)
/// - ACCEPTED: Show "팔로잉" button (can unfollow)
/// - REJECTED: Show disabled state with cooldown message
class FollowButton extends StatefulWidget {
  final int targetUserId;
  final FollowStatus? initialStatus;
  final VoidCallback? onStatusChanged;

  const FollowButton({
    super.key,
    required this.targetUserId,
    this.initialStatus,
    this.onStatusChanged,
  });

  @override
  State<FollowButton> createState() => _FollowButtonState();
}

class _FollowButtonState extends State<FollowButton> {
  final FollowService _followService = FollowService();
  FollowStatus? _currentStatus;
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _currentStatus = widget.initialStatus;
    if (_currentStatus == null) {
      _loadFollowStatus();
    }
  }

  Future<void> _loadFollowStatus() async {
    try {
      final statusResponse = await _followService.getFollowStatus(widget.targetUserId);
      setState(() {
        _currentStatus = statusResponse.status;
      });
    } catch (e) {
      print('Error loading follow status: $e');
    }
  }

  Future<void> _handleFollowAction() async {
    if (_isLoading) return;

    setState(() {
      _isLoading = true;
    });

    try {
      switch (_currentStatus) {
        case null:
          // Send follow request
          final result = await _followService.sendFollowRequest(widget.targetUserId);
          if (result.success) {
            setState(() {
              _currentStatus = FollowStatus.requested;
            });
            widget.onStatusChanged?.call();
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
          break;

        case FollowStatus.requested:
          // Cancel request
          final success = await _followService.cancelFollowRequest(widget.targetUserId);
          if (success) {
            setState(() {
              _currentStatus = null;
            });
            widget.onStatusChanged?.call();
            if (mounted) {
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('요청을 취소했습니다')),
              );
            }
          }
          break;

        case FollowStatus.accepted:
          // Unfollow
          final confirmed = await showDialog<bool>(
            context: context,
            builder: (context) => AlertDialog(
              title: const Text('언팔로우'),
              content: const Text('정말 언팔로우하시겠습니까?'),
              actions: [
                TextButton(
                  onPressed: () => Navigator.of(context).pop(false),
                  child: const Text('취소'),
                ),
                TextButton(
                  onPressed: () => Navigator.of(context).pop(true),
                  child: const Text('언팔로우'),
                ),
              ],
            ),
          );

          if (confirmed == true) {
            final success = await _followService.unfollow(widget.targetUserId);
            if (success) {
              setState(() {
                _currentStatus = null;
              });
              widget.onStatusChanged?.call();
              if (mounted) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('언팔로우했습니다')),
                );
              }
            }
          }
          break;

        case FollowStatus.rejected:
          // Show cooldown message
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('일정 기간 후에 다시 요청할 수 있습니다')),
            );
          }
          break;
      }
    } catch (e) {
      print('Error handling follow action: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('오류가 발생했습니다: $e')),
        );
      }
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return const SizedBox(
        width: 100,
        height: 36,
        child: Center(
          child: SizedBox(
            width: 20,
            height: 20,
            child: CircularProgressIndicator(strokeWidth: 2),
          ),
        ),
      );
    }

    final ButtonStyle buttonStyle;
    final String buttonText;
    final Color backgroundColor;
    final Color textColor;
    final bool isEnabled;

    switch (_currentStatus) {
      case null:
        // Not following - primary button
        buttonText = '팔로우';
        backgroundColor = Colors.blue;
        textColor = Colors.white;
        isEnabled = true;
        break;

      case FollowStatus.requested:
        // Request sent - can cancel
        buttonText = '요청중';
        backgroundColor = Colors.grey[300]!;
        textColor = Colors.black87;
        isEnabled = true;
        break;

      case FollowStatus.accepted:
        // Following - can unfollow
        buttonText = '팔로잉';
        backgroundColor = Colors.grey[300]!;
        textColor = Colors.black87;
        isEnabled = true;
        break;

      case FollowStatus.rejected:
        // Rejected - disabled
        buttonText = '거부됨';
        backgroundColor = Colors.grey[200]!;
        textColor = Colors.grey;
        isEnabled = false;
        break;
    }

    buttonStyle = ElevatedButton.styleFrom(
      backgroundColor: backgroundColor,
      foregroundColor: textColor,
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 8),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(20),
      ),
      elevation: 0,
    );

    return SizedBox(
      height: 36,
      child: ElevatedButton(
        onPressed: isEnabled ? _handleFollowAction : null,
        style: buttonStyle,
        child: Text(
          buttonText,
          style: const TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.bold,
          ),
        ),
      ),
    );
  }
}
