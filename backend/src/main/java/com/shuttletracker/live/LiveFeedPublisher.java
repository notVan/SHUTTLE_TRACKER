package com.shuttletracker.live;

/** Pushes live updates to every open dashboard. Services depend on this, not on WebSocket classes. */
public interface LiveFeedPublisher {

    /** Sends {"type": type, "data": data} as JSON to all connected dashboards. */
    void broadcast(String type, Object data);
}
