package com.hcfcore;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Faction {

    private final String name;
    private final UUID leader;

    private final Set<UUID> members = new HashSet<>();
    private final Set<UUID> officers = new HashSet<>();
    private final Set<String> allies = new HashSet<>();

    private double dtr;
    private double balance;

    public Faction(String name, UUID leader, double startingDtr) {
        this.name = name;
        this.leader = leader;
        this.dtr = startingDtr;
        this.balance = 0.0;

        members.add(leader);
    }

    public String getName() {
        return name;
    }

    public UUID getLeader() {
        return leader;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public Set<UUID> getOfficers() {
        return officers;
    }

    public Set<String> getAllies() {
        return allies;
    }

    public double getDtr() {
        return dtr;
    }

    public void setDtr(double dtr) {
        this.dtr = dtr;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public void addMember(UUID uuid) {
        members.add(uuid);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
        officers.remove(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    public boolean isOfficer(UUID uuid) {
        return officers.contains(uuid);
    }

    public void promote(UUID uuid) {
        if (members.contains(uuid)) {
            officers.add(uuid);
        }
    }

    public void demote(UUID uuid) {
        officers.remove(uuid);
    }

    public boolean isLeader(UUID uuid) {
        return leader.equals(uuid);
    }

    public void addAlly(String faction) {
        allies.add(faction);
    }

    public void removeAlly(String faction) {
        allies.remove(faction);
    }

    public boolean isAlly(String faction) {
        return allies.contains(faction);
    }
}