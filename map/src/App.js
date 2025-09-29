
/*

TEST CORDINATES FOR IOT DEVICES (they leave enough space to make tests)

37.9710, 23.7655

37.9672, 23.7671

*/

import { GoogleMap, LoadScript, Marker, InfoWindow, Circle } from '@react-google-maps/api';
import { useState, useEffect, useRef, useCallback } from 'react';
import './App.css';
import websocketService from './services/websocket-service';
import { SensorData } from './SensorData';
import { AndroidDeviceData } from './AndroidData';

const containerStyle = {
  width: '100%',
  height: '100vh'
};

// Centered around Athens
const center = {
  lat: 37.99,
  lng: 23.73
};

function App() {
  const [selectedMarker, setSelectedMarker] = useState(null);
  const [markers, setMarkers] = useState([]); // IoT sensors
  const [androidDevice, setAndroidDevice] = useState(null); // Android device
  const [mapLoaded, setMapLoaded] = useState(false);
  const [moderateMarkers, setModerateMarkers] = useState([]);
  const [criticalMarkers, setCriticalMarkers] = useState([]);
  const mapRef = useRef(null);
  const rectangleRef = useRef(null);

  useEffect(() => {

    // Initialize both WebSocket connections
    websocketService.connect();
    websocketService.connectAndroidLocation();

    // Subscribe to IoT sensor messages
    const unsubscribe = websocketService.subscribe((newSensorData) => {
      console.log("Received WebSocket message:", newSensorData);
      const updatedSensor = SensorData.fromWebSocket(newSensorData);

      setMarkers((currentMarkers) => {
        const index = currentMarkers.findIndex(marker => marker.id === updatedSensor.id);
        if (index === -1) {
          return [...currentMarkers, updatedSensor];
        } else {
          const updatedMarkers = [...currentMarkers];
          updatedMarkers[index] = updatedSensor;
          return updatedMarkers;
        }
      });

      if (selectedMarker && selectedMarker.id === updatedSensor.id) {
        setSelectedMarker(updatedSensor);
      }
    });

    // Subscribe to Android Location messages
    const unsubscribeAndroid = websocketService.subscribeAndroidLocation((newAndroidData) => {

      // update position based on new data
      console.log("Received Android location message:", newAndroidData);
      const updatedDevice = AndroidDeviceData.fromWebSocket(newAndroidData);

      setAndroidDevice(updatedDevice);

      // checking specifically to select the correct sprite (png) based on id 
      if (selectedMarker && selectedMarker.id === updatedDevice.id) {
        setSelectedMarker(updatedDevice);
      }
    });

    // Cleanup on unmount
    return () => {
      unsubscribe();
      unsubscribeAndroid();
      websocketService.disconnect();
    };
  }, [selectedMarker]);

  // Update markers for different statuses
  useEffect(() => {
    setModerateMarkers(markers.filter(marker => marker.getStatus() === 'Moderate'));
    setCriticalMarkers(markers.filter(marker => marker.getStatus() === 'Critical'));
  }, [markers]);

  const onLoad = (map) => {
    mapRef.current = map;
    setMapLoaded(true);
  };

  // choose icon for the different type of markers
  const getMarkerIcon = (marker) => {
    if (!mapLoaded || !window.google) {
      return { url: '/img_default.png', scaledSize: { width: 32, height: 32 } };
    }

    if (marker instanceof AndroidDeviceData) {
      return {
        url: '/img_user.png', // Unique Android device icon
        scaledSize: new window.google.maps.Size(40, 40)
      };
    }

    return {
      url: marker.getStatus() === 'Critical'
        ? '/img_critical.png'
        : marker.getStatus() === 'Moderate'
        ? '/img_moderate.png'
        : '/img_default.png',
      scaledSize: new window.google.maps.Size(28, 28)
    };
  };

  // using callback to keep the old reference (not draw blank)
  const drawRectangle = useCallback((color, targetMarkers) => {
    if (!mapLoaded || !window.google || targetMarkers.length < 2) return;

    const [device1, device2] = targetMarkers.slice(0, 2);
    const bounds = new window.google.maps.LatLngBounds();

    bounds.extend(new window.google.maps.LatLng(device1.lat, device1.lng));
    bounds.extend(new window.google.maps.LatLng(device2.lat, device2.lng));

    if (rectangleRef.current) {
      rectangleRef.current.setMap(null);
    }

    rectangleRef.current = new window.google.maps.Rectangle({
      bounds: bounds,
      map: mapRef.current,
      strokeColor: color,
      strokeOpacity: 0,
      strokeWeight: 0,
      fillColor: color,
      fillOpacity: 0.35,
    });
  }, [mapLoaded]);

  // Draw rectangles based on marker status
  useEffect(() => {
    if (mapLoaded) {
      if (markers.some(marker => marker.isDis)) {
        if (rectangleRef.current) {
          rectangleRef.current.setMap(null);
        }
        return;
      }

      // completed logic for circles, this works as per assignment requests
      if ((criticalMarkers.length >= 1 && moderateMarkers.length >= 1) || criticalMarkers.length >= 2) {

        drawRectangle("red", markers);
      } else if (moderateMarkers.length >= 2) {
      
        drawRectangle("yellow", markers);
      } else if (rectangleRef.current) {
      
        rectangleRef.current.setMap(null);
      }
    }
  }, [mapLoaded, moderateMarkers, criticalMarkers, markers, drawRectangle]);

  // defining structure with html
  // for entities markers -> circles, android device (user device)
  return (
    <div className="App">
      <LoadScript googleMapsApiKey="AIzaSyCa3wzqfmj4mlerWhZK4lEBMrWXAHsuzfM">
        <GoogleMap
          mapContainerStyle={containerStyle}
          center={center}
          zoom={6}
          onLoad={onLoad}
        >
          {mapLoaded &&
            markers.map(marker => (
              <Marker
                key={marker.id}
                position={{ lat: marker.lat, lng: marker.lng }}
                onClick={() => setSelectedMarker(marker)}
                icon={getMarkerIcon(marker)}
              />
            ))
          }

          {/* Android Device Marker (Separate from IoT devices, to avoid confusion) */}
          {mapLoaded && androidDevice && (
            <Marker
              key={androidDevice.id}
              position={{ lat: androidDevice.lat, lng: androidDevice.lng }}
              onClick={() => setSelectedMarker(androidDevice)}
              icon={getMarkerIcon(androidDevice)}
            />
          )}

          {/* Render circles for IoT devices only */}
          {mapLoaded &&
            markers.map(marker => (
              <Circle
                key={`${marker.id}-circle`}
                center={{ lat: marker.lat, lng: marker.lng }}
                radius={100} // Define the drawn surface of rectangle
                options={{
                  strokeColor: marker.isDis ? 'red' : 'lime',
                  strokeOpacity: 0,
                  strokeWeight: 0,
                  fillColor: marker.isDis ? 'red' : 'lime',
                  fillOpacity: 0.35,
                }}
              />
            ))
          }

          {mapLoaded && selectedMarker && (
            <InfoWindow
              position={{ lat: selectedMarker.lat, lng: selectedMarker.lng }}
              onCloseClick={() => setSelectedMarker(null)}
            > 
              <div>
                <h3>{selectedMarker.label}</h3>
                <p>Status: {selectedMarker.getStatus()}</p>
                {!(selectedMarker instanceof AndroidDeviceData) && (
                  <>
                    <p>Battery: {selectedMarker.battery}%</p>
                    <p>Temperature: {selectedMarker.temperature}°C</p>
                    <p>Smoke: {selectedMarker.smoke}</p>
                    <p>Gas: {selectedMarker.gas}</p>
                    <p>UV: {selectedMarker.uv}</p>
                  </>
                )}
              </div>
            </InfoWindow>
          )}
        </GoogleMap>
      </LoadScript>
    </div>
  );
}

export default App;
