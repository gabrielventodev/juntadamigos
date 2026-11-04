# juntadamigos

App Android (Kotlin) que sugiere un lugar real de encuentro para 2 o más amigos, minimizando el tiempo total de viaje según cómo viaja cada uno (auto, transporte público, a pie, bici).

## Estado

- [x] **Paso 1: motor de cálculo** (`core/`, Kotlin puro, sin Android)
- [ ] Paso 2: app Android MVP (Jetpack Compose)
- [ ] Paso 3: pantalla de resultados (mapa, top 3, compartir)
- [ ] Paso 4: publicación en Play Store

## Cómo decide el lugar

1. Calcula el centro geográfico de los participantes (el dueño de la app cuenta como uno más).
2. Busca hasta 15 lugares de la categoría elegida (café, bar, restaurante, parque, shopping) alrededor de ese centro con **Places API (New)**.
3. Pide a **Routes API** (`computeRouteMatrix`) el tiempo de cada participante a cada candidato, una matriz por modo de viaje.
4. Descarta candidatos sin ruta para alguien y, si se configuró, los que superan un tope de viaje individual.
5. Ordena por **menor tiempo total**, desempata por el peor viaje individual y después por rating.

El tope individual (`maxIndividualTravel`) existe porque "menor tiempo total" castiga al que vive lejos: si tres están juntos y uno lejos, el lugar queda pegado al grupo.

## Uso

```kotlin
val finder = MeetingPointFinder(
    placeSearch = GooglePlaceSearch(apiKey),
    travelTimeMatrix = GoogleRouteMatrix(apiKey),
)
val result = finder.find(
    participants = listOf(
        Participant("yo", "Gabriel", LatLng(-34.60, -58.38), TravelMode.DRIVE),
        Participant("ana", "Ana", LatLng(-34.58, -58.43), TravelMode.TRANSIT),
    ),
    category = PlaceCategory.BAR,
    maxIndividualTravel = 60.minutes, // opcional
)
result.suggestions.forEach { println("${it.place.name}: total ${it.totalTravelTime}") }
```

## APIs de Google necesarias

Habilitar en Google Cloud Console, en el mismo proyecto:

| API | Para qué | Uso por búsqueda |
|---|---|---|
| Places API (New) | Lugares candidatos y autocompletado de direcciones | 1 Nearby Search |
| Routes API | Matriz de tiempos de viaje | participantes × 15 elementos |
| Geocoding API | Dirección escrita a coordenadas (alternativa al autocompletado) | 1 por dirección |
| Maps SDK for Android | Mapa en la app (paso 3) | por carga de mapa |

Costo: con 4 amigos son ~60 elementos de matriz por búsqueda. El tráfico en tiempo real para auto (`trafficAware = true`) usa un SKU más caro; se puede desactivar. Configurar un tope de gasto en Cloud Billing antes de publicar.

## Desarrollo

Requiere JDK 17+.

```bash
./gradlew :core:test
```
