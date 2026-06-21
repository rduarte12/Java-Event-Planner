// Ponto de entrada da aplicacao Java Event Planner.
// Carrega os dados dos CSV em memoria e abre o login por selecao (RF01).
// A janela principal vem na proxima etapa.

import java.util.List;

import javax.swing.SwingUtilities;

import model.Event;
import persistence.EventDao;
import persistence.ReminderDao;
import service.AppState;
import service.ReminderService;
import ui.LoginDialog;
import ui.MainWindow;
import ui.ReminderAlertDialog;

public class Main {

    // RNF01 / DT02: aplicacao single-threaded.
    // Nao criamos Thread, Runnable nem SwingWorker. Toda a UI roda na EDT
    // nativa do Swing, agendada aqui via SwingUtilities.invokeLater.
    public static void main(String[] args) {
        System.out.println("Java Event Planner - inicializando...");

        SwingUtilities.invokeLater(() -> {
            // DT01/RNF02: le os dois CSV uma unica vez no startup
            EventDao eventDao = new EventDao();
            ReminderDao reminderDao = new ReminderDao();
            eventDao.load();
            reminderDao.load();

            AppState state = new AppState(eventDao, reminderDao);

            // RF01: bloqueia ate o usuario escolher quem e (modal)
            LoginDialog login = new LoginDialog(state);
            login.setVisible(true);

            // so chega aqui depois do login confirmado (modal).
            if (state.isLoggedIn()) {
                System.out.println("Usuario ativo: " + state.getActiveUser());

                // RF13/RF14/RF15: calcula os alertas das proximas 24h e, se houver
                // algum, mostra o JDialog unico ANTES de carregar a tela principal.
                ReminderService reminders = new ReminderService(eventDao, reminderDao);
                List<Event> alerts = reminders.computeStartupAlerts();
                if (!alerts.isEmpty()) {
                    new ReminderAlertDialog(null, alerts).setVisible(true);
                }

                MainWindow main = new MainWindow(state);
                main.setVisible(true);
            }
        });
    }
}
