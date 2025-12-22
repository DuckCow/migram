import 'package:flutter/material.dart';

class VoteButton extends StatelessWidget {
  final int upvotes;
  final int downvotes;
  final String? currentUserVote; // 'UP', 'DOWN', or null
  final Function(String) onVote;
  final bool isCompact; // Compact mode for comments, full mode for posts

  const VoteButton({
    super.key,
    required this.upvotes,
    required this.downvotes,
    this.currentUserVote,
    required this.onVote,
    this.isCompact = false,
  });

  int get netVotes => upvotes - downvotes;

  @override
  Widget build(BuildContext context) {
    if (isCompact) {
      return _buildCompactMode(context);
    } else {
      return _buildFullMode(context);
    }
  }

  Widget _buildCompactMode(BuildContext context) {
    // Compact mode: Only show if netVotes > 0
    if (netVotes <= 0) {
      return const SizedBox.shrink();
    }

    return GestureDetector(
      onTap: () => onVote('UP'),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        decoration: BoxDecoration(
          color: currentUserVote == 'UP'
              ? Colors.orange.withOpacity(0.1)
              : Colors.transparent,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              '👍',
              style: TextStyle(
                fontSize: 14,
                color: currentUserVote == 'UP' ? Colors.orange : null,
              ),
            ),
            const SizedBox(width: 4),
            Text(
              '+$netVotes',
              style: TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w500,
                color: currentUserVote == 'UP' ? Colors.orange : Colors.grey,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildFullMode(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        // Upvote button
        GestureDetector(
          onTap: () => onVote('UP'),
          child: Container(
            padding: const EdgeInsets.all(6),
            decoration: BoxDecoration(
              color: currentUserVote == 'UP'
                  ? Colors.orange.withOpacity(0.2)
                  : Colors.transparent,
              borderRadius: BorderRadius.circular(4),
            ),
            child: Icon(
              Icons.arrow_upward,
              size: 20,
              color: currentUserVote == 'UP' ? Colors.orange : Colors.grey,
            ),
          ),
        ),
        const SizedBox(width: 8),
        // Vote count
        Text(
          netVotes > 0 ? '+$netVotes' : netVotes.toString(),
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.bold,
            color: netVotes > 0
                ? Colors.orange
                : netVotes < 0
                    ? Colors.blue
                    : Colors.grey,
          ),
        ),
        const SizedBox(width: 8),
        // Downvote button
        GestureDetector(
          onTap: () => onVote('DOWN'),
          child: Container(
            padding: const EdgeInsets.all(6),
            decoration: BoxDecoration(
              color: currentUserVote == 'DOWN'
                  ? Colors.blue.withOpacity(0.2)
                  : Colors.transparent,
              borderRadius: BorderRadius.circular(4),
            ),
            child: Icon(
              Icons.arrow_downward,
              size: 20,
              color: currentUserVote == 'DOWN' ? Colors.blue : Colors.grey,
            ),
          ),
        ),
      ],
    );
  }
}
