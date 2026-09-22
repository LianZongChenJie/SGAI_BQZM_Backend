param(
    [string]$RedisHost = "10.168.56.101",
    [int]$Port = 6379,
    [string]$Password = "",
    [string]$Commands = "PING",
    [int]$WaitMs = 900
)

# Send raw Redis commands over a .NET socket and print the raw RESP reply.
# Read-only commands only (PING / INFO / DBSIZE / SCAN / CLIENT LIST ...).
[Console]::OutputEncoding = [Text.Encoding]::UTF8

$client = New-Object System.Net.Sockets.TcpClient
$task = $client.ConnectAsync($RedisHost, $Port)
if (-not $task.Wait(8000) -or -not $client.Connected) {
    Write-Host "[error] connect failed or timeout"
    exit 1
}
Write-Host ("[connect] {0}:{1}" -f $RedisHost, $Port)

$ns = $client.GetStream()
$ns.ReadTimeout = 300
$ns.WriteTimeout = 5000
$enc = [Text.Encoding]::UTF8

function Send-Redis([string]$cmd) {
    $bytes = $enc.GetBytes($cmd + "`r`n")
    $ns.Write($bytes, 0, $bytes.Length)
    $ns.Flush()
}

# Password sentinels: "-" / "none" / "null" / empty all mean "no AUTH"
if ($Password -ne "" -and $Password -ne "-" -and $Password -ne "none" -and $Password -ne "null") {
    Send-Redis ("AUTH " + $Password)
    Start-Sleep -Milliseconds 200
    $ns.ReadTimeout = 300
    try { $null = $ns.Read((New-Object byte[] 256), 0, 256) } catch { }
}

$buf = New-Object byte[] 65536

foreach ($cmd in ($Commands -split ";")) {
    $cmd = $cmd.Trim()
    if ($cmd.Length -eq 0) { continue }
    Write-Host ""
    Write-Host (">>> " + $cmd)
    Send-Redis $cmd
    $deadline = (Get-Date).AddMilliseconds($WaitMs)
    while ((Get-Date) -lt $deadline) {
        try {
            $n = $ns.Read($buf, 0, $buf.Length)
            if ($n -le 0) { Write-Host "[closed]"; break }
            $text = $enc.GetString($buf, 0, $n)
            foreach ($line in ($text -split "`r`n")) {
                if ($line.Length -gt 0) { Write-Host $line }
            }
        } catch [System.IO.IOException] {
        } catch {
            break
        }
    }
}

$client.Close()
