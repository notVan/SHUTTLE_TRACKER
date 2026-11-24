using System.ComponentModel.DataAnnotations;
using Shuttle_Tracker.Services;

namespace Shuttle_Tracker.Models;

public class VehicleFormModel
{
    [Required, StringLength(50)]
    public string Name { get; set; } = "";

    [StringLength(20)]
    public string? Plate { get; set; }

    [Required, RegularExpression(@"^\d{15}$", ErrorMessage = "IMEI must be 15 digits (printed on the ZX303).")]
    public string Imei { get; set; } = "";

    public int? RouteId { get; set; }
}

public class VehiclesPageModel
{
    public List<VehicleDto> Vehicles { get; init; } = [];
    public List<RouteDto> Routes { get; init; } = [];
    public VehicleFormModel Form { get; init; } = new();
    public string? Error { get; init; }
}
