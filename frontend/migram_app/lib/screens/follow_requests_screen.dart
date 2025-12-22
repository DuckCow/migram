import 'package:flutter/material.dart';
import 'package:timeago/timeago.dart' as timeago;
import '../models/follow_model.dart';
import '../services/follow_service.dart';

/// Follow Requests Screen
/// Shows pending follow requests (받은 요청)
/// User can accept or reject requests
class FollowRequestsScreen extends StatefulWidget {
  const FollowRequestsScreen({super.key});

  @override
  State<FollowRequestsScreen> createState() => _FollowRequestsScreenState();
}

class _FollowRequestsScreenState extends State<FollowRequestsScreen> {
  final FollowService _followService = FollowService();
  List<FollowRequest> _requests = [];
  bool _isLoading = false;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _loadRequests();
  }

  Future<void> _loadRequests() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final requests = await _followService.getPendingRequests();
      setState(() {
        _requests = requests;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _errorMessage = '요청 목록을 불러오는데 실패했습니다: $e';
        _isLoading = false;
      });
    }
  }

  Future<void> _acceptRequest(FollowRequest request) async {
    try {
      final result = await _followService.acceptFollowRequest(request.id);

      if (result.success) {
        setState(() {
          _requests.remove(request);
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
          SnackBar(content: Text('오류가 발생했습니다: $e')),
        );
      }
    }
  }

  Future<void> _rejectRequest(FollowRequest request) async {
    try {
      final result = await _followService.rejectFollowRequest(request.id);

      if (result.success) {
        setState(() {
          _requests.remove(request);
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
          SnackBar(content: Text('오류가 발생했습니다: $e')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('팔로우 요청'),
      ),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_errorMessage != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.error_outline, size: 64, color: Colors.grey),
            SizedBox(height: 16),
            Text(_errorMessage!, style: TextStyle(color: Colors.grey)),
            SizedBox(height: 16),
            ElevatedButton(
              onPressed: _loadRequests,
              child: Text('다시 시도'),
            ),
          ],
        ),
      );
    }

    if (_requests.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.people_outline, size: 64, color: Colors.grey),
            SizedBox(height: 16),
            Text(
              '받은 팔로우 요청이 없습니다',
              style: TextStyle(color: Colors.grey, fontSize: 16),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _loadRequests,
      child: ListView.builder(
        itemCount: _requests.length,
        itemBuilder: (context, index) {
          final request = _requests[index];
          return _buildRequestCard(request);
        },
      ),
    );
  }

  Widget _buildRequestCard(FollowRequest request) {
    final requester = request.requester;

    return Card(
      margin: EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Padding(
        padding: EdgeInsets.all(16),
        child: Row(
          children: [
            // Avatar
            CircleAvatar(
              radius: 24,
              backgroundImage: requester.profilePicture != null
                  ? NetworkImage(requester.profilePicture!)
                  : null,
              child: requester.profilePicture == null
                  ? Text(
                      requester.displayName[0].toUpperCase(),
                      style: TextStyle(fontSize: 20),
                    )
                  : null,
            ),
            SizedBox(width: 12),

            // User info
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    requester.displayName,
                    style: TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 16,
                    ),
                  ),
                  SizedBox(height: 4),
                  Text(
                    timeago.format(request.createdAt, locale: 'ko'),
                    style: TextStyle(
                      color: Colors.grey,
                      fontSize: 12,
                    ),
                  ),
                ],
              ),
            ),

            // Action buttons
            Column(
              children: [
                // Accept button
                SizedBox(
                  width: 80,
                  child: ElevatedButton(
                    onPressed: () => _acceptRequest(request),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.blue,
                      foregroundColor: Colors.white,
                      padding: EdgeInsets.symmetric(vertical: 8),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(8),
                      ),
                    ),
                    child: Text('수락', style: TextStyle(fontSize: 12)),
                  ),
                ),
                SizedBox(height: 8),
                // Reject button
                SizedBox(
                  width: 80,
                  child: OutlinedButton(
                    onPressed: () => _rejectRequest(request),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: Colors.grey[700],
                      padding: EdgeInsets.symmetric(vertical: 8),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(8),
                      ),
                    ),
                    child: Text('거부', style: TextStyle(fontSize: 12)),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
