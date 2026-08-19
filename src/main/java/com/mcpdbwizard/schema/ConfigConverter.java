package com.mcpdbwizard.schema;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

/**
 * Command-line converter between the two generation-config formats: the classic {@code .pb2}
 * (Java properties) file and the new {@code .json} format modelled by {@link Schema}. The direction
 * is chosen from the file extensions, so it converts either way:
 *
 * <pre>
 *   java -cp mcpdbwizard-app.jar com.mcpdbwizard.schema.ConfigConverter in.pb2  out.json
 *   java -cp mcpdbwizard-app.jar com.mcpdbwizard.schema.ConfigConverter in.json out.pb2
 * </pre>
 *
 * <p>A {@code .pb2}&rarr;{@code .json}&rarr;{@code .pb2} round-trip reproduces the original property
 * set exactly (see {@code SchemaRoundTripTest}).
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class ConfigConverter {

    private ConfigConverter() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: ConfigConverter <in.(pb2|json)> <out.(pb2|json)>");
            System.exit(2);
        }
        convert(new File(args[0]), new File(args[1]));
    }

    /** Convert {@code in} to {@code out}, picking the direction from their {@code .pb2}/{@code .json} extensions. */
    public static void convert(File in, File out) throws IOException {
        Schema schema = load(in);
        if (out.getName().toLowerCase().endsWith(".json")) {
            Files.write(out.toPath(), schema.toJson().getBytes(StandardCharsets.UTF_8));
        } else {
            try (OutputStream os = new FileOutputStream(out)) {
                schema.toPb2().store(os, com.mcpdbwizard.pub.Namer.param_prod_name + " Properties");
            }
        }
    }

    private static Schema load(File in) throws IOException {
        if (in.getName().toLowerCase().endsWith(".json")) {
            String json = new String(Files.readAllBytes(in.toPath()), StandardCharsets.UTF_8);
            return new Schema(json);
        }
        Properties p = new Properties();
        try (var is = Files.newInputStream(in.toPath())) {
            p.load(is);
        }
        return new Schema(p);
    }
}
