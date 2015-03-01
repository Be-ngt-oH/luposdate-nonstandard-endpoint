luposdate_js_client
===================

This is an extension to the standard endpoint of
[LUPOSDATE](https://github.com/luposdate/luposdate). Run the java program
lupos.endpoint.ExtendedEndpoint to start the LUPOSDATE SPARQL endpoint. The standard
endpoint serves an HTML form under `http://localhost:8080` which you can use to
run a SPARQL query against the endpoint.

**It will create an index on the first run or when run with command line
  argument `--rebuild-index`.**

This extension provides a set of new endpoints, which can be accessed via
following routes: `/nonstandard/sparql`, `/nonstandard/sparql/info`,
`/nonstandard/sparql/graphs`, `/nonstandard/rif`, `/nonstandard/rif/info` and
`/nonstandard/rif/graphs`.

These can be used to perform offline queries, retrieve ASTs and operator graphs.
In general it's a good idea to build up a request for `nonstandard/sparql` or
`nonstandard/rif` and send the exact same request to `info` and `graphs` routes
to retrieve more information. Not needed parameters will be ignored. All
extensions are mainly intended as a backend for
[Semantic Web education tools](https://github.com/hauptbenutzer/luposdate-spa-client).

POST to /nonstandard/sparql
===========================

Returns the result of a SPARQL query running against provided RDF data.


Request body must be a JSON object. Two short examples:

## Minimal configuration ##

    Request

    {
        "query": "SELECT * WHERE { ?s ?p ?o. } LIMIT 10",
        "rdf": "@prefix dc: <http://purl.org/dc/elements/1.1/>. <http://en.wikipedia.org/wiki/Tony_Benn> dc:title \"Tony Benn\"; dc:publisher \"Wikipedia\"."
    }

    Response

    {
        "JSON": {
            "head": {
                "vars": [
                    "p",
                    "s",
                    "o"
                ]
            },
            "results": {
                "bindings": [
                    {
                        "p": {
                            "type": "uri",
                            "value": "http://purl.org/dc/elements/1.1/title"
                        },
                        "s": {
                            "type": "uri",
                            "value": "http://en.wikipedia.org/wiki/Tony_Benn"
                        },
                        "o": {
                            "type": "literal",
                            "value": "Tony Benn"
                        }
                    },
                    {
                        "p": {
                            "type": "uri",
                            "value": "http://purl.org/dc/elements/1.1/publisher"
                        },
                        "s": {
                            "type": "uri",
                            "value": "http://en.wikipedia.org/wiki/Tony_Benn"
                        },
                        "o": {
                            "type": "literal",
                            "value": "Wikipedia"
                        }
                    }
                ]
            }
        }
    }

## Alternative output formats ##

    Request

    {
        "query": "SELECT * WHERE { ?s ?p ?o. } LIMIT 10",
        "rdf": "@prefix dc: <http://purl.org/dc/elements/1.1/>. <http://en.wikipedia.org/wiki/Tony_Benn> dc:title \"Tony Benn\"; dc:publisher \"Wikipedia\"."
        "formats": ["plain", "xml", "html"]
    }

    Response (raw)

    {
        "Plain": "[{?p=<http://purl.org/dc/elements/1.1/title>, ?s=<http://en.wikipedia.org/wiki/Tony_Benn>, ?o=\\"Tony Benn\\"}, {?p=<http://purl.org/dc/elements/1.1/publisher>, ?s=<http://en.wikipedia.org/wiki/Tony_Benn>, ?o=\\"Wikipedia\\"}]",
        "XML": "<?xml version=\\"1.0\\"?>\\n<sparql xmlns:rdf=\\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\\" xmlns:xs=\\"http://www.w3.org/2001/XMLSchema#\\" xmlns=\\"http://www.w3.org/2005/sparql-results#\\">\\n <head>\\n  <variable name=\\"p\\"/>\\n  <variable name=\\"s\\"/>\\n  <variable name=\\"o\\"/>\\n </head>\\n <results>\\n   <result>\\n    <binding name=\\"p\\">\\n     <uri>http://purl.org/dc/elements/1.1/title</uri>\\n    </binding>\\n    <binding name=\\"s\\">\\n     <uri>http://en.wikipedia.org/wiki/Tony_Benn</uri>\\n    </binding>\\n    <binding name=\\"o\\">\\n     <literal>Tony Benn</literal>\\n    </binding>\\n   </result>\\n   <result>\\n    <binding name=\\"p\\">\\n     <uri>http://purl.org/dc/elements/1.1/publisher</uri>\\n    </binding>\\n    <binding name=\\"s\\">\\n     <uri>http://en.wikipedia.org/wiki/Tony_Benn</uri>\\n    </binding>\\n    <binding name=\\"o\\">\\n     <literal>Wikipedia</literal>\\n    </binding>\\n   </result>\\n </results>\\n</sparql>",
        "Comma Separated Values (CSV)": "p,s,o\\n<http://purl.org/dc/elements/1.1/title>,<http://en.wikipedia.org/wiki/Tony_Benn>,\\"Tony Benn\\"\\n<http://purl.org/dc/elements/1.1/publisher>,<http://en.wikipedia.org/wiki/Tony_Benn>,\\"Wikipedia\\"\\n"
    }

    Response (pretty without escaping - this is not valid JSON)

    {
        "Plain": "
            [{?p=<http://purl.org/dc/elements/1.1/title>, ?s=<http://en.wikipedia.org/wiki/Tony_Benn>, ?o="Tony Benn"}, {?p=<http://purl.org/dc/elements/1.1/publisher>, ?s=<http://en.wikipedia.org/wiki/Tony_Benn>, ?o="Wikipedia"}]
        ",
        "XML": "
            <?xml version="1.0"?>
            <sparql xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:xs="http://www.w3.org/2001/XMLSchema#" xmlns="http://www.w3.org/2005/sparql-results#">
             <head>
              <variable name="p"/>
              <variable name="s"/>
              <variable name="o"/>
             </head>
             <results>
               <result>
                <binding name="p">
                 <uri>http://purl.org/dc/elements/1.1/title</uri>
                </binding>
                <binding name="s">
                 <uri>http://en.wikipedia.org/wiki/Tony_Benn</uri>
                </binding>
                <binding name="o">
                 <literal>Tony Benn</literal>
                </binding>
               </result>
               <result>
                <binding name="p">
                 <uri>http://purl.org/dc/elements/1.1/publisher</uri>
                </binding>
                <binding name="s">
                 <uri>http://en.wikipedia.org/wiki/Tony_Benn</uri>
                </binding>
                <binding name="o">
                 <literal>Wikipedia</literal>
                </binding>
               </result>
             </results>
            </sparql>
        ",
        "Comma Separated Values (CSV)": "
            <http://purl.org/dc/elements/1.1/title>,<http://en.wikipedia.org/wiki/Tony_Benn>,"Tony Benn"
            <http://purl.org/dc/elements/1.1/publisher>,<http://en.wikipedia.org/wiki/Tony_Benn>,"Wikipedia"
        "
    }

    
## JSON Keys ##

**query (mandatory)**

Query must be a valid SPARQL query

**rdf (mandatory)**

RDF Data to perform the query on. RDF Data must be provided in N3 format.

**formats (optional)**

JSON Array of Strings.\
Valid formats are:\
`Comma Separated Values (CSV)`, `XML with Query Triples`, `Query-Triples`,
`JSON`, `JSON with Query-Triples`, `Tab Separated Values (TSV)`, `Colored HTML`,
`Colored HTML with Query-Triples`, `HTML`, `XML`, `Plain`, `HTML with
Query-Triples`\
Aliases:\
`text/csv`, `application/sparql-results+xml+querytriples`, `text/n3`,
`application/sparql-results+json`,
`application/sparql-results+json+querytriples`, `text/tsv`, `colored html`,
`colored html with query-triples`, `html`, `application/sparql-results+xml`,
`text/plain`, `html with query-triples`

**Please note that `Query-Triples` (`text/n3`) formatter is broken at the moment (02/2015)!**

**evaluator (optional)**

Must be one of `MemoryIndex`, `RDF3X`, `Stream`, `Jena` or `Sesame`.\
Defaults to `MemoryIndex`

**inference (optional)**

Must be one of `NONE`, `RIF`, `RDFS` or `OWL2RL`.\
Defaults to `NONE`.

**inferenceGeneration (optional)**

Only relevant if `inference` is `RDFS` or `OWL2RL`.\
Must be one of `GENERATED`, `GENERATEDOPT` or `FIXED`.\
Defaults to `FIXED`.

**owl2rlInconsistencyCheck (optional)**

Only relevant if `inference` is `OWL2RL`.\
Must be a boolean.\
Defaults to `false`.

**rif (optional)**

RIF rule set to be used for RIF inference.\
Only relevant if `inference` is `RIF`.\
Defaults to `( empty string )`.

## Errors ##

The server usually responds with an error object describing what went wrong.
This is not a complete list - just some examples.

**Missing RDF (400 Bad Request)**

    Request

    {
        "query": "SELECT * WHERE { ?s ?p ?o. } LIMIT 10"
    }

    Response

    {
        "error": "Key \"rdf\" must be present in body."
    }

**Malformed JSON (400 Bad Request)**

    Request

    {
        foobar
    }

    Response

    {
        "error": "Expected a ':' after a key at 13 [character 1 line 4]"
    }

For semantic errors the server issues a 200 OK response.

**Bad query (200 OK)**

    Request

    {
        "query": "foobar",
        "rdf": "@prefix dc: <http://purl.org/dc/elements/1.1/>. <http://en.wikipedia.org/wiki/Tony_Benn> dc:title \"Tony Benn\"; dc:publisher \"Wikipedia\"."
    }

    Response

    {
        "queryError": "Lexical error at line 1, column 7.  Encountered: <EOF> after : \"foobar\""
    }

**Another bad query (200 OK)**

    {
        "query": "SELECT ** WHERE { ?s ?p ?o. } LIMIT 10",
        "rdf": " @prefix dc: <http://purl.org/dc/elements/1.1/>. <http://en.wikipedia.org/wiki/Tony_Benn> dc:title \"Tony Benn\"; dc:publisher \"Wikipedia\"."
    }

    Response

    {
        "queryError": "Encountered \" \"*\" \"* \"\" at line 1, column 9.\nWas expecting one of:\n    \"{\" ...\n    \"WHERE\" ...\n    \"FROM\" ...\n    "
    }

**Bad RDF (200 OK)**

    Request

    {
        "query": "SELECT * WHERE { ?s ?p ?o. } LIMIT 10",
        "rdf": "foobar"
    }

    Response

    {
        "rdfError": "Lexical error at line 1, column 7.  Encountered: <EOF> after : \"foobar\""
    }

POST to /nonstandard/sparql/info
================================

Returns additional compilation information about a SPARQL query. At its best it
will be the AST for the query, the query in core SPARQL and the AST of the core
SPARQL query.

Request body must be a JSON object.

## Minimal configuration ##

    Request

    {
        "query": "SELECT * WHERE { ?s ?p ?o. } LIMIT 10"
    }

    Response (nested output format)

    {
        "AST": {
            "children": [
                {
                    "children": [
                        {
                            "children": [
                                {
                                    "children": [
                                        {
                                            "description": "Var s",
                                            "classification": "TerminalNode",
                                            "type": "ASTVar"
                                        },
                                        {
                                            "description": "Var p",
                                            "classification": "TerminalNode",
                                            "type": "ASTVar"
                                        },
                                        {
                                            "children": [
                                                {
                                                    "description": "Var o",
                                                    "classification": "TerminalNode",
                                                    "type": "ASTVar"
                                                }
                                            ],
                                            "description": "ObjectList",
                                            "classification": "NonTerminalNode",
                                            "type": "ASTObjectList"
                                        }
                                    ],
                                    "description": "TripleSet",
                                    "classification": "UnknownNode",
                                    "type": "ASTTripleSet"
                                }
                            ],
                            "description": "GroupConstraint",
                            "classification": "NonTerminalNode",
                            "type": "ASTGroupConstraint"
                        },
                        {
                            "description": "Limit 10",
                            "classification": "HighLevelOperator",
                            "type": "ASTLimit"
                        }
                    ],
                    "description": "SelectQuery disctinct :false reduced:false select all:true",
                    "classification": "QueryHead",
                    "type": "ASTSelectQuery"
                }
            ],
            "description": "Query",
            "classification": "QueryHead",
            "type": "ASTQuery"
        },
        "coreAST": {
            "children": [
                {
                    "children": [
                        {
                            "children": [
                                {
                                    "children": [
                                        {
                                            "description": "Var s",
                                            "classification": "TerminalNode",
                                            "type": "ASTVar"
                                        },
                                        {
                                            "description": "Var p",
                                            "classification": "TerminalNode",
                                            "type": "ASTVar"
                                        },
                                        {
                                            "children": [
                                                {
                                                    "description": "Var o",
                                                    "classification": "TerminalNode",
                                                    "type": "ASTVar"
                                                }
                                            ],
                                            "description": "ObjectList",
                                            "classification": "NonTerminalNode",
                                            "type": "ASTObjectList"
                                        }
                                    ],
                                    "description": "TripleSet",
                                    "classification": "UnknownNode",
                                    "type": "ASTTripleSet"
                                }
                            ],
                            "description": "GroupConstraint",
                            "classification": "NonTerminalNode",
                            "type": "ASTGroupConstraint"
                        },
                        {
                            "description": "Limit 10",
                            "classification": "HighLevelOperator",
                            "type": "ASTLimit"
                        }
                    ],
                    "description": "SelectQuery disctinct :false reduced:false select all:true",
                    "classification": "QueryHead",
                    "type": "ASTSelectQuery"
                }
            ],
            "description": "Query",
            "classification": "QueryHead",
            "type": "ASTQuery"
        },
        "coreSPARQL": "SELECT *\n\nWHERE \n{\n?s ?p ?o .\n} LIMIT 10\n"
    }

## Alternative output formats ##

    Request

    {
        "query": "SELECT * WHERE { ?s ?p ?o. } LIMIT 10",
        "astFormat": "graph"
    }

    Response

    {
        "AST": {
            "nodes": [
                {
                    "depth": 0,
                    "description": "Query",
                    "id": 909246165,
                    "type": "ASTQuery",
                    "classification": "QueryHead"
                },
                {
                    "depth": 1,
                    "description": "SelectQuery disctinct :false reduced:false select all:true",
                    "id": 533882334,
                    "type": "ASTSelectQuery",
                    "classification": "QueryHead"
                },
                {
                    "depth": 2,
                    "description": "GroupConstraint",
                    "id": 1619217372,
                    "type": "ASTGroupConstraint",
                    "classification": "NonTerminalNode"
                },
                {
                    "depth": 2,
                    "description": "Limit 10",
                    "id": 1953778864,
                    "type": "ASTLimit",
                    "classification": "HighLevelOperator"
                },
                {
                    "depth": 3,
                    "description": "TripleSet",
                    "id": 756432169,
                    "type": "ASTTripleSet",
                    "classification": "UnknownNode"
                },
                {
                    "depth": 4,
                    "description": "Var s",
                    "id": 560349632,
                    "type": "ASTVar",
                    "classification": "TerminalNode"
                },
                {
                    "depth": 4,
                    "description": "Var p",
                    "id": 1530999707,
                    "type": "ASTVar",
                    "classification": "TerminalNode"
                },
                {
                    "depth": 4,
                    "description": "ObjectList",
                    "id": 218559887,
                    "type": "ASTObjectList",
                    "classification": "NonTerminalNode"
                },
                {
                    "depth": 5,
                    "description": "Var o",
                    "id": 300248691,
                    "type": "ASTVar",
                    "classification": "TerminalNode"
                }
            ],
            "edges": {
                "218559887": [
                    "300248691"
                ],
                "533882334": [
                    "1619217372",
                    "1953778864"
                ],
                "756432169": [
                    "560349632",
                    "1530999707",
                    "218559887"
                ],
                "909246165": [
                    "533882334"
                ],
                "1619217372": [
                    "756432169"
                ]
            }
        },
        "coreAST": {
            "nodes": [
                {
                    "depth": 0,
                    "description": "Query",
                    "id": 43931086,
                    "type": "ASTQuery",
                    "classification": "QueryHead"
                },
                {
                    "depth": 1,
                    "description": "SelectQuery disctinct :false reduced:false select all:true",
                    "id": 1351709677,
                    "type": "ASTSelectQuery",
                    "classification": "QueryHead"
                },
                {
                    "depth": 2,
                    "description": "GroupConstraint",
                    "id": 1786863357,
                    "type": "ASTGroupConstraint",
                    "classification": "NonTerminalNode"
                },
                {
                    "depth": 2,
                    "description": "Limit 10",
                    "id": 1353524008,
                    "type": "ASTLimit",
                    "classification": "HighLevelOperator"
                },
                {
                    "depth": 3,
                    "description": "TripleSet",
                    "id": 552418518,
                    "type": "ASTTripleSet",
                    "classification": "UnknownNode"
                },
                {
                    "depth": 4,
                    "description": "Var s",
                    "id": 2067279165,
                    "type": "ASTVar",
                    "classification": "TerminalNode"
                },
                {
                    "depth": 4,
                    "description": "Var p",
                    "id": 464450065,
                    "type": "ASTVar",
                    "classification": "TerminalNode"
                },
                {
                    "depth": 4,
                    "description": "ObjectList",
                    "id": 572728879,
                    "type": "ASTObjectList",
                    "classification": "NonTerminalNode"
                },
                {
                    "depth": 5,
                    "description": "Var o",
                    "id": 1801561393,
                    "type": "ASTVar",
                    "classification": "TerminalNode"
                }
            ],
            "edges": {
                "43931086": [
                    "1351709677"
                ],
                "552418518": [
                    "2067279165",
                    "464450065",
                    "572728879"
                ],
                "572728879": [
                    "1801561393"
                ],
                "1351709677": [
                    "1786863357",
                    "1353524008"
                ],
                "1786863357": [
                    "552418518"
                ]
            }
        },
        "coreSPARQL": "SELECT *\n\nWHERE \n{\n?s ?p ?o .\n} LIMIT 10\n"
    }

## JSON Keys ##

**query (mandatory)**

Query must be a valid SPARQL query

**evaluator (optional)**

Must be one of `MemoryIndex`, `RDF3X`, `Stream`, `Jena` or `Sesame`.\
Defaults to `MemoryIndex`

**Please note that not all evaluators provided the same level of information (if
  at all)!**

## Errors ##

The server usually responds with an error object describing what went wrong.
This is not a complete list - just some examples.

**Requested evaluator does not provide information (200 OK)**

    Request

    {
        "query": "SELECT * WHERE { ?s ?p ?o. }",
        "evaluator": "Jena"
    }

    Response

    {
        "info": "Compiler does not provide additional information."
    }

POST to /nonstandard/sparql/graphs
==================================

Returns operator graphs for each optimization phase used when processing the
query.

Request body must be a JSON object. The request format is the same as for
[POST to /nonstandard/sparql].

## Minimal configuration ##

    Request

    {
        "query": "SELECT * WHERE { ?s ?p ?o. } LIMIT 10",
        "rdf": "@prefix dc: <http://purl.org/dc/elements/1.1/>.\n <http://en.wikipedia.org/wiki/Tony_Benn> dc:title \"Tony Benn\"; dc:publisher \"Wikipedia\"."
    }

    Response (always graph output format)

    {
        "optimization": {
            "steps": [
                {
                    "description": "Before a possible correction of the operator graph...",
                    "ruleName": "correctoperatorgraphPackageDescription",
                    "operatorGraph": {
                        "nodes": [
                            {
                                "depth": 0,
                                "description": "MemoryIndexRoot",
                                "id": 0,
                                "type": "BasicOperatorByteArray"
                            },
                            {
                                "depth": 1,
                                "description": "Memory Index Scan on\nTriplePattern (?s, ?p, ?o)",
                                "id": 44,
                                "type": "BasicOperatorByteArray"
                            },
                            {
                                "depth": 2,
                                "description": "Limit 10",
                                "id": 154,
                                "type": "BasicOperatorByteArray"
                            },
                            {
                                "depth": 3,
                                "description": "Result",
                                "id": 186,
                                "type": "BasicOperatorByteArray"
                            }
                        ],
                        "edges": {
                            "0": [
                                "44"
                            ],
                            "44": [
                                "154"
                            ],
                            "154": [
                                "186"
                            ]
                        }
                    }
                },
                {
                    "description": "Before logical optimization...",
                    "ruleName": "logicaloptimizationPackageDescription",
                    "operatorGraph": {
                        "nodes": [
                            {
                                "depth": 0,
                                "description": "MemoryIndexRoot",
                                "id": 0,
                                "type": "BasicOperatorByteArray"
                            },
                            {
                                "depth": 1,
                                "description": "Memory Index Scan on\nTriplePattern (?s, ?p, ?o)",
                                "id": 44,
                                "type": "BasicOperatorByteArray"
                            },
                            {
                                "depth": 2,
                                "description": "Limit 10",
                                "id": 154,
                                "type": "BasicOperatorByteArray"
                            },
                            {
                                "depth": 3,
                                "description": "Result",
                                "id": 186,
                                "type": "BasicOperatorByteArray"
                            }
                        ],
                        "edges": {
                            "0": [
                                "44"
                            ],
                            "44": [
                                "154"
                            ],
                            "154": [
                                "186"
                            ]
                        }
                    }
                },
                {
                    "description": "After optimizing the join order...",
                    "ruleName": "optimizingjoinord;erRule",
                    "operatorGraph": {
                        "nodes": [
                            {
                                "depth": 0,
                                "description": "MemoryIndexRoot",
                                "id": 0,
                                "type": "BasicOperatorByteArray"
                            },
                            {
                                "depth": 1,
                                "description": "Memory Index Scan on\nTriplePattern (?s, ?p, ?o)",
                                "id": 44,
                                "type": "BasicOperatorByteArray"
                            },
                            {
                                "depth": 2,
                                "description": "Limit 10",
                                "id": 154,
                                "type": "BasicOperatorByteArray"
                            },
                            {
                                "depth": 3,
                                "description": "Result",
                                "id": 186,
                                "type": "BasicOperatorByteArray"
                            }
                        ],
                        "edges": {
                            "0": [
                                "44"
                            ],
                            "44": [
                                "154"
                            ],
                            "154": [
                                "186"
                            ]
                        }
                    }
                },
                {
                    "description": "After physical optimization...",
                    "ruleName": "physicaloptimizationRule",
                    "operatorGraph": {
                        "nodes": [
                            {
                                "depth": 0,
                                "description": "MemoryIndexRoot",
                                "id": 0,
                                "type": "BasicOperatorByteArray"
                            },
                            {
                                "depth": 1,
                                "description": "Memory Index Scan on\nTriplePattern (?s, ?p, ?o)",
                                "id": 44,
                                "type": "BasicOperatorByteArray"
                            },
                            {
                                "depth": 2,
                                "description": "Limit 10",
                                "id": 154,
                                "type": "BasicOperatorByteArray"
                            },
                            {
                                "depth": 3,
                                "description": "Result",
                                "id": 186,
                                "type": "BasicOperatorByteArray"
                            }
                        ],
                        "edges": {
                            "0": [
                                "44"
                            ],
                            "44": [
                                "154"
                            ],
                            "154": [
                                "186"
                            ]
                        }
                    }
                }
            ]
        },
        "prefix": {
            "pre-defined": {
                "<http://www.w3.org/2001/XMLSchema#>": "xsd",
                "<http://www.w3.org/1999/02/22-rdf-syntax-ns#>": "rdf",
                "<http://www.w3.org/2005/xpath-functions#>": "fn",
                "<http://www.w3.org/2000/01/rdf-schema#>": "rdfs"
            },
            "prefixes": {},
            "names": []
        }
    }    

POST to /nonstandard/rif
========================

Request format is the same as for POSTs to [POST to /nonstandard/sparql], except that
`query` must be RIF. Also optional `evaluator` key is ignored.

**Please note that some formatters like `JSON` aren't supporting RIF results yet (02/2015)!**

## Minimal configuration ##

    Request

    {
        "query": "Document(\n  Prefix(cpt <http://example.com/concepts#>)\n  Prefix(ppl <http://example.com/people#>)\n  Prefix(bks <http://example.com/books#>)\n\n  Group\n  (\n    Forall ?Buyer ?Item ?Seller (\n cpt:buy(?Buyer ?Item ?Seller) :- cpt:sell(?Seller ?Item ?Buyer)\n    )\n \n    cpt:sell(ppl:John bks:LeRif ppl:Mary)\n  )\n)",
        "rdf": "",
        "formats": ["plain"]
    }

    Response

    {
        "Plain": "[<http://example.com/concepts#buy>(<http://example.com/people#Mary>, <http://example.com/books#LeRif>, <http://example.com/people#John>)]"
    }

POST to /nonstandard/rif/info
========================

Returns additional compilation information about a RIF document. It will return
the AST and the rule set.

Request format is the same as for POSTs to [POST to /nonstandard/sparql/info], except that
`query` must be RIF. Also optional `evaluator` key is ignored.

## Minimal configuration ##

    Request

    {
        "query": "Document(\n  Prefix(cpt <http://example.com/concepts#>)\n  Prefix(ppl <http://example.com/people#>)\n  Prefix(bks <http://example.com/books#>)\n\n  Group\n  (\n    Forall ?Buyer ?Item ?Seller (\n cpt:buy(?Buyer ?Item ?Seller) :- cpt:sell(?Seller ?Item ?Buyer)\n    )\n \n    cpt:sell(ppl:John bks:LeRif ppl:Mary)\n  )\n)"
    }

    Response (nested output format and stripped down - these are big even for small queries)

    {
        "AST": {
            [...] (format same as rulesAST)
        }
        "rulesAST": {
            "children": [
                {
                    "children": [
                        {
                            "description": "?Item",
                            "type": "RuleVariable"
                        },
                        {
                            "description": "?Buyer",
                            "type": "RuleVariable"
                        },
                        {
                            "description": "?Seller",
                            "type": "RuleVariable"
                        },
                        {
                            "description": "<http://example.com/concepts#buy>(?Buyer, ?Item, ?Seller))",
                            "type": "RulePredicate"
                        },
                        {
                            "description": "<http://example.com/concepts#sell>(?Seller, ?Item, ?Buyer))",
                            "type": "RulePredicate"
                        }
                    ],
                    "description": "<http://example.com/concepts#buy>(?Buyer, ?Item, ?Seller))",
                    "type": "Rule"
                }
            ],
            "description": "Document\nPrefix: rdf - http://www.w3.org/1999/02/22-rdf-syntax-ns#\nPrefix: cpt - http://example.com/concepts#\nPrefix: rdfs - http://www.w3.org/2000/01/rdf-schema#\nPrefix: bks - http://example.com/books#\nPrefix: xs - http://www.w3.org/2001/XMLSchema#\nPrefix: ppl - http://example.com/people#\nPrefix: rif - http://www.w3.org/2007/rif#\n",
            "type": "Document"
        }
    }

POST to /nonstandard/rif/graphs
==================================

Returns operator graphs for each optimization phase used when processing the
query.

Request body must be a JSON object. The request format is the same as for
[POST to /nonstandard/rif].
