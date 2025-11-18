// app/admin/layout.tsx
// Ajoute des metadata pour indiquer aux moteurs de recherche de ne pas indexer les pages /admin
export const metadata = {
  robots: {
    index: false,
    follow: false,
  },
};

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return <>{children}</>;
}

