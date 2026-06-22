package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.time.LocalDate;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import model.Event;
import persistence.EventDao;
import service.AppState;

// Busca modal (RF12). Fica por cima da tela principal e filtra os eventos em
// memoria em tempo real (a cada tecla) pelos campos titulo, descricao e local.
// Os resultados sao somente leitura: nao tem botao de editar nem deletar aqui.
public class SearchDialog extends JDialog {

    private final EventDao eventDao;
    private final JTextField queryField = new JTextField(28);
    private final DefaultListModel<Event> listModel = new DefaultListModel<>();
    private final JList<Event> resultList = new JList<>(listModel);
    private final AppState state;
    private final JButton btnEdit = new JButton("Editar");
    private final JButton btnDelete = new JButton("Excluir");
    private final JLabel statusLabel = new JLabel("Digite uma palavra-chave para buscar...");

    public SearchDialog(Frame owner, EventDao eventDao, AppState state) {
        super(owner, "Buscar e Gerenciar eventos", true);
        this.eventDao = eventDao;
        this.state = state;
        

        JPanel top = new JPanel(new BorderLayout(6, 4));
        top.setBorder(BorderFactory.createEmptyBorder(10, 12, 6, 12));
        JPanel searchPanel = new JPanel(new BorderLayout(6, 0));
        searchPanel.add(new JLabel("Palavra-chave:"), BorderLayout.WEST);
        searchPanel.add(queryField, BorderLayout.CENTER);

        top.add(searchPanel, BorderLayout.NORTH);
        top.add(statusLabel, BorderLayout.SOUTH); // Aviso fixo abaixo da barra

        // Estiliza o statusLabel 
        statusLabel.setForeground(Color.GRAY);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(resultList);
        scroll.setPreferredSize(new Dimension(560, 320));
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnEdit.setEnabled(false);
        btnDelete.setEnabled(false);
        bottomPanel.add(btnEdit);
        bottomPanel.add(btnDelete);

        // Lógica de Segurança (RF03 e RF10/11)
        resultList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Event selectedEvent = resultList.getSelectedValue();
                if (selectedEvent != null) {
                    // Verifica se o usuário ativo é o dono do evento selecionado
                    boolean isOwner = selectedEvent.getOwner().equals(state.getActiveUser());
                    btnEdit.setEnabled(isOwner);
                    btnDelete.setEnabled(isOwner);
                } else {
                    btnEdit.setEnabled(false);
                    btnDelete.setEnabled(false);
                }
            }
        });

        // Ações dos botões (esqueletos)
        btnDelete.addActionListener(e -> deleteSelectedEvent());
        btnEdit.addActionListener(e -> editSelectedEvent());

        // busca em tempo real: qualquer mudanca no texto refiltra
        queryField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refresh(); }
            @Override public void removeUpdate(DocumentEvent e) { refresh(); }
            @Override public void changedUpdate(DocumentEvent e) { refresh(); }
        });

        JPanel content = new JPanel(new BorderLayout());
        content.add(top, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        setContentPane(content);
        content.add(bottomPanel, BorderLayout.SOUTH);

        refresh(); // estado inicial (campo vazio)
        pack();
        setLocationRelativeTo(owner);
    }

    // refiltra os eventos conforme o texto e remonta a lista de resultados
    private void refresh() {
        listModel.clear(); // Limpa a lista de resultados
        String q = queryField.getText().trim().toLowerCase(Locale.ROOT);

        if (q.isEmpty()) {
            statusLabel.setText("Digite uma palavra-chave para buscar...");
        } else {
            int achados = 0;
            for (Event e : eventDao.getAll()) {
                if (matches(e, q)) {
                    listModel.addElement(e); // Adiciona apenas o objeto Event
                    achados++;
                }
            }

            if (achados == 0) {
                statusLabel.setText("Nenhum evento encontrado.");
            } else {
                statusLabel.setText(achados + " evento(s) encontrado(s)."); // Bônus de usabilidade
            }
        }

        // Garante que os botões de ação travem se a busca for alterada
        btnEdit.setEnabled(false);
        btnDelete.setEnabled(false);
    }

    // confere se a palavra aparece no titulo, descricao ou local
    private static boolean matches(Event e, String q) {
        return contains(e.getTitle(), q)
                || contains(e.getDescription(), q)
                || contains(e.getLocation(), q);
    }

    private static boolean contains(String field, String q) {
        return field != null && field.toLowerCase(Locale.ROOT).contains(q);
    }

    private void deleteSelectedEvent() {
        Event selected = resultList.getSelectedValue();
        if (selected == null) return;

        boolean deleteSeries = false;
        
        // RF24: Se o evento faz parte de uma série, apresenta opções modais de exclusão
        if (selected.getRecurrenceId() != null) {
            String[] options = {"Somente esta", "Esta e as futuras", "Cancelar"};
            int choice = JOptionPane.showOptionDialog(this,
                    "Este evento faz parte de uma série recorrente.\nDeseja excluir somente esta ocorrência ou esta e todas as futuras?",
                    "Excluir Série Recorrente",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            
            if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) {
                return; // Aborta
            }
            if (choice == 1) {
                deleteSeries = true;
            }
        } else {
            // Confirmação simples para eventos comuns
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Tem certeza que deseja excluir o evento '" + selected.getTitle() + "'?",
                    "Confirmar Exclusão",
                    JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
        }

        if (deleteSeries) {
            String recId = selected.getRecurrenceId();
            LocalDate baseDate = selected.getDate();
            
            // Filtra os IDs das ocorrências futuras da série para limpar a cascata de lembretes
            java.util.List<String> idsToRemove = eventDao.getAll().stream()
                    .filter(e -> recId.equals(e.getRecurrenceId()) && !e.getDate().isBefore(baseDate))
                    .map(Event::getId)
                    .collect(java.util.stream.Collectors.toList());

            // Executa a remoção em massa na memória (DT08)
            eventDao.getAll().removeIf(e -> recId.equals(e.getRecurrenceId()) && !e.getDate().isBefore(baseDate));
            state.getReminderDao().getAll().removeIf(r -> idsToRemove.contains(r.getEventId()));
        } else {
            // Remoção isolada da ocorrência selecionada
            eventDao.getAll().remove(selected);
            state.getReminderDao().getAll().removeIf(r -> r.getEventId().equals(selected.getId()));
        }

        // Sincroniza as alterações no disco usando reescrita completa
        eventDao.rewriteAll();
        state.getReminderDao().rewriteAll();

        refresh(); // Atualiza o JList instantaneamente
        JOptionPane.showMessageDialog(this, "Operação realizada com sucesso.");
    }

    private void editSelectedEvent() {
        Event selected = resultList.getSelectedValue();
        if (selected == null) return;

        // PASSAMOS o 'selected' como o 4º parâmetro para ativar o modo de edição
        EventFormDialog dialog = new EventFormDialog((Frame) this.getOwner(), state, selected.getDate(), selected, () -> {
            refresh(); // Quando fechar a edição, limpa a JList e traz as informações novas
        });
        
        dialog.setVisible(true);
    }
}
