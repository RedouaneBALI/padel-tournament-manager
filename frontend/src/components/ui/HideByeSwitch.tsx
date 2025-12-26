import { Switch } from '@/src/components/ui/Switch';

interface HideByeSwitchProps {
  hasBye: boolean;
  hideBye: boolean;
  setHideBye: (value: boolean) => void;
}

export default function HideByeSwitch({ hasBye, hideBye, setHideBye }: HideByeSwitchProps) {
  if (!hasBye) return null;

  return (
    <div className="mb-4 flex items-center gap-2">
      <Switch id="hide-bye" checked={hideBye} onCheckedChange={setHideBye} />
      <label htmlFor="hide-bye" className="text-sm">Masquer les matchs BYE</label>
    </div>
  );
}
