# Drives a fake shuttle along the sample route by sending positions to Traccar's OsmAnd port (5055).
# Lets you test Traccar -> backend -> dashboard before any ZX303 hardware arrives.
# The IMEI must belong to a vehicle registered in the app (the sample data uses 868120301000011).
#
#   .\scripts\send-test-position.ps1
#   .\scripts\send-test-position.ps1 -Imei 868120301000011 -IntervalSeconds 3

param(
    [string]$Imei = "868120301000011",
    [string]$TraccarHost = "localhost",
    [int]$Port = 5055,
    [int]$IntervalSeconds = 2,
    [int]$Laps = 3
)

# Same placeholder stops as database/init/02_sample_data.sql
$stops = @(
    @{ lat = 5.6377; lon = -0.1863 },  # Main Gate
    @{ lat = 5.6441; lon = -0.1851 },  # Night Market
    @{ lat = 5.6507; lon = -0.1868 },  # Balme Library
    @{ lat = 5.6553; lon = -0.1903 }   # Commonwealth Hall
)
$stepsPerLeg = 10
$battery = 90

for ($lap = 1; $lap -le $Laps; $lap++) {
    for ($i = 0; $i -lt $stops.Count - 1; $i++) {
        $a = $stops[$i]; $b = $stops[$i + 1]
        for ($s = 0; $s -lt $stepsPerLeg; $s++) {
            $f = $s / $stepsPerLeg
            $lat = $a.lat + ($b.lat - $a.lat) * $f
            $lon = $a.lon + ($b.lon - $a.lon) * $f
            $speedKnots = [math]::Round((25 + (Get-Random -Maximum 10)) / 1.852, 1)
            $ts = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
            $url = "http://${TraccarHost}:${Port}/?id=$Imei&lat=$lat&lon=$lon&speed=$speedKnots&batt=$battery&timestamp=$ts"
            try {
                Invoke-WebRequest -Uri $url -Method Get -UseBasicParsing | Out-Null
                Write-Host ("lap {0} leg {1}  {2:N5},{3:N5}" -f $lap, ($i + 1), $lat, $lon)
            } catch {
                Write-Warning "Traccar not reachable at ${TraccarHost}:${Port} - $($_.Exception.Message)"
            }
            Start-Sleep -Seconds $IntervalSeconds
        }
    }
    $battery = [math]::Max(5, $battery - 5)
}
