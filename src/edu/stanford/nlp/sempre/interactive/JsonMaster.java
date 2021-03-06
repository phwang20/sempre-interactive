package edu.stanford.nlp.sempre.interactive;

import java.util.List;
import java.util.Map;

import edu.stanford.nlp.sempre.Builder;
import edu.stanford.nlp.sempre.Example;
import edu.stanford.nlp.sempre.Json;
import edu.stanford.nlp.sempre.Master;
import edu.stanford.nlp.sempre.Session;
import fig.basic.LogInfo;
import fig.basic.Option;

/**
 * Handle queries in json as opposed to lisp tree
 */
public class JsonMaster extends Master {
  public static class Options {
    @Option(gloss = "Write out new grammar rules")
    public String intOutputPath;
    @Option(gloss = "each session gets a different model with its own parameters")
    public boolean independentSessions = false;
    @Option(gloss = "number of utterances to return for autocomplete")
    public int autocompleteCount = 5;
    @Option(gloss = "only allow interactive commands")
    public boolean onlyInteractive = false;
    
    @Option(gloss = "allow regular commands specified in Master")
    public boolean allowRegularCommands = false;
  }

  public static Options opts = new Options();

  public JsonMaster(Builder builder) {
    super(builder);
  }

  @Override
  protected void printHelp() {
    // interactive commands
    LogInfo.log("Should not be run from commandline");
    super.printHelp();
  }

  @Override
  public void runServer() {
    InteractiveServer server = new InteractiveServer(this);
    server.run();
  }

  @Override
  public Response processQuery(Session session, String line) {
    LogInfo.begin_track("InteractiveMaster.handleQuery");
    LogInfo.logs("session %s", session.id);
    LogInfo.logs("query %s", line);
    line = line.trim();
    Response response = new Response();
    handleCommand(session, line, response);
    LogInfo.end_track();
    return response;
  }

  void handleCommand(Session session, String line, Response response) {
    @SuppressWarnings("unchecked")
    List<Object> args = Json.readValueHard(line, List.class);
    String command = (String) args.get(0);
    QueryStats stats = new QueryStats(response, command);
    // Start of interactive commands
    if (command.equals("q")) {
      // Create example
      String utt = (String) args.get(1);
      Example ex = exampleFromUtterance(utt, session);

      builder.parser.parse(builder.params, ex, false);

      stats.size(ex.predDerivations != null ? ex.predDerivations.size() : 0);
      stats.status(InteractiveUtils.getParseStatus(ex));

      LogInfo.logs("parse stats: %s", response.stats);
      response.ex = ex;
    } else if (command.equals("accept")) {
      
    } else if (command.equals("context")) {
      if (args.size() == 1) {
        LogInfo.logs("%s", session.context);
      } else {
        session.context = new JsonContextValue(args.get(1)); 
        response.stats.put("context_length", args.get(1).toString().length());
      }
    } else {
      LogInfo.log("Invalid command: " + args);
    }
  }

  private static Example exampleFromUtterance(String utt, Session session) {
    Example.Builder b = new Example.Builder();
    b.setId(session.id);
    b.setUtterance(utt);
    b.setContext(session.context);
    Example ex = b.createExample();
    ex.preprocess();
    return ex;
  }
}
