package de.komoot.photon;

import com.google.common.base.Joiner;
import de.komoot.photon.elasticsearch.Searcher;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * date: 31.10.14
 *
 * @author christoph
 */
public class RequestHandler extends Route {
	private final Searcher searcher;
	private final Set<String> supportedLanguages;

	protected RequestHandler(String path, Searcher searcher, String languages) {
		super(path);
		this.searcher = searcher;
		this.supportedLanguages = new HashSet<String>(Arrays.asList(languages.split(",")));
	}

	@Override
	public String handle(Request request, Response response) {
		// parse query term
		String query = request.queryParams("q");
		if(query == null) {
			halt(400, "missing search term 'q': /?q=berlin");
		}

		// parse preferred language
		String languageOnly = request.queryParams("locale");
                if(languageOnly == null || languageOnly.isEmpty()) {
                    languageOnly = "en";
                } else {
                    languageOnly = languageOnly.replace("-", "_");
                    if (languageOnly.contains("_"))
                        languageOnly = languageOnly.substring(0, 2);

                    if(!supportedLanguages.contains(languageOnly)) 
                        languageOnly = "en";
                }

		// parse location bias
		Double lon = null, lat = null;
		try {
			lon = Double.valueOf(request.queryParams("lon"));
			lat = Double.valueOf(request.queryParams("lat"));
		} catch(Exception nfe) {
		}
                
                String point = request.queryParams("point");
                if(point != null && (lat == null || lon == null)) {
                    String[] fromStrs = point.split(",");
                    if (fromStrs.length == 2)
                    {
                        try
                        {
                            lon = Double.parseDouble(fromStrs[1]);
                            lat = Double.parseDouble(fromStrs[0]);
                        } catch (Exception ex)
                        {
                        }
                    }
                }

		// parse limit for search results
		int limit;
		try {
			limit = Math.min(50, Integer.parseInt(request.queryParams("limit")));
		} catch(Exception e) {
			limit = 15;
		}

        String osmKey = request.queryParams("osm_key");
        String osmValue = request.queryParams("osm_value");

        long start = System.currentTimeMillis();
        List<JSONObject> results = searcher.search(query, languageOnly, lon, lat, osmKey,osmValue,limit, true);
		if(results.isEmpty()) {
			// try again, but less restrictive
			results = searcher.search(query, languageOnly, lon, lat, osmKey,osmValue,limit, false);
		}
                long end = System.currentTimeMillis();

		// build geojson
		final JSONObject collection = new JSONObject();
                collection.put("took", end - start);
                collection.put("locale", languageOnly);
		collection.put("hits", results);

		response.type("application/json; charset=utf-8");
		response.header("Access-Control-Allow-Origin", "*");

		if(request.queryParams("debug") != null)
			return collection.toString(4);

		return collection.toString();
	}
}
