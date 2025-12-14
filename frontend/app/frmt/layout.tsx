'use client';

import BottomNav, { BottomNavItem } from '@/src/components/ui/BottomNav';
import { usePathname } from 'next/navigation';
import { Home as HomeIcon, Users as UsersIcon, Calculator as CalculatorIcon } from 'lucide-react';
import { useEffect } from 'react';

const navItems: BottomNavItem[] = [
	{ href: '/', label: 'Accueil', Icon: HomeIcon },
	{ href: '/frmt/classement/hommes', label: 'Hommes', Icon: UsersIcon },
	{ href: '/frmt/classement/femmes', label: 'Femmes', Icon: UsersIcon },
	{ href: '/frmt/calculateur-points', label: 'Calculateur', Icon: CalculatorIcon },
];

export default function FRMTLayout({ children }: { children: React.ReactNode }) {
	const pathname = usePathname() ?? '';

	useEffect(() => {
		// Hide the main header banner for FRMT pages except calculator
		const header = document.querySelector('header.sticky');
		if (header && pathname !== '/frmt/calculateur-points') {
			(header as HTMLElement).style.display = 'none';
		} else if (header && pathname === '/frmt/calculateur-points') {
			(header as HTMLElement).style.display = '';
		}

		// Cleanup when component unmounts (when leaving FRMT pages)
		return () => {
			const header = document.querySelector('header.sticky');
			if (header) {
				(header as HTMLElement).style.display = '';
			}
		};
	}, [pathname]);

	return (
		<div className="bg-background">
			<main>
				{children}
			</main>
			<BottomNav items={navItems} pathname={pathname} />
		</div>
	);
}
