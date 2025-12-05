import { SlidersHorizontal } from 'lucide-react'

type Props = {
  onClick: () => void
}

export default function FilterButton({ onClick }: Props) {
  return (
    <button
      onClick={() => {
        console.log('FilterButton clicked')
        onClick()
      }}
      className="h-10 w-full md:w-auto flex items-center justify-center md:justify-start gap-2 px-4 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-[#1b2d5e]"
    >
      <SlidersHorizontal size={16} />
      <span>Filtres</span>
    </button>
  )
}