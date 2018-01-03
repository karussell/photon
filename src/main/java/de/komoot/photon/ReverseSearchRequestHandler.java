package de.komoot.photon;

import de.komoot.photon.query.BadRequestException;
import de.komoot.photon.query.ReverseRequest;
import de.komoot.photon.query.ReverseRequestFactory;
import de.komoot.photon.searcher.ReverseElasticsearchSearcher;
import de.komoot.photon.searcher.ReverseRequestHandler;
import de.komoot.photon.searcher.ReverseRequestHandlerFactory;
import de.komoot.photon.utils.ConvertToGeoJson;
import org.elasticsearch.client.Client;
import org.json.JSONObject;
import spark.Request;
import spark.Response;
import spark.RouteImpl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static spark.Spark.halt;

/**
 * @author svantulden
 */
public class ReverseSearchRequestHandler<R extends ReverseRequest> extends RouteImpl {
    private final ReverseRequestFactory reverseRequestFactory;
    private final ReverseRequestHandlerFactory requestHandlerFactory;
    // private final ConvertToGeoJson geoJsonConverter;

    ReverseSearchRequestHandler(String path, Client esNodeClient, String languages) {
        super(path);
        Set<String> supportedLanguages = new HashSet<String>(Arrays.asList(languages.split(",")));
        this.reverseRequestFactory = new ReverseRequestFactory(supportedLanguages);
        // this.geoJsonConverter = new ConvertToGeoJson();
        this.requestHandlerFactory = new ReverseRequestHandlerFactory(new ReverseElasticsearchSearcher(esNodeClient));
    }

    @Override
    public String handle(Request request, Response response) {
        long start = System.currentTimeMillis();
        R photonRequest = null;
        try {
            photonRequest = reverseRequestFactory.create(request);
        } catch (BadRequestException e) {
            JSONObject json = new JSONObject();
            json.put("message", e.getMessage());
            halt(e.getHttpStatus(), json.toString());
        }
        ReverseRequestHandler<R> handler = requestHandlerFactory.createHandler(photonRequest);
        List<JSONObject> results = handler.handle(photonRequest);
        long end = System.currentTimeMillis();
        // GH change
        final JSONObject collection = new JSONObject();
        collection.put("took", end - start);
        collection.put("hits", results);
        response.type("application/json; charset=utf-8");
        response.header("Access-Control-Allow-Origin", "*");
        if (request.queryParams("debug") != null)
            return collection.toString(4);

        return collection.toString();
    }
}
