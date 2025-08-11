import { toPng } from 'html-to-image';

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