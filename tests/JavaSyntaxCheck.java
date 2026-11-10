import com.sun.source.util.JavacTask;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Parses Java source without resolving Android SDK classes or external libraries. */
public final class JavaSyntaxCheck {
    private JavaSyntaxCheck() {
    }

    public static void main(String[] args) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("A JDK with javac is required.");
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        List<File> sourceFiles = new java.util.ArrayList<>();
        for (String arg : args) {
            sourceFiles.add(new File(arg));
        }

        try (StandardJavaFileManager files =
                compiler.getStandardFileManager(diagnostics, null, null)) {
            Iterable<? extends JavaFileObject> sources =
                    files.getJavaFileObjectsFromFiles(sourceFiles);
            JavacTask task = (JavacTask) compiler.getTask(
                    null, files, diagnostics, Arrays.asList("-proc:none"), null, sources);
            task.parse();
        }

        int errorCount = 0;
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                errorCount++;
                System.err.println(diagnostic);
            }
        }

        if (errorCount > 0) {
            throw new IllegalStateException(errorCount + " Java syntax errors");
        }
        System.out.println("Java syntax valid: " + sourceFiles.size() + " files");
    }
}
