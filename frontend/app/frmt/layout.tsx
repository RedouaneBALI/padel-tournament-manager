'use client';

import BottomNav, { BottomNavItem } from '@/src/components/ui/BottomNav';
import { usePathname } from 'next/navigation';
import { Home as HomeIcon, Users as UsersIcon, Calculator as CalculatorIcon } from 'lucide-react';

const navItems: BottomNavItem[] = [
	{ href: '/', label: 'Accueil', Icon: HomeIcon },
	{ href: '/frmt/classement/hommes', label: 'Hommes', Icon: UsersIcon },
	{ href: '/frmt/classement/femmes', label: 'Femmes', Icon: UsersIcon },
	{ href: '/frmt/calculateur-points', label: 'Calculateur', Icon: CalculatorIcon },
];

export default function FRMTLayout({ children }: { children: React.ReactNode }) {
	const pathname = usePathname() ?? '';

	return (
		<>
			<BottomNav items={navItems} pathname={pathname} />
			{children}
		</>
	);
}
