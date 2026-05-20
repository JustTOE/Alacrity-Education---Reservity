import { createBrowserRouter } from "react-router-dom";
import App from "./App";
import AccountRoute from "./routes/AccountRoute";
import LandingRoute from "./routes/LandingRoute";
import ListingsRoute from "./routes/ListingsRoute";
import LoginRoute from "./routes/LoginRoute";
import PassRoute from "./routes/PassRoute";
import PlaceholderRoute from "./routes/PlaceholderRoute";
import RegisterRoute from "./routes/RegisterRoute";
import AdminRoute from "./routes/AdminRoute";
import CreateListingRoute from "./routes/CreateListingRoute";
import { RequireAuth } from "@/auth/RequireAuth";

export const router = createBrowserRouter([
  {
    path: "/",
    element: <App />,
    children: [
      { index: true, element: <LandingRoute /> },
      {
        path: "spaces",
        children: [
          { index: true, element: <ListingsRoute /> },
          { path: ":id", element: <ListingsRoute /> },
        ],
      },
      { path: "login", element: <LoginRoute /> },
      { path: "register", element: <RegisterRoute /> },
      { 
        path: "admin", 
        element: (
          <RequireAuth>
            <AdminRoute />
          </RequireAuth>
        )
      },
      { 
        path: "create", 
        element: (
          <RequireAuth>
            <CreateListingRoute />
          </RequireAuth>
        )
      },
      {
        path: "pass/:id",
        element: (
          <RequireAuth>
            <PassRoute />
          </RequireAuth>
        ),
      },
      {
        path: "account",
        element: (
          <RequireAuth>
            <AccountRoute />
          </RequireAuth>
        ),
      },
      {
        path: "*",
        element: <PlaceholderRoute title="Not found" body="That page doesn't exist (yet). Try Home or Spaces." />,
      },
    ],
  },
]);
