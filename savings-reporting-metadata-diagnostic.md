# Diagnostic du probleme de metadata reporting sur Savings Account Transactions

## Objectif

Comprendre pourquoi le bouton `Generate Report` sur la page `Savings Account Transactions` echoue avec le message suivant:

`The requested resource is not available. Reporting meta-data entry not found.`

## Resume de la conclusion

Le probleme ne vient pas d'un moteur de reporting casse cote backend, ni d'un `report code` Java manquant.

Le backend Fineract resout le report uniquement par son nom, via la colonne `stretchy_report.report_name`.

Les reports standard existants pour les transactions d'epargne verifies localement sont:

- `Savings Deposit`
- `Savings Withdrawal`

L'erreur observee est reproduite lorsqu'on appelle un nom de report inexistant, par exemple:

- `Savings Transactions`
- `Savings Account Transactions`

Le diagnostic a ensuite ete affine avec l'URL exacte du navigateur.

L'UI n'appelait pas seulement un libelle generique comme `Savings Account Transactions`, mais aussi:

- `Client Saving Transactions`

Ce nom correspond a un ancien report `Pentaho` visible dans les fichiers de sample SQL et dans les permissions historiques, mais il n'est pas present dans les metadata actives du tenant local.

La cause probable devient donc plus precise:

- soit l'UI appelle un nom de report obsolete ou legacy
- soit elle depend d'un report Pentaho non installe dans cette distribution Fineract

## Comment le diagnostic a ete mene

### 1. Verification du point d'entree backend

J'ai commence par verifier quel endpoint traite l'execution des reports.

Point d'entree identifie:

- [/home/ulrich/fineract/fineract/fineract-provider/src/main/java/org/apache/fineract/infrastructure/dataqueries/api/RunreportsApiResource.java]( /home/ulrich/fineract/fineract/fineract-provider/src/main/java/org/apache/fineract/infrastructure/dataqueries/api/RunreportsApiResource.java )

Constat:

- le bouton passe par `/v1/runreports/{reportName}`
- le backend ne cherche pas un identifiant technique distinct a cette etape
- il cherche directement `reportName`

### 2. Verification de l'endroit exact ou l'erreur est levee

J'ai ensuite verifie le log applicatif local.

Trace observee:

- [/home/ulrich/fineract/fineract/build/fineract/logs/fineract.2026-05-13.log]( /home/ulrich/fineract/fineract/build/fineract/logs/fineract.2026-05-13.log )

Exception levee dans:

- [/home/ulrich/fineract/fineract/fineract-provider/src/main/java/org/apache/fineract/infrastructure/dataqueries/service/ReadReportingServiceImpl.java]( /home/ulrich/fineract/fineract/fineract-provider/src/main/java/org/apache/fineract/infrastructure/dataqueries/service/ReadReportingServiceImpl.java )

Le lookup critique est:

```sql
SELECT coalesce(report_type,'') AS report_type
FROM stretchy_report
WHERE report_name = ?
```

Cela montre que l'erreur signifie tres precisement:

> aucune ligne `stretchy_report` ne correspond au `reportName` demande.

### 3. Verification des metadata attendues dans le code / changelog

J'ai ensuite retrouve la declaration des reports standards dans les changelogs de donnees Fineract.

Definitions trouvees dans:

- [/home/ulrich/fineract/fineract/fineract-provider/src/main/resources/db/changelog/tenant/parts/0002_initial_data.xml]( /home/ulrich/fineract/fineract/fineract-provider/src/main/resources/db/changelog/tenant/parts/0002_initial_data.xml )
- [/home/ulrich/fineract/fineract/fineract-provider/src/main/resources/db/changelog/tenant/parts/0003_postgresql_specific_initial_data.xml]( /home/ulrich/fineract/fineract/fineract-provider/src/main/resources/db/changelog/tenant/parts/0003_postgresql_specific_initial_data.xml )

Reports identifies:

- `Savings Deposit`
- `Savings Withdrawal`

Mappings de parametres identifies:

- `officeId`
- `loanOfficerId`
- `savingsTransactionId`

Ces mappings sont associes via `stretchy_report_parameter`.

### 4. Verification directe dans la base locale

La stack locale tournait avec Docker, MariaDB et le tenant `default`.

J'ai verifie les containers actifs avec:

```bash
docker ps --format '{{.Names}}\t{{.Image}}\t{{.Ports}}'
```

Puis j'ai verifie le tenant en base:

```bash
docker exec mariadb mariadb -uroot -p<root_password> -e "DESCRIBE fineract_tenants.tenants; SELECT * FROM fineract_tenants.tenants;"
```

Constat:

- un seul tenant configure: `default`
- schema cible: `fineract_default`

Ensuite, verification des reports en base:

```bash
docker exec mariadb mariadb -uroot -p<root_password> -e "
SELECT report_name, report_type, report_subtype, use_report
FROM fineract_default.stretchy_report
WHERE report_name IN ('Savings Deposit','Savings Withdrawal');

SELECT sr.report_name, srp.report_parameter_name, srp.parameter_id
FROM fineract_default.stretchy_report_parameter srp
JOIN fineract_default.stretchy_report sr ON sr.id = srp.report_id
WHERE sr.report_name IN ('Savings Deposit','Savings Withdrawal')
ORDER BY sr.report_name, srp.id;"
```

Resultat:

- `Savings Deposit` existe
- `Savings Withdrawal` existe
- les mappings `officeId`, `loanOfficerId`, `savingsTransactionId` existent aussi

### 5. Verification via l'API Fineract

J'ai verifie que l'API voyait bien ces reports:

```bash
curl -ksu mifos:password \
  -H 'Fineract-Platform-TenantId: default' \
  'https://localhost:8443/fineract-provider/api/v1/reports?fields=id,reportName,reportType'
```

Resultat observe:

- `Savings Deposit`
- `Savings Withdrawal`

### 6. Reproduction comparee du comportement correct et incorrect

J'ai ensuite execute un vrai report avec un `savingsTransactionId` existant.

Appel qui fonctionne:

```bash
curl -ksu mifos:password \
  -H 'Fineract-Platform-TenantId: default' \
  'https://localhost:8443/fineract-provider/api/v1/runreports/Savings%20Deposit?R_officeId=-1&R_loanOfficerId=-1&R_savingsTransactionId=2'
```

Resultat:

- retour JSON valide avec `columnHeaders` et data

Puis j'ai appele un nom de report plausible mais inexistant:

```bash
curl -ksu mifos:password \
  -H 'Fineract-Platform-TenantId: default' \
  'https://localhost:8443/fineract-provider/api/v1/runreports/Savings%20Transactions?R_officeId=-1&R_loanOfficerId=-1&R_savingsTransactionId=2'
```

Et aussi:

```bash
curl -ksu mifos:password \
  -H 'Fineract-Platform-TenantId: default' \
  'https://localhost:8443/fineract-provider/api/v1/runreports/Savings%20Account%20Transactions?R_officeId=-1&R_loanOfficerId=-1&R_savingsTransactionId=2'
```

Resultat:

- erreur `404`
- message `Reporting meta-data entry not found.`

Ce comportement est exactement celui qui etait signale.

### 7. Verification de l'URL exacte remontee par le navigateur

L'erreur navigateur a montre cette URL:

```text
https://localhost:8443/fineract-provider/api/v1/runreports/Client%20Saving%20Transactions?tenantIdentifier=default&locale=fr&dateFormat=dd%20MMMM%20yyyy&output-type=PDF&R_startDate=12%20mai%202026&R_endDate=13%20mai%202026&R_savingsAccountId=000000001
```

J'ai rejoue cet appel tel quel:

```bash
curl -ksu mifos:password \
  -H 'Fineract-Platform-TenantId: default' \
  'https://localhost:8443/fineract-provider/api/v1/runreports/Client%20Saving%20Transactions?tenantIdentifier=default&locale=fr&dateFormat=dd%20MMMM%20yyyy&output-type=PDF&R_startDate=12%20mai%202026&R_endDate=13%20mai%202026&R_savingsAccountId=000000001'
```

Resultat:

- meme `404`
- meme message `Reporting meta-data entry not found.`

Ensuite, j'ai verifie si `Client Saving Transactions` existait reellement dans le catalogue `/v1/reports` du tenant local.

Resultat:

- aucune entree `Client Saving Transactions`
- aucune entree `Client Savings Summary`

Donc l'UI appelle bien un report qui n'existe pas dans les metadata actives du backend local.

### 8. Verification du statut de `Client Saving Transactions`

Le nom `Client Saving Transactions` existe encore dans le repo, mais uniquement comme trace legacy.

Ce que j'ai trouve:

- une permission historique `READ_Client Saving Transactions` dans [/home/ulrich/fineract/fineract/fineract-provider/src/main/resources/db/changelog/tenant/parts/0002_initial_data.xml]( /home/ulrich/fineract/fineract/fineract-provider/src/main/resources/db/changelog/tenant/parts/0002_initial_data.xml )
- une definition sample SQL du report dans [/home/ulrich/fineract/fineract/fineract-provider/src/main/resources/sql/migrations/sample_data/load_sample_data.sql]( /home/ulrich/fineract/fineract/fineract-provider/src/main/resources/sql/migrations/sample_data/load_sample_data.sql )

Mais cette definition sample est de type:

- `Pentaho`

Extrait constate dans les samples:

- `Client Saving Transactions`, `report_type = 'Pentaho'`

Or la documentation du projet indique explicitement:

- [/home/ulrich/fineract/fineract/fineract-doc/modules/ROOT/pages/faq.adoc]( /home/ulrich/fineract/fineract/fineract-doc/modules/ROOT/pages/faq.adoc )

Message important:

> Fineract does not ship Pentaho reports or the related libraries, due to compliance issues with Apache licence.

Cela explique le decalage:

- le frontend semble encore connaitre un ancien nom de report
- mais cette distribution Fineract locale ne fournit pas ce report Pentaho dans `stretchy_report`

## Quel etait le probleme

Le probleme etait un decalage entre:

- le nom de report attendu par Fineract backend
- et le nom de report probablement envoye depuis l'interface

Le backend attend un `reportName` exact correspondant a une ligne de `stretchy_report`, ici:

- `Savings Deposit`
- `Savings Withdrawal`

Mais l'erreur correspond au cas ou l'appel se fait avec un nom de type:

- `Savings Transactions`
- `Savings Account Transactions`
- `Client Saving Transactions`

Ces noms ressemblent a des libelles de page ou a des reports legacy, mais ne correspondent pas a une metadata active existante dans le tenant local.

## Comment le probleme a ete resolu

Le probleme a ete resolu au niveau du diagnostic, pas par une modification du code backend local.

Ce qui a ete etabli de maniere certaine:

- le backend local est sain
- les metadata sont presentes dans la base locale
- le report fonctionne quand on utilise le bon nom
- l'erreur apparait quand on utilise un nom de report invalide

La resolution attendue dans l'environnement qui pose probleme est donc:

### Cas 1. Le frontend envoie le mauvais nom ou un nom legacy

Correction attendue:

- remplacer le nom appele par `Savings Deposit` ou `Savings Withdrawal` selon le type de transaction
- ou supprimer la dependance au report `Client Saving Transactions` si l'UI a ete reprise depuis un ancien frontend Mifos / Pentaho

Indice utile dans le code Fineract:

- [/home/ulrich/fineract/fineract/fineract-provider/src/main/java/org/apache/fineract/infrastructure/campaigns/sms/service/SmsCampaignDomainServiceImpl.java]( /home/ulrich/fineract/fineract/fineract-provider/src/main/java/org/apache/fineract/infrastructure/campaigns/sms/service/SmsCampaignDomainServiceImpl.java )

Ce code choisit deja:

- `Savings Deposit`
- `Savings Withdrawal`

### Cas 2. L'environnement cible attend encore un report Pentaho non fourni

Correction attendue:

- soit migrer l'UI pour utiliser un report Fineract actif
- soit deployer une distribution qui embarque encore les reports Pentaho necessaires

Important:

- sur Apache Fineract standard, les reports Pentaho ne sont pas livres
- avoir la permission `READ_Client Saving Transactions` ne suffit pas
- il faut aussi une ligne reelle dans `stretchy_report`

### Cas 3. Les metadata sont absentes dans l'environnement cible

Correction attendue:

- reimporter les lignes `stretchy_report`
- reimporter les lignes `stretchy_report_parameter`

Sources de reimport possibles:

- [/home/ulrich/fineract/fineract/fineract-provider/src/main/resources/db/changelog/tenant/parts/0002_initial_data.xml]( /home/ulrich/fineract/fineract/fineract-provider/src/main/resources/db/changelog/tenant/parts/0002_initial_data.xml )
- [/home/ulrich/fineract/fineract/fineract-provider/src/main/resources/sql/migrations/sample_data/load_sample_data.sql]( /home/ulrich/fineract/fineract/fineract-provider/src/main/resources/sql/migrations/sample_data/load_sample_data.sql )

Important:

- sur un tenant deja initialise, Liquibase ne recreera pas automatiquement ces lignes si le changeset est deja marque comme applique

## Commandes utiles de verification

### Verifier les reports en base

```sql
SELECT report_name, report_type, report_subtype, use_report
FROM stretchy_report
WHERE report_name IN (
  'Savings Deposit',
  'Savings Withdrawal',
  'Savings Transactions',
  'Savings Account Transactions',
  'Client Saving Transactions',
  'Client Savings Summary'
);
```

### Verifier les mappings de parametres

```sql
SELECT sr.report_name, srp.report_parameter_name, srp.parameter_id
FROM stretchy_report_parameter srp
JOIN stretchy_report sr ON sr.id = srp.report_id
WHERE sr.report_name IN ('Savings Deposit', 'Savings Withdrawal')
ORDER BY sr.report_name, srp.id;
```

### Verifier via l'API quels reports existent

```bash
curl -ksu mifos:password \
  -H 'Fineract-Platform-TenantId: default' \
  'https://localhost:8443/fineract-provider/api/v1/reports?fields=id,reportName,reportType'
```

### Tester un appel correct

```bash
curl -ksu mifos:password \
  -H 'Fineract-Platform-TenantId: default' \
  'https://localhost:8443/fineract-provider/api/v1/runreports/Savings%20Deposit?R_officeId=-1&R_loanOfficerId=-1&R_savingsTransactionId=2'
```

### Tester un appel incorrect qui reproduit l'erreur

```bash
curl -ksu mifos:password \
  -H 'Fineract-Platform-TenantId: default' \
  'https://localhost:8443/fineract-provider/api/v1/runreports/Savings%20Account%20Transactions?R_officeId=-1&R_loanOfficerId=-1&R_savingsTransactionId=2'
```

```bash
curl -ksu mifos:password \
  -H 'Fineract-Platform-TenantId: default' \
  'https://localhost:8443/fineract-provider/api/v1/runreports/Client%20Saving%20Transactions?tenantIdentifier=default&locale=fr&dateFormat=dd%20MMMM%20yyyy&output-type=PDF&R_startDate=12%20mai%202026&R_endDate=13%20mai%202026&R_savingsAccountId=000000001'
```

## Conclusion finale

Le probleme diagnostique est un probleme de nom de report demande, pas un bug general du backend de reporting.

Le backend Fineract attend une entree exacte dans `stretchy_report.report_name`.

Pour les transactions d'epargne, les noms valides identifies sont:

- `Savings Deposit`
- `Savings Withdrawal`

Si l'UI appelle `Savings Transactions`, `Savings Account Transactions` ou `Client Saving Transactions`, elle obtient exactement l'erreur observee dans cette stack.

Dans le cas de `Client Saving Transactions`, la piste la plus forte est maintenant un appel frontend vers un ancien report Pentaho non disponible dans cette distribution Apache Fineract.

Si un autre environnement reproduit l'erreur avec le bon nom, alors il faut verifier que les metadata ont bien ete conservees dans la base de ce tenant et les reimporter si necessaire.