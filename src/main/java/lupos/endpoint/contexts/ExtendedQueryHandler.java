package lupos.endpoint.contexts;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import lupos.datastructures.queryresult.QueryResult;
import lupos.endpoint.EvaluationHelper;
import lupos.endpoint.EvaluationHelper.GENERATION;
import lupos.endpoint.EvaluationHelper.SPARQLINFERENCE;
import lupos.endpoint.EvaluationHelper.SPARQLINFERENCEMATERIALIZATION;
import lupos.endpoint.server.Endpoint;
import lupos.endpoint.server.format.Formatter;
import lupos.endpoint.server.format.JSONFormatter;
import lupos.misc.Triple;
import lupos.misc.Tuple;
import lupos.sparql1_1.ParseException;
import lupos.sparql1_1.TokenMgrError;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.rio.RDFParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.n3.turtle.TurtleParseException;
import com.hp.hpl.jena.query.QueryParseException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

@SuppressWarnings("restriction")
public class ExtendedQueryHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(ExtendedQueryHandler.class.getName());

	private final int HTTP_OK = 200;
	private final int HTTP_BAD_REQUEST = 400;
	private final int HTTP_METHOD_NOT_ALLOWED = 405;
	private final int HTTP_INTERNAL_SERVER_ERROR = 500;

	private final boolean RIF_EVALUATION;

	public ExtendedQueryHandler(boolean rifEvaluation) {
		super();
		this.RIF_EVALUATION = rifEvaluation;
	}

	@Override
	public void handle(HttpExchange t) throws IOException {
		LOGGER.info("Handling {} from {}", t.getRequestMethod(), t
				.getRequestHeaders().getFirst("Host"));

		JSONObject response = new JSONObject();
		// We default to error status in order to guarantee that HTTP_OK is only
		// responded if everything went well
		int responseStatus = HTTP_INTERNAL_SERVER_ERROR;

		try {
			// Only process POST requests
			String requestMethod = t.getRequestMethod();
			if (!requestMethod.equalsIgnoreCase("POST")) {
				LOGGER.info("Received {}, but only POST is allowed",
						requestMethod);
				t.getResponseHeaders().add("Allow", "POST");
				response.put("error", "Only POSTs will be processed.");
				responseStatus = HTTP_METHOD_NOT_ALLOWED;
				return;
			}

			// Parse the request
			ExecutionParameters parameters = null;
			try {
				String requestBody = IOUtils.toString(t.getRequestBody());
				parameters = ExecutionParameters
						.getParametersFromJson(requestBody);
			} catch (RuntimeException e) {
				LOGGER.info("Bad request: {}", e.getMessage());
				if (e.getCause() != null) {
					LOGGER.info("Cause: {}", e.getCause().toString());
				}
				response.put("error", e.getMessage());
				responseStatus = HTTP_BAD_REQUEST;
				return;
			}

			LOGGER.info("Received query: {}", parameters.QUERY);
			LOGGER.info("Received rdf: {}", parameters.RDF);
			LOGGER.info("Using evaluator with index: {}",
					parameters.EVALUATOR_INDEX);
			LOGGER.info("Using inference mode: {}", parameters.INFERENCE);
			if (!parameters.RIF.equals("")) {
				LOGGER.info("Received rif rules: {}", parameters.RIF);
			}

			if (this.RIF_EVALUATION) {
				LOGGER.info("Starting processing RIF");
			} else {
				LOGGER.info("Starting processing SPARQL");
			}
			Tuple<String, QueryResult[]> result = null;
			try {
				// Use the magic getQueryResult method
				// We get all parameters from the request (or the default
				// values) except for two.
				// Second parameter indicates if we are processing RIF.
				// Fifth parameters indicates that we don't want to store
				// inferred triples in the DB (We process everything in memory).
				result = EvaluationHelper
						.getQueryResult(
								parameters.EVALUATOR_INDEX,
								this.RIF_EVALUATION,
								parameters.INFERENCE,
								parameters.INFERENCE_GENERATION,
								SPARQLINFERENCEMATERIALIZATION.COMBINEDQUERYOPTIMIZATION,
								parameters.OWL2RL_INCONSISTENCY_CHECK,
								parameters.RDF, parameters.RIF,
								parameters.QUERY);
			} catch (TokenMgrError | ParseException | QueryParseException
					| MalformedQueryException
					| lupos.rif.generated.parser.ParseException
					| lupos.rif.generated.parser.TokenMgrError e) {
				LOGGER.info("Malformed query: {}", e.getMessage());
				Triple<Integer, Integer, String> detailedError = EvaluationHelper
						.dealWithThrowableFromQueryParser(e);
				Integer line = detailedError.getFirst();
				Integer column = detailedError.getSecond();
				String error = detailedError.getThird();

				JSONObject errorJson = new JSONObject();
				if (line != -1) {
					errorJson.put("line", line);
				}
				if (column != -1) {
					errorJson.put("column", column);
				}
				errorJson.put("errorMessage", error);
				response.put("queryError", errorJson);
				// We send HTTP_OK, because the actual HTTP request was correct
				responseStatus = HTTP_OK;
				return;
			} catch (TurtleParseException | RDFParseException e) {
				LOGGER.info("Malformed rdf: {}", e.getMessage());
				Triple<Integer, Integer, String> detailedError = EvaluationHelper
						.dealWithThrowableFromRDFParser(e);
				Integer line = detailedError.getFirst();
				Integer column = detailedError.getSecond();
				String error = detailedError.getThird();

				JSONObject errorJson = new JSONObject();
				if (line != -1) {
					errorJson.put("line", line);
				}
				if (column != -1) {
					errorJson.put("column", column);
				}
				errorJson.put("errorMessage", error);
				response.put("rdfError", errorJson);
				responseStatus = HTTP_OK;
				return;
			}
			LOGGER.info("Finished processing");
			boolean emptyResult = result.getSecond().length == 0;

			// Format the output
			if (emptyResult) {
				for (Formatter formatter : parameters.FORMATTERS) {
					if (formatter instanceof JSONFormatter) {
						response.append(formatter.getName(), new JSONObject());
					} else {
						response.append(formatter.getName(), "");
					}
				}
			}

			for (QueryResult queryResult : result.getSecond()) {
				for (Formatter formatter : parameters.FORMATTERS) {
					OutputStream os = new ByteArrayOutputStream();

					formatter.writeResult(os, queryResult.getVariableSet(),
							queryResult);
					// In case of JSON formatting, we'll insert it non-escaped
					// as a
					// nested JSON object
					if (formatter instanceof JSONFormatter) {
						response.append(formatter.getName(),
								new JSONObject(os.toString()));
					} else {
						response.append(formatter.getName(), os.toString());
					}
				}
			}
			responseStatus = HTTP_OK;
		} catch (Exception e) {
			LOGGER.error("Encountered exception {}", e.toString(), e);
			response = new JSONObject();
			response.put("error", e);
			responseStatus = HTTP_INTERNAL_SERVER_ERROR;
		} finally {
			setDefaultHeaders(t);
			t.sendResponseHeaders(responseStatus, response.toString().length());
			t.getResponseBody().write(response.toString().getBytes());
			t.getResponseBody().close();
			LOGGER.info("Responded {} with {}", responseStatus,
					response.toString());
		}
	}

	private void setDefaultHeaders(HttpExchange t) {
		t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
		t.getResponseHeaders().add("Content-Type", "application/json");
	}
}

class ExecutionParameters {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(ExecutionParameters.class.getName());

	public final String QUERY;
	public final String RDF;
	public final int EVALUATOR_INDEX;
	public final SPARQLINFERENCE INFERENCE;
	public final GENERATION INFERENCE_GENERATION;
	public final boolean OWL2RL_INCONSISTENCY_CHECK;
	public final String RIF;
	public final Set<Formatter> FORMATTERS;

	private ExecutionParameters(String query, String rdf, int evaluatorIndex,
			SPARQLINFERENCE inference, GENERATION inferenceGeneration,
			boolean owl2rlInconsistencyCheck, String rif,
			Set<Formatter> formatters) {
		this.QUERY = query;
		this.RDF = rdf;
		this.EVALUATOR_INDEX = evaluatorIndex;
		this.INFERENCE = inference;
		this.INFERENCE_GENERATION = inferenceGeneration;
		this.OWL2RL_INCONSISTENCY_CHECK = owl2rlInconsistencyCheck;
		this.RIF = rif;
		this.FORMATTERS = formatters;
	}

	/**
	 * Factory method for ExecutionParameters. It parses a JSON request and
	 * returns all parameters needed for execution.
	 * 
	 * @param json
	 *            the JSON object
	 * @return all parameters needed for execution
	 * @throws JSONException
	 *             if JSON was malformed
	 * @throws RuntimeException
	 *             if something else went wrong
	 */
	public static ExecutionParameters getParametersFromJson(String json) {
		// Parse the JSON body and retrieve request keys
		// At least "query" and "rdf" must be there
		String query = null;
		String rdf = null;
		int evaluatorIndex = EvaluationHelper
				.getEvaluatorIndexByName("MemoryIndex");
		SPARQLINFERENCE inference = SPARQLINFERENCE.NONE;
		GENERATION inferenceGeneration = GENERATION.FIXED;
		boolean owl2rlInconsistencyCheck = false;
		String rif = "";
		Set<Formatter> formatters = new HashSet<>();
		formatters.add(Endpoint.getRegisteredFormatters().get("json"));

		try {
			JSONObject request = new JSONObject(json);

			if (!request.has("query")) {
				LOGGER.info("Missing key \"query\" in request body");
				throw new RuntimeException(
						"Key \"query\" must be present in body.");
			}
			query = request.getString("query");

			if (!request.has("rdf")) {
				LOGGER.info("Missing key \"rdf\" in request body");
				throw new RuntimeException(
						"Key \"rdf\" must be present in body.");
			}

			rdf = request.getString("rdf");

			if (request.has("evaluator")) {
				String evaluator = request.getString("evaluator");
				try {
					evaluatorIndex = EvaluationHelper
							.getEvaluatorIndexByName(evaluator);
				} catch (RuntimeException e) {
					LOGGER.info("Requested Evaluator {} not registered",
							evaluator);
					throw new RuntimeException(String.format(
							"Evaluator %s not registered.", evaluator));
				}
			}

			if (request.has("inference")) {
				String requestedInferenceMode = request.getString("inference");
				try {
					inference = SPARQLINFERENCE.valueOf(requestedInferenceMode);
				} catch (IllegalArgumentException e) {
					LOGGER.info("Requested inference mode {} not known",
							requestedInferenceMode);
					throw new RuntimeException(String.format(
							"Inference mode %s not known.",
							requestedInferenceMode), e);
				}
			}

			if (request.has("inferenceGeneration")) {
				String requestedInferenceGeneration = request
						.getString("inferenceGeneration");
				try {
					inferenceGeneration = GENERATION
							.valueOf(requestedInferenceGeneration);
				} catch (IllegalArgumentException e) {
					LOGGER.info(
							"Requested inference generation mode {} not known",
							requestedInferenceGeneration);
					throw new RuntimeException(String.format(
							"Inference generation mode %s not known.",
							requestedInferenceGeneration), e);
				}
			}

			if (request.has("owl2rlInconsistencyCheck")) {
				owl2rlInconsistencyCheck = request
						.getBoolean("owl2rlInconsistencyCheck");
			}

			if (request.has("rif")) {
				rif = request.getString("rif");
			}

			if (request.has("formats")) {
				formatters.clear();
				JSONArray formats = request.getJSONArray("formats");
				for (int i = 0; i < formats.length(); i++) {
					String formatName = formats.getString(i).toLowerCase();
					Formatter formatter = Endpoint.getRegisteredFormatters()
							.get(formatName);
					if (formatter != null) {
						formatters.add(formatter);
					} else {
						LOGGER.info(
								"There is no formatter registered for request output format {}",
								formatName);
						throw new RuntimeException(
								String.format(
										"There is no formatter for request ouput format %s.",
										formatName));
					}
				}
			}
		} catch (JSONException e) {
			LOGGER.info("Received malformed JSON");
			throw e;
		}

		return new ExecutionParameters(query, rdf, evaluatorIndex, inference,
				inferenceGeneration, owl2rlInconsistencyCheck, rif, formatters);
	}
}
