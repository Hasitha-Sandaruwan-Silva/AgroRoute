# AgroRoute startup branding

The adaptive launcher retains the existing `ic_launcher` and `ic_launcher_round`
references. A new full geometric leaf silhouette contains one broad, continuous
route cutout, on a solid forest background. A monochrome silhouette supports themed launchers.
The artwork fits the central 66 dp safe circle on the 108 dp adaptive canvas.

The existing AndroidX native splash uses the same transparent 288 dp vector and
day/night colors. Its exit callback hands off directly to the Compose overlay,
which starts with the native mark and background. If the app's saved theme is
different from the system theme, the palette blends over the opening 400 ms.
The native wordmark slot is intentionally unused: the wordmark appears once,
as part of the animation. No splash activity or navigation destination is added.

The intro runs for 2400 ms with a 200 ms exit fade. Botanical forms emerge,
the mark settles upward, the route reveals, the wordmark and tagline appear,
and a destination dot travels the route. The real app remains composed behind
the intro and keeps ownership of session restoration and all routing.
The overlay observes the existing activity-scoped AuthViewModel's
`sessionResolved` flag; it never calls restore, login, or navigate. It waits two
render frames after readiness before fading away. A small linear loading
continuation appears only if the animation finishes before session resolution.
Completion is saved across activity recreation; ordinary warm resumes do not
replay the intro. Native Compose animation duration scaling is respected.

Authentication, role detection, Demo Mode, repositories, notification callbacks,
and data initialization are unchanged. The transient status-bar override takes
priority over screen headers underneath it, then releases that priority on exit.

Run these PowerShell scripts from the repository root to regenerate the vectors
and the visual contact sheet (Windows, System.Drawing and Segoe UI required):

```powershell
./tools/branding/Generate-StartupBranding.ps1
./tools/branding/Preview-StartupBranding.ps1
```

`startup-branding-preview.png` illustrates circle, rounded-square, squircle,
small-size, and light/dark treatments. It is a concept contact sheet, not a device
screenshot. `BrandedStartupPresentationTest` captures actual Compose frames and
tests the readiness gate, continued underlying composition, and no replay on theme change.

Native splash sizing: https://developer.android.com/develop/ui/views/launch/splash-screen

Verification: debug build and lint passed; 8 existing role navigation unit tests
and 12 device presentation tests passed on the connected Pixel 7. The device
checks include 5 intro tests, Farmer roots in both themes, and Supplier header,
tab, action, and theme checks with isolated demo data. Actual light/dark intro
frames were captured and visually inspected. A real cold launcher start reached
Welcome, and a subsequent launcher return reused the existing task (14 ms).
Live signed-in Farmer/Supplier session restoration and older physical Android
versions were not exercised; no account credentials or production records were changed.
