/*
 * Tiny helper used by BootstrapData's startup banner.
 *
 * Wraps a URL in ESC (\u001B) control characters before it's printed to the console,
 * intended to make the Swagger UI link stand out in the startup output.
 */
package com.java.journeysspringmvc.util;

public class FormatUrl {
    public static String of(String url) {
        return "\u001B" + url + "\u001B";
    }
}
