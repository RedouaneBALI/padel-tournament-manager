'use client';

interface GroupSelectorProps {
  groups: string[];
  value: 'ALL' | string;
  onChange: (v: 'ALL' | string) => void;
}

export default function GroupSelector({ groups, value, onChange }: GroupSelectorProps) {
  return (
    <div className="w-full flex justify-center sm:max-w-[400px]">
      <select
        id="group-select"
        value={value}
        onChange={(e) => onChange(e.target.value as 'ALL' | string)}
        aria-label="Sélection de poule"
        className="w-full max-w-xl px-3 py-2 border border-border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-colors"
      >
        <option value="ALL">Tous</option>
        {groups.map((g) => (
          <option key={g} value={g}>{`Poule ${g}`}</option>
        ))}
      </select>
    </div>
  );
}
