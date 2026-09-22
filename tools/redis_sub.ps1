param(
    [string]$RedisHost = "10.168.56.101",
    [int]$Port = 6379,
    [string]$Password = "",
    [string]$Channel = "jeecg_redis_topic",
    [int]$Seconds = 15
)

# Talk raw Redis protocol over a .NET socket (no redis-cli, no Java).
# Read-only: only AUTH + SUBSCRIBE, never writes anything.
[Console]::OutputEncoding = [Text.Encoding]::UTF8

$client = New-Object System.Net.Sockets.TcpClient
Write-Host ("[target] {0}:{1} channel={2} listen={3}s" -f $RedisHost, $Port, $Channel, $Seconds)

$task = $client.ConnectAsync($RedisHost, $Port)
if (-not $task.Wait(8000) -or -not $client.Connected) {
    Write-Host "[error] connect failed or timeout"
    exit 1
}
Write-Host "[connect] ok"

$ns = $client.GetStream()
$ns.ReadTimeout = 1000
$ns.WriteTimeout = 5000
$enc = [Text.Encoding]::UTF8

function Send-Redis([string]$cmd) {
    $bytes = $enc.GetBytes($cmd + "`r`n")
    $ns.Write($bytes, 0, $bytes.Length)
    $ns.Flush()
}

# Password sentinels: "-" / "none" / "null" / empty all mean "no AUTH"
# (PowerShell swallows a bare empty argument, so pass "-" or "none" for passwordless Redis)
if ($Password -ne "" -and $Password -ne "-" -and $Password -ne "none" -and $Password -ne "null") {
    Send-Redis ("AUTH " + $Password)
    Start-Sleep -Milliseconds 300
}
Send-Redis ("SUBSCRIBE " + $Channel)

$buf = New-Object byte[] 65536
$deadline = (Get-Date).AddSeconds($Seconds)
$count = 0

while ((Get-Date) -lt $deadline) {
    try {
        $n = $ns.Read($buf, 0, $buf.Length)
        if ($n -le 0) { Write-Host "[closed] peer closed the connection"; break }
        $now = (Get-Date).ToString("HH:mm:ss.fff")
        $text = $enc.GetString($buf, 0, $n)
        foreach ($line in ($text -split "`r`n")) {
            if ($line.Length -eq 0) { continue }
            if ($line.StartsWith("*") -or $line.StartsWith("$")) { continue }
            if ($line.StartsWith("[") -or $line.StartsWith("{")) {
                $count = $count + 1
                Write-Host ("[{0}] msg : {1}" -f $now, $line)
            } else {
                Write-Host ("[{0}] resp: {1}" -f $now, $line)
            }
        }
    } catch [System.IO.IOException] {
        # read timeout: expected, keep waiting
    } catch {
        Write-Host ("[read error] " + $_.Exception.Message)
        break
    }
}

Write-Host ("[done] messages received: " + $count)
$client.Close()
