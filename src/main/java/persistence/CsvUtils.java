package persistence;

import java.awt.GraphicsEnvironment;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.swing.JOptionPane;

// Helpers usados pelos DAOs pra mexer com CSV.
// Coisas de baixo nivel: gerar id, escapar/desescapar campo, formatar datas.
// Tudo metodo estatico, nao faz sentido instanciar isso.
public final class CsvUtils {

    private CsvUtils() {
        // classe so de utilitarios, nao instancia
    }

    // formatos fixos definidos na spec (secao 4.1)
    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // gera o UUID que vira chave primaria de event/reminder
    public static String newUuid() {
        return UUID.randomUUID().toString();
    }

    // --- datas e horas ---

    public static String formatDate(LocalDate d) {
        return d.format(DATE_FMT);
    }

    public static String formatTime(LocalTime t) {
        return t.format(TIME_FMT);
    }

    // LocalDate.parse ja aceita ISO YYYY-MM-DD direto
    public static LocalDate parseDate(String s) {
        return LocalDate.parse(s);
    }

    // LocalTime.parse aceita HH:mm direto
    public static LocalTime parseTime(String s) {
        return LocalTime.parse(s);
    }

    // --- escape / unescape de campo (regra RFC 4180) ---

    // se o campo tem virgula, aspas ou quebra de linha, envolve em aspas duplas
    // e duplica as aspas internas. senao devolve igual.
    public static String escape(String field) {
        if (field == null) {
            return "";
        }
        boolean precisaAspas = field.contains(",") || field.contains("\"")
                || field.contains("\n") || field.contains("\r");
        if (precisaAspas) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    // monta uma linha de CSV a partir dos campos ja escapando cada um
    public static String toLine(String... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(escape(fields[i]));
        }
        return sb.toString();
    }

    // um campo entre aspas pode ter quebra de linha dentro, entao uma "linha
    // logica" do CSV as vezes ocupa varias linhas fisicas do arquivo. aqui a
    // gente junta as linhas fisicas de volta em registros completos: enquanto
    // o numero de aspas estiver impar (aspas abertas), continua juntando.
    public static List<String> joinRecords(List<String> physicalLines) {
        List<String> registros = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        boolean aspasAbertas = false;
        for (String line : physicalLines) {
            if (buf.length() > 0) {
                buf.append('\n'); // restaura a quebra que estava dentro do campo
            }
            buf.append(line);
            for (int i = 0; i < line.length(); i++) {
                if (line.charAt(i) == '"') {
                    aspasAbertas = !aspasAbertas;
                }
            }
            if (!aspasAbertas) {
                registros.add(buf.toString());
                buf.setLength(0);
            }
        }
        if (buf.length() > 0) {
            // arquivo terminou com aspas abertas (malformado) - guarda do jeito que veio
            registros.add(buf.toString());
        }
        return registros;
    }

    // quebra uma linha de CSV em campos respeitando as aspas.
    // dentro de aspas, "" vira uma aspa literal e a virgula nao separa.
    public static List<String> parseLine(String line) {
        List<String> campos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean dentroAspas = false;
        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (dentroAspas) {
                if (c == '"') {
                    // aspa dupla seguida = aspa literal
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        atual.append('"');
                        i += 2;
                    } else {
                        dentroAspas = false;
                        i++;
                    }
                } else {
                    atual.append(c);
                    i++;
                }
            } else {
                if (c == '"') {
                    dentroAspas = true;
                    i++;
                } else if (c == ',') {
                    campos.add(atual.toString());
                    atual.setLength(0);
                    i++;
                } else {
                    atual.append(c);
                    i++;
                }
            }
        }
        campos.add(atual.toString());
        return campos;
    }

    // troca nulo por string vazia, pra nao escrever "null" no CSV
    public static String nz(String s) {
        return s == null ? "" : s;
    }

    // RNF05: avisa o erro sem nunca jogar stack trace na cara do usuario.
    // em ambiente sem tela (headless) cai pro stderr so com a mensagem.
    static void notifyError(String message) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println(message);
        } else {
            JOptionPane.showMessageDialog(null, message, "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
