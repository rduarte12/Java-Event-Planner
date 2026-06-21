package persistence;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import model.Category;
import model.Event;

// DAO do events.csv (secao 4.1).
// Segue DT01/RNF02: le o arquivo UMA vez no startup e mantem tudo num
// ArrayList em memoria, que e a fonte unica de verdade. Nenhuma navegacao
// rele o disco. Persistencia hibrida (DT03/RNF03): append pra evento novo,
// full rewrite depois de editar/excluir.
public class EventDao {

    public static final String DEFAULT_FILE = "events.csv";
    private static final String HEADER =
            "id,title,date,time,duration,location,description,category,owner,recurrence_id";

    private final Path file;
    private final List<Event> events = new ArrayList<>();

    public EventDao() {
        this(DEFAULT_FILE);
    }

    public EventDao(String filePath) {
        this.file = Paths.get(filePath);
    }

    // chamado uma unica vez no startup. se o arquivo nao existe, cria so
    // com o cabecalho. linhas com formato invalido sao puladas (e avisadas).
    public void load() {
        events.clear();
        try {
            if (!Files.exists(file)) {
                writeHeaderOnly();
                return;
            }
            // junta linhas fisicas em registros logicos (campos com quebra de
            // linha dentro de aspas ocupam mais de uma linha do arquivo)
            List<String> records = CsvUtils.joinRecords(
                    Files.readAllLines(file, StandardCharsets.UTF_8));
            int invalidas = 0;
            for (int i = 0; i < records.size(); i++) {
                if (i == 0) {
                    continue; // pula cabecalho
                }
                String line = records.get(i);
                if (line.isBlank()) {
                    continue;
                }
                try {
                    events.add(parse(line));
                } catch (RuntimeException badLine) {
                    invalidas++; // data/hora/categoria/numero invalido nessa linha
                }
            }
            if (invalidas > 0) {
                CsvUtils.notifyError(invalidas + " linha(s) de " + file
                        + " foram ignoradas por formato invalido.");
            }
        } catch (IOException ex) {
            CsvUtils.notifyError("Nao foi possivel ler o arquivo " + file + ".");
        }
    }

    // colecao viva em memoria - fonte unica de verdade.
    // a UI altera essa lista e depois chama rewriteAll().
    public List<Event> getAll() {
        return events;
    }

    // DT03: evento novo so anexa a linha no fim do arquivo.
    public void append(Event e) {
        events.add(e);
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            w.write(toLine(e));
            w.newLine();
        } catch (IOException ex) {
            CsvUtils.notifyError("Nao foi possivel salvar o evento em " + file + ".");
        }
    }

    // DT03/RNF03: reescreve o CSV inteiro a partir da memoria.
    // usado depois de editar ou excluir eventos.
    public void rewriteAll() {
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write(HEADER);
            w.newLine();
            for (Event e : events) {
                w.write(toLine(e));
                w.newLine();
            }
        } catch (IOException ex) {
            CsvUtils.notifyError("Nao foi possivel reescrever o arquivo " + file + ".");
        }
    }

    // --- helpers internos ---

    private void writeHeaderOnly() throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write(HEADER);
            w.newLine();
        }
    }

    // transforma um Event numa linha de CSV, escapando o que precisar
    private String toLine(Event e) {
        return CsvUtils.toLine(
                CsvUtils.nz(e.getId()),
                CsvUtils.nz(e.getTitle()),
                e.getDate() != null ? CsvUtils.formatDate(e.getDate()) : "",
                e.getTime() != null ? CsvUtils.formatTime(e.getTime()) : "",
                String.valueOf(e.getDuration()),
                CsvUtils.nz(e.getLocation()),
                CsvUtils.nz(e.getDescription()),
                e.getCategory() != null ? e.getCategory().name() : "",
                CsvUtils.nz(e.getOwner()),
                CsvUtils.nz(e.getRecurrenceId()));
    }

    // transforma uma linha de CSV num Event. pode lancar excecao se algum
    // campo estiver mal formatado - quem chama trata pulando a linha.
    private Event parse(String line) {
        List<String> f = CsvUtils.parseLine(line);
        if (f.size() < 9) {
            throw new IllegalArgumentException("linha com colunas faltando");
        }
        Event e = new Event();
        e.setId(f.get(0));
        e.setTitle(f.get(1));
        e.setDate(CsvUtils.parseDate(f.get(2)));
        e.setTime(CsvUtils.parseTime(f.get(3)));
        e.setDuration(Integer.parseInt(f.get(4)));
        e.setLocation(f.get(5));
        e.setDescription(f.get(6));
        e.setCategory(Category.valueOf(f.get(7)));
        e.setOwner(f.get(8));
        // recurrence_id e opcional: vazio vira nulo (evento nao-recorrente)
        String rec = f.size() > 9 ? f.get(9) : "";
        e.setRecurrenceId(rec.isEmpty() ? null : rec);
        return e;
    }
}
