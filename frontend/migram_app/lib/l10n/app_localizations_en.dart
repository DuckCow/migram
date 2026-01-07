// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class AppLocalizationsEn extends AppLocalizations {
  AppLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get errorGeneric => 'An error occurred. Please try again.';

  @override
  String get errorLoadBoards => 'Failed to load boards. Please try again.';

  @override
  String get errorLoadPosts => 'Failed to load posts. Please try again.';

  @override
  String get errorCreatePost => 'Failed to create post. Please try again.';

  @override
  String get errorUpdatePost => 'Failed to update post. Please try again.';

  @override
  String get errorDeletePost => 'Failed to delete post. Please try again.';

  @override
  String get errorVote => 'Failed to vote. Please try again.';

  @override
  String get errorLoadComments => 'Failed to load comments. Please try again.';

  @override
  String get errorCreateComment =>
      'Failed to create comment. Please try again.';

  @override
  String get errorUpdateComment =>
      'Failed to update comment. Please try again.';

  @override
  String get errorDeleteComment =>
      'Failed to delete comment. Please try again.';

  @override
  String get errorNetwork => 'Network error. Please check your connection.';

  @override
  String get successPostCreated => 'Post created successfully';

  @override
  String get successPostUpdated => 'Post updated successfully';

  @override
  String get successPostDeleted => 'Post deleted successfully';

  @override
  String get successCommentCreated => 'Comment added successfully';

  @override
  String get successCommentUpdated => 'Comment updated successfully';

  @override
  String get successCommentDeleted => 'Comment deleted successfully';

  @override
  String get appName => 'Migram';

  @override
  String get all => 'All';

  @override
  String get cancel => 'Cancel';

  @override
  String get save => 'Save';

  @override
  String get delete => 'Delete';

  @override
  String get edit => 'Edit';

  @override
  String get post => 'Post';

  @override
  String get comment => 'Comment';

  @override
  String get comments => 'Comments';

  @override
  String get reply => 'Reply';

  @override
  String get share => 'Share';

  @override
  String get profile => 'Profile';

  @override
  String get logout => 'Logout';

  @override
  String get login => 'Login';

  @override
  String get boardFilter => 'Board Filter';

  @override
  String get board => 'Board';

  @override
  String get boards => 'Boards';

  @override
  String get boardRequired => 'Board *';

  @override
  String get selectBoard => 'Select a board';

  @override
  String get pleaseSelectBoard => 'Please select a board';

  @override
  String get noBoardsAvailable => 'No boards available';

  @override
  String get clearAll => 'Clear All';

  @override
  String get viewAll => 'View All';

  @override
  String applyBoards(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: 'Apply $count Boards',
      one: 'Apply 1 Board',
    );
    return '$_temp0';
  }

  @override
  String get createPost => 'Create Post';

  @override
  String get editPost => 'Edit Post';

  @override
  String get deletePost => 'Delete Post';

  @override
  String get noPostsYet => 'No posts yet\nCreate your first post!';

  @override
  String get postAnonymously => 'Post anonymously';

  @override
  String get anonymousPostInfo => 'Your identity will not be displayed';

  @override
  String get anonymous => 'Anonymous';

  @override
  String get title => 'Title';

  @override
  String get titleLabel => 'Title *';

  @override
  String get titleHint => 'Enter post title';

  @override
  String get titleValidation => 'Please enter a title';

  @override
  String get titleMinLength => 'Title must be at least 2 characters';

  @override
  String get content => 'Content';

  @override
  String get contentLabel => 'Content *';

  @override
  String get contentHint => 'Enter post content';

  @override
  String get contentValidation => 'Please enter content';

  @override
  String get contentMinLength => 'Content must be at least 5 characters';

  @override
  String get requiredFields => '* Required fields';

  @override
  String get chooseFromGallery => 'Choose from Gallery';

  @override
  String get takePhoto => 'Take Photo';

  @override
  String get removeImage => 'Remove Image';

  @override
  String get changeImage => 'Change Image';

  @override
  String get addImage => 'Add Image';

  @override
  String get addImageOptional => 'Add Image (optional)';

  @override
  String get addImageExistingRemoved => 'Add Image (existing image removed)';

  @override
  String get newImageSelected => 'New image selected';

  @override
  String get restoreExistingImage => 'Restore Existing Image';

  @override
  String get errorPickImage => 'Failed to pick image. Please try again.';

  @override
  String get writeComment => 'Write a comment...';

  @override
  String get writeReply => 'Write a reply...';

  @override
  String get replyingTo => 'Replying to';

  @override
  String get cannotReplyToReply =>
      'Cannot reply to a reply. You can only reply to top-level comments.';

  @override
  String get hideReplies => 'Hide replies';

  @override
  String viewReplies(int count) {
    return 'View replies ($count)';
  }

  @override
  String commentsCount(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count Comments',
      one: '1 Comment',
      zero: 'No Comments',
    );
    return '$_temp0';
  }

  @override
  String get confirmLogout => 'Are you sure you want to logout?';

  @override
  String get confirmDeletePost => 'Are you sure you want to delete this post?';

  @override
  String get postNotFound => 'Post not found';

  @override
  String get seeTranslation => 'See translation';

  @override
  String get hideTranslation => 'Hide translation';

  @override
  String get translating => 'Translating...';

  @override
  String get translationFailed => 'Translation failed. Please try again.';

  @override
  String get errorTranslate => 'Failed to translate. Please try again.';
}
