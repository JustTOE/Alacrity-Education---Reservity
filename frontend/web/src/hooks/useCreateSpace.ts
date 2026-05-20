import { useState } from "react";
import { spacesApi, CreateSpaceRequest } from "@/api/spaces";
import type { Space } from "@/types";

interface UseCreateSpaceState {
  loading: boolean;
  error: string | null;
  success: boolean;
}

export function useCreateSpace() {
  const [state, setState] = useState<UseCreateSpaceState>({
    loading: false,
    error: null,
    success: false,
  });

  const submit = async (data: CreateSpaceRequest): Promise<Space | null> => {
    setState({ loading: true, error: null, success: false });
    try {
      const space = await spacesApi.create(data);
      setState({ loading: false, error: null, success: true });
      return space;
    } catch (err) {
      const errorMsg =
        err instanceof Error ? err.message : "Unknown error occurred";
      setState({ loading: false, error: errorMsg, success: false });
      return null;
    }
  };

  const reset = () =>
    setState({ loading: false, error: null, success: false });

  return { ...state, submit, reset };
}
