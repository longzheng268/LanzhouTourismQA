#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Gradle wrapper for Windows PowerShell
    
.DESCRIPTION
    This script allows running gradle commands from PowerShell on Windows
    without needing the .bat or shell versions.
    
.EXAMPLE
    .\gradlew.ps1 clean build
    .\gradlew.ps1 build --stacktrace
    .\gradlew.ps1 tasks
#>

param(
    [Parameter(ValueFromRemainingArguments=$true)]
    [string[]]$Arguments = @()
)

# 检查gradle是否可用
try {
    $gradleVersion = gradle --version 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Error "无法执行gradle命令。请确保Gradle已安装并添加到PATH中。"
        exit 1
    }
} catch {
    Write-Error "找不到gradle命令。请确保Gradle已安装。"
    exit 1
}

Write-Host "使用 Gradle 执行构建..." -ForegroundColor Green
Write-Host "gradle $($Arguments -join ' ')" -ForegroundColor Cyan

# 执行gradle命令
& gradle @Arguments

# 传递exit code
exit $LASTEXITCODE
