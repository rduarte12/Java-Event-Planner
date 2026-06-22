package ui;

import java.awt.BorderLayout;
import java.time.LocalDate;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import persistence.EventDao;

public class TeamView extends JPanel {

    private final TeamScheduleTableModel tableModel;
    private final JTable scheduleTable;
    private final EventDao eventDao;

    // Recebe o DAO e a lista de usuários cadastrados no sistema
    public TeamView(EventDao eventDao, List<String> allUsers) {
        super(new BorderLayout());
        this.eventDao = eventDao;

        // 1. Instancia o modelo da tabela com os usuários
        tableModel = new TeamScheduleTableModel(allUsers);
        
        // 2. Cria a tabela baseada no modelo
        scheduleTable = new JTable(tableModel);
        
        // 3. Aplica o renderizador visual customizado em todas as colunas
        TeamScheduleCellRenderer renderer = new TeamScheduleCellRenderer();
        for (int i = 0; i < scheduleTable.getColumnCount(); i++) {
            scheduleTable.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        // 4. Configurações estéticas do JTable
        scheduleTable.setRowHeight(30); 
        scheduleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scheduleTable.setCellSelectionEnabled(true);
        scheduleTable.getTableHeader().setReorderingAllowed(false); // Impede o usuário de arrastar colunas

        // 5. Envelopa no JScrollPane (isso cria os sliders automaticamente)
        JScrollPane scrollPane = new JScrollPane(scheduleTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    // Método espelho ao showDay do DailyView
    public void showDay(LocalDate date) {
        // Busca todos os eventos da memória e atualiza o TableModel
        tableModel.updateData(eventDao.getAll(), date);
    }
}