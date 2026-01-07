import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'app_localizations_en.dart';
import 'app_localizations_ko.dart';

// ignore_for_file: type=lint

/// Callers can lookup localized strings with an instance of AppLocalizations
/// returned by `AppLocalizations.of(context)`.
///
/// Applications need to include `AppLocalizations.delegate()` in their app's
/// `localizationDelegates` list, and the locales they support in the app's
/// `supportedLocales` list. For example:
///
/// ```dart
/// import 'l10n/app_localizations.dart';
///
/// return MaterialApp(
///   localizationsDelegates: AppLocalizations.localizationsDelegates,
///   supportedLocales: AppLocalizations.supportedLocales,
///   home: MyApplicationHome(),
/// );
/// ```
///
/// ## Update pubspec.yaml
///
/// Please make sure to update your pubspec.yaml to include the following
/// packages:
///
/// ```yaml
/// dependencies:
///   # Internationalization support.
///   flutter_localizations:
///     sdk: flutter
///   intl: any # Use the pinned version from flutter_localizations
///
///   # Rest of dependencies
/// ```
///
/// ## iOS Applications
///
/// iOS applications define key application metadata, including supported
/// locales, in an Info.plist file that is built into the application bundle.
/// To configure the locales supported by your app, you’ll need to edit this
/// file.
///
/// First, open your project’s ios/Runner.xcworkspace Xcode workspace file.
/// Then, in the Project Navigator, open the Info.plist file under the Runner
/// project’s Runner folder.
///
/// Next, select the Information Property List item, select Add Item from the
/// Editor menu, then select Localizations from the pop-up menu.
///
/// Select and expand the newly-created Localizations item then, for each
/// locale your application supports, add a new item and select the locale
/// you wish to add from the pop-up menu in the Value field. This list should
/// be consistent with the languages listed in the AppLocalizations.supportedLocales
/// property.
abstract class AppLocalizations {
  AppLocalizations(String locale)
      : localeName = intl.Intl.canonicalizedLocale(locale.toString());

  final String localeName;

  static AppLocalizations? of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations);
  }

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  /// A list of this localizations delegate along with the default localizations
  /// delegates.
  ///
  /// Returns a list of localizations delegates containing this delegate along with
  /// GlobalMaterialLocalizations.delegate, GlobalCupertinoLocalizations.delegate,
  /// and GlobalWidgetsLocalizations.delegate.
  ///
  /// Additional delegates can be added by appending to this list in
  /// MaterialApp. This list does not have to be used at all if a custom list
  /// of delegates is preferred or required.
  static const List<LocalizationsDelegate<dynamic>> localizationsDelegates =
      <LocalizationsDelegate<dynamic>>[
    delegate,
    GlobalMaterialLocalizations.delegate,
    GlobalCupertinoLocalizations.delegate,
    GlobalWidgetsLocalizations.delegate,
  ];

  /// A list of this localizations delegate's supported locales.
  static const List<Locale> supportedLocales = <Locale>[
    Locale('en'),
    Locale('ko')
  ];

  /// No description provided for @errorGeneric.
  ///
  /// In en, this message translates to:
  /// **'An error occurred. Please try again.'**
  String get errorGeneric;

  /// No description provided for @errorLoadBoards.
  ///
  /// In en, this message translates to:
  /// **'Failed to load boards. Please try again.'**
  String get errorLoadBoards;

  /// No description provided for @errorLoadPosts.
  ///
  /// In en, this message translates to:
  /// **'Failed to load posts. Please try again.'**
  String get errorLoadPosts;

  /// No description provided for @errorCreatePost.
  ///
  /// In en, this message translates to:
  /// **'Failed to create post. Please try again.'**
  String get errorCreatePost;

  /// No description provided for @errorUpdatePost.
  ///
  /// In en, this message translates to:
  /// **'Failed to update post. Please try again.'**
  String get errorUpdatePost;

  /// No description provided for @errorDeletePost.
  ///
  /// In en, this message translates to:
  /// **'Failed to delete post. Please try again.'**
  String get errorDeletePost;

  /// No description provided for @errorVote.
  ///
  /// In en, this message translates to:
  /// **'Failed to vote. Please try again.'**
  String get errorVote;

  /// No description provided for @errorLoadComments.
  ///
  /// In en, this message translates to:
  /// **'Failed to load comments. Please try again.'**
  String get errorLoadComments;

  /// No description provided for @errorCreateComment.
  ///
  /// In en, this message translates to:
  /// **'Failed to create comment. Please try again.'**
  String get errorCreateComment;

  /// No description provided for @errorUpdateComment.
  ///
  /// In en, this message translates to:
  /// **'Failed to update comment. Please try again.'**
  String get errorUpdateComment;

  /// No description provided for @errorDeleteComment.
  ///
  /// In en, this message translates to:
  /// **'Failed to delete comment. Please try again.'**
  String get errorDeleteComment;

  /// No description provided for @errorNetwork.
  ///
  /// In en, this message translates to:
  /// **'Network error. Please check your connection.'**
  String get errorNetwork;

  /// No description provided for @successPostCreated.
  ///
  /// In en, this message translates to:
  /// **'Post created successfully'**
  String get successPostCreated;

  /// No description provided for @successPostUpdated.
  ///
  /// In en, this message translates to:
  /// **'Post updated successfully'**
  String get successPostUpdated;

  /// No description provided for @successPostDeleted.
  ///
  /// In en, this message translates to:
  /// **'Post deleted successfully'**
  String get successPostDeleted;

  /// No description provided for @successCommentCreated.
  ///
  /// In en, this message translates to:
  /// **'Comment added successfully'**
  String get successCommentCreated;

  /// No description provided for @successCommentUpdated.
  ///
  /// In en, this message translates to:
  /// **'Comment updated successfully'**
  String get successCommentUpdated;

  /// No description provided for @successCommentDeleted.
  ///
  /// In en, this message translates to:
  /// **'Comment deleted successfully'**
  String get successCommentDeleted;

  /// No description provided for @appName.
  ///
  /// In en, this message translates to:
  /// **'Migram'**
  String get appName;

  /// No description provided for @all.
  ///
  /// In en, this message translates to:
  /// **'All'**
  String get all;

  /// No description provided for @cancel.
  ///
  /// In en, this message translates to:
  /// **'Cancel'**
  String get cancel;

  /// No description provided for @save.
  ///
  /// In en, this message translates to:
  /// **'Save'**
  String get save;

  /// No description provided for @delete.
  ///
  /// In en, this message translates to:
  /// **'Delete'**
  String get delete;

  /// No description provided for @edit.
  ///
  /// In en, this message translates to:
  /// **'Edit'**
  String get edit;

  /// No description provided for @post.
  ///
  /// In en, this message translates to:
  /// **'Post'**
  String get post;

  /// No description provided for @comment.
  ///
  /// In en, this message translates to:
  /// **'Comment'**
  String get comment;

  /// No description provided for @comments.
  ///
  /// In en, this message translates to:
  /// **'Comments'**
  String get comments;

  /// No description provided for @reply.
  ///
  /// In en, this message translates to:
  /// **'Reply'**
  String get reply;

  /// No description provided for @share.
  ///
  /// In en, this message translates to:
  /// **'Share'**
  String get share;

  /// No description provided for @profile.
  ///
  /// In en, this message translates to:
  /// **'Profile'**
  String get profile;

  /// No description provided for @logout.
  ///
  /// In en, this message translates to:
  /// **'Logout'**
  String get logout;

  /// No description provided for @login.
  ///
  /// In en, this message translates to:
  /// **'Login'**
  String get login;

  /// No description provided for @boardFilter.
  ///
  /// In en, this message translates to:
  /// **'Board Filter'**
  String get boardFilter;

  /// No description provided for @board.
  ///
  /// In en, this message translates to:
  /// **'Board'**
  String get board;

  /// No description provided for @boards.
  ///
  /// In en, this message translates to:
  /// **'Boards'**
  String get boards;

  /// No description provided for @boardRequired.
  ///
  /// In en, this message translates to:
  /// **'Board *'**
  String get boardRequired;

  /// No description provided for @selectBoard.
  ///
  /// In en, this message translates to:
  /// **'Select a board'**
  String get selectBoard;

  /// No description provided for @pleaseSelectBoard.
  ///
  /// In en, this message translates to:
  /// **'Please select a board'**
  String get pleaseSelectBoard;

  /// No description provided for @noBoardsAvailable.
  ///
  /// In en, this message translates to:
  /// **'No boards available'**
  String get noBoardsAvailable;

  /// No description provided for @clearAll.
  ///
  /// In en, this message translates to:
  /// **'Clear All'**
  String get clearAll;

  /// No description provided for @viewAll.
  ///
  /// In en, this message translates to:
  /// **'View All'**
  String get viewAll;

  /// No description provided for @applyBoards.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, =1{Apply 1 Board} other{Apply {count} Boards}}'**
  String applyBoards(int count);

  /// No description provided for @createPost.
  ///
  /// In en, this message translates to:
  /// **'Create Post'**
  String get createPost;

  /// No description provided for @editPost.
  ///
  /// In en, this message translates to:
  /// **'Edit Post'**
  String get editPost;

  /// No description provided for @deletePost.
  ///
  /// In en, this message translates to:
  /// **'Delete Post'**
  String get deletePost;

  /// No description provided for @noPostsYet.
  ///
  /// In en, this message translates to:
  /// **'No posts yet\nCreate your first post!'**
  String get noPostsYet;

  /// No description provided for @postAnonymously.
  ///
  /// In en, this message translates to:
  /// **'Post anonymously'**
  String get postAnonymously;

  /// No description provided for @anonymousPostInfo.
  ///
  /// In en, this message translates to:
  /// **'Your identity will not be displayed'**
  String get anonymousPostInfo;

  /// No description provided for @anonymous.
  ///
  /// In en, this message translates to:
  /// **'Anonymous'**
  String get anonymous;

  /// No description provided for @title.
  ///
  /// In en, this message translates to:
  /// **'Title'**
  String get title;

  /// No description provided for @titleLabel.
  ///
  /// In en, this message translates to:
  /// **'Title *'**
  String get titleLabel;

  /// No description provided for @titleHint.
  ///
  /// In en, this message translates to:
  /// **'Enter post title'**
  String get titleHint;

  /// No description provided for @titleValidation.
  ///
  /// In en, this message translates to:
  /// **'Please enter a title'**
  String get titleValidation;

  /// No description provided for @titleMinLength.
  ///
  /// In en, this message translates to:
  /// **'Title must be at least 2 characters'**
  String get titleMinLength;

  /// No description provided for @content.
  ///
  /// In en, this message translates to:
  /// **'Content'**
  String get content;

  /// No description provided for @contentLabel.
  ///
  /// In en, this message translates to:
  /// **'Content *'**
  String get contentLabel;

  /// No description provided for @contentHint.
  ///
  /// In en, this message translates to:
  /// **'Enter post content'**
  String get contentHint;

  /// No description provided for @contentValidation.
  ///
  /// In en, this message translates to:
  /// **'Please enter content'**
  String get contentValidation;

  /// No description provided for @contentMinLength.
  ///
  /// In en, this message translates to:
  /// **'Content must be at least 5 characters'**
  String get contentMinLength;

  /// No description provided for @requiredFields.
  ///
  /// In en, this message translates to:
  /// **'* Required fields'**
  String get requiredFields;

  /// No description provided for @chooseFromGallery.
  ///
  /// In en, this message translates to:
  /// **'Choose from Gallery'**
  String get chooseFromGallery;

  /// No description provided for @takePhoto.
  ///
  /// In en, this message translates to:
  /// **'Take Photo'**
  String get takePhoto;

  /// No description provided for @removeImage.
  ///
  /// In en, this message translates to:
  /// **'Remove Image'**
  String get removeImage;

  /// No description provided for @changeImage.
  ///
  /// In en, this message translates to:
  /// **'Change Image'**
  String get changeImage;

  /// No description provided for @addImage.
  ///
  /// In en, this message translates to:
  /// **'Add Image'**
  String get addImage;

  /// No description provided for @addImageOptional.
  ///
  /// In en, this message translates to:
  /// **'Add Image (optional)'**
  String get addImageOptional;

  /// No description provided for @addImageExistingRemoved.
  ///
  /// In en, this message translates to:
  /// **'Add Image (existing image removed)'**
  String get addImageExistingRemoved;

  /// No description provided for @newImageSelected.
  ///
  /// In en, this message translates to:
  /// **'New image selected'**
  String get newImageSelected;

  /// No description provided for @restoreExistingImage.
  ///
  /// In en, this message translates to:
  /// **'Restore Existing Image'**
  String get restoreExistingImage;

  /// No description provided for @errorPickImage.
  ///
  /// In en, this message translates to:
  /// **'Failed to pick image. Please try again.'**
  String get errorPickImage;

  /// No description provided for @writeComment.
  ///
  /// In en, this message translates to:
  /// **'Write a comment...'**
  String get writeComment;

  /// No description provided for @writeReply.
  ///
  /// In en, this message translates to:
  /// **'Write a reply...'**
  String get writeReply;

  /// No description provided for @replyingTo.
  ///
  /// In en, this message translates to:
  /// **'Replying to'**
  String get replyingTo;

  /// No description provided for @cannotReplyToReply.
  ///
  /// In en, this message translates to:
  /// **'Cannot reply to a reply. You can only reply to top-level comments.'**
  String get cannotReplyToReply;

  /// No description provided for @hideReplies.
  ///
  /// In en, this message translates to:
  /// **'Hide replies'**
  String get hideReplies;

  /// No description provided for @viewReplies.
  ///
  /// In en, this message translates to:
  /// **'View replies ({count})'**
  String viewReplies(int count);

  /// No description provided for @commentsCount.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, =0{No Comments} =1{1 Comment} other{{count} Comments}}'**
  String commentsCount(int count);

  /// No description provided for @confirmLogout.
  ///
  /// In en, this message translates to:
  /// **'Are you sure you want to logout?'**
  String get confirmLogout;

  /// No description provided for @confirmDeletePost.
  ///
  /// In en, this message translates to:
  /// **'Are you sure you want to delete this post?'**
  String get confirmDeletePost;

  /// No description provided for @postNotFound.
  ///
  /// In en, this message translates to:
  /// **'Post not found'**
  String get postNotFound;

  /// No description provided for @seeTranslation.
  ///
  /// In en, this message translates to:
  /// **'See translation'**
  String get seeTranslation;

  /// No description provided for @hideTranslation.
  ///
  /// In en, this message translates to:
  /// **'Hide translation'**
  String get hideTranslation;

  /// No description provided for @translating.
  ///
  /// In en, this message translates to:
  /// **'Translating...'**
  String get translating;

  /// No description provided for @translationFailed.
  ///
  /// In en, this message translates to:
  /// **'Translation failed. Please try again.'**
  String get translationFailed;

  /// No description provided for @errorTranslate.
  ///
  /// In en, this message translates to:
  /// **'Failed to translate. Please try again.'**
  String get errorTranslate;
}

class _AppLocalizationsDelegate
    extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  Future<AppLocalizations> load(Locale locale) {
    return SynchronousFuture<AppLocalizations>(lookupAppLocalizations(locale));
  }

  @override
  bool isSupported(Locale locale) =>
      <String>['en', 'ko'].contains(locale.languageCode);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}

AppLocalizations lookupAppLocalizations(Locale locale) {
  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'en':
      return AppLocalizationsEn();
    case 'ko':
      return AppLocalizationsKo();
  }

  throw FlutterError(
      'AppLocalizations.delegate failed to load unsupported locale "$locale". This is likely '
      'an issue with the localizations generation tool. Please file an issue '
      'on GitHub with a reproducible sample app and the gen-l10n configuration '
      'that was used.');
}
