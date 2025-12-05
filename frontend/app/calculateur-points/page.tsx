import { redirect } from 'next/navigation';

export default function OldPointsCalculatorRedirect() {
  // Permanent redirect (301) to new path under /frmt
  redirect('/frmt/calculateur-points');
}
