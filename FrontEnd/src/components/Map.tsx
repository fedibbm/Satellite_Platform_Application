"use client";

import { useEffect, useRef } from 'react';
import { MapContainer, TileLayer, FeatureGroup, useMap } from 'react-leaflet';
import { EditControl } from 'react-leaflet-draw';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import 'leaflet-draw/dist/leaflet.draw.css';
import { Feature as GeoJSONFeature } from 'geojson'; // Import GeoJSON Feature type

interface MapProps {
  onShapeCreated: (e: any) => void;
  imageUrl?: string;
  onClearShape: () => void;
  initialRegion?: GeoJSONFeature | null; // Add initialRegion prop
}

// Component to handle image layer updates
const ImageLayerComponent = ({ url }: { url?: string }) => {
  const map = useMap();
  const layerRef = useRef<L.TileLayer | null>(null);

  useEffect(() => {
    if (layerRef.current) {
      map.removeLayer(layerRef.current);
      layerRef.current = null;
    }

    if (url) {
      console.log('Adding new tile layer with URL:', url);
      layerRef.current = L.tileLayer(url);
      layerRef.current.addTo(map);
    }

    return () => {
      if (layerRef.current) {
        map.removeLayer(layerRef.current);
      }
    };
  }, [url, map]);

  return null;
};

// Component to handle auto-resizing when container dimensions change
const ResizeHandler = () => {
  const map = useMap();
  useEffect(() => {
    const resizeObserver = new ResizeObserver(() => {
      map.invalidateSize();
    });
    const container = map.getContainer();
    resizeObserver.observe(container);
    return () => resizeObserver.unobserve(container);
  }, [map]);
  return null;
};

// Component to handle initial region drawing and map fitting
const InitialRegionHandler = ({ region }: { region?: GeoJSONFeature | null }) => {
    const map = useMap();
    const featureGroup = L.featureGroup().addTo(map); // Use a separate feature group or the main one? Let's use the main one via ref later.

    useEffect(() => {
        // This effect should ideally access the main FeatureGroup ref from the parent Map component
        // For now, let's assume we can access it or create a temporary one.
        // A better approach might be to pass the featureGroupRef down or handle this logic in the main Map component.

        // Let's try handling it directly in the main Map component's useEffect instead.
        // This component might not be necessary if we modify the main Map component's effect.

    }, [region, map]);

    return null;
}


const Map: React.FC<MapProps> = ({ onShapeCreated, imageUrl, onClearShape, initialRegion }) => {
  const featureGroupRef = useRef<L.FeatureGroup>(null); // Use L.FeatureGroup type
  const mapRef = useRef<L.Map | null>(null); // Ref to access map instance

  useEffect(() => {
    // Clear layers if image URL is removed (existing logic)
    if (!imageUrl && featureGroupRef.current) {
      featureGroupRef.current.clearLayers();
    }
  }, [imageUrl]);

  // Effect to handle drawing the initial region
  useEffect(() => {
    if (initialRegion && featureGroupRef.current && mapRef.current) {
        console.log("Drawing initial region:", initialRegion);
        const featureGroup = featureGroupRef.current;
        featureGroup.clearLayers(); // Clear any previous drawings

        try {
            const geoJsonLayer = L.geoJSON(initialRegion);
            // Leaflet-draw requires individual shapes (L.Polygon, etc.), not a LayerGroup.
            // We extract the actual layers from the L.geoJSON group and add them directly.
            geoJsonLayer.eachLayer((layer: any) => {
                if (layer instanceof L.Polygon && !(layer instanceof L.Rectangle)) {
                    const latlngs = layer.getLatLngs()[0] as any[];
                    // GeoJSON rectangles are saved as Polygons with 5 points (closed loop)
                    if (Array.isArray(latlngs) && latlngs.length === 5) {
                        const bounds = layer.getBounds();
                        // Check if the polygon's points perfectly match the corners of its bounding box
                        const hasNW = latlngs.some((ll: any) => ll.lat === bounds.getNorthWest().lat && ll.lng === bounds.getNorthWest().lng);
                        const hasNE = latlngs.some((ll: any) => ll.lat === bounds.getNorthEast().lat && ll.lng === bounds.getNorthEast().lng);
                        const hasSE = latlngs.some((ll: any) => ll.lat === bounds.getSouthEast().lat && ll.lng === bounds.getSouthEast().lng);
                        const hasSW = latlngs.some((ll: any) => ll.lat === bounds.getSouthWest().lat && ll.lng === bounds.getSouthWest().lng);

                        if (hasNW && hasNE && hasSE && hasSW) {
                            // It's a perfect rectangle, recreate it as an L.Rectangle
                            const rect = L.rectangle(bounds);
                            featureGroup.addLayer(rect);
                            return;
                        }
                    }
                }
                featureGroup.addLayer(layer);
            });

            // Fit map bounds to the drawn layer
            mapRef.current.fitBounds(geoJsonLayer.getBounds());
        } catch (error) {
            console.error("Error adding initial region GeoJSON layer:", error);
        }
    }
    // Run when initialRegion changes
  }, [initialRegion]);


  const handleShapeCreated = (e: any) => {
    console.log('Shape created:', e.layer.toGeoJSON());
    onShapeCreated(e);
  };

  const handleShapeEdited = (e: any) => {
    e.layers.eachLayer((layer: any) => {
      console.log('Shape edited:', layer.toGeoJSON());
      onShapeCreated({ layer }); // Reuse the creation shape handler to update the config
    });
  };

  return (
    <MapContainer
      ref={mapRef} // Assign the ref here
      center={[0, 0]}
      zoom={2}
      className="h-full w-full rounded-lg relative z-0"
      // whenReady prop doesn't take the instance, ref is used instead
    >
      <ResizeHandler />
      <TileLayer
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
      />
      <FeatureGroup ref={featureGroupRef}>
        <EditControl
          position="topright"
          onCreated={handleShapeCreated}
          onEdited={handleShapeEdited}
          onDeleted={onClearShape}
          edit={{
            edit: true,
            remove: true,
          }}
          draw={{
            rectangle: true,
            polygon: true,
            circle: false,
            circlemarker: false,
            marker: false,
            polyline: false,
          }}
        />
      </FeatureGroup>
      <ImageLayerComponent url={imageUrl} />
    </MapContainer>
  );
};

export default Map;
