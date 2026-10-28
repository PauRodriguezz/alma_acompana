package ar.edu.utn.frm.gelab.alma.config;

import java.time.Duration;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import ar.edu.utn.frm.gelab.alma.common.imagenes.AlmacenImagenes;

/**
 * Sirve GET /imagenes/** desde la carpeta de subidas. Es público porque no empieza con /api
 * (ver SecurityConfig). El resolver de recursos de Spring rechaza rutas con "..".
 */
@Configuration
public class ImagenesWebConfig implements WebMvcConfigurer {

    private final AlmacenImagenes almacenImagenes;

    public ImagenesWebConfig(AlmacenImagenes almacenImagenes) {
        this.almacenImagenes = almacenImagenes;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Los nombres son UUID y no se reescriben: se puede cachear varios días.
        registry.addResourceHandler("/imagenes/**")
                .addResourceLocations(almacenImagenes.getCarpeta().toUri().toString())
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());
    }
}
