package com.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;

import java.util.Arrays;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    // Environment variable key for the base path
    final static String BASE_PATH = "BASE_PATH";
    // Path to the ffmpeg executable
    final static String FFMPEG_PATH = "/artifacts/ffmpeg/ffmpeg.exe";
    // Default flag for help
    final static String HELP_FLAG = "-h";
    // Query parameter key for command
    final static String COMMAND_QUERY = "command";

    @FunctionName("HttpExample")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = { HttpMethod.GET,
                    HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) throws IOException {
        context.getLogger().info("Java HTTP trigger processed a request.");

        // Parse query parameter
        String flags = request.getQueryParameters().get(COMMAND_QUERY);

        // If no command is provided, use the help flag
        if (flags == null || flags.isBlank()) {
            flags = HELP_FLAG;
        }

        // Prepare the command to execute
        Runtime rt = Runtime.getRuntime();
        String[] commands = { System.getenv(BASE_PATH) + FFMPEG_PATH, flags };
        Process proc = null;
        StringBuilder returnOutput = new StringBuilder();

        try {
            // Execute the command
            proc = rt.exec(commands);
            // Read the output from the command
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(proc.getErrorStream()))) {
                String line;
                // Log and collect standard output
                while ((line = reader.readLine()) != null) {
                    context.getLogger().info(line);
                    returnOutput.append(line).append("\n");
                }

                // Log and collect error output
                while ((line = errorReader.readLine()) != null) {
                    context.getLogger().severe(line);
                    returnOutput.append(line).append("\n");
                }
            }
            // Wait for the process to complete
            proc.waitFor();
        } catch (IOException | InterruptedException e) {
            // Handle exceptions and return a bad request response
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Invalid command:" + Arrays.toString(commands) + returnOutput.toString())
                    .build();
        }

        // Return the output of the command as the response
        return request.createResponseBuilder(HttpStatus.OK).body(returnOutput.toString()).build();
    }
}