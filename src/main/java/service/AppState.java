package service;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import model.Event;
import persistence.EventDao;
import persistence.ReminderDao;

// Estado global da aplicacao (DT05). Guarda quem e o Usuario Ativo e serve
// como ponto central pra acessar os DAOs ja carregados em memoria.
// Tudo single-threaded: e criado e usado so na EDT, sem nenhuma thread.
public class AppState {

    // conjunto fixo inicial de usuarios do sistema. mesmo num events.csv vazio
    // sempre tem com quem logar.
    private static final String[] DEFAULT_USERS = {"Alice", "Bruno", "Carla", "Diego", "Eduarda"};

    private final EventDao eventDao;
    private final ReminderDao reminderDao;

    // nome do Usuario Ativo escolhido no login. fica nulo ate o login (RF01).
    private String activeUser;

    public AppState(EventDao eventDao, ReminderDao reminderDao) {
        this.eventDao = eventDao;
        this.reminderDao = reminderDao;
    }

    // lista de usuarios pra tela de login: a base fixa + todos os owners
    // distintos que ja aparecem no events.csv. TreeSet pra ordenar e nao repetir.
    public List<String> getUsers() {
        Set<String> users = new TreeSet<>();
        for (String u : DEFAULT_USERS) {
            users.add(u);
        }
        for (Event e : eventDao.getAll()) {
            String owner = e.getOwner();
            if (owner != null && !owner.isBlank()) {
                users.add(owner);
            }
        }
        return new java.util.ArrayList<>(users);
    }

    public String getActiveUser() {
        return activeUser;
    }

    public void setActiveUser(String activeUser) {
        this.activeUser = activeUser;
    }

    // true depois que o login foi confirmado
    public boolean isLoggedIn() {
        return activeUser != null && !activeUser.isBlank();
    }

    // RF02: carimba o Usuario Ativo como dono do evento. todo evento novo
    // (e cada ocorrencia de uma serie) passa por aqui na hora de criar.
    public void stampOwner(Event e) {
        if (!isLoggedIn()) {
            throw new IllegalStateException("nenhum usuario ativo selecionado");
        }
        e.setOwner(activeUser);
    }

    // RF03 (base): so o dono pode editar/excluir. usado depois pra
    // habilitar/desabilitar os botoes.
    public boolean isOwnedByActiveUser(Event e) {
        return isLoggedIn() && activeUser.equals(e.getOwner());
    }

    public EventDao getEventDao() {
        return eventDao;
    }

    public ReminderDao getReminderDao() {
        return reminderDao;
    }
}
