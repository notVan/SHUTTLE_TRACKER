using Microsoft.AspNetCore.Mvc;
using Shuttle_Tracker.Models;
using Shuttle_Tracker.Services;

namespace Shuttle_Tracker.Controllers;

/// <summary>Fleet register: list shuttles and bind a new tracker IMEI to a shuttle.</summary>
public class VehiclesController : Controller
{
    private readonly BackendApiClient _backend;

    public VehiclesController(BackendApiClient backend)
    {
        _backend = backend;
    }

    public async Task<IActionResult> Index(CancellationToken ct)
    {
        return await Page(new VehicleFormModel(), null, ct);
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Create([Bind(Prefix = "Form")] VehicleFormModel form, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            return await Page(form, null, ct);
        }
        try
        {
            var result = await _backend.CreateVehicleAsync(new VehicleRequest(form.Name, form.Plate, form.Imei, form.RouteId), ct);
            return result.Success ? RedirectToAction(nameof(Index)) : await Page(form, result.Error, ct);
        }
        catch (HttpRequestException e)
        {
            return await Page(form, $"Backend unreachable: {e.Message}", ct);
        }
    }

    private async Task<IActionResult> Page(VehicleFormModel form, string? error, CancellationToken ct)
    {
        try
        {
            return View("Index", new VehiclesPageModel
            {
                Vehicles = await _backend.GetVehiclesAsync(ct),
                Routes = await _backend.GetRoutesAsync(ct),
                Form = form,
                Error = error,
            });
        }
        catch (HttpRequestException e)
        {
            return View("Index", new VehiclesPageModel { Form = form, Error = $"Backend unreachable: {e.Message}" });
        }
    }
}
