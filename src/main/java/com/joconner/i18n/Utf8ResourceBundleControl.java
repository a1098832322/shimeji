package com.joconner.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * Created by joconner on 1/11/17.
 */
public class Utf8ResourceBundleControl extends PackageableResourceControl {


    public Utf8ResourceBundleControl() {}

    public Utf8ResourceBundleControl(boolean isPackageBased) {
        super(isPackageBased);
    }

    public ResourceBundle newBundle(String baseName, Locale locale, String format,
                                    ClassLoader loader, boolean reload)
            throws IllegalAccessException, InstantiationException, IOException {
        String bundleName = toBundleName(baseName, locale);
        ResourceBundle bundle = null;
        if (format.equals("java.class")) {
            bundle = super.newBundle(baseName, locale, format, loader, reload);
        } else if (format.equals("java.properties")) {
            final String resourceName = bundleName.contains("://") ? null :
                    toResourceName(bundleName, "properties");
            if (resourceName == null) {
                return bundle;
            }
            final ClassLoader classLoader = loader;
            InputStream stream = null;
            if (reload) {
                stream = reload(resourceName, classLoader);
            } else {
                stream = classLoader.getResourceAsStream(resourceName);
            }
            if (stream != null) {
                Reader reader = new InputStreamReader(stream, "UTF-8");
                try {
                    bundle = new PropertyResourceBundle(reader);
                } finally {
                    reader.close();
                }
            }
        } else {
            throw new IllegalArgumentException("Unknown format: " + format);
        }
        return bundle;
    }

    InputStream reload(String resourceName, ClassLoader classLoader) throws IOException {
        InputStream stream = null;
        URL url = classLoader.getResource(resourceName);
        if (url != null) {
            URLConnection connection = url.openConnection();
            if (connection != null) {
                // Disable caches to get fresh data for
                // reloading.
                connection.setUseCaches(false);
                stream = connection.getInputStream();
            }
        }
        return stream;
    }


}
