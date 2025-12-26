import 'package:flutter/material.dart';
import 'package:timeago/timeago.dart' as timeago;
import '../models/post_model.dart';
import '../screens/user_profile_screen.dart';
import 'vote_button.dart';

class PostCard extends StatelessWidget {
  final Post post;
  final VoidCallback onTap;
  final Function(String) onVote;

  const PostCard({
    super.key,
    required this.post,
    required this.onTap,
    required this.onVote,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Card(
        margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Header: Avatar, nickname, timestamp, board name
              Row(
                children: [
                  // Avatar and Nickname (tappable if not anonymous)
                  Expanded(
                    child: InkWell(
                      onTap: post.isAnonymous || post.user == null
                          ? null
                          : () {
                              Navigator.of(context).push(
                                MaterialPageRoute(
                                  builder: (context) =>
                                      UserProfileScreen(userId: post.user!.id),
                                ),
                              );
                            },
                      child: Row(
                        children: [
                          // Avatar
                          CircleAvatar(
                            radius: 16,
                            backgroundColor:
                                post.isAnonymous ? Colors.grey[400] : null,
                            backgroundImage: !post.isAnonymous &&
                                    post.user?.profilePicture != null
                                ? NetworkImage(post.user!.profilePicture!)
                                : null,
                            child: post.isAnonymous
                                ? const Icon(
                                    Icons.person,
                                    size: 18,
                                    color: Colors.white,
                                  )
                                : (post.user?.profilePicture == null
                                    ? Text(
                                        post.user!.displayName[0].toUpperCase(),
                                        style: const TextStyle(fontSize: 14),
                                      )
                                    : null),
                          ),
                          const SizedBox(width: 8),
                          // Nickname or "Anonymous"
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  post.isAnonymous
                                      ? 'Anonymous'
                                      : post.user!.displayName,
                                  style: TextStyle(
                                    fontWeight: FontWeight.bold,
                                    fontSize: 14,
                                    color: post.isAnonymous
                                        ? Colors.grey[600]
                                        : null,
                                  ),
                                ),
                                Text(
                                  timeago.format(post.createdAt, locale: 'ko'),
                                  style: const TextStyle(
                                    color: Colors.grey,
                                    fontSize: 12,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                  // Board name
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                    decoration: BoxDecoration(
                      color: Colors.grey[200],
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(
                      post.boardName,
                      style: const TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.w500,
                        color: Colors.black87,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              // Title
              Text(
                post.title,
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                ),
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
              const SizedBox(height: 6),
              // Content preview
              Text(
                post.content,
                style: const TextStyle(fontSize: 14),
                maxLines: 3,
                overflow: TextOverflow.ellipsis,
              ),
              // Image preview (if exists)
              if (post.imageUrl != null) ...[
                const SizedBox(height: 8),
                ClipRRect(
                  borderRadius: BorderRadius.circular(8),
                  child: Image.network(
                    post.imageUrl!,
                    height: 200,
                    width: double.infinity,
                    fit: BoxFit.cover,
                    errorBuilder: (context, error, stackTrace) {
                      return Container(
                        height: 200,
                        color: Colors.grey[300],
                        child: const Center(
                          child: Icon(Icons.error, color: Colors.grey),
                        ),
                      );
                    },
                  ),
                ),
              ],
              const SizedBox(height: 12),
              // Footer: Vote buttons and comment count
              Row(
                children: [
                  VoteButton(
                    upvotes: post.upvotes,
                    downvotes: post.downvotes,
                    currentUserVote: post.currentUserVote,
                    onVote: onVote,
                    isCompact: false,
                  ),
                  const Spacer(),
                  Row(
                    children: [
                      const Icon(
                        Icons.comment_outlined,
                        size: 18,
                        color: Colors.grey,
                      ),
                      const SizedBox(width: 4),
                      Text(
                        '${post.commentCount}',
                        style: const TextStyle(
                          color: Colors.grey,
                          fontSize: 14,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
