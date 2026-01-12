'use client';

import { Suspense } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useRouter, useSearchParams } from 'next/navigation';
import { getRecentActions } from '@/lib/api';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { ActionBadge, ContentTypeBadge } from '@/components/visibility-badge';

function HistoryContent() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const page = parseInt(searchParams.get('page') || '0');

  const { data, isLoading, error } = useQuery({
    queryKey: ['recentActions', page],
    queryFn: () => getRecentActions({ page, size: 50 }),
  });

  const handlePageChange = (newPage: number) => {
    const params = new URLSearchParams();
    params.set('page', newPage.toString());
    router.push(`/admin/history?${params.toString()}`);
  };

  if (isLoading) {
    return (
      <Card>
        <CardContent className="p-8 text-center">
          <p className="text-gray-500">Loading history...</p>
        </CardContent>
      </Card>
    );
  }

  if (error) {
    return (
      <Card>
        <CardContent className="p-8 text-center">
          <p className="text-red-500">
            Error loading history: {(error as Error).message}
          </p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Moderation History</CardTitle>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-20">ID</TableHead>
              <TableHead>Action</TableHead>
              <TableHead>Content</TableHead>
              <TableHead>Admin</TableHead>
              <TableHead>Note</TableHead>
              <TableHead>Date</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data?.content.map((action) => (
              <TableRow key={action.id}>
                <TableCell className="font-mono">{action.id}</TableCell>
                <TableCell>
                  <ActionBadge action={action.actionType} />
                </TableCell>
                <TableCell>
                  <div className="flex items-center gap-2">
                    <ContentTypeBadge type={action.contentType} />
                    <span className="font-mono text-sm">#{action.contentId}</span>
                  </div>
                </TableCell>
                <TableCell>{action.adminName}</TableCell>
                <TableCell className="text-gray-500 max-w-xs truncate">
                  {action.note || '-'}
                </TableCell>
                <TableCell className="text-gray-500 text-sm">
                  {action.createdAt}
                </TableCell>
              </TableRow>
            ))}
            {data?.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-gray-500 py-8">
                  No moderation actions yet
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>

        {/* Pagination */}
        {data && data.totalPages > 1 && (
          <div className="flex justify-between items-center mt-4">
            <p className="text-sm text-gray-500">
              Page {data.number + 1} of {data.totalPages} ({data.totalElements}{' '}
              total)
            </p>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={data.first}
                onClick={() => handlePageChange(page - 1)}
              >
                Previous
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={data.last}
                onClick={() => handlePageChange(page + 1)}
              >
                Next
              </Button>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

export default function HistoryPage() {
  return (
    <Suspense
      fallback={
        <Card>
          <CardContent className="p-8 text-center">
            <p className="text-gray-500">Loading...</p>
          </CardContent>
        </Card>
      }
    >
      <HistoryContent />
    </Suspense>
  );
}
