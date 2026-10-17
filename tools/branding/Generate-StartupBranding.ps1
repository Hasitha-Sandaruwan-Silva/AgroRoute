Add-Type -AssemblyName System.Drawing
$res = Join-Path (Split-Path (Split-Path $PSScriptRoot -Parent) -Parent) 'app/src/main/res'
$leaf = 'M27,55 C27,39 40,29 57,29 L75,30 L77,48 C77,66 63,79 47,79 C35,79 27,69 27,55 Z M40,69 C40,56 60,57 60,46 L60,41 C60,36 67,36 67,41 L67,46 C67,62 47,62 47,69 C47,74 40,74 40,69 Z'
function Write-Asset($relative, $content) {
    $target = Join-Path $res $relative
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($target)) | Out-Null
    [IO.File]::WriteAllText($target, $content, [Text.UTF8Encoding]::new($false))
}
$vector = '<?xml version="1.0" encoding="utf-8"?><vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="{0}dp" android:height="{0}dp" android:viewportWidth="108" android:viewportHeight="108">{1}</vector>'
$path = '<path android:fillColor="{0}" android:fillType="evenOdd" android:pathData="' + $leaf + '" />'
Write-Asset 'drawable/ic_launcher_foreground.xml' ($vector -f '108', ($path -f '#B8DE85'))
Write-Asset 'drawable/ic_launcher_monochrome.xml' ($vector -f '108', ($path -f '#FFFFFFFF'))
Write-Asset 'drawable/splash_icon.xml' ($vector -f '288', ($path -f '@color/startup_mark'))

