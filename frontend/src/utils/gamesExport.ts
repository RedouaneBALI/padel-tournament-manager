import type { Game } from '@/src/types/game';
import { stageLabels } from '@/src/types/stage';

export function exportGamesAsCSV(games: Game[], roundStage: string, roundName?: string) {
  // Construire les lignes CSV
  const headers = ['Heure', 'Court', 'Équipe A', 'Équipe B', 'Score', 'Statut'];
  const rows = games.map((game) => {
    const teamAName = game.teamA
      ? `${game.teamA.player1Name || ''} / ${game.teamA.player2Name || ''}`.trim()
      : 'TBD';
    const teamBName = game.teamB
      ? `${game.teamB.player1Name || ''} / ${game.teamB.player2Name || ''}`.trim()
      : 'TBD';

    const scoreText = game.score && game.score.sets && game.score.sets.length > 0
      ? game.score.sets.map((set) => `${set.teamAScore}-${set.teamBScore}`).join(' ')
      : '-';

    const status = game.finished ? 'Terminé' : game.scheduledTime ? 'Planifié' : 'À planifier';

    const time = game.scheduledTime || '-';
    const court = game.court || '-';

    return [time, court, teamAName, teamBName, scoreText, status];
  });

  // Créer le contenu CSV
  const csvContent = [
    headers.join(','),
    ...rows.map((row) => row.map((cell) => `"${cell}"`).join(','))
  ].join('\n');

  // Créer et télécharger le fichier
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const link = document.createElement('a');
  const url = URL.createObjectURL(blob);

  const stageName = stageLabels[roundStage as keyof typeof stageLabels] || roundStage;
  const fileName = `matchs_${stageName.replace(/\s+/g, '_')}_${new Date().toISOString().split('T')[0]}.csv`;

  link.setAttribute('href', url);
  link.setAttribute('download', fileName);
  link.style.visibility = 'hidden';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

