# Git Helper Scripts for MoneyManager Development (PowerShell)

param(
    [Parameter(Position=0)]
    [string]$Command,
    [Parameter(Position=1)]
    [string]$Name
)

function Show-Status {
    Write-Host "=== Git Status ===" -ForegroundColor Blue
    git status
    Write-Host ""
    Write-Host "=== Recent Commits ===" -ForegroundColor Blue
    git log --oneline -5
}

function Create-Feature {
    param([string]$FeatureName)
    
    if ([string]::IsNullOrEmpty($FeatureName)) {
        Write-Host "Error: Please provide a feature name" -ForegroundColor Red
        Write-Host "Usage: .\git-helper.ps1 feature feature-name"
        return
    }
    
    Write-Host "Creating feature branch: feature/$FeatureName" -ForegroundColor Green
    git checkout -b "feature/$FeatureName"
}

function Create-Fix {
    param([string]$FixName)
    
    if ([string]::IsNullOrEmpty($FixName)) {
        Write-Host "Error: Please provide a fix name" -ForegroundColor Red
        Write-Host "Usage: .\git-helper.ps1 fix fix-name"
        return
    }
    
    Write-Host "Creating fix branch: fix/$FixName" -ForegroundColor Green
    git checkout -b "fix/$FixName"
}

function Quick-Commit {
    param([string]$Message)
    
    if ([string]::IsNullOrEmpty($Message)) {
        Write-Host "Error: Please provide a commit message" -ForegroundColor Red
        Write-Host "Usage: .\git-helper.ps1 commit `"Your commit message`""
        return
    }
    
    Write-Host "Adding all changes and committing..." -ForegroundColor Green
    git add .
    git commit -m $Message
}

function Finish-Feature {
    $currentBranch = git branch --show-current
    
    if (-not $currentBranch.StartsWith("feature/")) {
        Write-Host "Error: Not on a feature branch" -ForegroundColor Red
        return
    }
    
    Write-Host "Switching to master and merging $currentBranch" -ForegroundColor Yellow
    git checkout master
    git merge $currentBranch
    
    Write-Host "Deleting feature branch: $currentBranch" -ForegroundColor Yellow
    git branch -d $currentBranch
    
    Write-Host "Feature merged and cleaned up!" -ForegroundColor Green
}

function Show-Help {
    Write-Host "MoneyManager Git Helper" -ForegroundColor Blue
    Write-Host ""
    Write-Host "Available commands:"
    Write-Host "  status              - Show git status and recent commits"
    Write-Host "  feature <name>      - Create new feature branch"
    Write-Host "  fix <name>          - Create new fix branch"
    Write-Host "  commit `"message`"    - Quick add and commit all changes"
    Write-Host "  finish              - Merge current feature branch to master and cleanup"
    Write-Host "  help                - Show this help"
    Write-Host ""
    Write-Host "Examples:"
    Write-Host "  .\git-helper.ps1 feature add-budget-tracking"
    Write-Host "  .\git-helper.ps1 commit `"Add transaction validation`""
    Write-Host "  .\git-helper.ps1 finish"
}

# Main script logic
switch ($Command) {
    "status" { Show-Status }
    "feature" { Create-Feature -FeatureName $Name }
    "fix" { Create-Fix -FixName $Name }
    "commit" { Quick-Commit -Message $Name }
    "finish" { Finish-Feature }
    "help" { Show-Help }
    default { 
        if ([string]::IsNullOrEmpty($Command)) {
            Show-Help
        } else {
            Write-Host "Unknown command: $Command" -ForegroundColor Red
            Show-Help
        }
    }
}