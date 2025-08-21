// src/validation/__tests__/tournament.test.ts
import { TournamentFormSchema } from '../tournament';

describe('TournamentFormSchema', () => {
  it('should accept valid defaults', () => {
    const result = TournamentFormSchema.safeParse({});
    expect(result.success).toBe(true);
  });

  it('should trim and validate name', () => {
    const result = TournamentFormSchema.safeParse({ name: '  Open de Paris  ' });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.name).toBe('Open de Paris');
    }
  });

  it('should reject empty name', () => {
    const result = TournamentFormSchema.safeParse({ name: '' });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toBe('Le tournoi doit avoir un nom.');
    }
  });

  it('should enforce nbMaxPairs range in KNOCKOUT', () => {
    const result = TournamentFormSchema.safeParse({ nbMaxPairs: 2, format: 'KNOCKOUT' });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].path).toContain('nbMaxPairs');
    }
  });

  it('should enforce date order', () => {
    const result = TournamentFormSchema.safeParse({
      startDate: '2025-10-10',
      endDate: '2025-10-05'
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues.some(i => i.path.includes('endDate'))).toBe(true);
    }
  });
});