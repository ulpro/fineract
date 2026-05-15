# Lancer la stack Fineract + Pentaho

## Démarrage

```bash
IMAGE_NAME=fineract:latest docker compose -f docker-compose-custom.yml up -d
```

## Si les reports Pentaho ont disparu (reset de la DB)

```bash
docker exec -i mariadb sh -c "mariadb -u root -pskdcnwauicn2ucnaecasdsajdnizucawencascdca fineract_default" < scripts/install-pentaho-client-saving-reports.sql
```

## Vérifier que tout est OK

```bash
# Pentaho enregistré au démarrage
docker logs fineract-fineract-1 2>&1 | grep -i pentaho

# Test report PDF
curl -k -u mifos:password -H 'Fineract-Platform-TenantId: default' \
  'https://localhost:8443/fineract-provider/api/v1/runreports/Client%20Saving%20Transactions?R_accountNo=000000001&R_startDate=01%20May%202026&R_endDate=13%20May%202026&R_OfficeId=1&output-type=PDF&locale=en&dateFormat=dd%20MMMM%20yyyy' \
  -o /tmp/test.pdf -w '%{http_code} | %{content_type}\n'
# Attendu : 200 | application/pdf
```

## ⚠️ Ne pas lancer

```bash
docker compose up -d   # ← sans -f docker-compose-custom.yml : le plugin Pentaho ne sera PAS monté
```
