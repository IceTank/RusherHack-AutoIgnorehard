package org.icetank;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;


import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.util.CommonColors;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.events.network.EventPacket;
import org.rusherhack.client.api.feature.module.ModuleCategory;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.client.api.setting.ColorSetting;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.logging.ILogger;
import org.rusherhack.core.setting.BooleanSetting;
import java.awt.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * Example rusherhack module
 *
 * @author John200410
 */
public class AutoIgnorehard extends ToggleableModule {

    /**
     * Settings
     */
    private final BooleanSetting logToChat = new BooleanSetting("Chat", "Logs ignored players and messages to chat", true);
    private final BooleanSetting logToFile = new BooleanSetting("File", "Logs ignored players and messages to file", true);

    private final ILogger logger = RusherHackAPI.createLogger("AutoIgnorehard");

    /**
     * Constructor
     */
    public AutoIgnorehard() {
        super("AutoIgnorehard", "Auto /ignorehard players that spam chat with discord invites", ModuleCategory.CLIENT);

        //register settings
        this.registerSettings(
                this.logToChat,
                this.logToFile
        );
    }

    final Pattern usernameAndContentPattern = Pattern.compile("(<\\w{1,16}>|\\w{1,16}) (.+)", Pattern.CASE_INSENSITIVE);
    final Pattern whisperPattern = Pattern.compile("(\\w{1,16}) whispers: (.+)", Pattern.CASE_INSENSITIVE);

    private boolean isWhisper(String content) {
        return whisperPattern.matcher(content).matches();
    }

    private boolean hasSpamTrigger(String content) {
        return content.toLowerCase().contains("gg/") || content.toLowerCase().contains(".com/invite/");
    }

    @Subscribe
    public void onPacketReceive(EventPacket.Receive event) {
        if (event.getPacket() instanceof ClientboundSystemChatPacket chatPacket) {
            String messageContent = chatPacket.content().getString();
            Optional<MatchResult> matchResult = usernameAndContentPattern.matcher(messageContent)
                    .results()
                    .findFirst();
            if (matchResult.isPresent()) {
                if (!handleChatMessage(chatPacket, matchResult.get().group(1), messageContent)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private boolean handleChatMessage(ClientboundSystemChatPacket chatPacket, String username, String message) {
        if (hasSpamTrigger(message)) {
            String command;
            if (username.startsWith("<") || isWhisper(message)) {
                command = "ignorehard " + username.replaceAll("[<>]", "");
            } else {
                command = "ignoredeathmsgs " + username;
            }
            Objects.requireNonNull(mc.getConnection()).sendCommand(command);

            if (logToFile.getValue()) {
                logMessageToFile(username.replaceAll("[<>]", ""), message);
            }
            if (logToChat.getValue()) {
                logMessageToChat(username.replaceAll("[<>]", ""), message, "/" + command);
            }
            return false; // Cancel the event to prevent the message from being displayed in chat
        }
        return true;
    }

    private void logMessageToFile(String username, String message) {
        Path path = Path.of(".").resolve("rusherhack/AutoIgnorehard/ignorelog.txt");
        String timestamp = LocalDateTime.now().toString();
        try {
            try {
                Files.createDirectory(path.getParent());
            } catch (FileAlreadyExistsException ignored) {
            }
            Files.writeString(path, String.format("%s %s: %s%n", timestamp, username, message), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            RusherHackAPI.getNotificationManager().warn("Failed to log message to file: " + e.getMessage());
            logger.error("Failed to log message to file", e);
        }
    }

    private void logMessageToChat(String username, String message, String revertCommand) {
        if (logToChat.getValue()) {
            Component revertAction = Component.literal("\n[Revert ignore]").withStyle(style ->
                    style.withColor(CommonColors.LIGHT_GRAY)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, revertCommand))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to revert ignore")))
            );
            RusherHackAPI.getNotificationManager().chat(Component.literal("Now ignoring '%s' for message: %s".formatted(username, message))
                    .withColor(CommonColors.GRAY).append(revertAction));
        }
    }
}
