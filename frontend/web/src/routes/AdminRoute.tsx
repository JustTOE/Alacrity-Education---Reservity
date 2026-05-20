// @ts-nocheck
import { useSpaces } from "@/hooks/useSpaces";
import { useNow } from "@/hooks/useNow";
import { VIBES } from "@/data/fixtures";
import AdminPage from "./_AdminPage";
import { useNavigate } from "react-router-dom";
import { useApp } from "@/state/AppStore";
import { useAuth } from "@/auth/AuthContext";

export default function AdminRouteWrapper() {
  const auth = useAuth();
  const navigate = useNavigate();
  const { data: spacesData } = useSpaces({ size: 100 });
  const { now } = useNow();
  const { showToast } = useApp();
  
  // Only allow ADMIN users to access this route
  if (auth.status !== "authenticated" || auth.user?.accountType !== "ADMIN") {
    return (
      <div style={{ padding: "40px 20px", textAlign: "center" }}>
        <h2>Access Denied</h2>
        <p>Only administrators can access the control panel.</p>
        <button onClick={() => navigate("/")} style={{ padding: "8px 16px", cursor: "pointer" }}>Go back</button>
      </div>
    );
  }

  if (!spacesData) return <div>Loading...</div>;

  const data = { SPACES: spacesData.spaces, now, VIBES, DATE_WINDOW: [] };
  return <AdminPage data={data} onNavigate={(p: any) => { if(p==='create') navigate('/create'); else if(p==='admin') navigate('/admin'); else navigate('/' + p); }} onToast={(m: any) => showToast(m, 'success')} />;
}