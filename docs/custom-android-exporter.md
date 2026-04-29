# Exporteur Android custom

Cette partie est l'etape avancee. Elle devient utile si aucune app d'export Health Connect ne sait envoyer proprement tes donnees vers l'API.

## Responsabilites

L'app Android doit :

- demander les permissions Health Connect ;
- lire les aggregats quotidiens ;
- construire le payload `docs/api.md` ;
- envoyer le payload au backend ;
- planifier l'envoi avec WorkManager.

## Permissions typiques

Exemples de donnees :

- pas ;
- sommeil ;
- calories actives ;
- sessions sportives ;
- frequence cardiaque ;
- poids ;
- nutrition.

Les permissions exactes dependent des types de records Health Connect disponibles sur ta version Android et des donnees ecrites par Zepp ou ton app nutrition.

## Pseudo-code

```kotlin
val start = LocalDate.now().minusDays(1).atStartOfDay(zone).toInstant()
val end = LocalDate.now().atStartOfDay(zone).toInstant()

val steps = healthConnectClient.aggregate(
    AggregateRequest(
        metrics = setOf(StepsRecord.COUNT_TOTAL),
        timeRangeFilter = TimeRangeFilter.between(start, end)
    )
)[StepsRecord.COUNT_TOTAL]

val payload = HealthPayload(
    date = LocalDate.now().minusDays(1).toString(),
    source = "health_connect",
    metrics = listOf(
        Metric("steps", steps ?: 0, "count")
    )
)

httpClient.post("$backendUrl/api/ingest/health-connect") {
    header("x-api-key", apiKey)
    contentType(ContentType.Application.Json)
    setBody(payload)
}
```

## Plan de build

1. Commencer avec les pas, sommeil, poids.
2. Ajouter calories actives et sport.
3. Ajouter nutrition seulement si l'app alimentaire ecrit bien dans Health Connect.
4. Ajouter un ecran de diagnostic pour voir les dernieres valeurs lues avant envoi.
