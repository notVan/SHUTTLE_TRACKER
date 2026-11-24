var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllersWithViews();

// Backend API layer (Spring Boot). Base URL comes from appsettings.json -> BackendApi:BaseUrl.
builder.Services.AddHttpClient<Shuttle_Tracker.Services.BackendApiClient>(client =>
{
    var baseUrl = builder.Configuration["BackendApi:BaseUrl"]
        ?? throw new InvalidOperationException("BackendApi:BaseUrl is not configured");
    client.BaseAddress = new Uri(baseUrl);
    client.Timeout = TimeSpan.FromSeconds(10);
});

var app = builder.Build();

if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler("/Home/Error");
    app.UseHsts();
}

app.UseHttpsRedirection();
app.UseRouting();

app.UseAuthorization();

app.MapStaticAssets();

app.MapControllerRoute(
    name: "default",
    pattern: "{controller=Home}/{action=Index}/{id?}")
    .WithStaticAssets();

app.Run();
