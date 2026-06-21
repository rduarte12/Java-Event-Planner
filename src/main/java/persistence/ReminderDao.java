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

import model.Reminder;

// DAO do reminders.csv (secao 4.2). Mesma ideia do EventDao: read-once no
// startup, colecao em memoria como fonte de verdade, append pra novo e full
// rewrite depois de editar/excluir (DT03/DT06). O reminders.csv e separado
// do events.csv pra manter a relacao 1:N limpa.
public class ReminderDao {

    public static final String DEFAULT_FILE = "reminders.csv";
    private static final String HEADER = "reminder_id,event_id,lead_time_minutes";

    private final Path file;
    private final List<Reminder> reminders = new ArrayList<>();

    public ReminderDao() {
        this(DEFAULT_FILE);
    }

    public ReminderDao(String filePath) {
        this.file = Paths.get(filePath);
    }

    // le o arquivo uma vez no startup. cria so com cabecalho se nao existir.
    public void load() {
        reminders.clear();
        try {
            if (!Files.exists(file)) {
                writeHeaderOnly();
                return;
            }
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
                    reminders.add(parse(line));
                } catch (RuntimeException badLine) {
                    invalidas++;
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

    // colecao viva em memoria - fonte unica de verdade
    public List<Reminder> getAll() {
        return reminders;
    }

    // consulta os reminders de um evento (relacao 1:N por event_id).
    // util pra montar alertas no startup e pra limpar quando o evento e excluido.
    public List<Reminder> findByEventId(String eventId) {
        List<Reminder> out = new ArrayList<>();
        for (Reminder r : reminders) {
            if (r.getEventId() != null && r.getEventId().equals(eventId)) {
                out.add(r);
            }
        }
        return out;
    }

    // reminder novo so anexa no fim
    public void append(Reminder r) {
        reminders.add(r);
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            w.write(toLine(r));
            w.newLine();
        } catch (IOException ex) {
            CsvUtils.notifyError("Nao foi possivel salvar o lembrete em " + file + ".");
        }
    }

    // reescreve o arquivo inteiro a partir da memoria (full rewrite)
    public void rewriteAll() {
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write(HEADER);
            w.newLine();
            for (Reminder r : reminders) {
                w.write(toLine(r));
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

    private String toLine(Reminder r) {
        return CsvUtils.toLine(
                CsvUtils.nz(r.getReminderId()),
                CsvUtils.nz(r.getEventId()),
                String.valueOf(r.getLeadTimeMinutes()));
    }

    private Reminder parse(String line) {
        List<String> f = CsvUtils.parseLine(line);
        if (f.size() < 3) {
            throw new IllegalArgumentException("linha com colunas faltando");
        }
        Reminder r = new Reminder();
        r.setReminderId(f.get(0));
        r.setEventId(f.get(1));
        r.setLeadTimeMinutes(Integer.parseInt(f.get(2)));
        return r;
    }
}
