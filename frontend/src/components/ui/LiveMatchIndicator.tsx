'use client';

interface LiveMatchIndicatorProps {
  className?: string;
  showLabel?: boolean;
}

export default function LiveMatchIndicator({ className = '', showLabel = true }: LiveMatchIndicatorProps) {
  return (
    <div className={`flex items-center gap-1.5 ${className}`}>
      <div className="relative">
        <div className="w-3 h-3 bg-red-500 rounded-full animate-pulse"></div>
        <div className="absolute top-0 left-0 w-3 h-3 bg-red-500 rounded-full animate-ping opacity-75"></div>
      </div>
      {showLabel && (
        <span className="text-xs font-semibold text-red-500 uppercase tracking-wide">Live</span>
      )}
    </div>
  );
}

