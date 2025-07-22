export default function TournamentHeader() {
  return (
    <div className="mb-8 text-center">
      <div className="flex items-center justify-center gap-2 mb-4">
        <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center">
          <svg
            className="w-5 h-5 text-white"
            fill="currentColor"
            viewBox="0 0 20 20"
            aria-hidden="true"
          >
            <path
              fillRule="evenodd"
              d="M10 2L3 7v11a1 1 0 001 1h12a1 1 0 001-1V7l-7-5zM10 18V8.5l6-4.5v14h-12V4l6 4.5V18z"
              clipRule="evenodd"
            />
          </svg>
        </div>
        <h1 className="text-3xl font-bold text-gray-900">Créer un Tournoi</h1>
      </div>
      <p className="text-gray-600">
        Remplissez les informations ci-dessous pour créer votre nouveau tournoi
      </p>
    </div>
  );
}