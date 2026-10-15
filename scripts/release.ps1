# Rilascia i microservizi sul cluster con lo SHA del commit attuale.
# Da lanciare dalla root del progetto, DOPO che la pipeline è verde.

$sha = git rev-parse HEAD
$services = "customer-service", "account-service", "transaction-service", "notification-service", "api-gateway"

Write-Host "Rilascio dello SHA $sha" -ForegroundColor Cyan

foreach ($s in $services) {
    Write-Host "`n=== $s ===" -ForegroundColor Yellow

    helm upgrade $s .\helm\microservice -f ".\helm\values\$s.yaml" --set image.tag=$sha --description "Release $sha"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "helm upgrade fallito per ${s}: mi fermo." -ForegroundColor Red
        exit 1
    }

    kubectl rollout status deployment/$s --timeout=180s
    if ($LASTEXITCODE -ne 0) {
        Write-Host "$s non è diventato Ready entro 3 minuti: mi fermo." -ForegroundColor Red
        exit 1
    }
}

Write-Host "`nRilascio completato." -ForegroundColor Green