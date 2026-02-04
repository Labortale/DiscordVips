# Discord ↔ Hytale LuckPerms Sync Plugin

Questo plugin Hytale integra Discord (JDA) con LuckPerms per sincronizzare automaticamente
i ruoli Discord (assegnati per esempio dal bot ufficiale Ko-fi) con i gruppi LuckPerms sul server Hytale.

Il plugin è self-contained: il bot Discord gira dentro lo stesso JAR del plugin Hytale.
Non esistono servizi esterni, webhook o microservizi separati.

---

## Obiettivo del plugin

- Collegare in modo verificabile un account Hytale ↔ Discord
- Rilevare aggiunte e rimozioni di ruoli Discord
- Tradurre i ruoli Discord in gruppi LuckPerms
- Garantire coerenza anche in caso di eventi persi
- Mantenere la persistenza al minimo indispensabile (SQLite via ORMLite)
- Il bot opera solo sulle guild specificate nella configurazione (`allowedGuildIds`), ignorando qualsiasi altro server Discord

---

## Flusso funzionale generale

1. Il giocatore Hytale esegue il comando /linkdiscord
2. Il server genera un codice temporaneo monouso
3. L’utente invia /link <codice> su Discord
4. Il bot Discord:
    - valida il codice
    - crea il collegamento Discord ↔ UUID Hytale
    - esegue una sincronizzazione immediata dei ruoli (differenziale)
5. Ogni variazione di ruolo Discord aggiorna LuckPerms
6. Un task periodico esegue una full sync di sicurezza

---

## Componenti principali

### LinkDiscordCommand

Responsabilità:
- Espone il comando Hytale /linkdiscord
- Genera un codice temporaneo con scadenza
- Registra il codice nel PendingLinkDatabase

Caratteristiche:
- Utilizzabile solo dai giocatori
- Codice valido 5 minuti
- Nessuna dipendenza diretta da Discord o LuckPerms

Motivazione:
- Separare la fase di inizializzazione del link dalla logica Discord
- Evitare accessi Discord dal contesto di comando Hytale

---
### DiscordBot

Classe centrale del sistema.

Responsabilità:
- Avvia e gestisce JDA
- Gestisce il comando Discord /link
- Ascolta eventi di aggiunta e rimozione ruoli
- Traduce lo stato Discord nello stato LuckPerms (differenziale)

Il bot gira nello stesso processo del server Hytale:
- condivide database
- condivide l’istanza LuckPerms
- non richiede IPC, HTTP o servizi esterni

---

## Linking Discord ↔ Hytale

### PendingLinkDatabase

Funzione:
- Memorizza temporaneamente i codici di link
- Garantisce:
    - uso singolo
    - scadenza temporale automatica

Implementazione fornita:
- InMemoryPendingLinkDatabase

Scelte progettuali:
- In-memory perché i codici non devono sopravvivere a un riavvio
- Riduzione della complessità e dell’I/O
- Sicurezza sufficiente per un handshake manuale
- I codici scadono automaticamente ogni 10 minuti se hanno superato il TTL

---

### Database (LinkDatabase)

Funzione:
- Persistenza duratura del collegamento Discord ↔ Hytale
- Garantisce relazione 1:1

Implementazione:
- OrmLiteLinkDatabase
- Tabella: discord_links

Entità DiscordLinkEntity:
- hytaleUuid (chiave primaria)
- discordId (univoco)
- linkedAt (timestamp)

Comportamento:
- Ogni nuovo link rimuove automaticamente link precedenti
- Il link sopravvive ai riavvii del server
- Garantisce unicità di UUID Hytale e ID Discord

Motivazione:
- Il collegamento è uno stato stabile e non temporaneo

---

## Sincronizzazione ruoli ↔ gruppi

### Logica di sincronizzazione

Per ogni membro Discord linkato:
- Lettura di tutti i ruoli Discord correnti
- Normalizzazione dei nomi (lowercase)
- Caricamento dell’utente LuckPerms
- Aggiornamento differenziale dei nodi group.*:
    - aggiunge solo i gruppi mancanti
    - rimuove solo i gruppi non più presenti
- Salvataggio dell’utente

Assunzione:
- Esiste una corrispondenza diretta tra nome del ruolo Discord e gruppo LuckPerms

Nota:
- Non viene fatta distinzione tra ruoli “tier” e non
- Tutta la gestione è basata sul prefisso group.*

---

### Eventi Discord

Eventi gestiti:
- GuildMemberRoleAddEvent
- GuildMemberRoleRemoveEvent

Comportamento:
- Ogni evento triggera una sincronizzazione differenziale del membro
- La sincronizzazione aggiorna solo i gruppi che devono cambiare, minimizzando scritture inutili su LuckPerms
- Lo stato LuckPerms viene sempre allineato a quello Discord corrente

Motivazione:
- Riduzione degli edge case
- Gli eventi Discord non garantiscono ordine o completezza

---

## Full Sync periodica

Funzione:
- Scorre tutte le guild Discord whitelistate
- Per ogni membro linkato riesegue la sincronizzazione differenziale

Scopo:
- Recuperare eventuali eventi persi
- Gestire riavvii del bot o del gateway Discord
- Garantire convergenza finale dello stato

Ottimizzazione:
- Itera solo sui membri linkati, riducendo carico e rischi di rate-limit su Discord

Costo:
- O(n membri Discord)
- Pensata per intervalli lunghi e controllati

---

## Scelte architetturali chiave

- Bot embedded nel plugin
- Event-driven con reconciliation periodica
- Persistenza minimale e mirata
- Separazione netta delle responsabilità
- Nessuna dipendenza da servizi esterni

---

## Estensioni previste

- Supporto multi-guild con mapping dedicato [low priority]
- Comandi amministrativi di resync manuale

---

## Stato del progetto

Il plugin è funzionalmente completo.
La struttura è pensata per essere estesa senza rifattorizzazioni invasive.
Questa documentazione è pensata per manutenzione interna e onboarding tecnico.