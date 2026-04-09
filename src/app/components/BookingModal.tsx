import { X, Calendar, Clock } from 'lucide-react';
import { useState } from 'react';

interface BookingModalProps {
  space: {
    id: number;
    name: string;
    type: string;
    price: number;
    image: string;
  } | null;
  onClose: () => void;
}

export function BookingModal({ space, onClose }: BookingModalProps) {
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    phone: '',
    date: '',
    startTime: '',
    endTime: '',
    attendees: ''
  });

  const [submitted, setSubmitted] = useState(false);

  if (!space) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitted(true);
    setTimeout(() => {
      onClose();
      setSubmitted(false);
      setFormData({
        name: '',
        email: '',
        phone: '',
        date: '',
        startTime: '',
        endTime: '',
        attendees: ''
      });
    }, 2000);
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData(prev => ({
      ...prev,
      [e.target.name]: e.target.value
    }));
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
      <div className="bg-background rounded-lg max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        <div className="sticky top-0 bg-background border-b border-border p-6 flex items-center justify-between">
          <h2>Book {space.name}</h2>
          <button
            onClick={onClose}
            className="p-2 hover:bg-accent rounded-lg transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {submitted ? (
          <div className="p-8 text-center">
            <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <svg className="w-8 h-8 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
              </svg>
            </div>
            <h3 className="mb-2">Booking Confirmed!</h3>
            <p className="text-muted-foreground">
              We've sent a confirmation email to {formData.email}
            </p>
          </div>
        ) : (
          <>
            <div className="p-6">
              <div className="flex gap-4 mb-6">
                <img
                  src={space.image}
                  alt={space.name}
                  className="w-32 h-24 object-cover rounded-lg"
                />
                <div>
                  <h3 className="mb-1">{space.name}</h3>
                  <p className="text-muted-foreground mb-2">{space.type}</p>
                  <p className="text-lg">${space.price}/hour</p>
                </div>
              </div>

              <form onSubmit={handleSubmit} className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block mb-2">Full Name</label>
                    <input
                      type="text"
                      name="name"
                      required
                      value={formData.name}
                      onChange={handleChange}
                      className="w-full px-4 py-2.5 bg-input-background rounded-lg border border-border focus:outline-none focus:ring-2 focus:ring-ring"
                      placeholder="John Doe"
                    />
                  </div>

                  <div>
                    <label className="block mb-2">Email</label>
                    <input
                      type="email"
                      name="email"
                      required
                      value={formData.email}
                      onChange={handleChange}
                      className="w-full px-4 py-2.5 bg-input-background rounded-lg border border-border focus:outline-none focus:ring-2 focus:ring-ring"
                      placeholder="john@example.com"
                    />
                  </div>

                  <div>
                    <label className="block mb-2">Phone</label>
                    <input
                      type="tel"
                      name="phone"
                      required
                      value={formData.phone}
                      onChange={handleChange}
                      className="w-full px-4 py-2.5 bg-input-background rounded-lg border border-border focus:outline-none focus:ring-2 focus:ring-ring"
                      placeholder="(555) 123-4567"
                    />
                  </div>

                  <div>
                    <label className="block mb-2">Number of Attendees</label>
                    <input
                      type="number"
                      name="attendees"
                      required
                      min="1"
                      value={formData.attendees}
                      onChange={handleChange}
                      className="w-full px-4 py-2.5 bg-input-background rounded-lg border border-border focus:outline-none focus:ring-2 focus:ring-ring"
                      placeholder="10"
                    />
                  </div>
                </div>

                <div>
                  <label className="block mb-2 flex items-center gap-2">
                    <Calendar className="w-4 h-4" />
                    Date
                  </label>
                  <input
                    type="date"
                    name="date"
                    required
                    value={formData.date}
                    onChange={handleChange}
                    className="w-full px-4 py-2.5 bg-input-background rounded-lg border border-border focus:outline-none focus:ring-2 focus:ring-ring"
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block mb-2 flex items-center gap-2">
                      <Clock className="w-4 h-4" />
                      Start Time
                    </label>
                    <input
                      type="time"
                      name="startTime"
                      required
                      value={formData.startTime}
                      onChange={handleChange}
                      className="w-full px-4 py-2.5 bg-input-background rounded-lg border border-border focus:outline-none focus:ring-2 focus:ring-ring"
                    />
                  </div>

                  <div>
                    <label className="block mb-2 flex items-center gap-2">
                      <Clock className="w-4 h-4" />
                      End Time
                    </label>
                    <input
                      type="time"
                      name="endTime"
                      required
                      value={formData.endTime}
                      onChange={handleChange}
                      className="w-full px-4 py-2.5 bg-input-background rounded-lg border border-border focus:outline-none focus:ring-2 focus:ring-ring"
                    />
                  </div>
                </div>

                <div className="pt-4 border-t border-border">
                  <button
                    type="submit"
                    className="w-full bg-primary text-primary-foreground py-3 rounded-lg hover:opacity-90 transition-opacity"
                  >
                    Confirm Booking
                  </button>
                </div>
              </form>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
