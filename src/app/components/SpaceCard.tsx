import { MapPin, Users, Clock } from 'lucide-react';

interface SpaceCardProps {
  id: number;
  name: string;
  type: string;
  location: string;
  capacity: number;
  price: number;
  image: string;
  availability: string;
  onBook: (space: any) => void;
}

export function SpaceCard({
  id,
  name,
  type,
  location,
  capacity,
  price,
  image,
  availability,
  onBook
}: SpaceCardProps) {
  return (
    <div className="bg-card rounded-lg overflow-hidden border border-border hover:shadow-lg transition-shadow">
      <div className="relative h-48 overflow-hidden">
        <img
          src={image}
          alt={name}
          className="w-full h-full object-cover"
        />
        <div className="absolute top-3 right-3 bg-background/90 px-3 py-1 rounded-full backdrop-blur-sm">
          <span className="text-sm">{type}</span>
        </div>
      </div>

      <div className="p-5">
        <h3 className="mb-2">{name}</h3>

        <div className="space-y-2 mb-4">
          <div className="flex items-center gap-2 text-muted-foreground">
            <MapPin className="w-4 h-4" />
            <span className="text-sm">{location}</span>
          </div>

          <div className="flex items-center gap-2 text-muted-foreground">
            <Users className="w-4 h-4" />
            <span className="text-sm">Up to {capacity} people</span>
          </div>

          <div className="flex items-center gap-2 text-muted-foreground">
            <Clock className="w-4 h-4" />
            <span className="text-sm">{availability}</span>
          </div>
        </div>

        <div className="flex items-center justify-between pt-4 border-t border-border">
          <div>
            <span className="text-2xl">${price}</span>
            <span className="text-muted-foreground text-sm">/hour</span>
          </div>

          <button
            onClick={() => onBook({ id, name, type, location, capacity, price, image, availability })}
            className="bg-primary text-primary-foreground px-6 py-2 rounded-lg hover:opacity-90 transition-opacity"
          >
            Book Now
          </button>
        </div>
      </div>
    </div>
  );
}
