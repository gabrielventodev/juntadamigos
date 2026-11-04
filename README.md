# juntadamigos

App Android (Kotlin) que sugiere un lugar real de encuentro para 2 o más amigos, minimizando el tiempo total de viaje según cómo viaja cada uno (auto, transporte público, a pie, bici).

## Estado

- [x] **Paso 1: motor de cálculo** (`core/`, Kotlin puro, sin Android)
- [x] **Paso 2: app Android MVP** (`app/`, Jetpack Compose)
- [x] **Paso 3: pantalla de resultados**: mapa con amigos y top 3, tiempo por persona, abrir en Google Maps y compartir por WhatsApp
- [ ] Paso 4: publicación en Play Store

## Cómo decide el lugar

1. Arma puntos de muestra: el centro geográfico y 4 puntos entre el centro y cada participante (el último es su casa). El dueño de la app cuenta como uno más.
2. Pide a **Routes API** (`computeRouteMatrix`) el tiempo real de todos a esos puntos y elige las 3 zonas de menor tiempo total ponderado.
3. Busca hasta 15 lugares de la categoría elegida (café, bar, restaurante, parque, shopping) en esas zonas con **Places API (New)**.
4. Pide el tiempo de cada participante a cada candidato, una matriz por modo de viaje.
5. Descarta candidatos sin ruta para alguien y, si se configuró, los que superan un tope de viaje individual.
6. Ordena por **menor tiempo total ponderado**: un minuto de quien va en transporte, a pie o en bici pesa 1.5 veces uno en auto (`Config.modeWeights`). Desempata por el peor viaje individual y después por rating. La app muestra los tiempos reales.

Por qué no buscar solo alrededor del centro: si uno va en auto y otro sin auto, el mejor lugar suele quedar del lado del que viaja más lento, y un círculo en el centro nunca llega ahí (además Places devuelve lo más popular del círculo, típicamente el microcentro).

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
| Places API (New) | Lugares candidatos y autocompletado de direcciones | hasta 3 Nearby Search + 1 sesión de autocompletado por dirección |
| Routes API | Matriz de tiempos de viaje | participantes × (1 + 4 × participantes) para elegir zonas + participantes × 15 |
| Geocoding API | Solo pruebas del motor; la app no la usa (no acepta keys restringidas a Android) | — |
| Maps SDK for Android | Mapa en la app (paso 3) | por carga de mapa |

Costo: con 4 amigos son ~130 elementos de matriz por búsqueda. El tráfico en tiempo real para auto (`trafficAware = true`) usa un SKU más caro; se puede desactivar. Configurar un tope de gasto en Cloud Billing antes de publicar.

## Desarrollo

Requiere JDK 17+ y Android SDK (API 35).

```bash
./gradlew :core:test            # tests del motor (con mocks)
./gradlew :app:assembleDebug    # APK de debug
```

### API key

- **App:** poné `MAPS_API_KEY=tu_key` en `local.properties` (no se commitea). El Secrets Gradle Plugin la expone como `BuildConfig.MAPS_API_KEY`. Sin key, la app compila con `local.defaults.properties` y avisa en pantalla.
- **Restricción:** restringí la key de la app a "Apps de Android" con el paquete `com.juntadamigos.app` y el SHA-1 de tu certificado de firma (`./gradlew :app:signingReport`). La app manda `X-Android-Package` y `X-Android-Cert` en cada request REST, que es lo que Google necesita para validar esa restricción. Limitá también las APIs de la key a Places API (New), Routes API y Maps SDK for Android (sin esta última el mapa de resultados queda gris).
- **Pruebas reales del motor** (cuestan plata, opt-in):

```bash
LIVE_GOOGLE_TESTS=1 GOOGLE_MAPS_API_KEY=... ./gradlew :core:test --tests '*LiveGoogleSmokeTest*' -i
```

CI (GitHub Actions) corre los tests del motor y compila el APK de debug en cada push y PR.
