'use client';

import { useState } from 'react';
import { Tournament } from '@/app/types/Tournament';
import TournamentOverviewTab from './TournamentOverviewTab';
import TournamentPlayersTabWrapper from './TournamentPlayersTabWrapper';
import TournamentResultsTab from './TournamentResultsTab';

const TABS = {
  OVERVIEW: 'overview',
  PLAYERS: 'joueurs',
  RESULTS: 'resultats',
};

const TAB_CONFIG = [
  { id: TABS.OVERVIEW, label: 'Aperçu' },
  { id: TABS.PLAYERS, label: 'Joueurs' },
  { id: TABS.RESULTS, label: 'Tableau' },
];

interface TournamentTabsProps {
  tournament: Tournament;
}

export default function TournamentTabs({ tournament }: TournamentTabsProps) {
  const [activeTab, setActiveTab] = useState(TABS.OVERVIEW);

  return (
    <>
      <div className="flex justify-center mb-6 space-x-4 border-b">
        {TAB_CONFIG.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`pb-2 px-4 font-semibold transition-colors duration-200 ${
              activeTab === tab.id
                ? 'border-b-2 border-[#1b2d5e] text-primary'
                : 'text-gray-500 hover:text-primary'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <div>
        {activeTab === TABS.OVERVIEW && <TournamentOverviewTab tournament={tournament} />}
        {activeTab === TABS.PLAYERS && <TournamentPlayersTabWrapper tournament={tournament} />}
        {activeTab === TABS.RESULTS && (
          <TournamentResultsTab />
        )}
      </div>
    </>
  );
}