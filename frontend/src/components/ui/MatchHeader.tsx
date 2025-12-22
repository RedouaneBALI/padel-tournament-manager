import React from 'react';
import { normalizeGroup, groupBadgeClasses } from '@/src/utils/groupBadge';
import LiveMatchIndicator from '@/src/components/ui/LiveMatchIndicator';
import SaveAndCancelButtons from '@/src/components/ui/SaveAndCancelButtons';
import { Edit3, Share } from 'lucide-react';
import { FaStar, FaRegStar } from 'react-icons/fa';

interface MatchHeaderProps {
  badgeLabel: string;
  pool?: { name?: string };
  editable: boolean;
  isInProgress: boolean;
  editing: boolean;
  isSaving: boolean;
  onCancel: () => void;
  onSave: () => void;
  onEdit: () => void;
  showExport?: boolean;
  onExport?: () => void;
  isFavorite?: boolean;
  onToggleFavorite?: () => void;
}

export default function MatchHeader({
  badgeLabel,
  pool,
  editable,
  isInProgress,
  editing,
  isSaving,
  onCancel,
  onSave,
  onEdit,
  showExport,
  onExport,
  isFavorite,
  onToggleFavorite,
}: MatchHeaderProps) {
  const group = normalizeGroup(pool?.name);

  const editControls = editing ? (
    <SaveAndCancelButtons
      isSaving={isSaving}
      onCancel={onCancel}
      bindEnter={editing}
      onSave={onSave}
    />
  ) : (
    <button
      onClick={(e) => {
        e.stopPropagation();
        onEdit();
      }}
      className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 hover:bg-accent hover:text-accent-foreground h-7 w-7 p-0 hover:bg-primary/10 hover:text-primary"
      title="Modifier les scores"
    >
      <Edit3 className="h-4 w-4" />
    </button>
  );

  return (
    <div className="flex justify-between items-start px-2 pt-2">
      <div className="flex items-center gap-2">
        {(badgeLabel !== '') && (
          <div
            className={[
              'inline-block text-xs font-medium rounded mt-1 mx-1 px-3 py-0.5',
              pool?.name ? groupBadgeClasses(group) : 'bg-border text-foreground'
            ].join(' ')}
          >
            {badgeLabel}
          </div>
        )}
        {onToggleFavorite && !editable && (
          <button
            onClick={(e) => {
              e.stopPropagation();
              onToggleFavorite();
            }}
            className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 hover:bg-accent hover:text-accent-foreground h-7 w-7 p-0 hover:bg-primary/10 hover:text-primary"
            title={isFavorite ? "Retirer des favoris" : "Ajouter aux favoris"}
          >
            {isFavorite ? (
              <FaStar className="h-4 w-4 text-yellow-500" />
            ) : (
              <FaRegStar className="h-4 w-4" />
            )}
          </button>
        )}
      </div>
      {(editable || showExport) && (
        <div className="z-10 ml-auto flex items-center gap-2">
          {editable ? editControls : (
            showExport && onExport && (
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  onExport();
                }}
                className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 hover:bg-accent hover:text-accent-foreground h-7 w-7 p-0 hover:bg-primary/10 hover:text-primary"
                title="Exporter l'image du match"
              >
                <Share className="h-4 w-4" />
              </button>
            )
          )}
        </div>
      )}
    </div>
  );
}
