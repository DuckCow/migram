'use client';

import { Suspense } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useRouter, useSearchParams } from 'next/navigation';
import { getReports } from '@/lib/api';
import { ReportStatus } from '@/lib/types';
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { StatusBadge, ContentTypeBadge } from '@/components/visibility-badge';

function ReportsContent() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const page = parseInt(searchParams.get('page') || '0');
  const status = (searchParams.get('status') as ReportStatus) || undefined;

  const { data, isLoading, error } = useQuery({
    queryKey: ['reports', { page, status }],
    queryFn: () => getReports({ page, size: 20, status }),
  });

  const handleStatusChange = (value: string) => {
    const params = new URLSearchParams();
    if (value && value !== 'ALL') {
      params.set('status', value);
    }
    params.set('page', '0');
    router.push(`/admin/reports?${params.toString()}`);
  };

  const handlePageChange = (newPage: number) => {
    const params = new URLSearchParams(searchParams.toString());
    params.set('page', newPage.toString());
    router.push(`/admin/reports?${params.toString()}`);
  };

  const handleRowClick = (reportId: number) => {
    router.push(`/admin/reports/${reportId}`);
  };

  if (isLoading) {
    return (
      <Card>
        <CardContent className="p-8 text-center">
          <p className="text-gray-500">Loading reports...</p>
        </CardContent>
      </Card>
    );
  }

  if (error) {
    return (
      <Card>
        <CardContent className="p-8 text-center">
          <p className="text-red-500">Error loading reports: {(error as Error).message}</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <div className="flex justify-between items-center">
            <CardTitle>Reports</CardTitle>
            <Select
              value={status || 'ALL'}
              onValueChange={handleStatusChange}
            >
              <SelectTrigger className="w-40">
                <SelectValue placeholder="Filter by status" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All</SelectItem>
                <SelectItem value="PENDING">Pending</SelectItem>
                <SelectItem value="RESOLVED">Resolved</SelectItem>
                <SelectItem value="REJECTED">Rejected</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-20">ID</TableHead>
                <TableHead>Type</TableHead>
                <TableHead>Reason</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Reporter</TableHead>
                <TableHead>Created</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data?.content.map((report) => (
                <TableRow
                  key={report.id}
                  className="cursor-pointer hover:bg-gray-100"
                  onClick={() => handleRowClick(report.id)}
                >
                  <TableCell className="font-mono">{report.id}</TableCell>
                  <TableCell>
                    <ContentTypeBadge type={report.contentType} />
                  </TableCell>
                  <TableCell>{report.reason}</TableCell>
                  <TableCell>
                    <StatusBadge status={report.status} />
                  </TableCell>
                  <TableCell>{report.reporterName}</TableCell>
                  <TableCell className="text-gray-500 text-sm">
                    {report.createdAt}
                  </TableCell>
                </TableRow>
              ))}
              {data?.content.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6} className="text-center text-gray-500 py-8">
                    No reports found
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>

          {/* Pagination */}
          {data && data.totalPages > 1 && (
            <div className="flex justify-between items-center mt-4">
              <p className="text-sm text-gray-500">
                Page {data.number + 1} of {data.totalPages} ({data.totalElements} total)
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
    </div>
  );
}

export default function ReportsPage() {
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
      <ReportsContent />
    </Suspense>
  );
}
