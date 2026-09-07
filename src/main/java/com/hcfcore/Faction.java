package com.hcfcore;

import org.bukkit.Location;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Faction {

    private final String name;
    private final UUID leader;

    private final Set<UUID> members = new HashSet<>();
    private final Set<UUID> officers = new HashSet<>();
    private final Set<String> allies = new HashSet<>();
    private final Set<String> enemies = new HashSet<>();

    private double dtr;
    private double balance;

    // Faction home
    private Location home;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public Faction(String name, UUID leader, double startingDtr) {

        this.name = name;
        this.leader = leader;

        this.dtr = Math.max(0.0, startingDtr);
        this.balance = 0.0;

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
    // LÍDER
    // =========================================================

    public boolean isLeader(UUID uuid) {

        if (uuid == null) {
            return false;
        }

        return leader != null && leader.equals(uuid);
    }

    // =========================================================
    // MIEMBROS
    // =========================================================

    public Set<UUID> getMembers() {
        return members;
    }

    public Set<UUID> getOfficers() {
        return officers;
    }

    public boolean isMember(UUID uuid) {

        if (uuid == null) {
            return false;
        }

        return members.contains(uuid);
    }

    public void addMember(UUID uuid) {

        if (uuid == null) {
            return;
        }

        members.add(uuid);
    }

    public void removeMember(UUID uuid) {

        if (uuid == null) {
            return;
        }

        // El líder no puede ser eliminado
        if (isLeader(uuid)) {
            return;
        }

        members.remove(uuid);
        officers.remove(uuid);
    }

    // =========================================================
    // OFFICERS
    // =========================================================

    public boolean isOfficer(UUID uuid) {

        if (uuid == null) {
            return false;
        }

        return officers.contains(uuid);
    }

    public void promote(UUID uuid) {

        if (uuid == null) {
            return;
        }

        if (!members.contains(uuid)) {
            return;
        }

        // El líder no necesita ser officer
        if (isLeader(uuid)) {
            return;
        }

        officers.add(uuid);
    }

    public void demote(UUID uuid) {

        if (uuid == null) {
            return;
        }

        // El líder nunca puede ser demoteado
        if (isLeader(uuid)) {
            return;
        }

        officers.remove(uuid);
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

        this.dtr = Math.max(0.0, dtr);
    }

    /**
     * Comprueba si la faction está Raidable.
     *
     * Una faction con DTR 0 o inferior
     * se considera Raidable.
     */
    public boolean isRaidable() {
        return dtr <= 0.0;
    }

    /**
     * Indica si la faction tiene DTR positivo.
     */
    public boolean isDtrPositive() {
        return dtr > 0.0;
    }

    /**
     * Resta DTR de forma segura.
     */
    public void removeDtr(double amount) {

        if (Double.isNaN(amount)
                || Double.isInfinite(amount)) {
            return;
        }

        if (amount <= 0.0) {
            return;
        }

        dtr = Math.max(
                0.0,
                dtr - amount
        );
    }

    /**
     * Añade DTR de forma segura.
     */
    public void addDtr(double amount) {

        if (Double.isNaN(amount)
                || Double.isInfinite(amount)) {
            return;
        }

        if (amount <= 0.0) {
            return;
        }

        dtr += amount;
    }

    // =========================================================
    // BALANCE DE FACTION
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
                || Double.isInfinite(amount)) {
            return;
        }

        if (amount <= 0) {
            return;
        }

        balance += amount;
    }

    public boolean withdraw(double amount) {

        if (Double.isNaN(amount)
                || Double.isInfinite(amount)) {
            return false;
        }

        if (amount <= 0) {
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

        if (faction == null || faction.isBlank()) {
            return;
        }

        allies.add(
                faction.toLowerCase()
        );
    }

    public void removeAlly(String faction) {

        if (faction == null) {
            return;
        }

        allies.remove(
                faction.toLowerCase()
        );
    }

    public boolean isAlly(String faction) {

        if (faction == null) {
            return false;
        }

        return allies.contains(
                faction.toLowerCase()
        );
    }

    // =========================================================
    // ENEMIES
    // =========================================================

    public Set<String> getEnemies() {
        return enemies;
    }

    public void addEnemy(String faction) {

        if (faction == null || faction.isBlank()) {
            return;
        }

        enemies.add(
                faction.toLowerCase()
        );
    }

    public void removeEnemy(String faction) {

        if (faction == null) {
            return;
        }

        enemies.remove(
                faction.toLowerCase()
        );
    }

    public boolean isEnemy(String faction) {

        if (faction == null) {
            return false;
        }

        return enemies.contains(
                faction.toLowerCase()
        );
    }

    // =========================================================
    // FACTION HOME
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