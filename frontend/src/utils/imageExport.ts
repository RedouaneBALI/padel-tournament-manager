/** @jsxImportSource react */
import React from 'react';
import { Game } from '@/src/types/game';
import { toBlob, toPng } from 'html-to-image';
import { toast } from 'react-toastify';
import MatchShareCard from '@/src/components/match/MatchShareCard';

/**
 * Exporte un élément DOM en PNG et déclenche le téléchargement.
 * @param elementId - ID de l'élément DOM à exporter
 * @param filename - Nom du fichier à télécharger
 */
export async function exportBracketAsImage(elementId: string, filename = 'bracket.png') {
  const node = document.getElementById(elementId);
  if (!node) return;

  const originalStyle = {
    overflow: node.style.overflow,
    width: node.style.width,
    height: node.style.height,
    maxWidth: node.style.maxWidth,
  };

  try {
    const scrollWidth = node.scrollWidth;
    const scrollHeight = node.scrollHeight;

    node.style.overflow = 'visible';
    node.style.width = `${scrollWidth}px`;
    node.style.height = `${scrollHeight}px`;
    node.style.maxWidth = 'none';

    await new Promise((resolve) => setTimeout(resolve, 50));

    const dataUrl = await toPng(node, {
      width: scrollWidth,
      height: scrollHeight,
    });

    const link = document.createElement('a');
    link.download = filename;
    link.href = dataUrl;
    link.click();
  } catch (err) {
    console.error('Erreur lors de l’export en image :', err);
  } finally {
    node.style.overflow = originalStyle.overflow;
    node.style.width = originalStyle.width;
    node.style.height = originalStyle.height;
    node.style.maxWidth = originalStyle.maxWidth;
  }
}

/**
 * Génère une image d'un match à partir de MatchShareCard et la partage ou télécharge.
 * @param game - L'objet Game pour le match
 * @param tournamentName - Nom du tournoi
 * @param club - Nom du club
 * @param customFileName - Nom personnalisé du fichier (optionnel)
 * @param customTitle - Titre personnalisé pour le partage (optionnel)
 * @param customText - Texte personnalisé pour le partage (optionnel)
 * @param level - Niveau de détail ou de qualité (optionnel)
 */
export async function shareMatchImage(
  game: Game,
  tournamentName?: string,
  club?: string,
  customFileName?: string,
  customTitle?: string,
  customText?: string,
  level?: string
) {
  let tempContainer: HTMLDivElement | null = null;

  try {
    // Create a temporary container off-DOM
    tempContainer = document.createElement('div');
    tempContainer.style.position = 'fixed';
    tempContainer.style.top = '-9999px';
    tempContainer.style.left = '-9999px';
    tempContainer.style.zIndex = '-9999';
    tempContainer.style.opacity = '0';
    tempContainer.style.pointerEvents = 'none';
    tempContainer.style.padding = '0';
    tempContainer.style.margin = '0';
    tempContainer.style.border = 'none';
    tempContainer.style.overflow = 'visible';

    // Add to DOM before rendering
    document.body.appendChild(tempContainer);

    // Render the component into this container
    const { createRoot } = await import('react-dom/client');
    const root = createRoot(tempContainer);
    root.render(
      React.createElement(MatchShareCard, { game, tournamentName, club, level })
    );

    // Wait for render to complete and force layout recalculation
    await new Promise(resolve => setTimeout(resolve, 100));

    // Trigger a reflow to ensure layout is calculated
    tempContainer.offsetHeight;

    // Capture the element
    const shareElement = tempContainer.querySelector('.match-share-card') as HTMLElement;
    if (!shareElement) throw new Error('Élément non trouvé');

    // Force another reflow
    shareElement.offsetHeight;

    // Use scrollHeight for actual content, scrollWidth for actual width
    const elementWidth = shareElement.scrollWidth;
    const elementHeight = shareElement.scrollHeight;

    // Get the primary color for background
    const primaryColor = getComputedStyle(document.documentElement).getPropertyValue('--color-primary').trim() || '#3b82f6';

    const blob = await toBlob(shareElement, {
      cacheBust: true,
      backgroundColor: primaryColor,
      width: elementWidth,
      height: elementHeight,
      pixelRatio: 2,
    });

    // Cleanup
    root.unmount();
    if (tempContainer.parentNode) {
      document.body.removeChild(tempContainer);
    }

    if (!blob) throw new Error('Échec de la génération de l\'image');

    // Default filename, title, text
    const teamAName = game.teamA ? `${game.teamA.player1Name} & ${game.teamA.player2Name}` : 'Équipe A';
    const teamBName = game.teamB ? `${game.teamB.player1Name} & ${game.teamB.player2Name}` : 'Équipe B';
    const fileName = customFileName || `match-${teamAName}-vs-${teamBName}.png`;
    const title = customTitle || `Match ${tournamentName || 'Tournoi'}`;
    const text = customText || `Résultats du match ${teamAName} vs ${teamBName}`;

    const shareData = {
      title,
      text,
      files: [new File([blob], fileName, { type: 'image/png' })],
    };

    if (navigator.canShare && navigator.canShare(shareData)) {
      try {
        await navigator.share(shareData);
        toast.success('Image partagée avec succès !');
        return;
      } catch (err) {
        if ((err as Error).name !== 'AbortError') console.warn('Erreur partage:', err);
      }
    }

    // Fallback: download the image
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
    toast.success('Image téléchargée avec succès !');

  } catch (error) {
    // Cleanup in case of error
    if (tempContainer?.parentNode) {
      try {
        document.body.removeChild(tempContainer);
      } catch (e) {
        console.warn('Erreur cleanup:', e);
      }
    }
    console.error('Erreur lors de l\'export:', error);
    toast.error('Impossible d\'exporter l\'image pour le moment.');
  }
}
