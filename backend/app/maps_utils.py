import requests
from app.config import GOOGLE_MAPS_API_KEY

def get_directions(origin_lat: float, origin_lng: float,
                   dest_lat: float, dest_lng: float,
                   waypoints: list[dict] = None):
    if not GOOGLE_MAPS_API_KEY:
        return _estimate_directions(origin_lat, origin_lng, dest_lat, dest_lng, waypoints)

    origin = f"{origin_lat},{origin_lng}"
    destination = f"{dest_lat},{dest_lng}"
    wp = ""
    if waypoints:
        wp = "|".join(f"{p['lat']},{p['lng']}" for p in waypoints)

    url = "https://maps.googleapis.com/maps/api/directions/json"
    params = {
        "origin": origin,
        "destination": destination,
        "key": GOOGLE_MAPS_API_KEY,
        "mode": "driving",
        "units": "metric",
        "language": "es"
    }
    if wp:
        params["waypoints"] = wp

    try:
        resp = requests.get(url, params=params, timeout=10)
        data = resp.json()
        if data["status"] != "OK":
            return _estimate_directions(origin_lat, origin_lng, dest_lat, dest_lng, waypoints)

        legs = []
        total_distance_m = 0
        total_duration_s = 0
        for leg in data["routes"][0]["legs"]:
            legs.append({
                "distance_m": leg["distance"]["value"],
                "distance_text": leg["distance"]["text"],
                "duration_s": leg["duration"]["value"],
                "duration_text": leg["duration"]["text"],
                "start_address": leg.get("start_address", ""),
                "end_address": leg.get("end_address", "")
            })
            total_distance_m += leg["distance"]["value"]
            total_duration_s += leg["duration"]["value"]

        polyline = data["routes"][0].get("overview_polyline", {}).get("points", "")

        return {
            "total_distance_m": total_distance_m,
            "total_distance_km": round(total_distance_m / 1000, 2),
            "total_duration_s": total_duration_s,
            "total_duration_min": round(total_duration_s / 60, 1),
            "total_duration_text": f"{total_duration_s // 60} min",
            "legs": legs,
            "polyline": polyline,
            "source": "google"
        }
    except Exception:
        return _estimate_directions(origin_lat, origin_lng, dest_lat, dest_lng, waypoints)


def _estimate_directions(origin_lat: float, origin_lng: float,
                         dest_lat: float, dest_lng: float,
                         waypoints: list[dict] = None):
    from math import radians, sin, cos, sqrt, atan2

    def haversine(lat1, lng1, lat2, lng2):
        R = 6371000
        dlat = radians(lat2 - lat1)
        dlng = radians(lng2 - lng1)
        a = sin(dlat/2)**2 + cos(radians(lat1)) * cos(radians(lat2)) * sin(dlng/2)**2
        c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c

    points = [{"lat": origin_lat, "lng": origin_lng}]
    if waypoints:
        points.extend(waypoints)
    points.append({"lat": dest_lat, "lng": dest_lng})

    total_distance_m = 0
    legs = []
    for i in range(len(points) - 1):
        d = haversine(points[i]["lat"], points[i]["lng"],
                      points[i+1]["lat"], points[i+1]["lng"])
        total_distance_m += d
        name_i = points[i].get("name", f"Punto {i}")
        name_j = points[i+1].get("name", f"Punto {i+1}")
        legs.append({
            "distance_m": round(d),
            "distance_text": f"{round(d/1000, 2)} km" if d >= 1000 else f"{round(d)} m",
            "duration_s": round(d / 1.4),
            "duration_text": f"{round(d / 1.4 / 60)} min",
            "start_address": name_i,
            "end_address": name_j
        })

    speed_ms = 1.4
    total_duration_s = round(total_distance_m / speed_ms)

    return {
        "total_distance_m": round(total_distance_m),
        "total_distance_km": round(total_distance_m / 1000, 2),
        "total_duration_s": total_duration_s,
        "total_duration_min": round(total_duration_s / 60, 1),
        "total_duration_text": f"{total_duration_s // 60} min",
        "legs": legs,
        "polyline": "",
        "source": "estimate"
    }
