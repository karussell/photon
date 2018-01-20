package de.komoot.photon.query;

import com.vividsolutions.jts.geom.Point;
import spark.Request;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author svantulden
 */
public class ReverseRequestFactory {
    private final Set<String> supportedLanguages;
    // private final LanguageChecker languageChecker;

    public ReverseRequestFactory(Set<String> supportedLanguages) {
        // this.languageChecker = new LanguageChecker(supportedLanguages);
        this.supportedLanguages = supportedLanguages;
    }

    public <R extends ReverseRequest> R create(Request webRequest) throws BadRequestException {
        // GH change, parse preferred language
        String language = PhotonRequestFactory.getLanguage(webRequest, supportedLanguages);
        Point location = PhotonRequestFactory.getPoint(webRequest);
        if (location == null) {
            throw new BadRequestException(400, "missing search term 'lat' and/or 'lon': /?lat=51.5&lon=8.0");
        }

        Double radius = 1d;
        String radiusParam = webRequest.queryParams("radius");
        if (radiusParam != null) {
            try {
                radius = Double.valueOf(radiusParam);
            } catch (Exception nfe) {
                throw new BadRequestException(400, "invalid search term 'radius', expected a number.");
            }
            if (radius <= 0) {
                throw new BadRequestException(400, "invalid search term 'radius', expected a strictly positive number.");
            } else {
                // limit search radius to 5000km
                radius = Math.min(radius, 5000d);
            }
        }

        String queryStringFilter = webRequest.queryParams("query_string_filter");
        Integer limit = 1;
        String limitParam = webRequest.queryParams("limit");
        if (limitParam != null) {
            try {
                limit = Integer.valueOf(limitParam);
            } catch (Exception nfe) {
                throw new BadRequestException(400, "invalid search term 'limit', expected an integer.");
            }
            if (limit <= 0) {
                throw new BadRequestException(400, "invalid search term 'limit', expected a strictly positive integer.");
            } else {
                // limit number of results to 50
                limit = Math.min(limit, 50);
            }
        }

        ReverseRequest reverseRequest = new ReverseRequest(location, language, radius, queryStringFilter, limit);
        return (R) reverseRequest;
    }
}
