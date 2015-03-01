package lupos.endpoint.contexts;

import java.io.IOException;

import lupos.endpoint.EvaluationHelper;
import lupos.endpoint.GraphSerialization;
import lupos.endpoint.GraphSerialization.AstFormat;
import lupos.gui.operatorgraph.graphwrapper.GraphWrapper;
import lupos.gui.operatorgraph.graphwrapper.GraphWrapperAST;
import lupos.gui.operatorgraph.graphwrapper.GraphWrapperASTRIF;
import lupos.gui.operatorgraph.graphwrapper.GraphWrapperRules;
import lupos.misc.Triple;
import lupos.sparql1_1.ParseException;
import lupos.sparql1_1.TokenMgrError;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

@SuppressWarnings("restriction")
public class InfoQueryHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(InfoQueryHandler.class.getName());

	private final int HTTP_OK = 200;
	private final int HTTP_BAD_REQUEST = 400;
	private final int HTTP_METHOD_NOT_ALLOWED = 405;
	private final int HTTP_INTERNAL_SERVER_ERROR = 500;

	public final boolean RIF_EVALUATION;

	public InfoQueryHandler(boolean rifEvaluation) {
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
			InfoQueryParameters parameters = null;
			try {
				String requestBody = IOUtils.toString(t.getRequestBody());
				parameters = InfoQueryParameters
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
			LOGGER.info("Using evaluator with index: {}",
					parameters.EVALUATOR_INDEX);
			LOGGER.info("Requested AST format is: {}", parameters.AST_FORMAT);

			if (this.RIF_EVALUATION) {
				LOGGER.info("Starting processing RIF");
			} else {
				LOGGER.info("Starting processing SPARQL");
			}
			Triple<GraphWrapper, String, GraphWrapper> result = null;
			try {
				// Use the magic getCompileInfo method
				result = EvaluationHelper.getCompileInfo(
						parameters.EVALUATOR_INDEX, this.RIF_EVALUATION,
						parameters.QUERY);
			} catch (TokenMgrError | ParseException
					| lupos.rif.generated.parser.ParseException
					| lupos.rif.generated.parser.TokenMgrError e) {
				LOGGER.info("Malformed query: {}", e.getMessage());
				response.put("queryError", e.getMessage());
				// We send HTTP_OK, because the actual HTTP request was correct
				responseStatus = HTTP_OK;
				return;
			}
			LOGGER.info("Finished processing");

			if (result == null) {
				response.put("info",
						"Compiler does not provide additional information.");
			} else {
				if (this.RIF_EVALUATION) {
					GraphWrapperASTRIF ast = (GraphWrapperASTRIF) result
							.getFirst();
					GraphWrapperRules astRules = (GraphWrapperRules) result
							.getThird();
					if (ast != null) {
						response.put("AST", GraphSerialization.rifAstToJson(
								ast, parameters.AST_FORMAT));
					}
					if (astRules != null) {
						response.put("rulesAST",
								GraphSerialization.rulesAstToJson(astRules,
										parameters.AST_FORMAT));
					}
				} else {
					GraphWrapperAST ast = (GraphWrapperAST) result.getFirst();
					String coreQuery = result.getSecond();
					GraphWrapperAST coreAst = (GraphWrapperAST) result
							.getThird();
					if (ast != null) {
						response.put("AST", GraphSerialization.astToJson(ast,
								parameters.AST_FORMAT));
					}
					if (coreQuery != null) {
						response.put("coreSPARQL", result.getSecond());
					}
					if (coreAst != null) {
						response.put("coreAST", GraphSerialization.astToJson(
								coreAst, parameters.AST_FORMAT));
					}
				}
			}

			responseStatus = HTTP_OK;
		} catch (Exception e) {
			LOGGER.error("Encountered exception {}", e.toString(), e);
			response = new JSONObject();
			response.put("error", e.toString());
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

class InfoQueryParameters {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(InfoQueryParameters.class.getName());

	public final String QUERY;
	public final int EVALUATOR_INDEX;
	public final AstFormat AST_FORMAT;

	private InfoQueryParameters(String query, int evaluatorIndex,
			AstFormat astFormat) {
		this.QUERY = query;
		this.EVALUATOR_INDEX = evaluatorIndex;
		this.AST_FORMAT = astFormat;
	}

	/**
	 * Factory method for AstQueryParameters. It parses a JSON request and
	 * returns all parameters needed for retrieving a corresponding AST.
	 * 
	 * @param json
	 *            the JSON object
	 * @return all parameters needed for retrieving AST
	 * @throws JSONException
	 *             if JSON was malformed
	 * @throws RuntimeException
	 *             if something else went wrong
	 */
	public static InfoQueryParameters getParametersFromJson(String json) {
		// Parse the JSON and retrieve request keys
		// Key "query" is mandatory
		String query = null;
		int evaluatorIndex = EvaluationHelper
				.getEvaluatorIndexByName("MemoryIndex");
		AstFormat astFormat = AstFormat.NESTED;

		try {
			JSONObject request = new JSONObject(json);

			if (!request.has("query")) {
				LOGGER.info("Missing key \"query\" in request body");
				throw new RuntimeException(
						"Key \"query\" must be present in body.");
			}
			query = request.getString("query");

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

			if (request.has("astFormat")) {
				String format = request.getString("astFormat");
				try {
					astFormat = AstFormat.valueOf(format.toUpperCase());
				} catch (IllegalArgumentException e) {
					LOGGER.info("Requested AST format {} is unknown.", format);
					throw new RuntimeException(String.format(
							"AST format %s is unknown.", format));
				}
			}
		} catch (JSONException e) {
			LOGGER.info("Received malformed JSON");
			throw e;
		}

		return new InfoQueryParameters(query, evaluatorIndex, astFormat);
	}
}
