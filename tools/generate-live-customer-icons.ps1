$ErrorActionPreference = 'Stop'

Add-Type -AssemblyName System.Drawing

$root = (Resolve-Path (Join-Path $PSScriptRoot '..\chrome-extensions\douyin-live-customer-v2\icons')).Path
New-Item -ItemType Directory -Force -Path $root | Out-Null

function New-RoundedPath([float]$x, [float]$y, [float]$width, [float]$height, [float]$radius) {
  $path = New-Object System.Drawing.Drawing2D.GraphicsPath
  $diameter = $radius * 2
  $path.AddArc($x, $y, $diameter, $diameter, 180, 90)
  $path.AddArc($x + $width - $diameter, $y, $diameter, $diameter, 270, 90)
  $path.AddArc($x + $width - $diameter, $y + $height - $diameter, $diameter, $diameter, 0, 90)
  $path.AddArc($x, $y + $height - $diameter, $diameter, $diameter, 90, 90)
  $path.CloseFigure()
  return $path
}

$bitmap = New-Object System.Drawing.Bitmap(512, 512, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$graphics.Clear([System.Drawing.Color]::FromArgb(242, 107, 33))

$bubblePath = New-RoundedPath 78 92 356 250 42
$whiteBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
$graphics.FillPath($whiteBrush, $bubblePath)
$tail = New-Object System.Drawing.Drawing2D.GraphicsPath
$tail.AddPolygon([System.Drawing.PointF[]]@(
  (New-Object System.Drawing.PointF(132, 328)),
  (New-Object System.Drawing.PointF(112, 405)),
  (New-Object System.Drawing.PointF(214, 335))
))
$graphics.FillPath($whiteBrush, $tail)

$cardPath = New-RoundedPath 138 150 210 126 22
$navyBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(25, 43, 77))
$graphics.FillPath($navyBrush, $cardPath)
$linePen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(95, 215, 194), 16)
$linePen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
$linePen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
$graphics.DrawLines($linePen, [System.Drawing.PointF[]]@(
  (New-Object System.Drawing.PointF(177, 216)),
  (New-Object System.Drawing.PointF(211, 249)),
  (New-Object System.Drawing.PointF(290, 181))
))

$graphics.Dispose(); $bubblePath.Dispose(); $tail.Dispose(); $cardPath.Dispose()
$whiteBrush.Dispose(); $navyBrush.Dispose(); $linePen.Dispose()
foreach ($size in @(16, 32, 48, 128)) {
  $target = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $g = [System.Drawing.Graphics]::FromImage($target)
  $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
  $g.DrawImage($bitmap, 0, 0, $size, $size)
  $target.Save((Join-Path $root ("icon{0}.png" -f $size)), [System.Drawing.Imaging.ImageFormat]::Png)
  $g.Dispose(); $target.Dispose()
}
$bitmap.Dispose()
Write-Host "Generated icons in $root"
