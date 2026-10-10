package com.nexus.core.utils;

import java.util.List;

import com.nexus.core.model.entities.CapacityForecast;

/**
 * Shared tolerant lane matching between shipment addresses and route
 * capacity lanes (city names vs full address text, either direction).
 */
public final class RouteCapacityMatcher {

    private RouteCapacityMatcher() {
    }

    public static String normLane(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    public static boolean laneCovers(String lane, String place) {
        String a = normLane(lane);
        String b = normLane(place);
        return !a.isEmpty() && !b.isEmpty() && (a.equals(b) || a.contains(b) || b.contains(a));
    }

    public static boolean routeCovers(CapacityForecast c, String pickup, String delivery) {
        return laneCovers(c.getOriginLane(), pickup) && laneCovers(c.getDestinationLane(), delivery);
    }

    /**
     * First capacity covering pickup → delivery that also declares a
     * transport mode, or {@code null} when none matches.
     */
    public static CapacityForecast matchWithMode(List<CapacityForecast> candidates, String pickup,
            String delivery) {
        if (candidates == null) return null;
        for (CapacityForecast c : candidates) {
            if (c.getTransportMode() == null) continue;
            if (routeCovers(c, pickup, delivery)) return c;
        }
        return null;
    }
}
