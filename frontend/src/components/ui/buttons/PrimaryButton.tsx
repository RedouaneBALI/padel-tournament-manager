'use client';

import React from 'react';

interface Props {
  children?: React.ReactNode;
  onClick?: () => void;
  disabled?: boolean;
  className?: string;
  type?: 'button' | 'submit' | 'reset';
  ariaLabel?: string;
  form?: string; // allow targeting a form by id when the button is outside the <form>
}

export default function PrimaryButton({ children, onClick, disabled = false, className = '', type = 'button', ariaLabel, form }: Props) {
  return (
    <button
      type={type}
      form={form}
      disabled={disabled}
      onClick={onClick}
      className={[
        'inline-flex items-center justify-center gap-2 whitespace-nowrap text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-60 disabled:cursor-not-allowed',
        'bg-green-600 text-on-primary hover:bg-green-700 h-9 rounded-md px-3 shadow-md',
        className,
      ].join(' ')}
      aria-label={ariaLabel}
    >
      {children}
    </button>
  );
}
