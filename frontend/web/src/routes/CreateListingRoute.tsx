// @ts-nocheck
import { useSpaces } from "@/hooks/useSpaces";
import { useNow } from "@/hooks/useNow";
import { VIBES } from "@/data/fixtures";
import CreateListingPage from "./_CreateListingPage";
import { useNavigate } from "react-router-dom";
import { useApp } from "@/state/AppStore";

export default function CreateListingRouteWrapper() {
  const { data: spacesData } = useSpaces({ size: 100 });
  const { now } = useNow();
  const navigate = useNavigate();
  const { showToast } = useApp();
  
  if (!spacesData) return <div>Loading...</div>;

  const data = { SPACES: spacesData.spaces, now, VIBES, DATE_WINDOW: [] };
  return <CreateListingPage data={data} onNavigate={(p: any) => { if(p==='create') navigate('/create'); else if(p==='admin') navigate('/admin'); else navigate('/' + p); }} onToast={(m: any) => showToast(m, 'success')} />;
}