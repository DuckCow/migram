// Enums matching backend
export type ContentType = 'POST' | 'COMMENT';
export type ContentVisibility = 'PUBLIC' | 'LIMITED' | 'HIDDEN' | 'REMOVED';
export type ReportStatus = 'PENDING' | 'RESOLVED' | 'REJECTED';
export type ReportReason =
  | 'SPAM'
  | 'HARASSMENT'
  | 'HATE_SPEECH'
  | 'VIOLENCE'
  | 'MISINFORMATION'
  | 'INAPPROPRIATE'
  | 'COPYRIGHT'
  | 'OTHER';
export type ModerationActionType =
  | 'NO_ACTION'
  | 'SET_LIMITED'
  | 'SET_HIDDEN'
  | 'SET_REMOVED'
  | 'RESTORE_PUBLIC';

// DTOs matching backend
export interface ReportResponseDTO {
  id: number;
  reporterId: number;
  reporterName: string;
  contentType: ContentType;
  contentId: number;
  reason: ReportReason;
  description: string | null;
  status: ReportStatus;
  reviewedById: number | null;
  reviewedByName: string | null;
  reviewedAt: string | null;
  createdAt: string;
}

export interface ReportReviewRequestDTO {
  status: 'RESOLVED' | 'REJECTED';
  action: ModerationActionType;
  adminNote?: string;
}

export interface ModerationActionResponseDTO {
  id: number;
  contentType: ContentType;
  contentId: number;
  reportId: number | null;
  actionType: ModerationActionType;
  adminId: number;
  adminName: string;
  note: string | null;
  createdAt: string;
}

// Paginated response from Spring Boot
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// Content preview (for displaying target content)
export interface PostPreview {
  id: number;
  title: string;
  content: string;
  visibility: ContentVisibility;
  createdAt: string;
}

export interface CommentPreview {
  id: number;
  content: string;
  visibility: ContentVisibility;
  createdAt: string;
}
