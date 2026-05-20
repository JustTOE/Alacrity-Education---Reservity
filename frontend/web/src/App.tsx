import { Outlet } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import Nav from "@/components/layout/Nav";
import Footer from "@/components/layout/Footer";
import ScrollToTop from "@/components/layout/ScrollToTop";
import BookingFlow from "@/components/booking/BookingFlow";
import ErrorBoundary from "@/components/ui/ErrorBoundary";
import Toast from "@/components/ui/Toast";
import { AppStoreProvider } from "@/state/AppStore";
import { AuthProvider } from "@/auth/AuthContext";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
      staleTime: 30_000,
    },
  },
});

export default function App() {
  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <AppStoreProvider>
            <div className="app">
              <ScrollToTop />
              <Nav />
              <main style={{ flex: 1 }}>
                <Outlet />
              </main>
              <Footer />
              <BookingFlow />
              <Toast />
            </div>
          </AppStoreProvider>
        </AuthProvider>
      </QueryClientProvider>
    </ErrorBoundary>
  );
}
