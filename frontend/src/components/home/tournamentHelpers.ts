// Helpers partagés pour l'affichage des tournois
export const formatDate = (d?: string) => {
  if (!d) return '';
  try {
    return new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' }).format(new Date(d));
  } catch (e) {
    return d || '';
  }
};

export const formatDateRange = (start?: string, end?: string) => {
  if (!start && !end) return '';
  if (!start) return formatDate(end);
  if (!end) return formatDate(start);

  const ds = new Date(start);
  const de = new Date(end);
  if (isNaN(ds.getTime()) || isNaN(de.getTime())) return `${formatDate(start)} — ${formatDate(end)}`;

  const sameMonth = ds.getMonth() === de.getMonth() && ds.getFullYear() === de.getFullYear();
  const sameYear = ds.getFullYear() === de.getFullYear();

  if (sameMonth) {
    return `${ds.getDate()}–${de.getDate()} ${new Intl.DateTimeFormat('fr-FR', { month: 'short', year: 'numeric' }).format(de)}`;
  }
  if (sameYear) {
    return `${new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'short' }).format(ds)} — ${new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' }).format(de)}`;
  }
  return `${formatDate(start)} — ${formatDate(end)}`;
};

export const levelEmoji = (_l?: string) => '🏅';

export const genderEmoji = (g?: string) => {
  if (!g) return '⚧';
  const s = String(g).toUpperCase();
  if (s === 'MEN' || s === 'M' || s === 'MALE') return '♂️';
  if (s === 'WOMEN' || s === 'F' || s === 'FEMALE') return '♀️';
  if (s.includes('MIX') || s.includes('MIXED')) return '⚥';
  return '⚧';
};

export const genderLabel = (g?: string) => {
  if (!g) return '';
  const s = String(g).toUpperCase();
  if (s === 'MEN') return 'Hommes';
  if (s === 'WOMEN') return 'Femmes';
  return s.charAt(0) + s.slice(1).toLowerCase();
};

export const filterActiveTournaments = (list: any[]) => {
  if (!list || !Array.isArray(list)) return [];
  const now = new Date();
  return list.filter((t: any) => {
    if (!t) return false;
    if (!t.startDate || !t.endDate) return false;
    const ds = new Date(t.startDate);
    const de = new Date(t.endDate);
    if (isNaN(ds.getTime()) || isNaN(de.getTime())) return false;
    return now.getTime() >= ds.getTime() && now.getTime() <= de.getTime();
  });
};

