# Libro MX (Forge 1.20.1)

Libro que se abre con la tecla **G**, con tres secciones: **Misiones**, **Notificaciones** y **Dificultad**.
Tema mexicano, texto de colores, emojis pixelados y vista previa 3D de cualquier mob (vanilla o de mods) en las misiones de "Matar entidad".

## Cómo compilarlo con GitHub Actions
1. Crea un repositorio nuevo en GitHub y sube **todo el contenido de este zip** (la carpeta `.github` debe quedar en la raíz del repositorio).
2. Ve a la pestaña **Actions**. El workflow "Compilar mod" corre solo con cada subida (o dale "Run workflow").
3. Cuando termine, abre la ejecución y descarga el artefacto **libromx-jar**. Dentro está `libromx-1.0.0.jar`.
4. Pon el `.jar` en la carpeta `mods` (en servidor y en cliente, el mod es necesario en ambos lados).

Para publicar un release con el jar: crea un tag que empiece con `v`, por ejemplo `v1.0.0`.

## Cómo compilarlo en tu computadora
Necesitas Java 17 y Gradle 8.1.1 (o genera el wrapper con `gradle wrapper --gradle-version 8.1.1`).
Luego: `gradle build`. El jar queda en `build/libs/`.

## Uso en el juego
- **G** abre y cierra el libro. Clic en una entrada para ver su detalle, rueda del mouse para desplazar la lista.
- Los **operadores** (nivel 2 o más) ven los botones **Nueva**, **Quitar** y, en Dificultad, **Aplicar** (cambia la dificultad real del servidor). Los jugadores normales solo leen.
- En **Nueva** hay un panel con título, descripción, 16 colores, botones N (negrita), K (cursiva), S (subrayado) y R (limpiar), y chips de emojis. Para misiones eliges el tipo (matar, obtener o craftear) y escribes el objetivo, por ejemplo `minecraft:creeper` o el id de un mob de un mod.
- Si el tipo es **Matar entidad**, el libro dibuja el modelo del mob. Para mobs de mods usa su id completo (por ejemplo `tumod:mi_mob`).
- Una **notificación** nueva también se manda al chat de todos.

## Texto con colores y emojis
- Colores: `&0` a `&f`, y hex con `&#FF8800`.
- Formato: `&l` negrita, `&o` cursiva, `&n` subrayado, `&m` tachado, `&r` limpiar.
- Emojis: `:corazon:` `:estrella:` `:calavera:` `:diamante:` `:chile:` `:cempasuchil:`

Los emojis son imágenes pixeladas propias (Minecraft no dibuja los emojis normales). Para agregar más:
1. Dibuja el icono de 8x8 y añádelo a `src/main/resources/assets/libromx/textures/font/emoji.png` (ancho = 8 x cantidad de emojis).
2. Agrega su carácter al final de la cadena `chars` en `assets/libromx/font/emoji.json` (`\uE006`, `\uE007`...).
3. Agrégalo en el mapa `EMOJI` de `util/TextFmt.java`.

## Comandos (solo operadores)
```
/libro mision agregar <kill|collect|craft> <objetivo> "<titulo>" <descripcion>
/libro mision quitar <numero>
/libro notificacion enviar "<titulo>" <texto>
/libro notificacion quitar <numero>
/libro dificultad agregar <peaceful|easy|normal|hard> "<titulo>" <descripcion>
/libro dificultad establecer <numero>
/libro dificultad quitar <numero>
/libro lista <mision|notificacion|dificultad>
```
Ejemplo: `/libro mision agregar kill minecraft:zombie "&a&lCaza de zombis" Elimina 10 zombis :calavera:`

## Dónde se guarda
Todo se guarda en `libromx.json` dentro de la carpeta del mundo, y se puede editar a mano con el servidor apagado.

## Qué NO incluye (por ahora)
- Seguimiento de progreso de las misiones ni entrega de recompensas: el libro muestra las misiones, pero no cuenta kills.
- La ruleta ni los otros libros de DeathCore.
