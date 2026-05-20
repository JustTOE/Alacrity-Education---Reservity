import type { Space } from "@/types";
import IsometricRoom from "./IsometricRoom";

interface PhotoFrameProps {
  space: Space;
  alt?: string;
  width?: number;
  height?: number;
}

/**
 * Renders the primary image for a space when available; otherwise falls back
 * to the brand-default `IsometricRoom` SVG. Used by SpaceCard, DetailPanel,
 * BookingFlow header, etc.
 */
export default function PhotoFrame({
  space,
  alt,
  width = 360,
  height = 240,
}: PhotoFrameProps) {
  const url =
    space.primaryImageUrl ??
    space.images?.find((i) => i.isPrimary)?.url ??
    space.images?.[0]?.url ??
    null;

  if (!url) {
    return <IsometricRoom space={space} width={width} height={height} />;
  }

  return (
    <img
      src={url}
      alt={alt ?? space.name}
      width={width}
      height={height}
      loading="lazy"
      style={{ width: "100%", height: "100%", objectFit: "cover", display: "block" }}
    />
  );
}
