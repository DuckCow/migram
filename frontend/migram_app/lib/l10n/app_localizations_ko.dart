// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Korean (`ko`).
class AppLocalizationsKo extends AppLocalizations {
  AppLocalizationsKo([String locale = 'ko']) : super(locale);

  @override
  String get errorGeneric => '오류가 발생했습니다. 다시 시도해주세요.';

  @override
  String get errorLoadBoards => '게시판 목록을 불러오는데 실패했습니다.';

  @override
  String get errorLoadPosts => '게시글을 불러오는데 실패했습니다.';

  @override
  String get errorCreatePost => '게시글 작성에 실패했습니다.';

  @override
  String get errorUpdatePost => '게시글 수정에 실패했습니다.';

  @override
  String get errorDeletePost => '게시글 삭제에 실패했습니다.';

  @override
  String get errorVote => '투표에 실패했습니다.';

  @override
  String get errorLoadComments => '댓글을 불러오는데 실패했습니다.';

  @override
  String get errorCreateComment => '댓글 작성에 실패했습니다.';

  @override
  String get errorUpdateComment => '댓글 수정에 실패했습니다.';

  @override
  String get errorDeleteComment => '댓글 삭제에 실패했습니다.';

  @override
  String get errorNetwork => '네트워크 오류가 발생했습니다. 연결을 확인해주세요.';

  @override
  String get successPostCreated => '게시글이 작성되었습니다';

  @override
  String get successPostUpdated => '게시글이 수정되었습니다';

  @override
  String get successPostDeleted => '게시글이 삭제되었습니다';

  @override
  String get successCommentCreated => '댓글이 추가되었습니다';

  @override
  String get successCommentUpdated => '댓글이 수정되었습니다';

  @override
  String get successCommentDeleted => '댓글이 삭제되었습니다';

  @override
  String get appName => 'Migram';

  @override
  String get all => '전체';

  @override
  String get cancel => '취소';

  @override
  String get save => '저장';

  @override
  String get delete => '삭제';

  @override
  String get edit => '수정';

  @override
  String get post => '게시';

  @override
  String get comment => '댓글';

  @override
  String get comments => '댓글';

  @override
  String get reply => '답글';

  @override
  String get share => '공유';

  @override
  String get profile => '프로필';

  @override
  String get logout => '로그아웃';

  @override
  String get login => '로그인';

  @override
  String get boardFilter => '게시판 필터';

  @override
  String get board => '게시판';

  @override
  String get boards => '게시판';

  @override
  String get boardRequired => '게시판 *';

  @override
  String get selectBoard => '게시판을 선택하세요';

  @override
  String get pleaseSelectBoard => '게시판을 선택해주세요';

  @override
  String get noBoardsAvailable => '게시판이 없습니다';

  @override
  String get clearAll => '전체 해제';

  @override
  String get viewAll => '전체 보기';

  @override
  String applyBoards(int count) {
    return '$count개 게시판 적용';
  }

  @override
  String get createPost => '새 게시글';

  @override
  String get editPost => '게시글 수정';

  @override
  String get deletePost => '게시글 삭제';

  @override
  String get noPostsYet => '게시글이 없습니다\n새로운 게시글을 작성해보세요!';

  @override
  String get postAnonymously => '익명으로 게시';

  @override
  String get anonymousPostInfo => '작성자 정보가 표시되지 않습니다';

  @override
  String get anonymous => '익명';

  @override
  String get title => '제목';

  @override
  String get titleLabel => '제목 *';

  @override
  String get titleHint => '게시글 제목을 입력하세요';

  @override
  String get titleValidation => '제목을 입력해주세요';

  @override
  String get titleMinLength => '제목은 최소 2자 이상이어야 합니다';

  @override
  String get content => '내용';

  @override
  String get contentLabel => '내용 *';

  @override
  String get contentHint => '게시글 내용을 입력하세요';

  @override
  String get contentValidation => '내용을 입력해주세요';

  @override
  String get contentMinLength => '내용은 최소 5자 이상이어야 합니다';

  @override
  String get requiredFields => '* 필수 항목입니다';

  @override
  String get chooseFromGallery => '갤러리에서 선택';

  @override
  String get takePhoto => '카메라로 촬영';

  @override
  String get removeImage => '이미지 제거';

  @override
  String get changeImage => '이미지 변경';

  @override
  String get addImage => '이미지 추가';

  @override
  String get addImageOptional => '이미지 추가 (선택)';

  @override
  String get addImageExistingRemoved => '이미지 추가 (기존 이미지 제거됨)';

  @override
  String get newImageSelected => '새 이미지 선택됨';

  @override
  String get restoreExistingImage => '기존 이미지 복원';

  @override
  String get errorPickImage => '이미지 선택에 실패했습니다.';

  @override
  String get writeComment => '댓글을 입력하세요...';

  @override
  String get writeReply => '답글을 입력하세요...';

  @override
  String get replyingTo => '님에게 답글';

  @override
  String get cannotReplyToReply => '답글에는 답글을 달 수 없습니다. 최상위 댓글에만 답글을 달 수 있습니다.';

  @override
  String get hideReplies => '답글 숨기기';

  @override
  String viewReplies(int count) {
    return '답글 보기 ($count개)';
  }

  @override
  String commentsCount(int count) {
    return '$count개의 댓글';
  }

  @override
  String get confirmLogout => '로그아웃 하시겠습니까?';

  @override
  String get confirmDeletePost => '정말로 이 게시글을 삭제하시겠습니까?';

  @override
  String get postNotFound => '게시글을 찾을 수 없습니다';

  @override
  String get seeTranslation => '번역 보기';

  @override
  String get hideTranslation => '번역 숨기기';

  @override
  String get translating => '번역 중...';

  @override
  String get translationFailed => '번역에 실패했습니다. 다시 시도해주세요.';

  @override
  String get errorTranslate => '번역에 실패했습니다. 다시 시도해주세요.';
}
