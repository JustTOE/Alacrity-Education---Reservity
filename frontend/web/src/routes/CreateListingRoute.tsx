// @ts-nocheck
import { useSpaces } from "@/hooks/useSpaces";
import { useNow } from "@/hooks/useNow";
import { useCreateSpace } from "@/hooks/useCreateSpace";
import { VIBES } from "@/data/fixtures";
import CreateListingPage from "./_CreateListingPage";
import { useNavigate } from "react-router-dom";
import { useApp } from "@/state/AppStore";
import { useQueryClient } from "@tanstack/react-query";

export default function CreateListingRouteWrapper() {
  const queryClient = useQueryClient();
  const { data: spacesData } = useSpaces({ size: 100 });
  const { now } = useNow();
  const navigate = useNavigate();
  const { showToast } = useApp();
  const { submit: submitSpace, loading, error } = useCreateSpace();
  
  if (!spacesData) return <div>Loading...</div>;

  const handleSubmit = async (formData: any) => {
    try {
      const space = await submitSpace({
        name: formData.name,
        type: formData.type || "open",
        building: formData.building,
        floor: parseInt(formData.floor) || 0,
        room: formData.room,
        seats: parseInt(formData.seats) || 0,
        area: parseInt(formData.area) || 0,
        price: formData.isFree ? 0 : (parseFloat(formData.price) || 0),
        blurb: formData.tagline,
        description: formData.description,
        amenities: Array.from(formData.amenities || []),
        rules: formData.rules || [],
        vibes: Array.from(formData.vibes || []),
        pinX: formData.pin?.x,
        pinY: formData.pin?.y,
        surprise: formData.surprise,
        dropIn: formData.dropIn,
        instantBook: formData.instantBook,
      });

      if (space) {
        showToast("Listing created successfully!", 'success');
        // Invalidate all spaces queries to reflect new listing everywhere
        await queryClient.invalidateQueries({ queryKey: ["spaces"] });
        // Navigate to the new space
        navigate(`/spaces/${space.id}`);
      } else {
        showToast(error || "Failed to create listing", 'error');
      }
    } catch (err: any) {
      showToast(err.message || "Error creating listing", 'error');
    }
  };


  const data = { SPACES: spacesData.spaces, now, VIBES, DATE_WINDOW: [] };
  return (
    <CreateListingPage
      data={data}
      onNavigate={(p: any) => {
        if (p === 'create') navigate('/create');
        else if (p === 'admin') navigate('/admin');
        else navigate('/' + p);
      }}
      onToast={(m: any) => showToast(m, 'success')}
      onSubmit={handleSubmit}
      isSubmitting={loading}
    />
  );
}