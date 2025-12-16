import React from 'react';

interface MatchFooterProps {
  editing: boolean;
  footerClass: string;
  localCourt: string;
  localScheduledTime: string;
  setLocalCourt: (court: string) => void;
  setLocalScheduledTime: (time: string) => void;
  court?: string;
  scheduledTime?: string;
  handleSave: () => void;
}

export default function MatchFooter({
  editing,
  footerClass,
  localCourt,
  localScheduledTime,
  setLocalCourt,
  setLocalScheduledTime,
  court,
  scheduledTime,
  handleSave,
}: MatchFooterProps) {
  const footerContent = editing ? (
    <div className="flex gap-4 items-center">
      <input
        type="text"
        value={localCourt}
        onChange={(e) => setLocalCourt(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === 'NumpadEnter') {
            e.preventDefault();
            handleSave();
          }
        }}
        enterKeyHint="done"
        className="px-2 py-1 rounded border text-sm text-foreground bg-card"
        placeholder="Court"
      />
      <input
        type="time"
        step="300"
        value={localScheduledTime}
        onChange={(e) => setLocalScheduledTime(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === 'NumpadEnter') {
            e.preventDefault();
            handleSave();
          }
        }}
        enterKeyHint="done"
        className="px-2 py-1 rounded border text-sm text-foreground bg-card"
      />
    </div>
  ) : (
    <div className="flex justify-between">
      <span>{court ?? localCourt}</span>
      <span>{scheduledTime ?? localScheduledTime}</span>
    </div>
  );

  return (
    <div
      className={[
        'border-t border-gray-300 px-4 py-2 text-sm',
        footerClass
      ].join(' ')}
    >
      {footerContent}
    </div>
  );
}
