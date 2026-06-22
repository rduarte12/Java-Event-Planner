package model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// Modelo de um evento. Cada objeto corresponde a uma linha do events.csv.
// Os nomes/campos batem certinho com as colunas do arquivo (secao 4.1 da spec).
// Eventos recorrentes nao tem classe propria: cada ocorrencia e um Event
// completo, todos compartilhando o mesmo recurrence_id (DT07).
public class Event {

    private String id;             // UUID gerado automatico, chave primaria
    private String title;          // titulo, nao pode ser vazio
    private LocalDate date;        // data no formato YYYY-MM-DD
    private LocalTime time;        // hora no formato HH:mm (24h)
    private int duration;          // duracao do evento em minutos
    private String location;       // local do evento (opcional)
    private String description;    // descricao livre (opcional)
    private Category category;     // MEETING, BIRTHDAY ou APPOINTMENT
    private String owner;          // nome do Usuario Ativo na hora da criacao
    private String recurrenceId;   // UUID da serie; vazio/nulo se nao for recorrente

    public Event() {
        // construtor vazio - os campos sao preenchidos pelos setters
    }

    // junta date + time num LocalDateTime so.
    // util pra logica de lembretes e ordenacao por horario depois.
    public LocalDateTime eventDateTime() {
        return LocalDateTime.of(date, time);
    }

    // getters e setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getRecurrenceId() {
        return recurrenceId;
    }

    public void setRecurrenceId(String recurrenceId) {
        this.recurrenceId = recurrenceId;
    }

    @Override
    public String toString() {
        // Garante que não vai dar erro caso algum campo esteja nulo
        String dataFormatada = (date != null) ? date.toString() : "Sem data";
        String horaFormatada = (time != null) ? time.toString() : "--:--";
        String categoriaFmt = (category != null) ? category.name() : "Sem categoria";
        
        // Retorna um formato elegante. Ex: "2026-06-25 14:00 - Reunião de Pauta [MEETING]"
        return String.format("%s %s - %s [%s]", dataFormatada, horaFormatada, title, categoriaFmt);
    }
}
