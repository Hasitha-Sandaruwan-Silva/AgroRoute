Add-Type -AssemblyName System.Drawing
function Read-VectorPath([string]$data) {
    $path = [Drawing.Drawing2D.GraphicsPath]::new()
    $tokens = [regex]::Matches($data, '[MLCZ]|-?\d+(?:\.\d+)?') | ForEach-Object Value
    $i = 0
    $current = [Drawing.PointF]::new(0,0)
    while ($i -lt $tokens.Count) {
        $cmd = $tokens[$i++]
        switch ($cmd) {
            'M' { $path.StartFigure(); $current = [Drawing.PointF]::new([float]$tokens[$i], [float]$tokens[$i+1]); $i += 2 }
            'L' { $next = [Drawing.PointF]::new([float]$tokens[$i], [float]$tokens[$i+1]); $path.AddLine($current,$next); $current=$next; $i+=2 }
            'C' {
                $a = [Drawing.PointF]::new([float]$tokens[$i], [float]$tokens[$i+1])
                $b = [Drawing.PointF]::new([float]$tokens[$i+2], [float]$tokens[$i+3])
                $next = [Drawing.PointF]::new([float]$tokens[$i+4], [float]$tokens[$i+5])
                $path.AddBezier($current,$a,$b,$next); $current=$next; $i+=6
            }
            'Z' { $path.CloseFigure() }
        }
    }
    return ,$path
}
function Paint-Vector($graphics,$file,$x,$y,$size,$override) {
    [xml]$xml = Get-Content $file
    $saved = $graphics.Save()
    $graphics.TranslateTransform($x,$y)
    $graphics.ScaleTransform($size/108,$size/108)
    foreach ($node in $xml.vector.path) {
        $path = Read-VectorPath $node.GetAttribute('android:pathData')
        $color = if ($override) { $override } else { $node.GetAttribute('android:fillColor') }
        $brush = [Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml($color))
        $graphics.FillPath($brush,$path)
        $brush.Dispose(); $path.Dispose()
    }
    $graphics.Restore($saved)
}
$bitmap = [Drawing.Bitmap]::new(1000,640)
$g = [Drawing.Graphics]::FromImage($bitmap)
$g.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.Clear([Drawing.ColorTranslator]::FromHtml('#E7E9E1'))
$font = [Drawing.Font]::new('Segoe UI',18,[Drawing.FontStyle]::Bold)
$small = [Drawing.Font]::new('Segoe UI',12)
$g.DrawString('AgroRoute / startup identity',$font,[Drawing.Brushes]::DarkSlateGray,30,18)
$foreground = 'app/src/main/res/drawable/ic_launcher_foreground.xml'
for ($j=0; $j -lt 3; $j++) {
    $x=30+$j*170
    $mask=[Drawing.Drawing2D.GraphicsPath]::new()
    if ($j -eq 0) { $mask.AddEllipse($x,80,140,140) }
    elseif ($j -eq 1) {
        $mask.AddArc($x,80,60,60,180,90); $mask.AddArc($x+80,80,60,60,270,90)
        $mask.AddArc($x+80,160,60,60,0,90); $mask.AddArc($x,160,60,60,90,90); $mask.CloseFigure()
    }
    else {
        $points = [Collections.Generic.List[Drawing.PointF]]::new()
        for ($step=0; $step -le 160; $step++) {
            $angle = 2 * [Math]::PI * $step / 160
            $c=[Math]::Cos($angle); $s=[Math]::Sin($angle)
            $points.Add([Drawing.PointF]::new($x+70+70*[Math]::Sign($c)*[Math]::Sqrt([Math]::Abs($c)),150+70*[Math]::Sign($s)*[Math]::Sqrt([Math]::Abs($s))))
        }
        $mask.AddPolygon($points.ToArray())
    }
    $saved=$g.Save(); $g.SetClip($mask)
    $brush=[Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml('#123D32'))
    $g.FillRectangle($brush,$x,80,140,140)
    # Launchers crop the adaptive 108 dp canvas to their central 72 dp mask.
    Paint-Vector $g $foreground ($x-35) 45 210 $null
    $g.Restore($saved); $mask.Dispose(); $brush.Dispose()
}
$g.DrawString('Circle / rounded square / squircle',$small,[Drawing.Brushes]::DarkSlateGray,30,240)
foreach ($size in @(48,32,24)) {
    $x=30+($size-24)*6
    $brush=[Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml('#123D32'))
    $g.FillEllipse($brush,$x,300,$size,$size)
    $saved=$g.Save(); $g.SetClip([Drawing.Rectangle]::new($x,300,$size,$size))
    Paint-Vector $g $foreground ($x-$size/4) (300-$size/4) ($size*1.5) $null
    $g.Restore($saved); $brush.Dispose()
}
$g.DrawString('Small launcher sizes',$small,[Drawing.Brushes]::DarkSlateGray,30,370)
for ($j=0; $j -lt 2; $j++) {
    $x=550+$j*225
    $bg=if ($j -eq 0) {'#F5F1E9'} else {'#0E211B'}
    $ink=if ($j -eq 0) {'#123D32'} else {'#B8DE85'}
    $brush=[Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml($bg))
    $g.FillRectangle($brush,$x,80,200,480)
    Paint-Vector $g 'app/src/main/res/drawable/splash_icon.xml' ($x+10) 200 180 $ink
    $brush.Dispose()
    $brush=[Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml($ink))
    $g.DrawString('AgroRoute',$font,$brush,($x+36),375)
    $g.DrawString('From Harvest to Destination',[Drawing.Font]::new('Segoe UI',9),$brush,($x+6),415)
    $brush.Dispose()
}
$g.DrawString('Light / dark intro concept',$small,[Drawing.Brushes]::DarkSlateGray,550,580)
$bitmap.Save((Join-Path (Get-Location) 'startup-branding-preview.png'),[Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $bitmap.Dispose(); $font.Dispose(); $small.Dispose()
