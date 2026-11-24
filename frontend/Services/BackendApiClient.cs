using System.Net;
using System.Net.Http.Json;

namespace Shuttle_Tracker.Services;

/// <summary>
/// The only place the MVC frontend talks to the Spring Boot backend.
/// Controllers call this; they never build HTTP requests themselves.
/// </summary>
public class BackendApiClient
{
    private readonly HttpClient _http;

    public BackendApiClient(HttpClient http)
    {
        _http = http;
    }

    public async Task<List<RouteDto>> GetRoutesAsync(CancellationToken ct = default) =>
        await _http.GetFromJsonAsync<List<RouteDto>>("api/routes", ct) ?? [];

    public async Task<List<VehicleDto>> GetVehiclesAsync(CancellationToken ct = default) =>
        await _http.GetFromJsonAsync<List<VehicleDto>>("api/vehicles", ct) ?? [];

    public async Task<List<LivePositionDto>> GetLivePositionsAsync(CancellationToken ct = default) =>
        await _http.GetFromJsonAsync<List<LivePositionDto>>("api/fleet/live", ct) ?? [];

    /// <summary>Registers a shuttle; the backend also creates the tracker in Traccar.</summary>
    public async Task<CreateResult> CreateVehicleAsync(VehicleRequest request, CancellationToken ct = default)
    {
        var response = await _http.PostAsJsonAsync("api/vehicles", request, ct);
        if (response.IsSuccessStatusCode)
        {
            return new CreateResult(true, null);
        }
        var problem = response.StatusCode is HttpStatusCode.BadRequest or HttpStatusCode.Conflict or HttpStatusCode.NotFound
            ? await response.Content.ReadFromJsonAsync<ProblemDetailsDto>(ct)
            : null;
        return new CreateResult(false, problem?.Detail ?? $"Backend returned {(int)response.StatusCode}");
    }
}

// Mirrors of backend/src/main/java/com/shuttletracker/dto/Dtos.java
public record StopDto(int Id, string Name, int Sequence, double Latitude, double Longitude);
public record RouteDto(int Id, string Name, string Color, List<StopDto> Stops);
public record VehicleDto(int Id, string Name, string? Plate, string Imei, int? RouteId, string? RouteName, bool LinkedToTraccar);
public record VehicleRequest(string Name, string? Plate, string Imei, int? RouteId);
public record LivePositionDto(int VehicleId, string VehicleName, int? RouteId, double Latitude, double Longitude,
                              double SpeedKmh, double Course, int? BatteryLevel, DateTimeOffset FixTime);
public record ProblemDetailsDto(string? Title, string? Detail);
public record CreateResult(bool Success, string? Error);
