# Publicación en Google Play (paso 4)

Orden pensado para no perder días: lo que tiene espera (verificación de cuenta, prueba cerrada de 14 días) arranca primero.

## 0. La restricción que define el calendario

Una **cuenta personal** de Play Console creada después de noviembre de 2023 no puede publicar en producción sin antes correr una **prueba cerrada con al menos 12 testers que sigan inscriptos 14 días seguidos**. Recién después se pide acceso a producción (y Google lo revisa, unos días más). La prueba interna no cuenta.

Cálculo realista: verificación de identidad (1 a 3 días) + armar la prueba + 14 días + revisión de acceso a producción ≈ **3 semanas como mínimo**. Si no tenés 12 personas que instalen la app y la abran de verdad, ese es el cuello de botella, no el código. Conseguí la lista (emails de cuentas de Google, idealmente con Android) **antes** de subir el primer AAB.

Una cuenta de **organización** evita esa regla pero exige número D-U-N-S de una empresa; para una microapp personal no compensa.

## 1. Cuenta de Play Console (tus manos)

1. Entrá a <https://play.google.com/console/signup> con la cuenta de Google que quieras dejar como dueña (no se puede transferir después sin migrar la app).
2. Elegí **Personal**, pagá los USD 25 (único pago).
3. Completá la verificación de identidad (documento) y verificá un teléfono Android con la app de Play Console.
4. El nombre de desarrollador y el email de contacto quedan **públicos** en la ficha. Si no querés exponer tu email personal, creá uno para la app.

## 2. Upload key (tus manos, una sola vez)

Play usa **Play App Signing**: vos firmás el AAB con tu *upload key*, Google lo re-firma con la *app signing key* que guarda él. Si perdés la upload key se puede resetear desde Play Console, pero lleva días; guardala bien.

En PowerShell, fuera de la carpeta del repo:

```powershell
mkdir $HOME\keys
keytool -genkeypair -v -keystore $HOME\keys\juntadamigos-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

(`keytool` viene con el JDK de Android Studio: `"C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"` si no está en el PATH.)

Guardá el `.jks` y las contraseñas en un gestor de contraseñas y en un backup fuera de la PC.

Creá `keystore.properties` en la raíz del repo (está en `.gitignore`, no se commitea). Usá barras `/`, no `\`:

```properties
storeFile=C:/Users/Gabriel/keys/juntadamigos-upload.jks
storePassword=...
keyAlias=upload
keyPassword=...
```

Sacá los SHA-1 que vas a necesitar en el paso 4:

```powershell
.\gradlew :app:signingReport
```

Anotá el SHA-1 de `debug` y el de `release`.

## 3. Generar y probar el release

```powershell
.\gradlew :app:bundleRelease -PversionCode=1 -PversionName=1.0.0
```

El AAB queda en `app\build\outputs\bundle\release\app-release.aab`. Cada subida a Play necesita un `versionCode` mayor que el anterior.

**Antes de subirlo, probá el release minificado en tu celular.** CI verifica que R8 compila, no que la app ande: un serializer o clase eliminada por R8 rompe la app recién en runtime. Desde Android Studio: *Build Variants* → `app` → `release`, Run. Hacé una búsqueda completa (autocompletado, resultados, mapa, abrir en Maps, compartir). Si algo falla solo en release, es R8: pasame el stack trace.

## 4. API key: restricción, cuotas y presupuesto (tus manos, en Google Cloud Console)

Verdad incómoda: la key viaja dentro del APK. Cualquiera puede extraerla, y los headers `X-Android-Package` / `X-Android-Cert` que valida la restricción se pueden falsificar desde fuera de Android. La restricción frena el abuso casual; **lo que limita la plata son las cuotas**. Un presupuesto en Billing solo avisa, no corta.

### 4.1 Restricción de la key

*APIs y servicios → Credenciales → tu key*:

- **Restricciones de aplicación: Apps para Android**, paquete `com.juntadamigos.app`, con estos SHA-1:
  - debug (para seguir desarrollando),
  - release / upload key (para probar el release local),
  - **app signing key de Play**: aparece *después* de subir el primer AAB, en Play Console → tu app → *Probar y publicar → Integridad de la app → Firma de apps*. **Sin este SHA-1, la app instalada desde Play da 403 en todas las búsquedas.** Es el error más probable del lanzamiento.
- **Restricciones de API**: solo Places API (New), Routes API y Maps SDK for Android.

Usá para la app una key distinta de la que usa el entorno de pruebas del motor (`GOOGLE_MAPS_API_KEY`), así podés rotar una sin romper la otra.

### 4.2 Cuotas (el tope real)

*APIs y servicios → Places API (New) → Cuotas y límites del sistema*, y lo mismo en Routes API. Bajá los límites por día/minuto a algo acorde al uso esperado. Referencia: una búsqueda con 4 amigos consume ~3 Nearby Search, ~4 sesiones de autocompletado y ~130 elementos de matriz de rutas. Para arrancar con 12 testers, unas pocas centenas de búsquedas diarias sobran. Subilos cuando haya uso real.

### 4.3 Presupuesto y alertas

*Facturación → Presupuestos y alertas → Crear presupuesto* sobre el proyecto de la key, con alertas al 50 %, 90 % y 100 % por email. Revisá también el crédito mensual gratuito de Maps Platform vigente en tu cuenta.

## 5. Política de privacidad hospedada

Play exige una URL pública (no un PDF ni un archivo del repo privado).

- Completá los dos campos marcados con `[...]` en [`docs/privacy-policy.md`](privacy-policy.md).
- Si el repo es público: *Settings → Pages → Deploy from branch → `main` / `/docs`*. Queda en `https://gabrielventodev.github.io/juntadamigos/privacy-policy.html` (GitHub Pages convierte el Markdown en HTML). Si es privado, Pages exige plan pago: pegá el texto en una página de Google Sites y usá esa URL.
- Abrí la URL en incógnito para confirmar que carga sin login.

## 6. Crear la app en Play Console

*Crear app* → nombre **Juntadamigos**, idioma predeterminado español (Latinoamérica), App, Gratis.

### 6.1 Contenido de la app (Política → Contenido de la app)

| Formulario | Respuesta |
|---|---|
| Política de privacidad | URL del paso 5 |
| Acceso a la app | Toda la funcionalidad está disponible sin restricciones (no hay login) |
| Anuncios | No contiene anuncios |
| Clasificación de contenido | Categoría "Utilidad, productividad, comunicación u otra"; responder No a todo (violencia, sexo, drogas, etc.). La app no permite que usuarios interactúen entre sí ni compartan ubicación en vivo; compartir por WhatsApp lo hace otra app. |
| Público objetivo | **18 años o más** (elegir menores activa la política de Familias y una revisión mucho más estricta) |
| App de noticias / gobierno / finanzas / salud | No |
| Eliminación de cuentas | No aplica: la app no crea cuentas |

### 6.2 Seguridad de los datos

"Recopilar" para Play incluye todo lo que sale del dispositivo, aunque sea hacia Google como proveedor y aunque nadie lo guarde. Por eso se declara aunque la app no tenga servidor.

- ¿Recopila o comparte datos? **Sí**.
- ¿Todos los datos se encriptan en tránsito? **Sí**.
- ¿Los usuarios pueden pedir que se borren? Elegir la opción de que no se almacenan / no aplica (la app no guarda nada).

Tipos de datos a marcar:

| Tipo | Recopilado | Compartido | Efímero | Obligatorio | Finalidad |
|---|---|---|---|---|---|
| Información personal → Dirección | Sí | No | Sí | Sí | Funcionalidad de la app |
| Ubicación → Ubicación precisa (coordenadas de las direcciones enviadas a Routes/Places) | Sí | No | Sí | Sí | Funcionalidad de la app |
| Info y rendimiento de la app → Registros de fallas, Diagnóstico | Sí | No | No | Sí | Análisis (lo recopila Maps SDK) |
| Dispositivo u otros IDs | Sí | No | No | Sí | Análisis / prevención de fraudes (Maps SDK) |

"Compartido = No" porque Google actúa como proveedor de servicio de la app, que Play excluye de "compartir". Las dos últimas filas salen de lo que declara el propio Maps SDK; confirmalas contra la página oficial de Google "Play Data safety for Maps SDK for Android" antes de enviar, porque Google la actualiza.

### 6.3 Ficha de Play Store (Hacer crecer → Presencia en la tienda → Ficha principal)

- **Nombre** (máx. 30): `Juntadamigos`
- **Descripción breve** (máx. 80): `Encontrá el lugar más justo para juntarte con tus amigos, según cómo viaja cada uno.`
- **Descripción completa** (máx. 4000):

```
¿Dónde nos juntamos? Juntadamigos responde eso en segundos.

Cargá tu dirección y la de tus amigos, elegí cómo viaja cada uno (auto, transporte público, a pie o bici) y qué buscan: café, bar, restaurante, parque o shopping. La app calcula los tiempos de viaje reales de todos y te sugiere los 3 lugares donde el grupo, en total, viaja menos.

Es justo de verdad:
• No busca solo en el punto medio del mapa: considera rutas y tiempos reales.
• Prioriza a quien viaja sin auto, para que no siempre le toque el viaje más largo.
• Podés poner un tope de viaje por persona para que nadie quede lejísimos.

Y después:
• Ves a todos y a los lugares sugeridos en el mapa, con el tiempo de cada uno.
• Abrís el lugar en Google Maps con un toque.
• Lo compartís por WhatsApp con el grupo.

Sin cuentas, sin anuncios y sin guardar tus datos: las direcciones solo se usan para calcular el encuentro.
```

- **Ícono**: PNG 512×512, 32 bits, sin transparencia en las esquinas recortadas por Play. Hoy la app tiene un ícono vectorial adaptativo; exportalo o hacé uno definitivo.
- **Gráfico de funciones**: PNG/JPG 1024×500.
- **Capturas de teléfono**: mínimo 2 (recomendado 4 a 8), 16:9 o 9:16, lado corto ≥ 320 px. Sugerencia: pantalla de carga de amigos, resultados con mapa, detalle de tiempos por persona, mensaje de WhatsApp.
- **Categoría**: Mapas y navegación (alternativa: Estilo de vida). **Email de contacto**: el público del paso 1.

## 7. Prueba cerrada (14 días)

1. *Probar y publicar → Pruebas → Prueba cerrada → Crear pista* (o usar "Alpha").
2. *Testers*: creá una lista de emails con los 12+ testers (sumá 2 o 3 de margen por si alguno se baja).
3. Subí el AAB del paso 3, completá las notas de versión y enviá a revisión.
4. Cuando esté aprobada, mandá a los testers el **link de inscripción** (están en la pestaña Testers). Cada uno tiene que aceptar desde ese link con la misma cuenta de Google de la lista y después instalar desde Play.
5. **En cuanto Play muestre la firma de apps, agregá ese SHA-1 a la key (paso 4.1)** y verificá que una búsqueda funcione en un celular que instaló desde Play.
6. Pediles que usen la app de verdad durante esas dos semanas y que te dejen comentarios: al pedir producción, Google pregunta cómo fue la prueba, qué cambiaste y cuánto se usó. Subir una versión corregida durante la prueba suma.
7. Al cumplirse los 14 días con 12+ inscriptos, en el *Panel* aparece **Solicitar acceso a producción**. Respondé el cuestionario con datos concretos.

## 8. Producción

Con el acceso aprobado: *Producción → Crear versión* → promové el mismo AAB (o uno nuevo con `versionCode` mayor) → lanzamiento por etapas al 20 % si querés limitar el gasto inicial de API → enviar a revisión (normalmente horas a pocos días).

## Requisitos técnicos ya cubiertos en el repo

- `targetSdk = 36`: obligatorio para apps nuevas desde el 31 de agosto de 2026.
- Firma de release desde `keystore.properties` o variables de entorno (`JUNTADAMIGOS_STORE_FILE`, `JUNTADAMIGOS_STORE_PASSWORD`, `JUNTADAMIGOS_KEY_ALIAS`, `JUNTADAMIGOS_KEY_PASSWORD`); nunca en el repo.
- `versionCode` / `versionName` por `-P` al compilar.
- R8 con `minify` y `shrinkResources`; CI compila el AAB de release en cada PR.
- La app manda `X-Android-Package` y `X-Android-Cert` con el certificado con el que está firmada en el teléfono, así que funciona igual con la firma de Play mientras su SHA-1 esté en la key.
