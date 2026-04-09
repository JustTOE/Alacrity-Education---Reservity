import { Search } from 'lucide-react';

interface FilterBarProps {
  searchTerm: string;
  selectedType: string;
  selectedCapacity: string;
  onSearchChange: (value: string) => void;
  onTypeChange: (value: string) => void;
  onCapacityChange: (value: string) => void;
}

export function FilterBar({
  searchTerm,
  selectedType,
  selectedCapacity,
  onSearchChange,
  onTypeChange,
  onCapacityChange
}: FilterBarProps) {
  return (
    <div className="bg-card border border-border rounded-lg p-6 shadow-sm">
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
          <input
            type="text"
            placeholder="Search spaces..."
            value={searchTerm}
            onChange={(e) => onSearchChange(e.target.value)}
            className="w-full pl-10 pr-4 py-2.5 bg-input-background rounded-lg border border-border focus:outline-none focus:ring-2 focus:ring-ring"
          />
        </div>

        <select
          value={selectedType}
          onChange={(e) => onTypeChange(e.target.value)}
          className="px-4 py-2.5 bg-input-background rounded-lg border border-border focus:outline-none focus:ring-2 focus:ring-ring"
        >
          <option value="all">All Types</option>
          <option value="Conference Room">Conference Room</option>
          <option value="Event Hall">Event Hall</option>
          <option value="Coworking Space">Coworking Space</option>
          <option value="Meeting Room">Meeting Room</option>
        </select>

        <select
          value={selectedCapacity}
          onChange={(e) => onCapacityChange(e.target.value)}
          className="px-4 py-2.5 bg-input-background rounded-lg border border-border focus:outline-none focus:ring-2 focus:ring-ring"
        >
          <option value="all">Any Capacity</option>
          <option value="1-10">1-10 people</option>
          <option value="11-25">11-25 people</option>
          <option value="26-50">26-50 people</option>
          <option value="51+">51+ people</option>
        </select>
      </div>
    </div>
  );
}
