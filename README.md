# Mis finanzas

app android nativa para llevar el control de tus finanzas personales, hecha pensada para guatemala
(quetzales, formato es-gt). la idea es simple: manejar tu plata desde el telefono sin depender de nadie.
todo vive dentro del dispositivo, nada se sube a internet y la base de datos va cifrada.

el nombre de paquete es `com.kratt.finanzas` y la app se llama "mis finanzas".

## De que va la app

es una app para anotar y entender en que se te va el dinero. con ella podes:

- crear cuentas (efectivo, banco, tarjeta de credito, ahorro, hogar, billetera digital, etc.)
- registrar movimientos: gastos, ingresos y transferencias entre cuentas
- organizar todo con categorias
- llevar compras en cuotas y gastos recurrentes (internet, salario, etc.)
- armar presupuestos por mes y por categoria
- ver reportes y graficas de como andas
- exportar e importar movimientos en csv
- guardar plantillas de movimientos para registrarlos mas rapido
- poner widgets y accesos directos en la pantalla de inicio
- crear metas de ahorro, aportes y compras planificadas
- adjuntar comprobantes (fotos y pdf) a cada movimiento, cifrados
- leer comprobantes con ocr local (sin nube) solo como ayuda
- sacar respaldos cifrados y restaurarlos, incluso en otro telefono
- preguntarle cosas a un asistente local que responde solo con tus propios datos

y todo esto detras de un bloqueo con huella o codigo del telefono.

## Con que esta hecho

- lenguaje: kotlin
- interfaz: jetpack compose con material 3
- scripts de build: gradle con kotlin dsl (gradle 8.13, agp 8.9.1, kotlin 2.1.20, ksp)
- sdk: compila y apunta a android 36, corre desde android 26 (minsdk 26), java/kotlin 17
- base de datos: room con sqlcipher (cifrado) y esquemas exportados
- preferencias: datastore (solo cosas no sensibles)
- tareas en segundo plano: workmanager (recordatorios, sin alarmas exactas)
- seguridad: androidx biometric + android keystore
- cifrado extra: bouncycastle (argon2id para los respaldos)
- graficas: vico
- widgets: glance
- inyeccion de dependencias: manual, con un `AppContainer`, sin hilt ni koin ni dagger

las versiones exactas de cada libreria viven en `gradle/libs.versions.toml`.

## Como esta organizado

la app usa clean architecture con mvvm en un solo modulo de gradle. la separacion es por paquetes
dentro de `com.kratt.finanzas`, mas o menos asi:

- `domain/` es el corazon: los modelos, los casos de uso y las reglas de negocio puras. aca vive
  toda la logica del dinero y no depende de android. hay como 17 modelos y mas de 50 casos de uso.
- `data/` es donde se guarda y se lee todo: room, repositorios, respaldos, csv, ocr, adjuntos,
  preferencias y demas. traduce entre la base de datos y el domain.
- `presentation/` son las pantallas de compose y sus viewmodels, una carpeta por funcion
  (cuentas, movimientos, reportes, metas, asistente, ajustes, etc.).
- `navigation/` arma el navhost, las rutas y el layout que se adapta al tamano de pantalla.
- `security/` maneja el bloqueo de la app, la huella y la proteccion de pantalla.
- `common/` utilidades chiquitas y compartidas: parseo de montos, formato de moneda, fechas, mascaras.
- `di/` el armado manual de dependencias (`AppContainer`, `AppViewModelProvider`).
- `reminder/` los recordatorios con workmanager.
- `widget/` los widgets de glance para la pantalla de inicio.

la regla de oro: las pantallas nunca tocan la base de datos directo, siempre pasan por repositorios
y casos de uso. y el dinero solo se calcula en el domain, nunca desde la interfaz.

## Estructura del proyecto

```
.
├── app/                      el modulo de la app
│   ├── build.gradle.kts      config de build, versiones, buildtypes
│   ├── proguard-rules.pro    reglas de r8 para release
│   ├── schemas/              esquemas de room exportados (1.json ... 6.json)
│   └── src/
│       ├── main/             codigo y recursos de la app
│       │   ├── java/com/kratt/finanzas/
│       │   │   ├── common/       utilidades compartidas
│       │   │   ├── data/         persistencia, repos, respaldos, csv, ocr, adjuntos
│       │   │   ├── di/           dependencias manuales
│       │   │   ├── domain/       modelos, casos de uso, reglas
│       │   │   ├── navigation/   rutas y navhost
│       │   │   ├── presentation/ pantallas compose + viewmodels
│       │   │   ├── reminder/     recordatorios (workmanager)
│       │   │   ├── security/     bloqueo, huella, flag_secure
│       │   │   ├── widget/       widgets glance
│       │   │   ├── FinanzasApplication.kt
│       │   │   └── MainActivity.kt
│       │   └── res/          strings en espanol, iconos, temas
│       ├── test/             pruebas unitarias (jvm)
│       ├── androidTest/      pruebas instrumentadas (en dispositivo)
│       └── staging/          variante de validacion minificada
├── docs/                     documentacion tecnica y evidencia por fase
├── gradle/                   wrapper y catalogo de versiones (libs.versions.toml)
├── build.gradle.kts          config raiz de gradle
├── settings.gradle.kts       modulos del proyecto
├── gradle.properties         flags de gradle
├── keystore.properties.example  plantilla para la firma de release (sin secretos)
├── gradlew / gradlew.bat     wrapper de gradle
└── README.md                 este archivo
```

la carpeta `build/` se genera sola al compilar, no hay que tocarla.

## La base de datos y el cifrado

la base se llama `finanzas.db` y va cifrada de punta a punta con sqlcipher. la clave de la base
se genera aleatoria y se guarda envuelta con una llave de android keystore que no se puede exportar.
los montos se guardan como enteros (centavos), las fechas como dia epoch y las marcas de tiempo en
milisegundos.

el esquema va en la version 6. cada cambio de esquema trae su migracion no destructiva (de la 1 a
la 6, encadenadas) y su json exportado en `app/schemas/`. nunca se borra ni se recrea la base para
"arreglar" algo: si una migracion falla, se conserva lo que hay y se muestra una pantalla de
recuperacion.

## Seguridad y privacidad

la app es 100% local y eso se cuida en serio:

- no pide permiso de internet (ni ningun permiso de red), asi que la plata no puede salir del telefono
- sin firebase, sin analytics, sin telemetria, sin reportes de crashes en la nube
- `allowBackup` en falso, para que los datos no se suban a respaldos de google
- en release la ventana usa `flag_secure` (nada de capturas ni grabacion de pantalla)
- el acceso queda detras de un bloqueo con huella o el codigo del telefono
- los respaldos portables van cifrados con argon2id + aes-256-gcm y una clave que solo vos sabes
- no se registran en el log montos, saldos, descripciones, nombres de cuentas ni claves

## El asistente local

hay un asistente que responde preguntas en espanol sobre tus propias finanzas, tipo "cuanto gaste
en alimentacion este mes" o "como va mi meta de la laptop". lo importante:

- funciona todo en el dispositivo, sin nube ni modelos remotos
- es de solo lectura: nunca crea, edita ni borra nada por su cuenta
- si le pedis que registre o borre algo, en el mejor caso te arma un borrador y te abre el formulario
  normal para que vos confirmes y guardes
- usa la misma logica de calculo que los reportes, asi que no inventa numeros
- si no tiene los datos, lo dice claro en vez de inventar

## Todo lo que incorpora

la app se fue armando por fases y hoy incluye:

- cuentas, categorias, movimientos, transferencias, editar y borrar
- compras en cuotas y movimientos recurrentes con sus ocurrencias
- presupuestos por mes y por categoria con avisos
- diez reportes financieros, graficas con vico y comparaciones entre meses
- exportar e importar csv (local, con el selector de archivos de android)
- plantillas de movimientos, favoritos y flujo rapido
- widgets y accesos directos en la pantalla de inicio
- metas de ahorro, aportes y compras planificadas
- adjuntos cifrados (imagenes y pdf) con ocr local opcional
- respaldo y restauracion cifrada, y migracion a otro telefono
- bloqueo de la app, temas claro/oscuro, color dinamico y modo de privacidad de saldos
- asistente financiero local
- centro de proteccion de datos y avisos sobre respaldos

## El dinero y los datos

el dinero nunca se maneja con `float` ni `double`. siempre son enteros `long` en centavos
(por ejemplo q125.75 se guarda como 12575). se parsea con `common/AmountParser` y se muestra con
`common/CurrencyFormatter` en formato es-gt (q1,234.56). toda la aritmetica pasa por un helper de
matematica segura que rechaza montos que se salgan de rango en vez de dar vueltas raras. el modo de
privacidad tapa los saldos ("q ••••••") sin cambiar los valores reales.

## Como compilar y probar

ojo: el `java` del sistema es jdk 8 y no sirve para gradle 8.13. hay que compilar con jdk 21
apuntando `JAVA_HOME` solo para esa corrida (sin tocar variables globales). en powershell:

```powershell
$env:JAVA_HOME='H:\Android\jbr'; .\gradlew.bat assembleDebug
```

comandos utiles:

- `.\gradlew.bat test` corre las pruebas unitarias (jvm)
- `.\gradlew.bat assembleDebug` arma el apk de debug
- `.\gradlew.bat connectedDebugAndroidTest` corre las pruebas instrumentadas (necesita un dispositivo)
- `.\gradlew.bat assembleRelease` / `bundleRelease` arman el release (queda sin firmar si no hay keystore)

hay tres buildtypes: `debug` (para desarrollar), `staging` (build minificado de validacion, con
sufijo `.staging` y firmado de debug) y `release` (con r8, recursos reducidos y no depurable).

## La carpeta docs

en `docs/` esta toda la documentacion tecnica del proyecto: modelo de amenazas y politicas de
seguridad, definiciones financieras, reglas de release, contrato de continuidad de datos, reportes
y evidencia de las pruebas por fase. es buen lugar para entender el "por que" de cada decision.
