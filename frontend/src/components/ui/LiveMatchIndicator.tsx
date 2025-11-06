'use client';

interface LiveMatchIndicatorProps {
  className?: string;
}

export default function LiveMatchIndicator({ className = '' }: LiveMatchIndicatorProps) {
  return (
    <div className={`relative ${className}`}>
      <div className="w-3 h-3 bg-green-400 rounded-full animate-pulse"></div>
      <div className="absolute top-0 left-0 w-3 h-3 bg-green-400 rounded-full animate-ping opacity-75"></div>
    </div>
  );
}

