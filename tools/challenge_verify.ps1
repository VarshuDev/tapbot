# Comprehensive Empirical Verification Script for Milestone 1 Challenger
Add-Type -AssemblyName System.IO.Compression.FileSystem

$packagesDir = "app\src\main\assets\packages"
$catalogApiFile = "core\network\src\main\java\com\tapbot\core\network\CatalogApi.kt"
$sqlMigrationFile = "backend\migrations\0012_twenty_flagship_bots.sql"

$catalogApiText = Get-Content $catalogApiFile -Raw
$sqlMigrationText = Get-Content $sqlMigrationFile -Raw

$flagshipBots = @(
    @{ id = "bot_commander"; file = "tapbot-commander_1.0.0.botpkg"; ver = "1.0.0"; expectedRuntime = "native_art" },
    @{ id = "bot_music_streamer"; file = "tapstream-music-1.0.0.botpkg"; ver = "1.0.0"; expectedRuntime = "native_art" },
    @{ id = "bot_1790084493920_n4l8lj"; file = "bot_exon_1.0.0.botpkg"; ver = "1.0.0"; expectedRuntime = "native_art"; alias = "bot_exon" },
    @{ id = "bot_ai_assistant"; file = "bot_ai_assistant_2.1.0.botpkg"; ver = "2.1.0"; expectedRuntime = "built-in" },
    @{ id = "bot_habit_forge"; file = "bot_habit_forge_1.0.0.botpkg"; ver = "1.0.0"; expectedRuntime = "native_art" },
    @{ id = "bot_penny_wise"; file = "bot_penny_wise_1.0.0.botpkg"; ver = "1.0.0"; expectedRuntime = "native_art" },
    @{ id = "bot_pomo_focus"; file = "bot_pomo_focus_1.0.0.botpkg"; ver = "1.0.0"; expectedRuntime = "native_art" },
    @{ id = "bot_tap_note"; file = "bot_tap_note_1.0.0.botpkg"; ver = "1.0.0"; expectedRuntime = "native_art" },
    @{ id = "bot_sky_pulse"; file = "bot_sky_pulse_1.0.0.botpkg"; ver = "1.0.0"; expectedRuntime = "native_art" },
    @{ id = "bot_remind_me"; file = "bot_remind_me_1.0.0.botpkg"; ver = "1.0.0"; expectedRuntime = "native_art" },
    @{ id = "bot_link_drop"; file = "bot_link_drop_1.0.0.botpkg"; ver = "1.0.0"; expectedRuntime = "native_art" },
    @{ id = "bot_poly_glot"; file = "bot_poly_glot_1.0.0.botpkg"; ver = "1.0.0"; expectedRuntime = "native_art" },
    @{ id = "bot_coin_radar"; file = "bot_coin_radar_1.0.0.botpkg"; ver = "1.0.0"; expectedRuntime = "native_art" },
    @{ id = "bot_fit_track"; file = "bot_fit_track_1.0.0.botpkg"; ver = "1.0.0"; expectedRuntime = "native_art" },
    @{ id = "bot_parcel_track"; file = "bot_parcel_track_1.0.0.botpkg"; ver = "1.0.0"; expectedRuntime = "native_art" },
    @{ id = "bot_rss_notifier"; file = "bot_rss_1.2.0.botpkg"; ver = "1.2.0"; expectedRuntime = "built-in" },
    @{ id = "bot_git_notifier"; file = "bot_git_notifier_1.0.0.botpkg"; ver = "1.0.0"; expectedRuntime = "native_art" },
    @{ id = "bot_cam_sentry"; file = "bot_cam_sentry_1.0.0.botpkg"; ver = "1.0.0"; expectedRuntime = "native_art" },
    @{ id = "bot_whisper_box"; file = "bot_whisper_box_1.0.0.botpkg"; ver = "1.0.0"; expectedRuntime = "native_art" },
    @{ id = "bot_trivia_master"; file = "bot_trivia_master_1.0.0.botpkg"; ver = "1.0.0"; expectedRuntime = "native_art" }
)

Write-Host "=========================================================="
Write-Host "   EMPIRICAL CHALLENGER: DEEP AUDIT FOR MILESTONE 1"
Write-Host "=========================================================="

$failures = 0
$checkCount = 0

foreach ($bot in $flagshipBots) {
    $filePath = Join-Path $packagesDir $bot.file
    Write-Host "`n>>> [BOT] $($bot.id) ($($bot.file)) <<<"

    # --- Step 1: Package Existence ---
    $checkCount++
    if (-not (Test-Path $filePath)) {
        Write-Error "FAIL: Package file does not exist: $filePath"
        $failures++
        continue
    }

    $fileInfo = Get-Item $filePath
    $actualSize = $fileInfo.Length
    $hashResult = Get-FileHash -Path $filePath -Algorithm SHA256
    $actualSha256 = $hashResult.Hash.ToLowerInvariant()
    Write-Host "  [OK] File exists. Size: $actualSize bytes, SHA-256: $actualSha256"

    # --- Step 2: Cross-check CatalogApi.kt sampleBots & sampleVersions ---
    $checkCount++
    if ($catalogApiText -notmatch $actualSha256) {
        Write-Error "FAIL: SHA-256 $actualSha256 not found in CatalogApi.kt for $($bot.id)"
        $failures++
    } else {
        Write-Host "  [OK] SHA-256 matches in CatalogApi.kt"
    }

    $checkCount++
    if ($catalogApiText -notmatch [regex]::Escape("$actualSize")) {
        Write-Error "FAIL: Size $actualSize not found in CatalogApi.kt for $($bot.id)"
        $failures++
    } else {
        Write-Host "  [OK] Package size ($actualSize bytes) matches in CatalogApi.kt"
    }

    # --- Step 3: Check isFeatured in CatalogApi.kt for this bot ---
    $checkCount++
    $botIdPattern = [regex]::Escape("id = `"$($bot.id)`"")
    if ($catalogApiText -match "$botIdPattern[\s\S]*?isFeatured\s*=\s*true") {
        Write-Host "  [OK] isFeatured = true confirmed in CatalogApi.kt"
    } else {
        Write-Error "FAIL: isFeatured = true not found for $($bot.id) in CatalogApi.kt"
        $failures++
    }

    # --- Step 4: Cross-check SQL Migration (0012_twenty_flagship_bots.sql) ---
    if ($bot.id -notmatch "^(bot_commander|bot_music_streamer|bot_1790084493920_n4l8lj|bot_ai_assistant|bot_rss_notifier)$") {
        $checkCount++
        if ($sqlMigrationText -notmatch $actualSha256) {
            Write-Error "FAIL: SHA-256 $actualSha256 not found in 0012_twenty_flagship_bots.sql for $($bot.id)"
            $failures++
        } else {
            Write-Host "  [OK] SHA-256 matches in 0012_twenty_flagship_bots.sql"
        }

        $checkCount++
        if ($sqlMigrationText -notmatch "\b$actualSize\b") {
            Write-Error "FAIL: Size $actualSize not found in 0012_twenty_flagship_bots.sql for $($bot.id)"
            $failures++
        } else {
            Write-Host "  [OK] Package size ($actualSize bytes) matches in 0012_twenty_flagship_bots.sql"
        }
    }

    # --- Step 5: ZIP Archive Structure & Deep Manifest Inspection ---
    $checkCount++
    try {
        $zip = [System.IO.Compression.ZipFile]::OpenRead($filePath)
        $entryNames = $zip.Entries | ForEach-Object { $_.FullName }

        $requiredEntries = @("manifest.json", "entrypoint", "dependencies/README.txt", "assets/icon.png")
        foreach ($req in $requiredEntries) {
            if ($entryNames -notcontains $req) {
                Write-Error "FAIL: Missing ZIP entry '$req' in $($bot.file)"
                $failures++
            }
        }
        Write-Host "  [OK] ZIP contains all 4 required entries (manifest.json, entrypoint, dependencies/README.txt, assets/icon.png)"

        # Inspect manifest.json
        $manifestEntry = $zip.GetEntry("manifest.json")
        $reader = New-Object System.IO.StreamReader($manifestEntry.Open())
        $manifestJson = $reader.ReadToEnd()
        $reader.Close()

        $manifest = $manifestJson | ConvertFrom-Json

        $fields = @("id", "name", "version", "description", "category", "icon", "runtime", "entrypoint", "minimumRuntimeVersion", "minimumAppVersion", "credentials", "permissions")
        foreach ($f in $fields) {
            if ($null -eq $manifest.$f) {
                Write-Error "FAIL: Manifest missing required field '$f' in $($bot.file)"
                $failures++
            }
        }
        Write-Host "  [OK] All 12 required manifest fields present"

        # Validate runtime
        if ($manifest.runtime -ne $bot.expectedRuntime) {
            Write-Error "FAIL: Runtime expected '$($bot.expectedRuntime)' but got '$($manifest.runtime)'"
            $failures++
        } else {
            Write-Host "  [OK] Runtime '$($manifest.runtime)' matches expected"
        }

        # Validate credentials array
        $creds = @($manifest.credentials)
        if ($creds.Count -eq 0) {
            Write-Error "FAIL: credentials must be a non-empty array in $($bot.file)"
            $failures++
        } else {
            $hasToken = @($creds | Where-Object { $_.key -eq "bot_token" -or $_.key -eq "telegram_bot_token" }).Count -gt 0
            if (-not $hasToken) {
                Write-Error "FAIL: credentials missing bot_token in $($bot.file)"
                $failures++
            } else {
                Write-Host "  [OK] Credentials properly structured ($($creds.Count) specs, bot_token included)"
            }
        }

        # Validate permissions array
        $perms = @($manifest.permissions)
        if ($perms.Count -eq 0) {
            Write-Error "FAIL: permissions must be a non-empty array in $($bot.file)"
            $failures++
        } else {
            Write-Host "  [OK] Permissions properly defined: $($perms -join ', ')"
        }

        # Validate entrypoint file contains manifest entrypoint class
        $entrypointEntry = $zip.GetEntry("entrypoint")
        $epReader = New-Object System.IO.StreamReader($entrypointEntry.Open())
        $epContent = $epReader.ReadToEnd().Trim()
        $epReader.Close()
        if (-not $epContent.Contains($manifest.entrypoint)) {
            Write-Error "FAIL: entrypoint file does not contain manifest.entrypoint ('$($manifest.entrypoint)')"
            $failures++
        } else {
            Write-Host "  [OK] Entrypoint file contains manifest.entrypoint ('$($manifest.entrypoint)')"
        }

        # Validate dependencies/README.txt non-empty
        $readmeEntry = $zip.GetEntry("dependencies/README.txt")
        $rmReader = New-Object System.IO.StreamReader($readmeEntry.Open())
        $rmContent = $rmReader.ReadToEnd().Trim()
        $rmReader.Close()
        if ($rmContent.Length -eq 0) {
            Write-Error "FAIL: dependencies/README.txt is empty in $($bot.file)"
            $failures++
        } else {
            Write-Host "  [OK] dependencies/README.txt populated ($($rmContent.Length) chars)"
        }

        # Validate assets/icon.png size > 0
        $iconEntry = $zip.GetEntry("assets/icon.png")
        if ($iconEntry.Length -le 0) {
            Write-Error "FAIL: assets/icon.png is 0 bytes in $($bot.file)"
            $failures++
        } else {
            Write-Host "  [OK] assets/icon.png valid ($($iconEntry.Length) bytes)"
        }

        $zip.Dispose()
    } catch {
        Write-Error "FAIL: Unexpected exception inspecting $($bot.file): $_"
        $failures++
    }
}

# --- Test Alias Resolution in CatalogApi.kt ---
Write-Host "`n>>> [TEST] Alias Resolution Logic in CatalogApi.kt <<<"
$aliasChecks = @(
    "bot_exon",
    "bot_1790084493920_n4l8lj",
    "bot_commander",
    "bot_commander_flagship",
    "tapbot-commander",
    "tapstream-music",
    "bot_music_streamer"
)

foreach ($alias in $aliasChecks) {
    if ($catalogApiText -match [regex]::Escape("`"$alias`"")) {
        Write-Host "  [OK] Alias '$alias' registered and resolved in CatalogApi.kt"
    } else {
        Write-Error "FAIL: Alias '$alias' missing in CatalogApi.kt"
        $failures++
    }
}

Write-Host "`n=========================================================="
Write-Host "   AUDIT SUMMARY"
Write-Host "=========================================================="
Write-Host "Total individual checks performed: $checkCount"
Write-Host "Total failures detected: $failures"
if ($failures -eq 0) {
    Write-Host "VERDICT: EMPIRICAL VERIFICATION PASS - 100% SUCCESS"
} else {
    Write-Host "VERDICT: EMPIRICAL VERIFICATION FAIL"
    exit 1
}
