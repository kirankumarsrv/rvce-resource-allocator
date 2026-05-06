package org.autodoc;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.javadoc.Javadoc;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class JavaDocExtractor {

    public static void main(String[] args) throws Exception {
        String rootDir = args.length > 0 ? args[0] : ".";
        List<String[]> data = new ArrayList<>();

        // ensure parser is configured for Java 17 language features
        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        StaticJavaParser.setConfiguration(config);

        Files.walk(Paths.get(rootDir))
            .filter(p -> p.toString().endsWith(".java"))
            .forEach(path -> processFile(path, data));

        writeCSV(data, Paths.get(rootDir).resolve("java_output.csv").toString());
        System.out.println("✅ Java parsing done!");
    }

    private static void processFile(Path path, List<String[]> data) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(path);

            String packageName = cu.getPackageDeclaration()
                    .map(p -> p.getNameAsString())
                    .orElse("");

            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
                String className = cls.getNameAsString();
                String classType = cls.isInterface() ? "Interface" : "Class";

                String classDoc = cls.getJavadoc().map(j -> j.toText()).orElse("");

                String annotations = String.join(";", cls.getAnnotations().stream().map(a -> a.getNameAsString()).toList());

                data.add(new String[]{path.toString(), packageName, classType, className, "", "", annotations, classDoc});

                for (MethodDeclaration method : cls.getMethods()) {
                    String methodName = method.getNameAsString();
                    String returnType = method.getType().toString();
                    String params = method.getParameters().stream().map(p -> p.getType() + " " + p.getName()).reduce((a, b) -> a + ", " + b).orElse("");
                    String methodDoc = method.getJavadoc().map(j -> j.toText()).orElse("");
                    String methodAnnotations = String.join(";", method.getAnnotations().stream().map(a -> a.getNameAsString()).toList());

                    data.add(new String[]{path.toString(), packageName, "Method", methodName, params, returnType, methodAnnotations, methodDoc});
                }
            });

            cu.findAll(EnumDeclaration.class).forEach(en -> {
                String name = en.getNameAsString();
                String doc = en.getJavadoc().map(Javadoc::toText).orElse("");
                data.add(new String[]{path.toString(), packageName, "Enum", name, "", "", "", doc});
            });

        } catch (Exception e) {
            System.out.println("Error parsing: " + path + " -> " + e.getMessage());
        }
    }

    private static void writeCSV(List<String[]> data, String outPath) throws IOException {
        FileWriter writer = new FileWriter(outPath);
        writer.append("File,Package,Type,Name,Parameters,ReturnType,Annotations,JavaDoc\n");

        for (String[] row : data) {
            writer.append(escape(row[0])).append(',')
                    .append(escape(row[1])).append(',')
                    .append(escape(row[2])).append(',')
                    .append(escape(row[3])).append(',')
                    .append(escape(row.length>4?row[4]:"" )).append(',')
                    .append(escape(row.length>5?row[5]:"" )).append(',')
                    .append(escape(row.length>6?row[6]:"" )).append(',')
                    .append(escape(row.length>7?row[7]:"" )).append('\n');
        }

        writer.close();
    }

    private static String escape(String s) {
        if (s == null) return "";
        String out = s.replace("\"", "\"\"");
        if (out.contains(",") || out.contains("\n") || out.contains("\"")) {
            return '"' + out + '"';
        }
        return out;
    }
}
