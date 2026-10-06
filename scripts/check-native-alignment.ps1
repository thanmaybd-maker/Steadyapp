param([string]$Apk = 'app\build\outputs\apk\debug\app-debug.apk')
$ErrorActionPreference = 'Stop'
$apkPath = (Resolve-Path -LiteralPath $Apk).Path
$reader = [System.IO.BinaryReader]::new([System.IO.File]::OpenRead($apkPath))
try {
    $tailLength = [int][Math]::Min(65557,$reader.BaseStream.Length)
    $reader.BaseStream.Position = $reader.BaseStream.Length - $tailLength
    $tail = $reader.ReadBytes($tailLength)
    $endIndex = -1
    for($index = $tail.Length - 22; $index -ge 0; $index--) {
        if([BitConverter]::ToUInt32($tail,$index) -eq 0x06054b50 -and $index+22+[BitConverter]::ToUInt16($tail,$index+20) -eq $tail.Length) { $endIndex=$index; break }
    }
    if($endIndex -lt 0) { throw 'ZIP end record missing' }
    $count = [BitConverter]::ToUInt16($tail,$endIndex+10)
    $centralOffset = [BitConverter]::ToUInt32($tail,$endIndex+16)
    if($count -eq 65535 -or $centralOffset -eq [uint32]::MaxValue) { throw 'ZIP64 is outside this check; use a compatible zipalign tool' }
    $reader.BaseStream.Position = $centralOffset
    $nativeEntries = @()
    for($entryIndex=0; $entryIndex -lt $count; $entryIndex++) {
        $header = $reader.ReadBytes(46)
        if($header.Length -ne 46 -or [BitConverter]::ToUInt32($header,0) -ne 0x02014b50) { throw 'Invalid central directory' }
        $nameLength = [BitConverter]::ToUInt16($header,28)
        $extraLength = [BitConverter]::ToUInt16($header,30)
        $commentLength = [BitConverter]::ToUInt16($header,32)
        $name = [Text.Encoding]::UTF8.GetString($reader.ReadBytes($nameLength))
        $reader.BaseStream.Position += $extraLength+$commentLength
        if($name -notmatch '^lib/[^/]+/[^/]+\.so$') { continue }
        $centralPosition = $reader.BaseStream.Position
        $method = [BitConverter]::ToUInt16($header,10)
        if($method -ne 0) { throw "Native entry is compressed: $name" }
        $reader.BaseStream.Position = [BitConverter]::ToUInt32($header,42)
        $local = $reader.ReadBytes(30)
        if([BitConverter]::ToUInt32($local,0) -ne 0x04034b50) { throw 'Invalid local header' }
        $dataOffset = $reader.BaseStream.Position + [BitConverter]::ToUInt16($local,26) + [BitConverter]::ToUInt16($local,28)
        if($dataOffset % 16384 -ne 0) { throw "Native ZIP data is not aligned to 16 KiB: $name" }
        $reader.BaseStream.Position = $dataOffset
        $bytes = $reader.ReadBytes([BitConverter]::ToUInt32($header,24))
        if($bytes.Length -lt 64 -or $bytes[0] -ne 127 -or [Text.Encoding]::ASCII.GetString($bytes,1,3) -ne 'ELF' -or $bytes[5] -ne 1) { throw 'Unsupported ELF header' }
        $is64 = $bytes[4] -eq 2
        if(-not $is64 -and $bytes[4] -ne 1) { throw 'Unsupported ELF class' }
        $programOffset = if($is64) { [BitConverter]::ToUInt64($bytes,32) } else { [BitConverter]::ToUInt32($bytes,28) }
        $programSize = [BitConverter]::ToUInt16($bytes, $(if($is64) {54} else {42}))
        $programCount = [BitConverter]::ToUInt16($bytes, $(if($is64) {56} else {44}))
        $minimumAlignment = [uint64]::MaxValue
        for($programIndex=0; $programIndex -lt $programCount; $programIndex++) {
            $offset = [int]($programOffset + $programIndex*$programSize)
            if($offset+$programSize -gt $bytes.Length) { throw 'ELF program headers exceed entry' }
            if([BitConverter]::ToUInt32($bytes,$offset) -ne 1) { continue }
            $alignment = if($is64) { [BitConverter]::ToUInt64($bytes,$offset+48) } else { [BitConverter]::ToUInt32($bytes,$offset+28) }
            $minimumAlignment = [Math]::Min($minimumAlignment,$alignment)
        }
        if($minimumAlignment -eq [uint64]::MaxValue -or $minimumAlignment -lt 16384) { throw "ELF LOAD alignment is below 16 KiB: $name" }
        $nativeEntries += [pscustomobject]@{Library=$name;ZipDataOffset=$dataOffset;MinimumLoadAlignment=$minimumAlignment}
        $reader.BaseStream.Position = $centralPosition
    }
    if($nativeEntries.Count -eq 0) { throw 'No native entries found' }
    $nativeEntries | Format-Table -AutoSize
    Write-Output "PASS: all $($nativeEntries.Count) native entries have 16 KiB ZIP and ELF LOAD alignment."
} finally { $reader.Dispose() }
