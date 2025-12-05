type SearchInputProps = {
  search: string
  onSearchChange: (value: string) => void
  placeholder?: string
}

export default function SearchInput({
  search,
  onSearchChange,
  placeholder = 'Rechercher...',
}: SearchInputProps) {
  return (
    <input
      type="text"
      placeholder={placeholder}
      value={search}
      onChange={(e) => onSearchChange(e.target.value)}
      className="h-10 w-full px-4 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-[#1b2d5e]"
    />
  )
}