import {
  Page,
  ReportResponseDTO,
  ReportReviewRequestDTO,
  ModerationActionResponseDTO,
  ReportStatus,
  ContentType,
  PostPreview,
  CommentPreview,
} from './types';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

// Helper for fetch with auth
async function fetchWithAuth<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      // TODO: Add actual auth token from cookie/session
      // 'Authorization': `Bearer ${token}`,
      ...options.headers,
    },
    credentials: 'include',
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`API Error ${response.status}: ${errorText}`);
  }

  return response.json();
}

// ==================== Reports API ====================

export async function getReports(params: {
  status?: ReportStatus;
  page?: number;
  size?: number;
}): Promise<Page<ReportResponseDTO>> {
  const searchParams = new URLSearchParams();
  if (params.status) searchParams.set('status', params.status);
  if (params.page !== undefined) searchParams.set('page', params.page.toString());
  if (params.size !== undefined) searchParams.set('size', params.size.toString());

  const query = searchParams.toString();
  return fetchWithAuth<Page<ReportResponseDTO>>(
    `/api/admin/reports${query ? `?${query}` : ''}`
  );
}

export async function getReport(id: number): Promise<ReportResponseDTO> {
  return fetchWithAuth<ReportResponseDTO>(`/api/admin/reports/${id}`);
}

export async function reviewReport(
  id: number,
  request: ReportReviewRequestDTO
): Promise<ReportResponseDTO> {
  return fetchWithAuth<ReportResponseDTO>(`/api/admin/reports/${id}/review`, {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

// ==================== Moderation Actions API ====================

export async function getModerationHistory(params: {
  contentType: ContentType;
  contentId: number;
}): Promise<ModerationActionResponseDTO[]> {
  const searchParams = new URLSearchParams();
  searchParams.set('contentType', params.contentType);
  searchParams.set('contentId', params.contentId.toString());

  return fetchWithAuth<ModerationActionResponseDTO[]>(
    `/api/admin/history?${searchParams.toString()}`
  );
}

export async function getRecentActions(params: {
  page?: number;
  size?: number;
}): Promise<Page<ModerationActionResponseDTO>> {
  const searchParams = new URLSearchParams();
  if (params.page !== undefined) searchParams.set('page', params.page.toString());
  if (params.size !== undefined) searchParams.set('size', params.size.toString());

  const query = searchParams.toString();
  return fetchWithAuth<Page<ModerationActionResponseDTO>>(
    `/api/admin/actions${query ? `?${query}` : ''}`
  );
}

// ==================== Content Preview API ====================

export async function getPostPreview(postId: number): Promise<PostPreview> {
  // This uses the public GET endpoint
  return fetchWithAuth<PostPreview>(`/posts/${postId}`);
}

export async function getCommentPreview(commentId: number): Promise<CommentPreview> {
  // This uses the public GET endpoint
  return fetchWithAuth<CommentPreview>(`/comments/${commentId}`);
}
