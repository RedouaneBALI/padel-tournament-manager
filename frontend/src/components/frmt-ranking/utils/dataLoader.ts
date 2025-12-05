// utils/dataLoader.ts
import fs from 'fs/promises';
import path from 'path';


/**
 * Charge les données d'un fichier JSON spécifique dans le dossier public.
 * @param filename Le nom du fichier JSON (ex: 'ranking-men.json', 'tournaments.json').
 * @returns Un tableau du type T ou un tableau vide en cas d'erreur.
 */
export async function loadDataFromJson<T>(filename: string): Promise<T[]> {
  const filePath = path.join(process.cwd(), 'public', filename);
  try {
    const fileContent = await fs.readFile(filePath, 'utf-8');
    const parsedJson = JSON.parse(fileContent);

    if (parsedJson && Array.isArray(parsedJson.data)) {
      return parsedJson.data as T[]; // Caste explicitement vers le type T[]
    } else {
      console.warn(`Avertissement: Le fichier ${filename} ne contient pas de propriété 'data' qui soit un tableau. Structure inattendue.`);
      return [];
    }
  } catch (error) {
    console.error(`Erreur lors de la lecture ou de l'analyse du fichier ${filename}:`, error);
    return [];
  }
}