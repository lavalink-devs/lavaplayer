package com.sedmelluq.discord.lavaplayer.source.youtube;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;

/**
 * Describes one signature cipher
 */
public class YoutubeSignatureCipher {
    private final List<YoutubeCipherOperation> operations = new ArrayList<>();
    String nFunction = "";
    String scriptTimestamp = "";
    String rawScript = "";

    /**
     * @param text Text to apply the cipher on
     * @return The result of the cipher on the input text
     */
    public String apply(String text) {
        StringBuilder builder = new StringBuilder(text);

        for (YoutubeCipherOperation operation : operations) {
            switch (operation.type) {
                case SWAP:
                    int position = operation.parameter % text.length();
                    char temp = builder.charAt(0);
                    builder.setCharAt(0, builder.charAt(position));
                    builder.setCharAt(position, temp);
                    break;
                case REVERSE:
                    builder.reverse();
                    break;
                case SLICE:
                case SPLICE:
                    builder.delete(0, operation.parameter);
                    break;
                default:
                    throw new IllegalStateException("All branches should be covered");
            }
        }

        return builder.toString();
    }

    /**
     * @param text         Text to transform
     * @param scriptEngine JavaScript engine to execute function
     * @return The result of the n parameter transformation
     */
    public String transform(String text, ScriptEngine scriptEngine) throws ScriptException, NoSuchMethodException {
        String transformed;

        scriptEngine.eval("n=" + nFunction);
        transformed = (String) ((Invocable) scriptEngine).invokeFunction("n", text);

        return transformed;
    }

    /**
     * @param operation The operation to add to this cipher
     */
    public void addOperation(YoutubeCipherOperation operation) {
        operations.add(operation);
    }

    /**
     * @return True if the cipher contains no operations.
     */
    public boolean isEmpty() {
        return operations.isEmpty();
    }

    /**
     * @param nFunction Extracted "n" function
     */
    public void setNFunction(String nFunction) {
        this.nFunction = nFunction;
    }

    /**
     * @param timestamp The timestamp in cipher
     */
    public void setTimestamp(String timestamp) {
        scriptTimestamp = timestamp;
    }

    /**
     * @param script Raw script
     */
    public void setRawScript(String script) {
        rawScript = script;
    }
}
