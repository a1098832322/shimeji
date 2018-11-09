package com.joconner.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author joconner
 */
public class PackageableResourceControl extends ResourceBundle.Control {

    boolean isPackageBased;

    public PackageableResourceControl() {
        this(true);
    }

    public PackageableResourceControl(boolean isPackageBased) {
        this.isPackageBased = isPackageBased;
    }

    /**
     * If this is a package-based control, converts a baseName resource file of
     * the form package-name.ResourceName to
     * package-name.locale-string.ResourceName. This controller separates
     * localized resources into their own subpackage and does not extend the
     * ResourceName.
     * <p>
     * If this is not a package-based control, converts a baseName resource
     * file to standard bundle names as provided by the default Java
     * ResourceBundle.Control class.
     */
    @Override
    public String toBundleName(String baseName, Locale locale) {
        String bundleName = null;
        if (isPackageBased) {
            int nBasePackage = baseName.lastIndexOf(".");
            String basePackageName = nBasePackage > 0 ? baseName.substring(0, nBasePackage) : "";
            String resName = nBasePackage > 0 ? baseName.substring(nBasePackage + 1) : baseName;
            String langSubPackage = locale.equals(Locale.ROOT) ? "" : locale.toLanguageTag().toLowerCase();
            StringBuilder strBuilder = new StringBuilder();
            if (nBasePackage > 0) {
                strBuilder.append(basePackageName).append(".");
            }
            if (langSubPackage.length() > 0) {
                strBuilder.append(langSubPackage).append(".");
            }
            strBuilder.append(resName);
            bundleName = strBuilder.toString();
        } else {
            bundleName = super.toBundleName(baseName, locale);
        }
        return bundleName;
    }

}
