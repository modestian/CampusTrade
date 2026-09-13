# ============================================================
# CampusTrade API Smoke Test
#    Part A: Feed flow  (publish -> admin audit -> fans feed)
#    Part B: Front page availability (student / admin / static)
#    Part C: Core API regression
# Usage: powershell -ExecutionPolicy Bypass -File scripts\api-test.ps1
# ============================================================
$ErrorActionPreference = 'Stop'
$BASE = 'http://localhost:8082'
$script:passCount = 0
$script:failCount = 0

function Check($name, $cond, $detail) {
    if ($cond) {
        $script:passCount++
        Write-Host "[PASS] $name" -ForegroundColor Green
    } else {
        $script:failCount++
        Write-Host "[FAIL] $name | $detail" -ForegroundColor Red
    }
}

function PostJson($path, $obj, $headers) {
    $p = @{
        Uri         = $BASE + $path
        Method      = 'Post'
        Body        = ($obj | ConvertTo-Json -Depth 6)
        ContentType = 'application/json; charset=utf-8'
    }
    if ($headers) { $p.Headers = $headers }
    return Invoke-RestMethod @p
}

function PutJson($path, $obj, $headers) {
    $p = @{
        Uri         = $BASE + $path
        Method      = 'Put'
        Body        = ($obj | ConvertTo-Json -Depth 6)
        ContentType = 'application/json; charset=utf-8'
    }
    if ($headers) { $p.Headers = $headers }
    return Invoke-RestMethod @p
}

function GetApi($path, $headers) {
    $p = @{ Uri = $BASE + $path; Method = 'Get' }
    if ($headers) { $p.Headers = $headers }
    return Invoke-RestMethod @p
}

Write-Host '===== CampusTrade Smoke Test =====' -ForegroundColor Cyan

# ---------- Part A: Feed flow ----------
Write-Host '--- Part A: Feed Flow (publish -> audit -> feed) ---' -ForegroundColor Yellow

# A1. seller 20230003 login
$r = PostJson '/student/login' @{ username = '20230003'; password = '123456' }
Check 'A1 seller login (20230003)' ($r.code -eq 1) "code=$($r.code) msg=$($r.msg)"
$t3 = $r.data.token

# A2. seller publishes a new product (status 0 = pending audit)
$title = 'SmokeFeed-' + (Get-Date -Format 'HHmmss')
$pub = PostJson '/student/products' @{
    title = $title; description = 'smoke test item'; categoryId = 1
    coverColor = '#6C5CE7'; price = 9.9; originalPrice = 99
    conditionLevel = 1; deliveryType = 1; campus = 'Main Campus'
} @{ token = $t3 }
Check 'A2 seller publish product' ($pub.code -eq 1) "code=$($pub.code) msg=$($pub.msg)"

# A3. locate the new product id via seller's pending list
$mine = GetApi '/student/my/products?status=0&page=1&pageSize=10' @{ token = $t3 }
$newItem = $mine.data.records | Where-Object { $_.title -eq $title } | Select-Object -First 1
Check 'A3 find new product in pending list' ($null -ne $newItem) 'new product not found'
$newProductId = $newItem.id

# A4. admin login
$ra = PostJson '/admin/login' @{ username = 'admin'; password = '123456' }
Check 'A4 admin login' ($ra.code -eq 1) "code=$($ra.code) msg=$($ra.msg)"
$at = $ra.data.token

# A5. admin approves -> triggers pushToFans
$au = PutJson "/admin/products/$newProductId/audit" @{ pass = $true; remark = 'ok' } @{ 'admin-token' = $at }
Check 'A5 admin approve product' ($au.code -eq 1) "code=$($au.code) msg=$($au.msg)"

# A6. fan 20230001 login + read feed
$rb = PostJson '/student/login' @{ username = '20230001'; password = '123456' }
Check 'A6 buyer/fan login (20230001)' ($rb.code -eq 1) "code=$($rb.code) msg=$($rb.msg)"
$t1 = $rb.data.token

$feed = GetApi '/student/feed?lastTime=0&offset=0' @{ token = $t1 }
$hit = $feed.data.products | Where-Object { $_.id -eq $newProductId }
Check 'A7 feed contains new product' ($null -ne $hit) "productId=$newProductId feedCount=$($feed.data.products.Count)"

# A8. feed also pages (minTime/offset returned)
Check 'A8 feed paging fields' ($feed.data.minTime -gt 0 -and $feed.data.offset -ge 1) "minTime=$($feed.data.minTime) offset=$($feed.data.offset)"

# ---------- Part B: front pages ----------
Write-Host '--- Part B: Front Pages ---' -ForegroundColor Yellow
$pages = @(
    '/student/index.html', '/student/login.html', '/student/feed.html',
    '/admin-ui/index.html', '/admin-ui/login.html',
    '/campus-static/js/app.js', '/campus-static/css/style.css'
)
foreach ($p in $pages) {
    try {
        $resp = Invoke-WebRequest -Uri ($BASE + $p) -UseBasicParsing -TimeoutSec 10
        Check "B $p" ($resp.StatusCode -eq 200) "status=$($resp.StatusCode)"
    } catch {
        Check "B $p" $false $_.Exception.Message
    }
}

# ---------- Part C: regression ----------
Write-Host '--- Part C: Core API Regression ---' -ForegroundColor Yellow
$pl = GetApi '/student/products?page=1&pageSize=10'
Check 'C1 product list' ($pl.code -eq 1 -and $pl.data.total -gt 0) "code=$($pl.code) total=$($pl.data.total)"

$hot = GetApi '/student/products/hot'
Check 'C2 hot ranking' ($hot.code -eq 1) "code=$($hot.code)"

$ws = GetApi '/admin/workspace' @{ 'admin-token' = $at }
Check 'C3 admin workspace' ($ws.code -eq 1) "code=$($ws.code)"

$unauth = $null
try { $unauth = GetApi '/student/feed' } catch { $unauth = $null }
Check 'C4 feed rejects anonymous' ($null -eq $unauth -or $unauth.code -ne 1) 'anonymous access should be blocked'

Write-Host ''
Write-Host "===== Result: PASS=$($script:passCount) FAIL=$($script:failCount) =====" -ForegroundColor Cyan
if ($script:failCount -gt 0) { exit 1 } else { exit 0 }
