package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import service.AppState;

// Janela principal (RF04/RF05). Divide a tela: calendario mensal a esquerda
// e um painel de abas a direita. Diaria e Semanal ja funcionam reagindo ao
// dia selecionado; a aba de Equipe ainda e placeholder.
public class MainWindow extends JFrame {

    private final AppState state;
    private final CalendarPanel calendarPanel;
    private final JTabbedPane tabs;
    private final DailyView dailyView;
    private final WeeklyView weeklyView;
    private final TeamView teamView;

    public MainWindow(AppState state) {
        super("Java Event Planner - " + state.getActiveUser());
        this.state = state;

        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // esquerda: calendario lendo a colecao em memoria
        calendarPanel = new CalendarPanel(state.getEventDao());

        // direita: abas. Diaria e Semanal leem o mesmo ArrayList em memoria.
        dailyView = new DailyView(state.getEventDao());
        weeklyView = new WeeklyView(state.getEventDao());
        teamView = new TeamView(state.getEventDao(), state.getUsers());

        tabs = new JTabbedPane();
        tabs.addTab("Visao Diaria", dailyView);
        tabs.addTab("Visao Semanal", weeklyView);
        tabs.addTab("Visao de Equipe (Persons)", teamView);

        // RF06/RF07/US03: clicar num dia atualiza as duas visoes na hora,
        // de forma sincrona na EDT (sem releitura de disco).
        calendarPanel.setDaySelectionListener(date -> {
            dailyView.showDay(date);
            weeklyView.showWeek(date);
            teamView.showDay(date);
        });

        // ja inicia mostrando o dia selecionado de partida (hoje)
        LocalDate inicial = calendarPanel.getSelectedDate();
        dailyView.showDay(inicial);
        weeklyView.showWeek(inicial);
        teamView.showDay(inicial);

        // barra de cima: criar evento (RF09) e buscar (RF12)
        JButton newEvent = new JButton("Novo Evento");
        newEvent.addActionListener(e -> openEventForm());
        JButton search = new JButton("Buscar");
        search.addActionListener(e -> openSearch());
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(newEvent);
        toolbar.add(search);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, calendarPanel, tabs);
        split.setDividerLocation(340);

        JPanel root = new JPanel(new BorderLayout());
        root.add(toolbar, BorderLayout.NORTH);
        root.add(split, BorderLayout.CENTER);
        setContentPane(root);

        setSize(900, 600);
        setMinimumSize(new java.awt.Dimension(700, 450));
        setLocationRelativeTo(null);
    }

    // abre o formulario de criacao ja com o dia selecionado preenchido.
    // depois de salvar, repinta o calendario e as duas visoes.
    private void openEventForm() {
        LocalDate dia = calendarPanel.getSelectedDate();
        EventFormDialog dialog = new EventFormDialog(this, state, dia, this::refreshViews);
        dialog.setVisible(true);
    }

    // abre a busca modal sobre a tela principal (RF12)
    private void openSearch() {
        SearchDialog dialog = new SearchDialog(this, state.getEventDao());
        dialog.setVisible(true);
    }

    // atualiza tudo a partir da colecao em memoria (apos criar evento)
    private void refreshViews() {
        calendarPanel.refresh();
        LocalDate sel = calendarPanel.getSelectedDate();
        dailyView.showDay(sel);
        weeklyView.showWeek(sel);
        teamView.showDay(sel);
    }

    // painel temporario pra aba de Equipe enquanto ela nao existe
    /* private JComponent placeholder(String name) {
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JLabel(name + " (em construcao)", SwingConstants.CENTER), BorderLayout.CENTER);
        return p;
    } */
}
