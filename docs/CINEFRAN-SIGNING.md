# CINEFRAN 0.3.6: firma y migración

La APK definitiva usa com.cinefran.tv (sin sufijo debug). La APK 0.3.5 instalada usa com.cinefran.tv.debug; puede permanecer instalada junto a la definitiva. No borres la anterior hasta comprobar la migración.

1. En Windows instala JDK 17 y GitHub CLI, ejecuta gh auth login con la cuenta propietaria del repositorio.
2. Guarda y ejecuta scripts/Initialize-CinefranSigning.ps1 desde PowerShell. Genera el almacén PKCS12 local, establece secretos de Actions y una variable de huella pública. No publiques el almacén ni la contraseña. Conserva copias seguras offline.
3. Ejecuta gh workflow run build.yml --repo FPerezGH/bambi --ref cinefran-stremio -f publish=true. El trabajo publish solo publica tras pruebas, lint, verificación del paquete y del certificado. Nunca uses el workflow heredado Publish release de Bambi.
4. Descarga CINEFRAN.apk de Releases, no el artefacto debug de Actions. Instálala sin desinstalar 0.3.5.
5. Para migrar el historial local desde 0.3.5 utiliza scripts/migrate_legacy.py con adb autorizado y Python 3 + cryptography. El archivo es cifrado; no exporta los tokens del almacén Android Keystore. Puedes introducir las claves API manualmente en el script o en CINEFRAN. Transfiere el .cfb a almacenamiento accesible desde Android TV; selecciona Restaurar copia en la pantalla inicial.
6. Reinicia y verifica todos los perfiles, listas e historial. Vuelve a vincular Trakt. Solo entonces elimina la anterior si quieres.
7. En versiones futuras, conserva el mismo applicationId y certificado, incrementa versionCode y versionName, y publica con el mismo workflow. El actualizador solo acepta CINEFRAN.apk de este repositorio, firma coincidente y versionCode superior.

La copia portátil cifra preferencias, perfiles, listas, historial, series seguidas y claves API mediante AES-256-GCM y PBKDF2-HMAC-SHA256. No copia vídeos descargados ni sesiones OAuth de Trakt. La restauración sustituye datos, no los combina. Guarda una copia anterior y recuerda la contraseña. Funciona a partir de Android 8 con un selector de archivos compatible.

Las compilaciones DEBUG son exclusivamente de validación: cada runner puede generar un certificado distinto. No instales sus artefactos como actualizaciones. El workflow de publicación obtiene versionCode y versionName del Gradle automáticamente y verifica la huella SHA-256 antes de publicar. El nombre de descarga permanente es CINEFRAN.apk.

## Alternativa sin instalar herramientas en Windows

Si no existe una firma de producción previa, se admite un único secreto de repositorio `CINEFRAN_SIGNING` con un objeto JSON que contiene `keystore_base64`, `password` y `alias`. La huella de este certificado está fijada en el workflow. La clave privada y su contraseña nunca deben incorporarse a Git ni a un issue. Conserva una copia segura fuera del repositorio. Tras crear el secreto desde Settings > Secrets and variables > Actions, vuelve a ejecutar todos los trabajos de la última ejecución Build de cinefran-stremio. El workflow publica automáticamente si todas las pruebas pasan y no existe ya esa versión. No sustituyas una firma ya utilizada en producción.
