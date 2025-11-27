'use client';

import React from 'react';

export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger';

type ButtonProps = React.ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
  className?: string;
};

const variantClassMap: Record<ButtonVariant, string> = {
  primary: 'btn-primary',
  secondary: 'btn-secondary',
  ghost: 'btn-ghost',
  danger: 'btn-danger',
};

export default React.forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  { children, className = '', variant = 'primary', type = 'button', ...rest },
  ref,
) {
  const classes = ['btn', variantClassMap[variant] || '', className].filter(Boolean).join(' ');
  return (
    <button ref={ref} type={type} className={classes} {...rest}>
      {children}
    </button>
  );
});

