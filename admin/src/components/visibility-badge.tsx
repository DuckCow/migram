import { Badge } from '@/components/ui/badge';
import { ContentVisibility, ReportStatus, ModerationActionType } from '@/lib/types';

// Visibility Badge with color coding
export function VisibilityBadge({ visibility }: { visibility: ContentVisibility }) {
  const variants: Record<ContentVisibility, { className: string; label: string }> = {
    PUBLIC: { className: 'bg-green-500 hover:bg-green-600', label: 'PUBLIC' },
    LIMITED: { className: 'bg-yellow-500 hover:bg-yellow-600', label: 'LIMITED' },
    HIDDEN: { className: 'bg-orange-500 hover:bg-orange-600', label: 'HIDDEN' },
    REMOVED: { className: 'bg-red-500 hover:bg-red-600', label: 'REMOVED' },
  };

  const { className, label } = variants[visibility];

  return (
    <Badge className={`${className} text-white`}>
      {label}
    </Badge>
  );
}

// Report Status Badge
export function StatusBadge({ status }: { status: ReportStatus }) {
  const variants: Record<ReportStatus, { className: string; label: string }> = {
    PENDING: { className: 'bg-blue-500 hover:bg-blue-600', label: 'PENDING' },
    RESOLVED: { className: 'bg-green-500 hover:bg-green-600', label: 'RESOLVED' },
    REJECTED: { className: 'bg-gray-500 hover:bg-gray-600', label: 'REJECTED' },
  };

  const { className, label } = variants[status];

  return (
    <Badge className={`${className} text-white`}>
      {label}
    </Badge>
  );
}

// Action Type Badge
export function ActionBadge({ action }: { action: ModerationActionType }) {
  const variants: Record<ModerationActionType, { className: string; label: string }> = {
    NO_ACTION: { className: 'bg-gray-400 hover:bg-gray-500', label: 'No Action' },
    SET_LIMITED: { className: 'bg-yellow-500 hover:bg-yellow-600', label: 'Set Limited' },
    SET_HIDDEN: { className: 'bg-orange-500 hover:bg-orange-600', label: 'Set Hidden' },
    SET_REMOVED: { className: 'bg-red-500 hover:bg-red-600', label: 'Set Removed' },
    RESTORE_PUBLIC: { className: 'bg-green-500 hover:bg-green-600', label: 'Restored' },
  };

  const { className, label } = variants[action];

  return (
    <Badge className={`${className} text-white`}>
      {label}
    </Badge>
  );
}

// Content Type Badge
export function ContentTypeBadge({ type }: { type: 'POST' | 'COMMENT' }) {
  return (
    <Badge variant="outline" className="font-mono">
      {type}
    </Badge>
  );
}
