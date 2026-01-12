'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useRouter, useParams } from 'next/navigation';
import { getReport, reviewReport, getModerationHistory } from '@/lib/api';
import { ModerationActionType, ReportReviewRequestDTO } from '@/lib/types';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  StatusBadge,
  ContentTypeBadge,
  ActionBadge,
} from '@/components/visibility-badge';

export default function ReportDetailPage() {
  const router = useRouter();
  const params = useParams();
  const queryClient = useQueryClient();
  const reportId = parseInt(params.id as string);

  const [showApproveDialog, setShowApproveDialog] = useState(false);
  const [showRejectDialog, setShowRejectDialog] = useState(false);
  const [selectedAction, setSelectedAction] = useState<ModerationActionType>('SET_HIDDEN');
  const [adminNote, setAdminNote] = useState('');

  // Fetch report details
  const {
    data: report,
    isLoading: reportLoading,
    error: reportError,
  } = useQuery({
    queryKey: ['report', reportId],
    queryFn: () => getReport(reportId),
  });

  // Fetch moderation history for this content
  const { data: history } = useQuery({
    queryKey: ['moderationHistory', report?.contentType, report?.contentId],
    queryFn: () =>
      getModerationHistory({
        contentType: report!.contentType,
        contentId: report!.contentId,
      }),
    enabled: !!report,
  });

  // Review mutation
  const reviewMutation = useMutation({
    mutationFn: (request: ReportReviewRequestDTO) =>
      reviewReport(reportId, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['report', reportId] });
      queryClient.invalidateQueries({ queryKey: ['reports'] });
      queryClient.invalidateQueries({ queryKey: ['moderationHistory'] });
      setShowApproveDialog(false);
      setShowRejectDialog(false);
      setAdminNote('');
    },
  });

  const handleApprove = () => {
    reviewMutation.mutate({
      status: 'RESOLVED',
      action: selectedAction,
      adminNote: adminNote || undefined,
    });
  };

  const handleReject = () => {
    reviewMutation.mutate({
      status: 'REJECTED',
      action: 'NO_ACTION',
      adminNote: adminNote || undefined,
    });
  };

  if (reportLoading) {
    return (
      <Card>
        <CardContent className="p-8 text-center">
          <p className="text-gray-500">Loading report...</p>
        </CardContent>
      </Card>
    );
  }

  if (reportError || !report) {
    return (
      <Card>
        <CardContent className="p-8 text-center">
          <p className="text-red-500">
            Error loading report: {(reportError as Error)?.message || 'Not found'}
          </p>
          <Button
            variant="outline"
            className="mt-4"
            onClick={() => router.push('/admin/reports')}
          >
            Back to Reports
          </Button>
        </CardContent>
      </Card>
    );
  }

  const isPending = report.status === 'PENDING';

  return (
    <div className="space-y-6">
      {/* Back button */}
      <Button variant="outline" onClick={() => router.push('/admin/reports')}>
        &larr; Back to Reports
      </Button>

      {/* Report Details */}
      <Card>
        <CardHeader>
          <div className="flex justify-between items-center">
            <CardTitle>Report #{report.id}</CardTitle>
            <StatusBadge status={report.status} />
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <p className="text-sm text-gray-500">Content Type</p>
              <ContentTypeBadge type={report.contentType} />
            </div>
            <div>
              <p className="text-sm text-gray-500">Content ID</p>
              <p className="font-mono">{report.contentId}</p>
            </div>
            <div>
              <p className="text-sm text-gray-500">Reason</p>
              <p className="font-medium">{report.reason}</p>
            </div>
            <div>
              <p className="text-sm text-gray-500">Reporter</p>
              <p>{report.reporterName}</p>
            </div>
            <div>
              <p className="text-sm text-gray-500">Created At</p>
              <p>{report.createdAt}</p>
            </div>
            {report.reviewedByName && (
              <div>
                <p className="text-sm text-gray-500">Reviewed By</p>
                <p>
                  {report.reviewedByName} at {report.reviewedAt}
                </p>
              </div>
            )}
          </div>

          {report.description && (
            <div>
              <p className="text-sm text-gray-500">Description</p>
              <p className="bg-gray-50 p-3 rounded border">{report.description}</p>
            </div>
          )}

          {/* Action Buttons */}
          {isPending && (
            <div className="flex gap-3 pt-4 border-t">
              <Button
                variant="destructive"
                onClick={() => setShowApproveDialog(true)}
              >
                Take Action
              </Button>
              <Button variant="outline" onClick={() => setShowRejectDialog(true)}>
                Reject Report
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Moderation History */}
      <Card>
        <CardHeader>
          <CardTitle>Moderation History</CardTitle>
        </CardHeader>
        <CardContent>
          {history && history.length > 0 ? (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Action</TableHead>
                  <TableHead>Admin</TableHead>
                  <TableHead>Note</TableHead>
                  <TableHead>Date</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {history.map((action) => (
                  <TableRow key={action.id}>
                    <TableCell>
                      <ActionBadge action={action.actionType} />
                    </TableCell>
                    <TableCell>{action.adminName}</TableCell>
                    <TableCell className="text-gray-500">
                      {action.note || '-'}
                    </TableCell>
                    <TableCell className="text-sm text-gray-500">
                      {action.createdAt}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          ) : (
            <p className="text-gray-500 text-center py-4">
              No previous moderation actions
            </p>
          )}
        </CardContent>
      </Card>

      {/* Approve Dialog */}
      <Dialog open={showApproveDialog} onOpenChange={setShowApproveDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Take Moderation Action</DialogTitle>
            <DialogDescription>
              Select an action to apply to this content.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div>
              <label className="text-sm font-medium">Action</label>
              <Select
                value={selectedAction}
                onValueChange={(v) => setSelectedAction(v as ModerationActionType)}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="SET_LIMITED">Set Limited (de-rank)</SelectItem>
                  <SelectItem value="SET_HIDDEN">Set Hidden (author only)</SelectItem>
                  <SelectItem value="SET_REMOVED">Set Removed (admin only)</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div>
              <label className="text-sm font-medium">Admin Note (optional)</label>
              <Textarea
                value={adminNote}
                onChange={(e) => setAdminNote(e.target.value)}
                placeholder="Reason for this action..."
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowApproveDialog(false)}>
              Cancel
            </Button>
            <Button
              variant="destructive"
              onClick={handleApprove}
              disabled={reviewMutation.isPending}
            >
              {reviewMutation.isPending ? 'Submitting...' : 'Apply Action'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Reject Dialog */}
      <Dialog open={showRejectDialog} onOpenChange={setShowRejectDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Reject Report</DialogTitle>
            <DialogDescription>
              This will dismiss the report without taking action on the content.
            </DialogDescription>
          </DialogHeader>
          <div className="py-4">
            <label className="text-sm font-medium">Admin Note (optional)</label>
            <Textarea
              value={adminNote}
              onChange={(e) => setAdminNote(e.target.value)}
              placeholder="Reason for rejection..."
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowRejectDialog(false)}>
              Cancel
            </Button>
            <Button onClick={handleReject} disabled={reviewMutation.isPending}>
              {reviewMutation.isPending ? 'Submitting...' : 'Reject Report'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
