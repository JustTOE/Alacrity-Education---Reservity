import { useState, useMemo } from 'react';
import { Building2, Search, Menu } from 'lucide-react';
import { SpaceCard } from './components/SpaceCard';
import { FilterBar } from './components/FilterBar';
import { BookingModal } from './components/BookingModal';

const SPACES = [
  {
    id: 1,
    name: "Modern Conference Hub",
    type: "Conference Room",
    location: "Downtown, New York",
    capacity: 20,
    price: 85,
    image: "https://images.unsplash.com/photo-1771147372627-7fffe86cf00b?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080",
    availability: "Available today"
  },
  {
    id: 2,
    name: "Executive Boardroom",
    type: "Conference Room",
    location: "Midtown, Manhattan",
    capacity: 15,
    price: 120,
    image: "https://images.unsplash.com/photo-1687945727613-a4d06cc41024?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080",
    availability: "Available tomorrow"
  },
  {
    id: 3,
    name: "Tech Innovation Space",
    type: "Conference Room",
    location: "Silicon Valley",
    capacity: 12,
    price: 95,
    image: "https://images.unsplash.com/photo-1771147372634-976f022c0033?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080",
    availability: "Available today"
  },
  {
    id: 4,
    name: "Panoramic Meeting Room",
    type: "Meeting Room",
    location: "Financial District",
    capacity: 25,
    price: 110,
    image: "https://images.unsplash.com/photo-1762176263996-a0713a49ee4d?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080",
    availability: "Available today"
  },
  {
    id: 5,
    name: "Contemporary Board Suite",
    type: "Conference Room",
    location: "Business Bay",
    capacity: 18,
    price: 100,
    image: "https://images.unsplash.com/photo-1764810815228-b7f9432eec5c?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080",
    availability: "Available this week"
  },
  {
    id: 6,
    name: "Collaborative Workspace",
    type: "Coworking Space",
    location: "Brooklyn Heights",
    capacity: 30,
    price: 75,
    image: "https://images.unsplash.com/photo-1560821630-1a7c45c3286e?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080",
    availability: "Available today"
  },
  {
    id: 7,
    name: "Premium Office Suite",
    type: "Coworking Space",
    location: "Central Business District",
    capacity: 8,
    price: 65,
    image: "https://images.unsplash.com/photo-1739863528149-0828fdb4d34c?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080",
    availability: "Available today"
  },
  {
    id: 8,
    name: "Modern Cowork Hub",
    type: "Coworking Space",
    location: "Tech Park",
    capacity: 40,
    price: 90,
    image: "https://images.unsplash.com/photo-1742630394179-67539224abc0?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080",
    availability: "Available this week"
  },
  {
    id: 9,
    name: "Grand Banquet Hall",
    type: "Event Hall",
    location: "Convention Center",
    capacity: 200,
    price: 350,
    image: "https://images.unsplash.com/photo-1762765684665-6b6855bb6fe6?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080",
    availability: "Available next week"
  },
  {
    id: 10,
    name: "Elegant Event Venue",
    type: "Event Hall",
    location: "Uptown Plaza",
    capacity: 150,
    price: 280,
    image: "https://images.unsplash.com/photo-1768851142332-75f3d1b47452?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080",
    availability: "Available today"
  },
  {
    id: 11,
    name: "Premium Event Space",
    type: "Event Hall",
    location: "Grand Hotel",
    capacity: 120,
    price: 250,
    image: "https://images.unsplash.com/photo-1759477274116-e3cb02d2b9d8?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080",
    availability: "Available tomorrow"
  },
  {
    id: 12,
    name: "Luxury Reception Hall",
    type: "Event Hall",
    location: "Waterfront District",
    capacity: 180,
    price: 320,
    image: "https://images.unsplash.com/photo-1768508951405-10e83c4a2872?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080",
    availability: "Available this week"
  }
];

export default function App() {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedType, setSelectedType] = useState('all');
  const [selectedCapacity, setSelectedCapacity] = useState('all');
  const [selectedSpace, setSelectedSpace] = useState<any>(null);

  const filteredSpaces = useMemo(() => {
    return SPACES.filter(space => {
      const matchesSearch = space.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                          space.location.toLowerCase().includes(searchTerm.toLowerCase());

      const matchesType = selectedType === 'all' || space.type === selectedType;

      let matchesCapacity = true;
      if (selectedCapacity !== 'all') {
        if (selectedCapacity === '1-10') matchesCapacity = space.capacity <= 10;
        else if (selectedCapacity === '11-25') matchesCapacity = space.capacity >= 11 && space.capacity <= 25;
        else if (selectedCapacity === '26-50') matchesCapacity = space.capacity >= 26 && space.capacity <= 50;
        else if (selectedCapacity === '51+') matchesCapacity = space.capacity >= 51;
      }

      return matchesSearch && matchesType && matchesCapacity;
    });
  }, [searchTerm, selectedType, selectedCapacity]);

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="bg-card border-b border-border sticky top-0 z-40 backdrop-blur-sm bg-card/95">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="bg-primary text-primary-foreground p-2 rounded-lg">
                <Building2 className="w-6 h-6" />
              </div>
              <div>
                <h1 className="text-xl">SpaceRental</h1>
                <p className="text-xs text-muted-foreground">Find Your Perfect Space</p>
              </div>
            </div>

            <nav className="hidden md:flex items-center gap-6">
              <a href="#spaces" className="text-sm hover:text-primary transition-colors">Browse Spaces</a>
              <a href="#" className="text-sm hover:text-primary transition-colors">How It Works</a>
              <a href="#" className="text-sm hover:text-primary transition-colors">List Your Space</a>
              <button className="bg-primary text-primary-foreground px-4 py-2 rounded-lg hover:opacity-90 transition-opacity">
                Sign In
              </button>
            </nav>

            <button className="md:hidden p-2 hover:bg-accent rounded-lg">
              <Menu className="w-6 h-6" />
            </button>
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <section className="bg-gradient-to-br from-primary/5 via-background to-background py-16 md:py-24">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <h1 className="text-4xl md:text-5xl lg:text-6xl mb-6 max-w-4xl mx-auto">
            Book the Perfect Space for Your Next Event
          </h1>
          <p className="text-lg md:text-xl text-muted-foreground mb-8 max-w-2xl mx-auto">
            From intimate meetings to grand celebrations, find and book professional venues by the hour
          </p>

          <div className="flex flex-col sm:flex-row gap-4 justify-center items-center">
            <div className="flex items-center gap-2 bg-card px-4 py-2 rounded-lg border border-border">
              <div className="w-2 h-2 bg-green-500 rounded-full"></div>
              <span className="text-sm">200+ Spaces Available</span>
            </div>
            <div className="flex items-center gap-2 bg-card px-4 py-2 rounded-lg border border-border">
              <div className="w-2 h-2 bg-blue-500 rounded-full"></div>
              <span className="text-sm">Instant Booking</span>
            </div>
            <div className="flex items-center gap-2 bg-card px-4 py-2 rounded-lg border border-border">
              <div className="w-2 h-2 bg-purple-500 rounded-full"></div>
              <span className="text-sm">Verified Venues</span>
            </div>
          </div>
        </div>
      </section>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12" id="spaces">
        <div className="mb-8">
          <h2 className="mb-2">Available Spaces</h2>
          <p className="text-muted-foreground">
            {filteredSpaces.length} {filteredSpaces.length === 1 ? 'space' : 'spaces'} found
          </p>
        </div>

        <div className="mb-8">
          <FilterBar
            searchTerm={searchTerm}
            selectedType={selectedType}
            selectedCapacity={selectedCapacity}
            onSearchChange={setSearchTerm}
            onTypeChange={setSelectedType}
            onCapacityChange={setSelectedCapacity}
          />
        </div>

        {filteredSpaces.length === 0 ? (
          <div className="text-center py-16">
            <Search className="w-16 h-16 mx-auto mb-4 text-muted-foreground" />
            <h3 className="mb-2">No spaces found</h3>
            <p className="text-muted-foreground">
              Try adjusting your search criteria
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {filteredSpaces.map(space => (
              <SpaceCard
                key={space.id}
                {...space}
                onBook={setSelectedSpace}
              />
            ))}
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="bg-card border-t border-border mt-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
            <div>
              <div className="flex items-center gap-2 mb-4">
                <Building2 className="w-5 h-5" />
                <span>SpaceRental</span>
              </div>
              <p className="text-sm text-muted-foreground">
                Your trusted platform for booking professional spaces by the hour.
              </p>
            </div>

            <div>
              <h4 className="mb-4">For Renters</h4>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li><a href="#" className="hover:text-foreground transition-colors">Browse Spaces</a></li>
                <li><a href="#" className="hover:text-foreground transition-colors">How It Works</a></li>
                <li><a href="#" className="hover:text-foreground transition-colors">Pricing</a></li>
              </ul>
            </div>

            <div>
              <h4 className="mb-4">For Owners</h4>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li><a href="#" className="hover:text-foreground transition-colors">List Your Space</a></li>
                <li><a href="#" className="hover:text-foreground transition-colors">Owner Dashboard</a></li>
                <li><a href="#" className="hover:text-foreground transition-colors">Resources</a></li>
              </ul>
            </div>

            <div>
              <h4 className="mb-4">Company</h4>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li><a href="#" className="hover:text-foreground transition-colors">About Us</a></li>
                <li><a href="#" className="hover:text-foreground transition-colors">Contact</a></li>
                <li><a href="#" className="hover:text-foreground transition-colors">Terms of Service</a></li>
              </ul>
            </div>
          </div>

          <div className="border-t border-border mt-8 pt-8 text-center text-sm text-muted-foreground">
            <p>&copy; 2026 SpaceRental. All rights reserved.</p>
          </div>
        </div>
      </footer>

      {/* Booking Modal */}
      {selectedSpace && (
        <BookingModal
          space={selectedSpace}
          onClose={() => setSelectedSpace(null)}
        />
      )}
    </div>
  );
}