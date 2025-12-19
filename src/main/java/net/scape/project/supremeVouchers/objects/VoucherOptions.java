package net.scape.project.supremeVouchers.objects;

import java.util.List;

public class VoucherOptions {

    // confirm use option
    private boolean confirm_use_enable;
    private String confirm_use_message;

    // allowed worlds
    private boolean allowed_worlds_enable;
    private List<String> allowed_worlds;
    private String allowed_worlds_message;

    private boolean combat_activation;
    private String combat_activation_message;

    public VoucherOptions(boolean confirmUseEnable, String confirmUseMessage, boolean allowedWorldsEnable, List<String> allowedWorlds, String allowedWorldsMessage, boolean combatActivation, String combatActivationMessage) {
        confirm_use_enable = confirmUseEnable;
        confirm_use_message = confirmUseMessage;
        allowed_worlds_enable = allowedWorldsEnable;
        allowed_worlds = allowedWorlds;
        allowed_worlds_message = allowedWorldsMessage;
        combat_activation = combatActivation;
        combat_activation_message = combatActivationMessage;
    }

    public boolean isConfirm_use_enable() {
        return confirm_use_enable;
    }

    public void setConfirm_use_enable(boolean confirm_use_enable) {
        this.confirm_use_enable = confirm_use_enable;
    }

    public String getConfirm_use_message() {
        return confirm_use_message;
    }

    public void setConfirm_use_message(String confirm_use_message) {
        this.confirm_use_message = confirm_use_message;
    }

    public boolean isAllowed_worlds_enable() {
        return allowed_worlds_enable;
    }

    public void setAllowed_worlds_enable(boolean allowed_worlds_enable) {
        this.allowed_worlds_enable = allowed_worlds_enable;
    }

    public List<String> getAllowed_worlds() {
        return allowed_worlds;
    }

    public void setAllowed_worlds(List<String> allowed_worlds) {
        this.allowed_worlds = allowed_worlds;
    }

    public String getAllowed_worlds_message() {
        return allowed_worlds_message;
    }

    public void setAllowed_worlds_message(String allowed_worlds_message) {
        this.allowed_worlds_message = allowed_worlds_message;
    }

    public boolean isCombatActivation() {
        return combat_activation;
    }

    public void setCombatActivation(boolean combat_activation) {
        this.combat_activation = combat_activation;
    }

    public String getCombatActivationMessage() {
        return combat_activation_message;
    }

    public void setCombatActivationMessage(String combat_activation_message) {
        this.combat_activation_message = combat_activation_message;
    }
}