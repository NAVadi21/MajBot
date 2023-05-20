package bot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * MajBot 1.0
 * 
 * The `Bot` class represents the core functionality of MajBot, a chatbot designed to interact with users based on predefined states and keywords. It allows users to send messages to the bot and receive responses based on the current state and matching keywords.
 * 
 * The class stores regular expression matches in a dictionary and keeps track of the current state and a `DataParser` object for retrieving state information.
 */
public class Bot {
    private HashMap<String,String> dictionary; // Store all regular expression matches
    String level = "0"; // Default state to start the bot
    DataParser parser;
    
    /**
     * Constructor for the `Bot` class.
     * 
     * @param level  The initial state level for the bot.
     * @param parser The `DataParser` object for retrieving state information.
     */
    public Bot(String level, DataParser parser) {
        dictionary = new HashMap<String,String>();
        this.level = level;
        this.parser = parser;
    }

    /**
     * Retrieves the current state message.
     * 
     * @return The current state message.
     */
    public String getMessage() {
        State state = parser.getState(level);
        return replaceMatches(state.getMessage()).trim();
    }

    /**
     * Sends a message to the bot and receives the response.
     * 
     * @param message The user's message to the bot.
     * @return The response from the bot.
     */
    public String send(String message) {
        String response = "";
        State state = parser.getState(level);

        // Check if it's the end of the tree
        if (state.getKeywords().isEmpty()) {
            this.level = "1";
        }

        // Match the keyword with the given message
        Keyword match = parse(message, state.getKeywords());

        // If no keyword is matched, display one of the invalid answers
        if (match == null) {
            response = parser.getInvalidAnswer();
        } else {
            // Check if a classname is provided for dynamic response
            if (match.className.length() > 0) {
                // Check for Weather dynamic response
                if (match.className.equals("Weather")) {
                    Weather weather = new Weather();
                    response = weather.getResponse(match.arg);
                    this.level = "1";
                }
            } else {
                // Get the new state and return the new message
                if (response.length() == 0) {
                    this.level = match.target;
                    state = parser.getState(level);

                    // Check if it's the end of the tree
                    if (state.getKeywords().isEmpty()) {
                        response = this.getMessage();
                        this.level = "1";
                    }
                }
            }
        }
        return response;
    }

    /**
     * Parses the given text to find the best match in the keywords.
     * 
     * @param text    The text to be parsed.
     * @param keylist The list of keywords to match against.
     * @return The best matching keyword.
     */
    private Keyword parse(String text, ArrayList<Keyword> keylist) {
        int bestMatch = -1; // Default match value
        Keyword match = null;

        // Loop through keywords
        for (int i = 0; i < keylist.size(); i++) {
            int matches = getMatches(text, keylist.get(i)); // Get number of matches for the keyword

            // If the match is better than the best match, replace it
            if (matches > -1 && matches > bestMatch) {
                match = keylist.get(i);
                bestMatch = matches;
            }
        }

        // Add the best answers regex variable value into the dictionary for future reference
        if (match != null) {
            if (match.learn.length() > 0) {
                // Get training data keyword and description
                String subject = dictionary.get(match.learn);
                String result = match.variableValue;

                // Create a new state for new trained data
                ArrayList<String> messages = new ArrayList<String>();
                messages.add(result);
                State myState = new State(String.valueOf(parser.stateCounter), messages, new ArrayList());
                parser.addState(myState);

                // Add the new trained keyword
                Keyword keyword = new Keyword(subject, myState.getId(), "", "", "", 1, "");
                State state = parser.getState("1");
                ArrayList<Keyword> keywords = state.getKeywords();
                keywords.add(keyword);
            } else {
                if (match.variableValue.length() > 0) {
                    dictionary.put(match.variable, match.variableValue);
                }
            }
        }
        return match;
    }

    /**
     * Gets the number of matches of the given keywords in the given list.
     * 
     * @param text    The text to be matched against.
     * @param keyword The keyword to match.
     * @return The number of matches.
     */
    private int getMatches(String text, Keyword keyword) {
        int result = -1; // No match by default

        // Return 0 matches when the keyword is "*"
        if (keyword.keyword.equals("*")) {
            return keyword.points;
        }

        // If a regex is expected
        if (keyword.variable.length() > 0) {
            String match = Regex.match(keyword.keyword, text);
            if (match.length() > 0) {
                keyword.variableValue = match;
                return keyword.points;
            }
        }

        String[] words = keyword.keyword.split(" ");

        // Loop through the list of keywords
        for (String word : words) {
            // If the current keyword is in the text, add points
            if (text.toLowerCase().indexOf(word.toLowerCase()) >= 0) {
                result = result + keyword.points + 1;
            } else {
                // Return -1 if one of the keywords does not exist
                return -1;
            }
        }
        return result;
    }

    /**
     * Replaces variables in the given text with values from the dictionary.
     * 
     * @param text The text to be processed.
     * @return The processed text with variables replaced.
     */
    public String replaceMatches(String text) {
        // Replace variables in the dictionary within the text
        for (Map.Entry<String, String> entry : dictionary.entrySet()) {
            text = text.replaceAll("\\[" + entry.getKey() + "\\]", entry.getValue());
        }

        // Remove empty variable tags
        return Regex.clear(text);
    }
}
