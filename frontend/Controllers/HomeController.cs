using System.Diagnostics;
using Microsoft.AspNetCore.Mvc;
using Shuttle_Tracker.Models;
using Shuttle_Tracker.Services;

namespace Shuttle_Tracker.Controllers;

/// <summary>Live map: initial state from the backend REST API, then live updates over its WebSocket.</summary>
public class HomeController : Controller
{
    private readonly BackendApiClient _backend;
    private readonly IConfiguration _config;

    public HomeController(BackendApiClient backend, IConfiguration config)
    {
        _backend = backend;
        _config = config;
    }

    public async Task<IActionResult> Index(CancellationToken ct)
    {
        var liveFeedUrl = _config["BackendApi:LiveFeedUrl"] ?? "";
        try
        {
            return View(new DashboardViewModel
            {
                Routes = await _backend.GetRoutesAsync(ct),
                Vehicles = await _backend.GetVehiclesAsync(ct),
                Positions = await _backend.GetLivePositionsAsync(ct),
                LiveFeedUrl = liveFeedUrl,
            });
        }
        catch (HttpRequestException e)
        {
            return View(new DashboardViewModel { LiveFeedUrl = liveFeedUrl, BackendError = e.Message });
        }
    }

    [ResponseCache(Duration = 0, Location = ResponseCacheLocation.None, NoStore = true)]
    public IActionResult Error()
    {
        return View(new ErrorViewModel { RequestId = Activity.Current?.Id ?? HttpContext.TraceIdentifier });
    }
}
