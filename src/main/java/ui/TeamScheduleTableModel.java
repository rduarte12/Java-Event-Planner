package ui;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import model.Event;

public class TeamScheduleTableModel extends AbstractTableModel {

    private final List<String> teamMembers; // Nomes dos membros da equipe
    private List<Event> events; // Lista de eventos para cada membro da equipe
    private LocalDate date; // Data para a qual a tabela está mostrando os eventos

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    public TeamScheduleTableModel(List<String> users) {
        this.teamMembers = users;
        this.events = new ArrayList<>();
        this.date = LocalDate.now(); // Inicializa com a data atual
    }

    // att os dados quandfo o usuario muda a data no Calendario
    public void updateData(List<Event> events, LocalDate date) {
        this.events = events;
        this.date = date;
        fireTableDataChanged(); // Notifica a tabela que os dados foram atualizados
    }

    @Override
    public int getRowCount() {
        return 48; // 24 horas * 2 slots por hora (slots de 30 min)
    }

    @Override
    public int getColumnCount() {
        return 1 + teamMembers.size(); // Coluna 0 (Horários) + Colunas dos Usuários
    }

    @Override
    public String getColumnName(int column) {
        if (column == 0) {
            return "Horário";
        }
        // Validação: verifica se o índice está dentro dos limites
        if (column < 1 || column > teamMembers.size()) {
            throw new IndexOutOfBoundsException("Coluna " + column + " inválida. Máximo: " + teamMembers.size());
        }
        return teamMembers.get(column - 1);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        // Validação de limites de linha
        if (rowIndex < 0 || rowIndex >= 48) {
            throw new IndexOutOfBoundsException("Linha " + rowIndex + " inválida. Deve estar entre 0 e 47.");
        }

        // Validação de coluna
        if (columnIndex < 0 || columnIndex > teamMembers.size()) {
            throw new IndexOutOfBoundsException("Coluna " + columnIndex + " inválida.");
        }

        LocalTime slotTime = slotIndexToTime(rowIndex);

        // Coluna 0 apenas exibe o texto do horário do slot
        if (columnIndex == 0) {
            return slotTime.format(timeFormatter);
        }

        // Recupera o usuário da coluna atual
        String columnOwner = teamMembers.get(columnIndex - 1);

        // Busca se existe um evento para ESTE usuário, NESTA data, QUE OCUPE este slot
        // de tempo
        return findEventForSlot(columnOwner, date, slotTime);
    }

    // Converte o índice da linha (0 a 47) para um LocalTime real (00:00 a 23:30)
    private LocalTime slotIndexToTime(int rowIndex) {
        int totalMinutes = rowIndex * 30;
        return LocalTime.of(totalMinutes / 60, totalMinutes % 60);
    }

    // Regra de negócio: Varre a memória filtrando o evento correspondente
    private Event findEventForSlot(String owner, LocalDate date, LocalTime time) {
        if (owner == null || owner.isBlank() || date == null || time == null) {
            return null; // Protege contra valores inválidos
        }

        for (Event event : this.events) {
            if (event == null) {
                continue; // Pula eventos null
            }

            // Verifica se pertence ao mesmo dono e mesmo dia
            if (event.getOwner() != null && event.getOwner().equalsIgnoreCase(owner) &&
                    event.getDate() != null && event.getDate().equals(date)) {

                LocalTime eventStart = event.getTime();
                if (eventStart == null) {
                    continue; // Pula se horário for null
                }

                int duration = event.getDuration();
                if (duration < 0) {
                    continue; // Pula se duração for inválida
                }

                LocalTime eventEnd = eventStart.plusMinutes(duration);

                // Verifica se o slot atual está dentro do intervalo do evento [Início, Fim)
                if ((time.equals(eventStart) || time.isAfter(eventStart)) && time.isBefore(eventEnd)) {
                    return event; // Retorna o objeto inteiro para o Renderer usar
                }
            }
        }
        return null; // Célula vazia
    }
}