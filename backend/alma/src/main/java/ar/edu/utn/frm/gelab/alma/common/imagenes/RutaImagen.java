package ar.edu.utn.frm.gelab.alma.common.imagenes;

import java.util.regex.Pattern;

/**
 * Rutas de imagen que acepta la API: las que devuelve la subida de imágenes ("/imagenes/<nombre>").
 * Un solo nivel, sin "..", sin barras extra.
 */
public final class RutaImagen {

    public static final String PREFIJO = "/imagenes/";

    private static final Pattern NOMBRE = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,200}");

    private RutaImagen() {
    }

    public static boolean esValida(String ruta) {
        if (ruta == null || !ruta.startsWith(PREFIJO)) {
            return false;
        }
        String nombre = ruta.substring(PREFIJO.length());
        return !nombre.contains("..") && NOMBRE.matcher(nombre).matches();
    }
}
