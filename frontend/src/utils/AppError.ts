export class AppError extends Error {
  constructor(public code: string) {
    super(code);
    this.name = 'AppError';
  }

  static UNAUTHORIZED = 'UNAUTHORIZED';
  static FORBIDDEN = 'FORBIDDEN';
  static ALREADY_VOTED = 'ALREADY_VOTED';
}

