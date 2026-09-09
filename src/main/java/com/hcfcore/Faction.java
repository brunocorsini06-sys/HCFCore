package com.hcfcore;
import org.bukkit.Location;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
public class Faction {
    // =========================================================
    // RANGOS
    // =========================================================
    public enum FactionRole {
        LEADER(3),
        CO_LEADER(2),
        CAPTAIN(1),
        MEMBER(0);
        private final int weight;
        FactionRole(int weight) {
            this.weight = weight;
        }
        public int getWeight() {
            return weight;
        }
        public boolean isHigherThan(FactionRole role) {
            return role != null && weight > role.weight;
        }
        public boolean isAtLeast(FactionRole role) {
            return role != null && weight >= role.weight;
        }
    }
    // =========================================================
    // PERMISOS
    // =========================================================
    public enum FactionPermission {
        INVITE,
        KICK,
        PROMOTE,
        DEMOTE,
        SET_HOME,
        MANAGE_RELATIONS,
        MANAGE_CLAIMS,
        WITHDRAW,
        DISBAND
    }
    // =========================================================
    // INFORMACIÓN
    // =========================================================
    private final String name;
    private final UUID leader;
    private final Set<UUID> members = new HashSet<>();
    private final Set<UUID> coLeaders = new HashSet<>();
    private final Set<UUID> captains = new HashSet<>();
    private final Set<String> allies = new HashSet<>();
    private final Set<String> enemies = new HashSet<>();
    // =========================================================
    // DTR
    // =========================================================
    private double dtr;
    private double maxDtr;
    private boolean dtrFrozen;
    /*
     * Última regeneración.
     *
     * No se guarda en SQLite porque es un dato temporal
     * de funcionamiento del servidor.
     */
    private long lastDtrRegeneration;
    // =========================================================
    // ECONOMÍA
    // =========================================================
    private double balance;
    // =========================================================
    // HOME
    // =========================================================
    private Location home;
    // =========================================================
    // CONSTRUCTOR COMPATIBLE
    // =========================================================
    public Faction(
            String name,
            UUID leader,
            double startingDtr
    ) {
        this(
                name,
                leader,
                startingDtr,
                5.0
        );
    }
    // =========================================================
    // CONSTRUCTOR PRINCIPAL
    // =========================================================
    public Faction(
            String name,
            UUID leader,
            double startingDtr,
            double maxDtr
    ) {
        this.name = name;
        this.leader = leader;
        if (Double.isNaN(maxDtr)
                || Double.isInfinite(maxDtr)
                || maxDtr <= 0.0) {
            maxDtr = 5.0;
        }
        this.maxDtr = maxDtr;
        if (Double.isNaN(startingDtr)
                || Double.isInfinite(startingDtr)) {
            startingDtr = 0.0;
        }
        this.dtr = Math.max(
                0.0,
                Math.min(
                        startingDtr,
                        this.maxDtr
                )
        );
        this.balance = 0.0;
        this.dtrFrozen = false;
        this.lastDtrRegeneration =
                System.currentTimeMillis();
        if (leader != null) {
            members.add(leader);
        }
    }
    // =========================================================
    // INFORMACIÓN
    // =========================================================
    public String getName() {
        return name;
    }
    public UUID getLeader() {
        return leader;
    }
    // =========================================================
    // LEADER
    // =========================================================
    public boolean isLeader(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        return leader != null
                && leader.equals(uuid);
    }
    // =========================================================
    // MIEMBROS
    // =========================================================
    public Set<UUID> getMembers() {
        return members;
    }
    public boolean isMember(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        return members.contains(uuid);
    }
    public boolean addMember(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        return members.add(uuid);
    }
    public void removeMember(UUID uuid) {
        if (uuid == null) {
            return;
        }
        /*
         * El Leader nunca puede ser eliminado
         * mediante este método.
         */
        if (isLeader(uuid)) {
            return;
        }
        members.remove(uuid);
        coLeaders.remove(uuid);
        captains.remove(uuid);
    }
    // =========================================================
    // CO-LEADERS
    // =========================================================
    public Set<UUID> getCoLeaders() {
        return coLeaders;
    }
    public boolean isCoLeader(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        return coLeaders.contains(uuid);
    }

    public boolean promoteToCoLeader(UUID uuid) {
    if (uuid == null) {
        return false;
    }
    if (!members.contains(uuid)) {
        return false;
    }
    if (isLeader(uuid)) {
        return false;
    }

    captains.remove(uuid);
    return coLeaders.add(uuid);
}
    public void demoteFromCoLeader(UUID uuid) {
        if (uuid == null) {
            return;
        }
        if (isLeader(uuid)) {
            return;
        }
        coLeaders.remove(uuid);
    }
    // =========================================================
    // CAPTAINS
    // =========================================================
    public Set<UUID> getCaptains() {
        return captains;
    }
    public boolean isCaptain(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        return captains.contains(uuid);
    }
    public void promoteToCaptain(UUID uuid) {
        if (uuid == null) {
            return;
        }
        if (!members.contains(uuid)) {
            return;
        }
        if (isLeader(uuid)
                || isCoLeader(uuid)) {
            return;
        }
        captains.add(uuid);
    }
    public void demoteFromCaptain(UUID uuid) {
        if (uuid == null) {
            return;
        }
        if (isLeader(uuid)
                || isCoLeader(uuid)) {
            return;
        }
        captains.remove(uuid);
    }
    // =========================================================
    // COMPATIBILIDAD CON OFFICER
    // =========================================================
    /*
     * Estos métodos se mantienen temporalmente para evitar
     * romper otros archivos del core que todavía puedan
     * llamarlos.
     *
     * En la nueva estructura, Officer equivale a Captain.
     */
    public Set<UUID> getOfficers() {
        return captains;
    }
    public boolean isOfficer(UUID uuid) {
        return isCaptain(uuid);
    }
    public void promote(UUID uuid) {
        promoteToCaptain(uuid);
    }
    public void demote(UUID uuid) {
        demoteFromCaptain(uuid);
    }
    // =========================================================
    // RANGO DE UN JUGADOR
    // =========================================================
    public FactionRole getRole(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        if (isLeader(uuid)) {
            return FactionRole.LEADER;
        }
        if (isCoLeader(uuid)) {
            return FactionRole.CO_LEADER;
        }
        if (isCaptain(uuid)) {
            return FactionRole.CAPTAIN;
        }
        if (isMember(uuid)) {
            return FactionRole.MEMBER;
        }
        return null;
    }
    public boolean hasRole(
            UUID uuid,
            FactionRole role
    ) {
        if (uuid == null || role == null) {
            return false;
        }
        return getRole(uuid) == role;
    }
    public boolean hasAtLeastRole(
            UUID uuid,
            FactionRole role
    ) {
        FactionRole current =
                getRole(uuid);
        if (current == null
                || role == null) {
            return false;
        }
        return current.isAtLeast(role);
    }
    // =========================================================
    // PERMISOS
    // =========================================================
    public boolean hasPermission(
            UUID uuid,
            FactionPermission permission
    ) {
        if (uuid == null
                || permission == null) {
            return false;
        }
        FactionRole role =
                getRole(uuid);
        if (role == null) {
            return false;
        }
        /*
         * Leader:
         * todos los permisos.
         */
        if (role == FactionRole.LEADER) {
            return true;
        }
        /*
         * Co-Leader:
         * casi todos los permisos.
         * No puede disbandear.
         */
        if (role == FactionRole.CO_LEADER) {
            return permission
                    != FactionPermission.DISBAND;
        }
        /*
         * Captain:
         * administración intermedia.
         */
        if (role == FactionRole.CAPTAIN) {
            return permission
                    == FactionPermission.INVITE
                    || permission
                    == FactionPermission.KICK
                    || permission
                    == FactionPermission.SET_HOME
                    || permission
                    == FactionPermission.MANAGE_RELATIONS;
        }
        /*
         * Member:
         * sin permisos administrativos.
         */
        return false;
    }
    // =========================================================
    // DTR
    // =========================================================
    public double getDtr() {
        return dtr;
    }
    public void setDtr(double dtr) {
        if (Double.isNaN(dtr)
                || Double.isInfinite(dtr)) {
            return;
        }
        this.dtr = Math.max(
                0.0,
                Math.min(
                        dtr,
                        maxDtr
                )
        );
    }
    public boolean isRaidable() {
        return dtr <= 0.0;
    }
    public boolean isDtrPositive() {
        return dtr > 0.0;
    }
    // =========================================================
    // DTR MÁXIMO
    // =========================================================
    public double getMaxDtr() {
        return maxDtr;
    }
    public void setMaxDtr(double maxDtr) {
        if (Double.isNaN(maxDtr)
                || Double.isInfinite(maxDtr)
                || maxDtr <= 0.0) {
            return;
        }
        this.maxDtr = maxDtr;
        if (dtr > this.maxDtr) {
            dtr = this.maxDtr;
        }
    }
    // =========================================================
    // DTR REMOVE
    // =========================================================
    public void removeDtr(double amount) {
        if (Double.isNaN(amount)
                || Double.isInfinite(amount)
                || amount <= 0.0) {
            return;
        }
        dtr = Math.max(
                0.0,
                dtr - amount
        );
    }
    // =========================================================
    // DTR ADD
    // =========================================================
    public void addDtr(double amount) {
        if (Double.isNaN(amount)
                || Double.isInfinite(amount)
                || amount <= 0.0) {
            return;
        }
        dtr = Math.min(
                maxDtr,
                dtr + amount
        );
    }
    // =========================================================
    // DTR FREEZE
    // =========================================================
    public boolean isDtrFrozen() {
        return dtrFrozen;
    }
    public void setDtrFrozen(boolean frozen) {
        this.dtrFrozen = frozen;
    }
    public void freezeDtr() {
        this.dtrFrozen = true;
    }
    public void unfreezeDtr() {
        this.dtrFrozen = false;
    }
    // =========================================================
    // DTR REGENERACIÓN
    // =========================================================
    public long getLastDtrRegeneration() {
        return lastDtrRegeneration;
    }
    public void setLastDtrRegeneration(
            long timestamp
    ) {
        if (timestamp < 0L) {
            return;
        }
        this.lastDtrRegeneration = timestamp;
    }
    public boolean canRegenerateDtr() {
        return !dtrFrozen
                && !isRaidable()
                && dtr < maxDtr;
    }
    // =========================================================
    // BALANCE
    // =========================================================
    public double getBalance() {
        return balance;
    }
    public void setBalance(double balance) {
        if (Double.isNaN(balance)
                || Double.isInfinite(balance)) {
            return;
        }
        this.balance = Math.max(
                0.0,
                balance
        );
    }
    public void deposit(double amount) {
        if (Double.isNaN(amount)
                || Double.isInfinite(amount)
                || amount <= 0.0) {
            return;
        }
        balance += amount;
    }
    public boolean withdraw(double amount) {
        if (Double.isNaN(amount)
                || Double.isInfinite(amount)
                || amount <= 0.0) {
            return false;
        }
        if (balance < amount) {
            return false;
        }
        balance -= amount;
        return true;
    }
    // =========================================================
    // ALLIES
    // =========================================================
    public Set<String> getAllies() {
        return allies;
    }
    public void addAlly(String faction) {
        if (faction == null
                || faction.isBlank()) {
            return;
        }
        allies.add(
                faction.toLowerCase(Locale.ROOT)
        );
    }
    public void removeAlly(String faction) {
        if (faction == null) {
            return;
        }
        allies.remove(
                faction.toLowerCase(Locale.ROOT)
        );
    }
    public boolean isAlly(String faction) {
        if (faction == null) {
            return false;
        }
        return allies.contains(
                faction.toLowerCase(Locale.ROOT)
        );
    }
    // =========================================================
    // ENEMIES
    // =========================================================
    public Set<String> getEnemies() {
        return enemies;
    }
    public void addEnemy(String faction) {
        if (faction == null
                || faction.isBlank()) {
            return;
        }
        enemies.add(
                faction.toLowerCase(Locale.ROOT)
        );
    }
    public void removeEnemy(String faction) {
        if (faction == null) {
            return;
        }
        enemies.remove(
                faction.toLowerCase(Locale.ROOT)
        );
    }
    public boolean isEnemy(String faction) {
        if (faction == null) {
            return false;
        }
        return enemies.contains(
                faction.toLowerCase(Locale.ROOT)
        );
    }
    // =========================================================
    // HOME
    // =========================================================
    public Location getHome() {
        if (home == null) {
            return null;
        }
        return home.clone();
    }
    public void setHome(Location home) {
        if (home == null
                || home.getWorld() == null) {
            return;
        }
        this.home = home.clone();
    }
    public void clearHome() {
        this.home = null;
    }
    public boolean hasHome() {
        return home != null
                && home.getWorld() != null;
    }
}