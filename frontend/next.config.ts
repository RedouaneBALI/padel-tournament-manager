import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  reactStrictMode: true,
  output: 'standalone', // Important pour Firebase Functions
  // Désactiver l'optimisation d'images pour Firebase
  images: {
    unoptimized: true
  },
  // Configuration pour éviter les problèmes de build sur Firebase
  trailingSlash: false,
  generateBuildId: async () => {
    return 'build-' + Date.now();
  },
};

export default nextConfig;