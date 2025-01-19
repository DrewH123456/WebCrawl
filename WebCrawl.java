// Drew Higginbotham CSS 436 Prof Dimpsey

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.http.HttpClient.Redirect;

public class WebCrawl {
  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      System.err.println("Usage: java WebCrawl <url> <num_hops>");
      System.exit(1);
    }
    String prevURI = args[0]; // stores previous URI link
    String stringHop = args[1];
    int numHop = 0;
    try {
      numHop = Integer.parseInt(stringHop);
      if (numHop >= 0) {
        // can use
      } else { // negative int
        System.err.println("Error: The number of hops must be a non-negative integer.");
        System.exit(1);
      }
    } catch (NumberFormatException e) { // not an int
      System.err.println("Error: Invalid input for the number of hops. Please enter a non-negative integer.");
      System.exit(1);
    }
    // HttpClient client = HttpClient.newHttpClient();
    HttpClient client = HttpClient.newBuilder()
        // for 300 code, redirect
        .followRedirects(Redirect.ALWAYS)
        .build();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(prevURI))
        .GET()
        .build();

    int i = 0; // num of hops so far
    String tempURI = prevURI; // stores link you are visiting
    int prevJump = 0; // stores previous URI's next URI to jump to
    boolean was500 = false; // checks if was previously 500
    ArrayList<String> pastURIs = new ArrayList<>();
    boolean foundURI = false; // true if valid URI found
    String withSlash = ""; // adds a slash to the end of string if doesn't already have

    while (i <= numHop) {
      try {
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        int status = response.statusCode();
        if (status < 200) { // handle later
          System.out.println(status);
        } else if (status < 300) { // if 200s, 300s, redirect. count as hop and extract first url
          withSlash = modifyURI(tempURI);
          if (!pastURIs.contains(withSlash)) { // first visit
            System.out.println("Hop " + i + ": " + tempURI);
            prevJump = 0;
            prevURI = tempURI; // saves tempURI if ever have to go back
            tempURI = modifyURI(tempURI); // modifies, adds to previously visited
            pastURIs.add(tempURI);
          }
          i++;
          foundURI = false;
          was500 = false;
          while (!foundURI) {
            // parse out first possible link
            tempURI = parseURI(prevJump, response.body()); // seeks for a next available URI
            prevJump++;
            // if no more, end while loop and print out i
            if (tempURI.equals("")) {
              i--;
              System.out.println("Stopped after " + i + " jumps. No more available links.");
              return;
            }
            withSlash = modifyURI(tempURI); // modifies, checks if already stored
            // continues searching otherwise
            if (!pastURIs.contains(withSlash)) {
              request = HttpRequest.newBuilder()
                  .uri(URI.create(tempURI))
                  .GET()
                  .build();
              foundURI = true;
            }
          }
        } else if (status < 500) { // if 400s, skip
          withSlash = modifyURI(tempURI);
          pastURIs.add(withSlash);
          System.out.println("Error " + status + ": at " + tempURI);
          i--;
          request = HttpRequest.newBuilder()
              .uri(URI.create(prevURI))
              .GET()
              .build();
        } else { // if 500s, retry once then skip
          // if was500 is true, visit prevURI
          if (was500) { // if 500 after retry, skip
            withSlash = modifyURI(tempURI);
            pastURIs.add(withSlash);
            System.out.println("Error " + status + ": retry failed at: " + tempURI);
            request = HttpRequest.newBuilder()
                .uri(URI.create(prevURI))
                .GET()
                .build();
            i--;
          } else { // if first time encountering 500, retry
            System.out.println("Error " + status + ": attempting retry at: " + tempURI);
            was500 = true;
            request = HttpRequest.newBuilder()
                .uri(URI.create(tempURI))
                .GET()
                .build();
          }
        }
      } catch (IOException | InterruptedException e) {
        System.out.println("Error: " + e.getMessage());
      }
    }
    System.out.println("Completed all " + numHop + " hops");

  }

  // parses out a URI from the response.body text
  public static String parseURI(int matcher_start, String body) {
    String next_uri = "";
    Pattern pattern = Pattern.compile("<a href=\"(http[^\"]+)\"");
    Matcher matcher = pattern.matcher(body);

    for (int i = 0; i < matcher_start; i++) { // iterates through loop, until matcher_start match
      if (!matcher.find()) {
        // Return an empty string if there are fewer matches than matcher_start
        return "";
      }
    }

    if (matcher.find()) { // take substring of body, extracting URI
      int start = matcher.start(1);
      int end = matcher.end(1);

      if (start != -1 && end != -1) {
        next_uri = body.substring(start, end);
      }
    }
    return next_uri;
  }

  public static String modifyURI(String URI) { // adds '/' at end and 's' in https to be stored in pastURIs
    if (URI.charAt(4) != 's') {
      URI = URI.substring(0, 4) + "s" + URI.substring(4);
    }
    if (!URI.endsWith("/")) {
      return URI + '/';
    }
    return URI;
  }
}