using Shuttle_Tracker.Services;

namespace Shuttle_Tracker.Models;

/// <summary>Everything the live map needs on first render; updates then arrive over the WebSocket.</summary>
public class DashboardViewModel
{
    public List<RouteDto> Routes { get; init; } = [];
    public List<VehicleDto> Vehicles { get; init; } = [];
    public List<LivePositionDto> Positions { get; init; } = [];
    public string LiveFeedUrl { get; init; } = "";
    public string? BackendError { get; init; }
}
