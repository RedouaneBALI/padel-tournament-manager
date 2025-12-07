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
  const yesterday = new Date(now.getTime() - 24 * 60 * 60 * 1000);
  const dayAfterTomorrow = new Date(now.getTime() + 2 * 24 * 60 * 60 * 1000);
  console.log('Filtering active tournaments. Now:', now, 'Yesterday:', yesterday, 'Day after tomorrow:', dayAfterTomorrow);
  console.log('Total tournaments received:', list.length);
  const filtered = list.filter((t: any) => {
    if (!t) {
      console.log('Tournament is null/undefined');
      return false;
    }
    if (!t.endDate) {
      console.log('Tournament has no endDate:', t.name || t.id);
      return false;
    }
    const de = new Date(t.endDate);
    if (isNaN(de.getTime())) {
      console.log('Invalid endDate for tournament:', t.name || t.id, 'endDate:', t.endDate);
      return false;
    }
    const included = de >= yesterday && de <= dayAfterTomorrow;
    console.log('Tournament:', t.name || t.id, 'endDate:', t.endDate, 'de:', de.toDateString(), 'included:', included);
    return included;
  });
  console.log('Filtered tournaments count:', filtered.length);
  return filtered;
};
