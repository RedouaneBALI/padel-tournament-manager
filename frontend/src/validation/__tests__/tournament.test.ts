// src/validation/__tests__/tournament.test.ts
import { TournamentFormSchema } from '../tournament';

describe('TournamentFormSchema', () => {
  it('should accept valid defaults', () => {
    const result = TournamentFormSchema.safeParse({ name: 'Test Tournament', config: { format: 'KNOCKOUT' } });
    expect(result.success).toBe(true);
  });

  it('should trim and validate name', () => {
    const result = TournamentFormSchema.safeParse({ name: '  Open de Paris  ', config: { format: 'KNOCKOUT' } });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.name).toBe('Open de Paris');
    }
  });

  it('should reject empty name', () => {
    const result = TournamentFormSchema.safeParse({ name: '', config: { format: 'KNOCKOUT' } });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toBe('Le nom du tournoi est requis.');
    }
  });

  it('should enforce mainDrawSize range in KNOCKOUT', () => {
    const result = TournamentFormSchema.safeParse({ name: 'Test', config: { format: 'KNOCKOUT', mainDrawSize: 1 } });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].path).toEqual(['config', 'mainDrawSize']);
    }
  });

  it('should enforce date order', () => {
    const result = TournamentFormSchema.safeParse({
      name: 'Test',
      config: { format: 'KNOCKOUT' },
      startDate: '2025-10-10',
      endDate: '2025-10-05'
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues.some(i => i.message.includes('date de fin'))).toBe(true);
    }
  });
});