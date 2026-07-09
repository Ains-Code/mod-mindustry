package mindustrytool.dto;

import lombok.Data;

/**
 * Represents a user's role (id, color, icon, level).
 * Previously nested inside the (now removed) global chat feature's ChatUser
 * class; extracted here since UserData/auth also depend on it.
 */
@Data
public class SimpleRole {
    String id;
    String color;
    String icon;
    int level;
}
