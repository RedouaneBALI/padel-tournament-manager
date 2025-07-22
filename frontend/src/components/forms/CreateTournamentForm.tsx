'use client';

import { useState } from 'react';
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

import TournamentHeader from '@/components/tournament/TournamentHeader';
import TournamentInfoSection from '@/components/tournament/TournamentInfoSection';
import TournamentLocationSection from '@/components/tournament/TournamentLocationSection';
import TournamentConfigSection from '@/components/tournament/TournamentConfigSection';
import TournamentDatesSection from '@/components/tournament/TournamentDatesSection';

export default function CreateTournamentForm() {
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    city: '',
    club: '',
    gender: '',
    level: '',
    tournamentFormat: '',
    nbSeeds: '',
    startDate: '',
    endDate: '',
  });

  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value === '' ? '' : value,
    }));
  };

  const handleSelectChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);

    // Filtrer les valeurs vides
    const payload = Object.fromEntries(
      Object.entries(formData).filter(([_, value]) => value !== '' && value !== null && value !== undefined)
    );

    try {
      const response = await fetch('http://localhost:8080/tournaments', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
      });

      if (response.ok) {
        toast.success('Tournoi créé avec succès !');
        // Reset form
        setFormData({
          name: '',
          description: '',
          city: '',
          club: '',
          gender: '',
          level: '',
          tournamentFormat: '',
          nbSeeds: 0,
          startDate: '',
          endDate: '',
        });
      } else {
        toast.error('Erreur lors de la création du tournoi');
      }
    } catch (error) {
      toast.error("Une erreur s'est produite. Veuillez réessayer.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 p-4 md:p-8">
      <div className="mx-auto max-w-4xl">
        <TournamentHeader />

        <div className="bg-white rounded-lg shadow-lg border border-gray-200">
          <div className="p-6 border-b border-gray-200">
            <h2 className="text-xl font-semibold text-gray-900 flex items-center gap-2">
              <div className="w-5 h-5 bg-blue-600 rounded flex items-center justify-center">
                <svg className="w-3 h-3 text-white" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M10 2L3 7v11a1 1 0 001 1h12a1 1 0 001-1V7l-7-5z" clipRule="evenodd" />
                </svg>
              </div>
              Informations du Tournoi
            </h2>
            <p className="text-gray-500 mt-1">
              Configurez les détails de votre tournoi
            </p>
          </div>

          <div className="p-6">
            <form onSubmit={handleSubmit} className="space-y-8">
              <TournamentInfoSection formData={formData} handleInputChange={handleInputChange} />
              <TournamentLocationSection formData={formData} handleInputChange={handleInputChange} />
              <TournamentConfigSection formData={formData} handleInputChange={handleInputChange} handleSelectChange={handleSelectChange} />
              <TournamentDatesSection formData={formData} handleInputChange={handleInputChange} />

              <div className="flex justify-end pt-6 border-t border-gray-200">
                <button
                  type="submit"
                  disabled={isSubmitting || !formData.name}
                  className="px-8 py-3 bg-blue-600 text-white font-medium rounded-md shadow-sm hover:bg-blue-700 focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  {isSubmitting ? 'Création en cours...' : 'Créer le tournoi'}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
      <ToastContainer position="top-center" autoClose={10000} hideProgressBar closeOnClick pauseOnFocusLoss draggable pauseOnHover />
    </div>
  );
}