package br.com.dfsc.hpsaplugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@Mojo(name = "copy-java", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class JavaFileCopyMojo extends AbstractMojo {

    @Parameter
    private List<SourceDestinationPair> files;

    public void execute() throws MojoExecutionException {

        if (files == null || files.isEmpty()) {
            throw new MojoExecutionException("No source/destination pairs provided.");
        }

        for (SourceDestinationPair pair : files) {
            processFile(pair.getSource(), pair.getDestination());
        }
    }

    private void processFile(String source, String destination) throws MojoExecutionException  {
        try (BufferedReader reader = new BufferedReader(new FileReader(source));
             BufferedWriter writer = new BufferedWriter(new FileWriter(destination))) {

            String line;
            boolean insideClass = false;

            while ((line = reader.readLine()) != null) {
                // Preserve a linha original com a indentação
                // Preservar declarações de importação
                if (line.startsWith("import")) {
                    writer.write(line);
                    writer.newLine();
                    continue; // Continue para a próxima linha após escrever import
                }

                // Verificar se a linha contém a declaração de uma classe
                if (line.matches(".*\\bclass\\b.*")) {
                    insideClass = true; // Entrando no corpo da classe
                    continue; // Ignorar a linha da declaração da classe
                }

                if (insideClass) {
                    writer.write(line); // Escrever linha mantendo indentação
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error copying Java file", e);
        }

        try {
            // Lê o conteúdo do arquivo
            StringBuilder fileContent = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(destination))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    fileContent.append(line).append(System.lineSeparator());
                }
            }

            // Remove o último '}' se existir
            String modifiedContent = removeLastBracket(fileContent.toString());

            // Escreve o conteúdo modificado de volta ao arquivo
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(destination))) {
                writer.write(modifiedContent);
            }

            System.out.println("Último '}' removido com sucesso.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static String removeLastBracket(String content) {
        // Verifica se o conteúdo termina com '}' e remove a última ocorrência
        int lastIndex = content.lastIndexOf("}");
        if (lastIndex != -1) {
            return content.substring(0, lastIndex) + content.substring(lastIndex + 1);
        }
        return content; // Retorna o conteúdo original se não houver '}'
    }

}